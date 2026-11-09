/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.testhelper.StandaloneModeInTests;
import eu.arrowhead.gatetunnel.GateTunnelMain;
import eu.arrowhead.gatetunnel.jpa.service.SessionDbService;

// The whole application (filters, management filter, controllers, JPA, Log4j2 logs table) against a fresh MySQL schema
// created by deployment/database/init.sql and restricted by least-privilege.sh (ddl-auto=validate). Ported from the
// persistence part of the 5.2.2 ContractTest. Run with -Dgt.it.db.url=... -Dgt.it.db.password=...; skipped otherwise.
// Development profile without TLS and with declared identities ("Bearer SYSTEM//<name>"); no relay is reachable.
@EnabledIfSystemProperty(named = "gt.it.db.url", matches = ".+")
@SpringBootTest(classes = { GateTunnelMain.class, MySqlEndToEndTest.Standalone.class }, properties = "spring.config.location=classpath:/application.properties")
@AutoConfigureMockMvc
public class MySqlEndToEndTest {

	//=================================================================================================
	// members

	@Autowired
	private MockMvc mvc;

	@Autowired
	private SessionDbService dbService;

	private final ObjectMapper json = new ObjectMapper();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@DynamicPropertySource
	static void properties(final DynamicPropertyRegistry registry) {
		registry.add("arrowhead.security.profile", () -> "development");
		registry.add("spring.datasource.url", () -> System.getProperty("gt.it.db.url"));
		registry.add("spring.datasource.password", () -> System.getProperty("gt.it.db.password"));
		registry.add("gatetunnel.relay.port", () -> "1");
	}

	//-------------------------------------------------------------------------------------------------
	private Map<?, ?> postJson(final String path, final String caller, final Object body, final int expected) throws Exception {
		final MvcResult result = mvc.perform(post(path).header("Authorization", "Bearer SYSTEM//" + caller).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
				.andExpect(status().is(expected))
				.andReturn();
		final Object parsed = json.readValue(result.getResponse().getContentAsString(), Object.class);

		return parsed instanceof final Map<?, ?> map ? map : Map.of("list", parsed);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void controlSessionsPersistCloseAndFreeTheirPorts() throws Exception {
		final Map<String, Object> request = Map.of("consumer", Map.of("name", "Consumer"), "provider", Map.of("name", "Provider"),
				"consumerCloud", Map.of("name", "CloudA", "operator", "Example"), "providerCloud", Map.of("name", "CloudB", "operator", "Example"),
				"relay", Map.of("name", "relay", "address", "relay", "port", 61617), "serviceDefinition", "temperature");

		final Map<?, ?> created = postJson("/gatetunnel/connect_provider", "Gatekeeper", request, 201);
		assertEquals("PROVIDER_READY", created.get("state"));
		final Object port = created.get("serverPort");
		final Map<?, ?> active = postJson("/gateway/connect_consumer", "Gatekeeper", Map.of("queueId", created.get("queueId")), 201);
		assertEquals("ACTIVE", active.get("state"));
		assertEquals(port, active.get("serverPort"));

		mvc.perform(get("/gatetunnel/mgmt/sessions").param("limit", "5").header("Authorization", "Bearer SYSTEM//LocalCloudManager"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.entries[0].state").value("ACTIVE"))
				.andExpect(jsonPath("$.entries[0].consumer.name").value("Consumer"))
				.andExpect(jsonPath("$.entries[0].relay.port").value(61617))
				.andExpect(jsonPath("$.entries[0].sessionStartedAt").isNumber());
		mvc.perform(get("/gatetunnel/mgmt/sessions").header("Authorization", "Bearer SYSTEM//Gatekeeper")).andExpect(status().isForbidden());

		// a closed session keeps its port until it is purged
		assertEquals(List.of(), postJson("/gatetunnel/close_sessions", "Gatekeeper", List.of(port), 200).get("list"));
		assertEquals(List.of(Map.of("port", port, "error", "Active session not found")), postJson("/close_sessions", "Gatekeeper", Map.of("ports", List.of(port)), 200).get("list"));
		postJson("/gatetunnel/connect_consumer", "Gatekeeper", Map.of("queueId", created.get("queueId")), 404);
		final Map<?, ?> second = postJson("/gatetunnel/connect_provider", "Gatekeeper", request, 201);
		assertTrue(((Number) second.get("serverPort")).intValue() != ((Number) port).intValue());
		postJson("/gatetunnel/connect_provider", "Gatekeeper", Map.of("serviceDefinition", "temperature"), 400);

		assertEquals(Map.of("closed", true), postJson("/gatetunnel/mgmt/sessions/close", "Sysop", Map.of("sessionId", second.get("sessionId")), 200));
		assertEquals(Map.of("closed", false), postJson("/gatetunnel/mgmt/sessions/close", "Sysop", Map.of("sessionId", "unknown"), 404));
		assertEquals(2, dbService.purgeClosed(Utilities.utcNow().plusMinutes(1)));
		final Map<?, ?> reused = postJson("/gatetunnel/connect_provider", "Gatekeeper", request, 201);
		assertEquals(port, reused.get("serverPort"));

		// inter-cloud session of an unconfigured peer: 400; status documents
		postJson("/gatetunnel/intercloud/provider", "Gatekeeper", Map.of("consumer", "C", "provider", "P", "serviceDefinition", "d", "peer", "GateTunnel.Evil.Company.arrowhead.eu"), 400);
		mvc.perform(get("/gatetunnel/mgmt/general/status").header("Authorization", "Bearer SYSTEM//LocalCloudManager"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.implementation").value("java-spring"))
				.andExpect(jsonPath("$.relay.connected").value(false))
				.andExpect(jsonPath("$.activeSessions").value(1));
		mvc.perform(get("/gatetunnel/echo").header("Authorization", "Bearer SYSTEM//Gatekeeper")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value("5.2.2"));
		mvc.perform(get("/gatetunnel/echo")).andExpect(status().isUnauthorized());
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	@TestConfiguration
	static class Standalone {

		//-------------------------------------------------------------------------------------------------
		@Bean
		StandaloneModeInTests standaloneMode() {
			return new StandaloneModeInTests() {
			};
		}
	}
}
