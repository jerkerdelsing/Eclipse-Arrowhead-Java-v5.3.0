/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.it;

import static eu.arrowhead.sysmonitor.service.Json.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.testhelper.StandaloneModeInTests;
import eu.arrowhead.sysmonitor.SysMonitorMain;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;
import eu.arrowhead.sysmonitor.service.AmqpIntake;
import eu.arrowhead.sysmonitor.service.Json;
import eu.arrowhead.sysmonitor.service.StatusService;

// AMQP -> store -> MQTT round trip of the whole application against local brokers with mutual TLS (RabbitMQ with SASL
// EXTERNAL, Mosquitto with use_identity_as_username) and a fresh MySQL schema. Only the orchestration is stubbed: it offers
// the Pump provider and SysMonitor itself (whose own common-library sysMonitor publisher then feeds the intake too).
// Run with -Dsysmonitor.it.broker.dir=<pki folder> -Dsysmonitor.it.amqp.host=... -Dsysmonitor.it.mqtt.host=...
// -Dsysmonitor.it.db.url=... -Dsysmonitor.it.db.password=...; skipped otherwise.
@EnabledIfSystemProperty(named = "sysmonitor.it.broker.dir", matches = ".+")
@SpringBootTest(classes = { SysMonitorMain.class, BrokerRoundTripTest.Standalone.class }, properties = "spring.config.location=classpath:/application.properties")
@AutoConfigureMockMvc
public class BrokerRoundTripTest {

	//=================================================================================================
	// members

	private static final String STORE_PASSWORD = "rt-store";
	private static final String OUTPUT = "arrowhead/TestCloud/sysmonitor/data";

	@MockitoBean
	private ArrowheadHttpService httpService;

	@Autowired
	private StatusService status;

	@Autowired
	private AmqpIntake intake;

	@Autowired
	private MonitorDbService store;

