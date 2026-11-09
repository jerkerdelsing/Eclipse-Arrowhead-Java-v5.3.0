/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.gatekeeper.jpa.entity.Cloud;
import eu.arrowhead.gatekeeper.jpa.entity.Relay;
import eu.arrowhead.gatekeeper.jpa.service.GatekeeperDbService;
import eu.arrowhead.gatekeeper.service.dto.CloudInput;
import eu.arrowhead.gatekeeper.service.dto.DTOConverter;
import eu.arrowhead.gatekeeper.service.dto.RelayInput;
import eu.arrowhead.gatekeeper.service.validation.GatekeeperValidation;

// Request shapes, checks and views of gatekeeperManagement, as the native 5.2.2 implementation had them.
public class ManagementServiceTest {

	//=================================================================================================
	// members

	private GatekeeperDbService db;
	private ManagementService service;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		db = mock(GatekeeperDbService.class);
		service = new ManagementService();
		ReflectionTestUtils.setField(service, "dbService", db);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("unchecked")
	public void acceptsAnArrayAListObjectOrOneObject() {
		final ArgumentCaptor<List<CloudInput>> rows = ArgumentCaptor.forClass(List.class);
		service.upsertClouds(List.of(m("name", "A", "operator", "Company"), m("name", "B", "operator", "Company")));
		service.upsertClouds(m("list", List.of(m("name", "C", "operator", "Company"))));
		service.upsertClouds(m("name", "D", "operator", "Company", "address", "gk", "port", "8449"));
		verify(db, org.mockito.Mockito.times(3)).upsertClouds(rows.capture());
		assertEquals(2, rows.getAllValues().get(0).size());
		assertEquals("C", rows.getAllValues().get(1).getFirst().name());
		assertEquals(new CloudInput("D", "Company", "gk", 8449, "{}"), rows.getAllValues().get(2).getFirst());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void anInvalidItemWritesNothing() {
		final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> service.upsertClouds(List.of(m("name", "A", "operator", "Company"), m("name", "B"))));
		assertEquals("Operator required", error.getMessage());
		verify(db, never()).upsertClouds(any());
		assertEquals("Name required", assertThrows(IllegalArgumentException.class, () -> service.upsertRelays(m("address", "relay", "port", 8883))).getMessage());
		assertEquals("Relay address and valid port required", assertThrows(IllegalArgumentException.class, () -> service.upsertRelays(m("name", "r", "address", "relay", "port", 70000))).getMessage());
		assertEquals("Relay address and valid port required", assertThrows(IllegalArgumentException.class, () -> service.upsertRelays(m("name", "r", "port", 8883))).getMessage());
		verify(db, never()).upsertRelays(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void relayDefaults() {
		assertEquals(new RelayInput("r", "relay", 8883, "MQTT", true, "{}"), GatekeeperValidation.relay(m("name", "r", "address", "relay", "port", "8883")));
		assertFalse(GatekeeperValidation.relay(m("name", "r", "address", "relay", "port", 8883, "secure", false)).secure());
		assertTrue(GatekeeperValidation.relay(m("name", "r", "address", "relay", "port", 8883, "secure", "no")).secure(), "only an explicit false is insecure");
		assertNull(GatekeeperValidation.cloud(m("name", "A", "operator", "Company")).gatekeeperPort());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void deletePathNeedsOperatorAndName() {
		when(db.deleteCloud("Company", "A")).thenReturn(true);
		assertTrue(service.deleteCloud("Company/A"));
		assertThrows(IllegalArgumentException.class, () -> service.deleteCloud("Company"));
		assertThrows(IllegalArgumentException.class, () -> service.deleteCloud("Company/A/x"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void viewsKeepTheNativeFields() {
		final Cloud cloud = new Cloud("A", "Company");
		cloud.setGatekeeperAddress("gk");
		cloud.setGatekeeperPort(8449);
		cloud.setMetadata("{\"k\":1}");
		assertEquals(m("name", "A", "operator", "Company", "neighbor", true, "secure", true, "gatekeeperAddress", "gk", "gatekeeperPort", 8449, "metadata", m("k", 1)),
				DTOConverter.cloud(cloud));

		final Relay relay = new Relay("r");
		relay.setAddress("relay");
		relay.setPort(8883);
		relay.setType("MQTT");
		relay.setMetadata("{}");
		relay.setCreatedAt(ZonedDateTime.parse("2026-01-01T00:00:00.500Z"));
		final Map<String, Object> view = DTOConverter.relay(relay);
		assertEquals(List.of("name", "address", "port", "type", "secure", "metadata", "createdAt"), List.copyOf(view.keySet()));
		assertEquals(1767225600.5, view.get("createdAt"));
		assertEquals(m("entries", List.of(), "count", 0), DTOConverter.entries(List.of()));
	}
}
