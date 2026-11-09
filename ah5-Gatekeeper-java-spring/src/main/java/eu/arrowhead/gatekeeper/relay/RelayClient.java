/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.relay;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.bytes;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.obj;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.parse;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.str;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.GatekeeperDefaults;
import eu.arrowhead.gatekeeper.service.ApiError;
import eu.arrowhead.gatekeeper.service.FederationSettings;
import jakarta.annotation.PreDestroy;

// The inter-cloud relay link: an MQTT v3 client (Paho) with the organisation-signed relay identity, not the system
// certificate. Topics, message fields, correlation and duplicate handling are those of the native 5.2.2 Gatekeeper and
// stay byte-for-byte compatible with the Go and Rust implementations:
//   arrowhead/relay/<target cloud>/gatekeeper/in/<sender common name>/<message id>
@Component
public class RelayClient {

	//=================================================================================================
	// members

	private static final int MAX_PENDING = 128;
	private static final int MAX_SEEN = 4096;
	private static final long SEEN_MILLIS = 120_000;
	private static final long REPLY_SECONDS = 15;

	private final Logger logger = LogManager.getLogger(this.getClass());

	private final FederationSettings settings;
	private final boolean enabled;
	private final String host;
	private final int port;
	private final String chainCert;
	private final String key;
	private final String caCert;
	private final String crlFile;
	private final String role = GatekeeperConstants.RELAY_ROLE;
	private final String ownCN;
	private final MqttClient client;