	@Autowired
	private MockMvc mvc;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@DynamicPropertySource
	static void properties(final DynamicPropertyRegistry registry) {
		final String dir = System.getProperty("sysmonitor.it.broker.dir");
		registry.add("arrowhead.security.profile", () -> "development");
		registry.add("spring.datasource.url", () -> System.getProperty("sysmonitor.it.db.url"));
		registry.add("spring.datasource.password", () -> System.getProperty("sysmonitor.it.db.password"));
		registry.add("sysmonitor.enabled", () -> "true");
		registry.add("sysmonitor.publish.interval", () -> "2");
		registry.add("sysmonitor.ssl.enabled", () -> "true");
		registry.add("sysmonitor.broker.address", () -> System.getProperty("sysmonitor.it.amqp.host"));
		registry.add("sysmonitor.broker.port", () -> "5671");
		registry.add("sysmonitor.local.cloud", () -> "TestCloud");
		registry.add("sysmonitor.ssl.key-store", () -> "file:" + dir + "/SysMonitor.p12");
		registry.add("sysmonitor.ssl.key-store-password", () -> STORE_PASSWORD);
		registry.add("sysmonitor.ssl.trust-store", () -> "file:" + dir + "/truststore.p12");
		registry.add("sysmonitor.ssl.trust-store-password", () -> STORE_PASSWORD);
		registry.add("sysmonitor.mqtt.address", () -> System.getProperty("sysmonitor.it.mqtt.host"));
		registry.add("sysmonitor.mqtt.port", () -> "8883");
		registry.add("arrowhead.ssl.crl-file", () -> dir + "/ca.crl");
		registry.add("sysmonitor.orchestration.interval.seconds", () -> "2");
		registry.add("sysmonitor.connect.interval.seconds", () -> "1");
		registry.add("sysmonitor.outbox.interval.millis", () -> "200");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void amqpToStoreToMqtt() throws Exception {
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION), eq("pull"), eq(Map.class), any(Object.class))).thenReturn(map("results", List.of(
				offer("Pump", "TestCloud.sysmonitor.Pump.pump", List.of("echo", "status")),
				offer("SysMonitor", "TestCloud.sysmonitor.SysMonitor.SysMonitor", List.of("echo", "power", "status")))));

		await("both broker connections", Duration.ofSeconds(60), status::isRunning);
		await("bindings", Duration.ofSeconds(30), () -> intake.applied().contains("TestCloud.sysmonitor.Pump.pump.*"));
		assertEquals(Set("TestCloud.sysmonitor.Pump.pump.*", "TestCloud.sysmonitor.SysMonitor.SysMonitor.*"), intake.applied());

		final Path dir = Path.of(System.getProperty("sysmonitor.it.broker.dir"));
		final BlockingQueue<String[]> received = new LinkedBlockingQueue<>();
		final MqttClient watcher = new MqttClient("ssl://" + System.getProperty("sysmonitor.it.mqtt.host") + ":8883", "Watcher", new MemoryPersistence());
		final MqttConnectionOptions options = new MqttConnectionOptions();
		options.setSocketFactory(context(dir.resolve("Watcher.p12"), dir.resolve("truststore.p12")).getSocketFactory());
		options.setHttpsHostnameVerificationEnabled(true);
		watcher.connect(options);
		// the (String, int, listener) overload of Paho 1.2.5 recurses endlessly
		watcher.subscribe(new MqttSubscription[] { new MqttSubscription(OUTPUT + "/#", 1) },
				new IMqttMessageListener[] { (topic, message) -> received.add(new String[] { topic, new String(message.getPayload(), StandardCharsets.UTF_8) }) });

		final ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(System.getProperty("sysmonitor.it.amqp.host"));
		factory.setPort(5671);
		factory.useSslProtocol(context(dir.resolve("Pump.p12"), dir.resolve("truststore.p12")));
		factory.enableHostnameVerification();
		factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
		try (Connection connection = factory.newConnection("Pump"); Channel channel = connection.createChannel()) {
			channel.confirmSelect();
			final String sourceTime = Instant.now().toString();
			final String valid = Json.json(map("schemaVersion", "1.0", "observationId", "rt-1", "providerSystem", "Pump", "serviceInstance", "pump", "observationType", "echo",
					"sourceTime", sourceTime, "value", map("status", "alive"), "unit", null, "quality", "good", "metadata", map("implementation", "it")));
			publish(channel, "TestCloud.sysmonitor.Pump.pump.echo", valid);
			publish(channel, "TestCloud.sysmonitor.Pump.pump.echo", valid); // duplicate
			publish(channel, "TestCloud.sysmonitor.Pump.pump.echo", Json.json(map("providerSystem", "Valve", "sourceTime", sourceTime, "value", map("status", "alive")))); // identity mismatch

			// the Pump observation on sysMonitorData
			final Map<String, Object> observation = next(received, OUTPUT + "/echo/Pump", Duration.ofSeconds(20));
			assertEquals(List.of("schemaVersion", "observationId", "providerSystem", "serviceInstance", "observationType", "sourceTime", "receivedTime", "publishedTime",
					"value", "unit", "quality", "pipeline", "metadata"), List.copyOf(observation.keySet()));
			assertEquals("rt-1", observation.get("observationId"));
			assertEquals(sourceTime, observation.get("sourceTime"));
			assertNotNull(observation.get("receivedTime"));
			assertNotNull(observation.get("publishedTime"));
			assertEquals(map("sysMonitor", "SysMonitor", "revision", 1), observation.get("pipeline"));
			assertEquals(map("implementation", "it"), observation.get("metadata"));

			// stored once, outbox drained, duplicate and forged observation not published
			assertEquals(1, store.history("Pump", List.of(), null, null, 100).size());
			assertNull(next(received, OUTPUT + "/echo/Pump", Duration.ofSeconds(3)), "the duplicate is not published again");
			assertTrue(store.history("Valve", List.of(), null, null, 100).isEmpty());

			// SysMonitor's own common-library observations pass the intake as well
			final Map<String, Object> own = next(received, OUTPUT + "/status/SysMonitor", Duration.ofSeconds(20));
			assertEquals("SysMonitor", own.get("serviceInstance"));

			// a disabled subscription unbinds the provider: further observations are not routed to SysMonitor
			mvc.perform(put("/sysmonitor/management/subscriptions/Pump").header("Authorization", "Bearer SYSTEM//LocalCloudManager")
					.contentType(MediaType.APPLICATION_JSON).content("{\"observationTypes\":[\"*\"],\"enabled\":false}"))
					.andExpect(status().isOk());
			assertFalse(intake.applied().contains("TestCloud.sysmonitor.Pump.pump.*"));
			publish(channel, "TestCloud.sysmonitor.Pump.pump.echo", Json.json(map("observationId", "rt-2", "sourceTime", Instant.now().toString(), "value", map("status", "alive"))));
			assertNull(next(received, OUTPUT + "/echo/Pump", Duration.ofSeconds(3)));
			assertEquals(1, store.history("Pump", List.of(), null, null, 100).size());
		} finally {
			watcher.disconnect();
			watcher.close();
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static java.util.Set<String> Set(final String... keys) {
		return new java.util.TreeSet<>(List.of(keys));
	}

	//-------------------------------------------------------------------------------------------------
	private static void publish(final Channel channel, final String routingKey, final String body) throws Exception {
		channel.basicPublish("arrowhead", routingKey, new AMQP.BasicProperties.Builder().contentType("application/json").deliveryMode(2).build(), body.getBytes(StandardCharsets.UTF_8));
		channel.waitForConfirmsOrDie(5000);
	}

	//-------------------------------------------------------------------------------------------------
	// next message on the topic (others are skipped), or null
	private static Map<String, Object> next(final BlockingQueue<String[]> received, final String topic, final Duration timeout) throws InterruptedException {
		final long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			final String[] message = received.poll(Math.max(1, (deadline - System.nanoTime()) / 1000000), TimeUnit.MILLISECONDS);
			if (message != null && message[0].equals(topic)) {
				return Json.obj(Json.parse(message[1]));
			}
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private static void await(final String what, final Duration timeout, final BooleanSupplier condition) throws InterruptedException {
		final long deadline = System.nanoTime() + timeout.toNanos();
		while (!condition.getAsBoolean()) {
			if (System.nanoTime() > deadline) {
				throw new AssertionError("Timed out waiting for " + what);
			}

			Thread.sleep(200);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static SSLContext context(final Path keyStore, final Path trustStore) throws Exception {
		final KeyStore keys = KeyStore.getInstance("PKCS12");
		try (InputStream in = Files.newInputStream(keyStore)) {
			keys.load(in, STORE_PASSWORD.toCharArray());
		}

		final KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagers.init(keys, STORE_PASSWORD.toCharArray());
		final KeyStore trust = KeyStore.getInstance("PKCS12");
		try (InputStream in = Files.newInputStream(trustStore)) {
			trust.load(in, STORE_PASSWORD.toCharArray());
		}

		final TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagers.init(trust);
		final SSLContext context = SSLContext.getInstance("TLS");
		context.init(keyManagers.getKeyManagers(), trustManagers.getTrustManagers(), null);
		return context;
	}

	//-------------------------------------------------------------------------------------------------
	private static Map<String, Object> offer(final String provider, final String baseRoutingKey, final List<String> types) {
		return map("providerName", provider, "serviceInstanceId", provider + "|sysMonitor|1.0.0", "metadata", map("observationTypes", types),
				"interfaces", List.of(map("templateName", "generic_amqps", "protocol", "amqps", "policy", "CERT_AUTH",
						"properties", map("accessAddresses", List.of("amqp"), "accessPort", 5671, "exchange", "arrowhead", "baseRoutingKey", baseRoutingKey, "operations", types))));
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	static class Standalone {

		//-------------------------------------------------------------------------------------------------
		@Bean
		StandaloneModeInTests standaloneMode() {
			return new StandaloneModeInTests() {
			};
		}
	}
}
