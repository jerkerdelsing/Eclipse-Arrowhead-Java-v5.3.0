/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service;

import static eu.arrowhead.gatetunnel.service.Wire.bytes;
import static eu.arrowhead.gatetunnel.service.Wire.id;
import static eu.arrowhead.gatetunnel.service.Wire.m;
import static eu.arrowhead.gatetunnel.service.Wire.obj;
import static eu.arrowhead.gatetunnel.service.Wire.parse;
import static eu.arrowhead.gatetunnel.service.Wire.str;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;

import eu.arrowhead.gatetunnel.GateTunnelConstants;
import eu.arrowhead.gatetunnel.security.TlsSupport;
import jakarta.annotation.PreDestroy;

// The inter-cloud relay link: a dedicated Paho MQTT v3 client with the GateTunnel's own relay identity (RELAY_CHAIN_CERT,
// RELAY_KEY, RELAY_CA_CERT; mutual TLS with host-name verification). Ported from the 5.2.2 implementation; the topics are
// part of the inter-cloud contract with the Go and Rust GateTunnels:
//   publish   arrowhead/relay/<peer cloud>/gatetunnel/in/<own CN>/<message id>
//   subscribe arrowhead/relay/<own cloud>/gatetunnel/in/#
// Requests carry a correlationId; the reply ("type":"reply") is sent to the requester's inbox. A message id is executed at
// most once per peer within two minutes; a repeated delivery gets the same reply.
@Component
public class RelayClient {

	//=================================================================================================
	// members

	private static final int QOS = 1;
	private static final long REQUEST_TIMEOUT_SECONDS = 15;
	private static final long SEEN_MILLIS = 120_000;
	private static final int MAX_SEEN = 4096;
	private static final int TOPIC_PARTS = 7;

	private final Logger logger = LogManager.getLogger(getClass());

