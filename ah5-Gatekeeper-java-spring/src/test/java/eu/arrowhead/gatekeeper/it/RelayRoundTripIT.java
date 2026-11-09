/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.it;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.obj;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import eu.arrowhead.gatekeeper.relay.RelayClient;
import eu.arrowhead.gatekeeper.relay.RelayHandler;
import eu.arrowhead.gatekeeper.relay.RelayTls;
import eu.arrowhead.gatekeeper.service.ApiError;
import eu.arrowhead.gatekeeper.service.FederationSettings;

// Two Gatekeeper relay clients (TestCloud and OtherCloud) and an observer against a real TLS MQTT broker (Mosquitto with
// require_certificate). Checks the request/reply round trip and the exact relay topics and fields. Run with
// -Dgk.it.relay.host=... -Dgk.it.relay.port=... -Dgk.it.relay.dir=<ca.crt, a.crt/a.key, b.crt/b.key>; skipped otherwise.
@EnabledIfSystemProperty(named = "gk.it.relay.host", matches = ".+")
public class RelayRoundTripIT {

	//=================================================================================================
	// members

	private static final String A_CN = "Gatekeeper.TestCloud.Company.arrowhead.eu";
	private static final String B_CN = "Gatekeeper.OtherCloud.Company.arrowhead.eu";

	private final String host = System.getProperty("gk.it.relay.host");
	private final int port = Integer.parseInt(System.getProperty("gk.it.relay.port", "8883"));
	private final Path dir = Path.of(System.getProperty("gk.it.relay.dir", "."));

	private RelayClient a;
	private RelayClient b;
	private MqttClient observer;
	private final List<String[]> observed = new CopyOnWriteArrayList<>();
	private final List<Map<String, Object>> handled = new CopyOnWriteArrayList<>();
	private final AtomicInteger handledCount = new AtomicInteger();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void connect() throws Exception {
		a = client("TestCloud", "a");
		b = client("OtherCloud", "b");
		a.setHandler(handler(B_CN, null));
		b.setHandler(handler(A_CN, m("cloud", "OtherCloud|Company", "results", List.of(m("providerName", "P")))));
		a.connect();
		b.connect();

		observer = new MqttClient("ssl://" + host + ":" + port, "observer-" + System.nanoTime(), new MemoryPersistence());
		final MqttConnectOptions options = new MqttConnectOptions();
		options.setSocketFactory(RelayTls.context(dir.resolve("a.crt").toString(), dir.resolve("a.key").toString(), dir.resolve("ca.crt").toString(), "").getSocketFactory());
		options.setCleanSession(true);
		observer.connect(options);
		observer.subscribe("arrowhead/relay/#", 1, (topic, message) -> observed.add(new String[] { topic, new String(message.getPayload(), StandardCharsets.UTF_8) }));
		Thread.sleep(1000); // the relay clients subscribe their inboxes on connectComplete
	}

	//-------------------------------------------------------------------------------------------------
	@AfterEach
	public void close() throws Exception {
		a.close();
		b.close();
		observer.disconnect();
		observer.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void requestAndReplyUseTheNativeTopicsAndFields() throws Exception {
		assertTrue(a.isConnected() && b.isConnected());
		final Map<String, Object> reply = a.request("OtherCloud", m("type", "gsd-request", "consumer", "C", "serviceRequirement", m("serviceDefinition", "temperature")));

		assertEquals("reply", reply.get("type"));
		assertEquals("OtherCloud|Company", reply.get("cloud"));
		assertEquals(B_CN, reply.get("senderCN"));
		assertEquals(1, handled.size());
		assertEquals("C", handled.getFirst().get("consumer"));
		assertEquals("gsd-request", handled.getFirst().get("type"));

		Thread.sleep(300);
		final String[] request = observed.stream().filter(o -> o[0].startsWith("arrowhead/relay/OtherCloud/")).findFirst().orElseThrow();
		final Map<String, Object> requestBody = obj(parse(request[1]));
		assertEquals("arrowhead/relay/OtherCloud/gatekeeper/in/" + A_CN + "/" + requestBody.get("id"), request[0]);
		assertEquals(reply.get("correlationId"), requestBody.get("correlationId"));

		final String[] answer = observed.stream().filter(o -> o[0].startsWith("arrowhead/relay/TestCloud/")).findFirst().orElseThrow();
		final Map<String, Object> answerBody = obj(parse(answer[1]));
		assertEquals("arrowhead/relay/TestCloud/gatekeeper/in/" + B_CN + "/" + answerBody.get("id"), answer[0]);
		assertEquals("reply", answerBody.get("type"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void aDuplicateRequestIsAnsweredOnceAndRepliedTwice() throws Exception {
		final String id = "dup-" + System.nanoTime();
		final byte[] payload = ("{\"type\":\"gsd-request\",\"consumer\":\"C\",\"serviceRequirement\":{\"serviceDefinition\":\"t\"},\"correlationId\":\"x\",\"id\":\"" + id + "\"}")
				.getBytes(StandardCharsets.UTF_8);
		final String topic = "arrowhead/relay/OtherCloud/gatekeeper/in/" + A_CN + "/" + id;
		observer.publish(topic, new MqttMessage(payload));
		observer.publish(topic, new MqttMessage(payload));
		// a retained message, a mismatched id and an unconfigured sender are ignored
		final MqttMessage retained = new MqttMessage(payload);
		retained.setRetained(true);
		observer.publish("arrowhead/relay/OtherCloud/gatekeeper/in/" + A_CN + "/retained-" + id, retained);
		observer.publish("arrowhead/relay/OtherCloud/gatekeeper/in/" + A_CN + "/other-id", new MqttMessage(payload));
		observer.publish("arrowhead/relay/OtherCloud/gatekeeper/in/Gatekeeper.Evil.Company.arrowhead.eu/" + id, new MqttMessage(payload));
		Thread.sleep(1500);

		assertEquals(1, handledCount.get());
		final long replies = observed.stream().filter(o -> o[0].startsWith("arrowhead/relay/TestCloud/gatekeeper/in/" + B_CN + "/")).count();
		assertEquals(2, replies);
		// clean up the retained message
		final MqttMessage clear = new MqttMessage(new byte[0]);
		clear.setRetained(true);
		observer.publish("arrowhead/relay/OtherCloud/gatekeeper/in/" + A_CN + "/retained-" + id, clear);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void anUnconfiguredNeighbourIsRefusedBeforeAnythingIsSent() {
		final ApiError error = assertThrows(ApiError.class, () -> a.request("ThirdCloud", m("type", "gsd-request")));
		assertEquals(403, error.getStatus());
		assertTrue(observed.stream().noneMatch(o -> o[0].contains("ThirdCloud")));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private RelayClient client(final String cloud, final String identity) throws Exception {
		return new RelayClient(new FederationSettings(cloud, "Company", "legacy-mtls", ""), true, host, port,
				dir.resolve(identity + ".crt").toString(), dir.resolve(identity + ".key").toString(), dir.resolve("ca.crt").toString(), "");
	}

	//-------------------------------------------------------------------------------------------------
	private RelayHandler handler(final String trusted, final Map<String, Object> answer) {
		return new RelayHandler() {
			@Override
			public boolean trustedPeer(final String peer) {
				return trusted.equals(peer);
			}

			@Override
			public Map<String, Object> incoming(final String peer, final Map<String, Object> message) {
				handled.add(message);
				handledCount.incrementAndGet();
				return answer;
			}
		};
	}
}
