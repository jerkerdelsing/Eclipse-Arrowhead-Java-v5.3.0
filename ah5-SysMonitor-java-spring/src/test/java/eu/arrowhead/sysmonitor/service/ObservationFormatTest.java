/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import static eu.arrowhead.sysmonitor.service.Json.map;
import static eu.arrowhead.sysmonitor.service.Json.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

// Ported from the native ContractTest: the observation identity is bound to the broker routing key, and the normalised envelope
// keeps the SysMonitor-AMQP-IDD / sysMonitorData field set and order.
public class ObservationFormatTest {

	//=================================================================================================
	// members

	private static final String ROUTING = "TestCloud.sysmonitor.Provider.instance.echo";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void observationIdentityBoundToBrokerRoutingKey() {
		final Map<String, Object> original = map("schemaVersion", "1.0", "providerSystem", "Provider", "serviceInstance", "instance", "observationType", "echo",
				"sourceTime", "2026-10-15T12:00:00Z", "value", map("status", "alive"));
		final Map<String, Object> normalized = ObservationFormat.normalize(original, ROUTING, "TestCloud");
		assertEquals("Provider", normalized.get("providerSystem"));
		assertNotNull(normalized.get("observationId"));
		assertEquals(normalized.get("observationId"), ObservationFormat.normalize(original, ROUTING, "TestCloud").get("observationId"));

		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(original, "TestCloud.sysmonitor.Other.instance.echo", "TestCloud"));
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(original, "GoCloud.sysmonitor.Provider.instance.echo", "TestCloud"));
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(map("value", map("status", "fiction"), "sourceTime", now()), ROUTING, "TestCloud"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void envelopeKeepsFieldsOrderAndDefaults() {
		final Map<String, Object> normalized = ObservationFormat.normalize(map("sourceTime", "2026-10-15T12:00:00Z", "value", map("status", "alive")), ROUTING, "TestCloud");
		assertEquals(List.of("schemaVersion", "observationId", "providerSystem", "serviceInstance", "observationType", "sourceTime", "receivedTime", "publishedTime",
				"value", "unit", "quality", "pipeline", "metadata"), List.copyOf(normalized.keySet()));
		assertEquals("1.0", normalized.get("schemaVersion"));
		assertEquals("instance", normalized.get("serviceInstance"));
		assertEquals("good", normalized.get("quality"));
		assertNull(normalized.get("publishedTime"));
		assertEquals(map("sysMonitor", "SysMonitor", "revision", 1), normalized.get("pipeline"));
		assertEquals(64, normalized.get("observationId").toString().length(), "derived id is a SHA-256 hex digest");
		assertTrue(Json.json(normalized).startsWith("{\"schemaVersion\":\"1.0\",\"observationId\":"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void derivedIdIgnoresKeyOrder() {
		final Map<String, Object> a = map("sourceTime", "2026-10-15T12:00:00Z", "value", map("status", "alive", "detail", "x"));
		final Map<String, Object> b = map("value", map("detail", "x", "status", "alive"), "sourceTime", "2026-10-15T12:00:00Z");
		assertEquals(ObservationFormat.normalize(a, ROUTING, "TestCloud").get("observationId"), ObservationFormat.normalize(b, ROUTING, "TestCloud").get("observationId"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void invalidObservationsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(List.of(), ROUTING, "TestCloud"));
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(map("sourceTime", "2026-10-15T12:00:00+01:00", "value", map("status", "alive")), ROUTING, "TestCloud"));
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(map("sourceTime", "yesterdayZ", "value", map("status", "alive")), ROUTING, "TestCloud"));
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(map("sourceTime", now()), ROUTING, "TestCloud"));
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(map("schemaVersion", "2.0", "sourceTime", now(), "value", map("status", "alive")), ROUTING, "TestCloud"));
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(map("sourceTime", now(), "value", map("supplyType", "nuclear")), "TestCloud.sysmonitor.Provider.instance.power", "TestCloud"));
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(map("sourceTime", now(), "value", 1), "TestCloud.sysmonitor.Provider.instance", "TestCloud"));
		assertThrows(IllegalArgumentException.class, () -> ObservationFormat.normalize(map("observationId", "x".repeat(256), "sourceTime", now(), "value", map("status", "alive")), ROUTING, "TestCloud"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void topicOfTheDataOutput() {
		assertEquals("arrowhead/TestCloud/sysmonitor/data/echo/Provider",
				ObservationFormat.topic("arrowhead/TestCloud/sysmonitor/data", map("observationType", "echo", "providerSystem", "Provider")));
	}
}