	private final FederationSettings settings;
	private final String role = GateTunnelConstants.RELAY_ROLE;
	private final String ownCn;
	private final MqttClient client;
	private final Map<String, Waiter> pending = new ConcurrentHashMap<>();
	private final Map<String, Reply> seen = new ConcurrentHashMap<>();
	private final ExecutorService dispatch = new ThreadPoolExecutor(4, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128));

	private volatile Predicate<String> trusted = peer -> false;
	private volatile Incoming handler = (peer, message) -> m("accepted", false, "error", "Relay operation refused");
	private volatile String lastError;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public RelayClient(final FederationSettings settings) throws MqttException {
		this.settings = settings;
		this.ownCn = settings.ownCommonName();
		this.client = new MqttClient("ssl://" + settings.getRelayHost() + ":" + settings.getRelayPort(), ownCn + "-java-" + id(), new MemoryPersistence());
		client.setTimeToWait(10000);
		client.setCallback(new MqttCallbackExtended() {
			@Override
			public void connectComplete(final boolean reconnect, final String uri) {
				try {
					client.subscribe(inbox(), QOS);
				} catch (final Exception ex) {
					lastError = "Relay subscribe failed";
					logger.error(lastError);
				}
			}

			@Override
			public void connectionLost(final Throwable cause) {
				logger.warn("Relay connection lost: {}", cause == null ? "" : cause.getMessage());
			}

			@Override
			public void deliveryComplete(final IMqttDeliveryToken token) {
				// QoS 1 acknowledged
			}

			@Override
			public void messageArrived(final String topic, final MqttMessage message) {
				receive(topic, message.getPayload(), message.isRetained());
			}
		});
	}

	//-------------------------------------------------------------------------------------------------
	// Set once by the inter-cloud session service: which peers are configured and what to do with their requests.
	public void bind(final Predicate<String> trustedPeers, final Incoming incoming) {
		this.trusted = trustedPeers;
		this.handler = incoming;
	}

	//-------------------------------------------------------------------------------------------------
	public String inbox() {
		return "arrowhead/relay/" + settings.getCloud() + "/" + role + "/in/#";
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isConnected() {
		return client.isConnected();
	}

	//-------------------------------------------------------------------------------------------------
	public synchronized void connect() throws Exception {
		if (client.isConnected()) {
			return;
		}

		final SSLContext tls = TlsSupport.pemContext(settings.getRelayChainCert(), settings.getRelayKey(), settings.getRelayCaCert(), null, null);
		final MqttConnectOptions options = new MqttConnectOptions();
		options.setSocketFactory(tls.getSocketFactory());
		options.setHttpsHostnameVerificationEnabled(true);
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		options.setConnectionTimeout(10);
		options.setKeepAliveInterval(30);
		options.setMaxInflight(64);
		client.connect(options);
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> status() {
		return m("connected", client.isConnected(),
				"broker", settings.getRelayHost() + ":" + settings.getRelayPort(),
				"inbox", inbox(),
				"profile", settings.getProfile(),
				"endToEndEncrypted", settings.isEndToEnd());
	}

	//-------------------------------------------------------------------------------------------------
	// Sends a request to the GateTunnel of a neighbour cloud and waits (15 s) for its reply.
	public Map<String, Object> request(final String cloud, final Map<String, Object> message) throws Exception {
		final String peer = GateTunnelConstants.SYSTEM_NAME + "." + cloud + "." + settings.organizationOf(cloud) + ".arrowhead.eu";
		if (!trusted.test(peer)) {
			throw new GateTunnelApiException(403, "Unconfigured relay neighbour");
		}
		if (pending.size() >= GateTunnelConstants.MAX_PENDING_RELAY_REQUESTS) {
			throw new GateTunnelApiException(503, "Too many relay requests");
		}

		final String correlation = id();
		final CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
		pending.put(correlation, new Waiter(peer, future));
		final Map<String, Object> copy = new LinkedHashMap<>(message);
		copy.put("correlationId", correlation);
		try {
			send(cloud, copy);
			return future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} finally {
			pending.remove(correlation);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public String getLastError() {
		return lastError;
	}

	//-------------------------------------------------------------------------------------------------
	@PreDestroy
	public void close() {
		dispatch.shutdownNow();
		try {
			if (client.isConnected()) {
				client.disconnect(1000);
			}
			client.close();
		} catch (final Exception ignored) {
			// shutting down
		}
		pending.values().forEach(p -> p.future().completeExceptionally(new IllegalStateException("Relay stopped")));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	void send(final String cloud, final Map<String, Object> message) throws Exception {
		if (!cloud.matches("[A-Za-z0-9_-]+")) {
			throw new IllegalArgumentException("Invalid relay target cloud");
		}
		if (!client.isConnected()) {
			throw new GateTunnelApiException(502, "Relay not connected");
		}

		message.putIfAbsent("id", id());
		final byte[] payload = bytes(message);
		if (payload.length > GateTunnelConstants.MAX_RELAY_PAYLOAD_BYTES) {
			throw new IllegalArgumentException("Relay payload exceeds 2 MiB");
		}
		client.publish("arrowhead/relay/" + cloud + "/" + role + "/in/" + ownCn + "/" + message.get("id"), payload, QOS, false);
	}

	//-------------------------------------------------------------------------------------------------
	void receive(final String topic, final byte[] data, final boolean retained) {
		try {
			if (retained || data.length > GateTunnelConstants.MAX_RELAY_PAYLOAD_BYTES) {
				return;
			}

			final String[] parts = topic.split("/", -1);
			if (parts.length != TOPIC_PARTS
					|| !"arrowhead".equals(parts[0])
					|| !"relay".equals(parts[1])
					|| !parts[2].equals(settings.getCloud())
					|| !parts[3].equals(role)
					|| !"in".equals(parts[4])
					|| !trusted.test(parts[5])) {
				return;
			}

			final Map<String, Object> message = obj(parse(data));
			if (!parts[6].equals(str(message.get("id")))) {
				return;
			}

			final String peer = parts[5];
			if ("reply".equals(message.get("type"))) {
				final Waiter waiter = pending.get(str(message.get("correlationId")));
				if (waiter != null && waiter.peer().equals(peer)) {
					message.put("senderCN", peer);
					waiter.future().complete(message);
				}
				return;
			}

			seen.entrySet().removeIf(e -> e.getValue().expires() < System.currentTimeMillis());
			final String key = peer + "/" + parts[6];
			if (seen.size() >= MAX_SEEN) {
				return;
			}

			final Reply candidate = new Reply(System.currentTimeMillis() + SEEN_MILLIS, new CompletableFuture<>());
			final Reply old = seen.putIfAbsent(key, candidate);
			final Reply reply = old == null ? candidate : old;
			dispatch.submit(() -> {
				try {
					if (old == null) {
						Map<String, Object> answer;
						try {
							answer = handler.handle(peer, message);
						} catch (final Exception ex) {
							answer = m("accepted", false, "reason", "Relay operation refused", "error", "Relay operation refused");
							lastError = ex.getClass().getSimpleName();
						}
						answer = new LinkedHashMap<>(answer);
						answer.put("id", id());
						answer.put("type", "reply");
						answer.put("correlationId", message.get("correlationId"));
						reply.future().complete(answer);
					}
					send(peer.split("\\.")[1], reply.future().get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS));
				} catch (final Exception ex) {
					lastError = "Relay response failed";
				}
			});
		} catch (final Exception ignored) {
			// malformed relay message: dropped
		}
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	@FunctionalInterface
	public interface Incoming {
		Map<String, Object> handle(String peer, Map<String, Object> message) throws Exception;
	}

	//-------------------------------------------------------------------------------------------------
	private record Waiter(String peer, CompletableFuture<Map<String, Object>> future) {
	}

	//-------------------------------------------------------------------------------------------------
	private record Reply(long expires, CompletableFuture<Map<String, Object>> future) {
	}
}