	private final Map<String, Waiter> pending = new ConcurrentHashMap<>();
	private final Map<String, Reply> seen = new ConcurrentHashMap<>();
	private final ExecutorService dispatch = new ThreadPoolExecutor(4, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128));

	private volatile RelayHandler handler;
	private volatile String lastError;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public RelayClient(
			final FederationSettings settings,
			@Value("${" + GatekeeperConstants.RELAY_ENABLED + ":" + GatekeeperDefaults.RELAY_ENABLED_DEFAULT + "}") final boolean enabled,
			@Value("${" + GatekeeperConstants.RELAY_HOST + ":" + GatekeeperDefaults.RELAY_HOST_DEFAULT + "}") final String host,
			@Value("${" + GatekeeperConstants.RELAY_PORT + ":" + GatekeeperDefaults.RELAY_PORT_DEFAULT + "}") final int port,
			@Value("${" + GatekeeperConstants.RELAY_CHAIN_CERT + ":" + GatekeeperDefaults.RELAY_CHAIN_CERT_DEFAULT + "}") final String chainCert,
			@Value("${" + GatekeeperConstants.RELAY_KEY + ":" + GatekeeperDefaults.RELAY_KEY_DEFAULT + "}") final String key,
			@Value("${" + GatekeeperConstants.RELAY_CA_CERT + ":" + GatekeeperDefaults.RELAY_CA_CERT_DEFAULT + "}") final String caCert,
			@Value("${" + GatekeeperConstants.RELAY_CRL_FILE + ":" + GatekeeperDefaults.RELAY_CRL_FILE_DEFAULT + "}") final String crlFile) throws MqttException {
		this.settings = settings;
		this.enabled = enabled;
		this.host = host;
		this.port = port;
		this.chainCert = chainCert;
		this.key = key;
		this.caCert = caCert;
		this.crlFile = crlFile;
		this.ownCN = settings.commonName(GatekeeperConstants.SYSTEM_NAME);
		this.client = new MqttClient("ssl://" + host + ":" + port, ownCN + "-java-" + UUID.randomUUID(), new MemoryPersistence());
		client.setTimeToWait(10_000);
		client.setCallback(new MqttCallbackExtended() {
			@Override
			public void connectComplete(final boolean reconnect, final String serverURI) {
				try {
					client.subscribe(inbox(), 1);
				} catch (final Exception ex) {
					lastError = "Relay subscribe failed";
				}
			}

			@Override
			public void connectionLost(final Throwable cause) {
				// automatic reconnect
			}

			@Override
			public void deliveryComplete(final IMqttDeliveryToken token) {
				// QoS 1, nothing to track
			}

			@Override
			public void messageArrived(final String topic, final MqttMessage message) {
				receive(topic, message.getPayload(), message.isRetained());
			}
		});
	}

	//-------------------------------------------------------------------------------------------------
	public void setHandler(final RelayHandler handler) {
		this.handler = handler;
	}

	//-------------------------------------------------------------------------------------------------
	public String inbox() {
		return "arrowhead/relay/" + settings.getCloud() + "/" + role + "/in/#";
	}

	//-------------------------------------------------------------------------------------------------
	// every 10 seconds, as the native relay worker
	@Scheduled(initialDelay = 0, fixedDelay = 10_000)
	public void reconnect() {
		if (!enabled) {
			return;
		}

		try {
			connect();
		} catch (final Exception ex) {
			lastError = ex.getClass().getSimpleName();
			logger.warn("Relay connection to {}:{} failed: {}", host, port, ex.getMessage());
			logger.debug(ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public synchronized void connect() throws Exception {
		if (client.isConnected()) {
			return;
		}

		final SSLContext tls = RelayTls.context(chainCert, key, caCert, crlFile);
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
	public boolean isConnected() {
		return client.isConnected();
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> status() {
		return m("connected", client.isConnected(),
				"broker", host + ":" + port,
				"inbox", inbox(),
				"profile", settings.getProfile(),
				"endToEndEncrypted", settings.endToEndEncrypted());
	}

	//-------------------------------------------------------------------------------------------------
	public String getLastError() {
		return lastError;
	}

	//-------------------------------------------------------------------------------------------------
	public void send(final String cloud, final Map<String, Object> message) throws MqttException {
		if (!cloud.matches("[A-Za-z0-9_-]+")) {
			throw new IllegalArgumentException("Invalid relay target cloud");
		}

		if (!client.isConnected()) {
			throw new ApiError(502, "Relay not connected");
		}

		message.putIfAbsent("id", UUID.randomUUID().toString());
		final byte[] payload = bytes(message);
		if (payload.length > GatekeeperConstants.MAX_RELAY_PAYLOAD_BYTES) {
			throw new IllegalArgumentException("Relay payload exceeds 2 MiB");
		}

		client.publish("arrowhead/relay/" + cloud + "/" + role + "/in/" + ownCN + "/" + message.get("id"), payload, 1, false);
	}

	//-------------------------------------------------------------------------------------------------
	// one request to the Gatekeeper of a configured neighbour cloud; its reply, or an exception after 15 seconds
	public Map<String, Object> request(final String cloud, final Map<String, Object> message) throws Exception {
		final String peer = "Gatekeeper." + cloud + "." + settings.organizationOf(cloud) + ".arrowhead.eu";
		if (handler == null || !handler.trustedPeer(peer)) {
			throw new ApiError(403, "Unconfigured relay neighbour");
		}

		if (pending.size() >= MAX_PENDING) {
			throw new ApiError(503, "Too many relay requests");
		}

		final String correlation = UUID.randomUUID().toString();
		final CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
		pending.put(correlation, new Waiter(peer, future));
		final Map<String, Object> copy = new LinkedHashMap<>(message);
		copy.put("correlationId", correlation);
		try {
			send(cloud, copy);
			return future.get(REPLY_SECONDS, TimeUnit.SECONDS);
		} finally {
			pending.remove(correlation);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// inbound relay message: a reply to a pending request, or a request of a configured neighbour (answered once per
	// message id within two minutes, duplicates get the same answer)
	void receive(final String topic, final byte[] data, final boolean retained) {
		try {
			if (retained || data.length > GatekeeperConstants.MAX_RELAY_PAYLOAD_BYTES) {
				return;
			}

			final String[] parts = topic.split("/", -1);
			final RelayHandler current = handler;
			if (current == null
					|| parts.length != 7
					|| !parts[0].equals("arrowhead")
					|| !parts[1].equals("relay")
					|| !parts[2].equals(settings.getCloud())
					|| !parts[3].equals(role)
					|| !parts[4].equals("in")
					|| !current.trustedPeer(parts[5])) {
				return;
			}

			final Map<String, Object> message = obj(parse(data));
			if (!parts[6].equals(str(message.get("id")))) {
				return;
			}

			final String peer = parts[5];
			final Object type = message.get("type");
			if (type == null) {
				return;
			}

			if (type.equals("reply")) {
				final Waiter waiter = pending.get(str(message.get("correlationId")));
				if (waiter != null && waiter.peer().equals(peer)) {
					message.put("senderCN", peer);
					waiter.future().complete(message);
				}

				return;
			}

			seen.entrySet().removeIf(e -> e.getValue().expires() < System.currentTimeMillis());
			final String seenKey = peer + "/" + parts[6];
			if (seen.size() >= MAX_SEEN) {
				return;
			}

			final Reply candidate = new Reply(System.currentTimeMillis() + SEEN_MILLIS, new CompletableFuture<>());
			final Reply old = seen.putIfAbsent(seenKey, candidate);
			final Reply reply = old == null ? candidate : old;
			dispatch.submit(() -> answer(current, peer, message, old == null, reply));
		} catch (final Exception ex) {
			logger.debug("Relay message ignored: {}", ex.getMessage());
		}
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
		} catch (final Exception ex) {
			logger.debug(ex);
		}

		pending.values().forEach(p -> p.future().completeExceptionally(new IllegalStateException("Relay stopped")));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void answer(final RelayHandler current, final String peer, final Map<String, Object> message, final boolean first, final Reply reply) {
		try {
			if (first) {
				Map<String, Object> answer;
				try {
					answer = current.incoming(peer, message);
				} catch (final Exception ex) {
					answer = m("accepted", false, "reason", "Relay operation refused", "error", "Relay operation refused");
					lastError = ex.getClass().getSimpleName();
					logger.info("Relay request of {} refused: {}", peer, ex.getMessage());
				}

				answer = new LinkedHashMap<>(answer);
				answer.put("id", UUID.randomUUID().toString());
				answer.put("type", "reply");
				answer.put("correlationId", message.get("correlationId"));
				reply.future().complete(answer);
			}

			send(peer.split("\\.")[1], reply.future().get(REPLY_SECONDS, TimeUnit.SECONDS));
		} catch (final Exception ex) {
			lastError = "Relay response failed";
			logger.debug(ex);
		}
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	private record Waiter(String peer, CompletableFuture<Map<String, Object>> future) {
	}

	//-------------------------------------------------------------------------------------------------
	private record Reply(long expires, CompletableFuture<Map<String, Object>> future) {
	}
}
