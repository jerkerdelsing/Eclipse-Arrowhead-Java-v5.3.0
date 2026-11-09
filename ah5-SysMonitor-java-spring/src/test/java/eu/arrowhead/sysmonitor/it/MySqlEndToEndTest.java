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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import eu.arrowhead.common.testhelper.StandaloneModeInTests;
import eu.arrowhead.sysmonitor.SysMonitorMain;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;
import eu.arrowhead.sysmonitor.service.ObservationFormat;

// The whole application (filters, management filter, controllers, JPA with ddl-auto=validate) against a fresh MySQL schema
// created by deployment/database/init.sql and restricted by least-privilege.sh. Run with -Dsysmonitor.it.db.url=...
// -Dsysmonitor.it.db.password=...; skipped otherwise. Development profile without TLS, declared identities
// ("Bearer SYSTEM//<name>") and without the broker workers.
@EnabledIfSystemProperty(named = "sysmonitor.it.db.url", matches = ".+")
@SpringBootTest(classes = { SysMonitorMain.class, MySqlEndToEndTest.Standalone.class }, properties = "spring.config.location=classpath:/application.properties")
@AutoConfigureMockMvc
public class MySqlEndToEndTest {

	//=================================================================================================
	// members

	private static final String MANAGER = "Bearer SYSTEM//LocalCloudManager";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private MonitorDbService store;

	@Autowired
	private JdbcTemplate jdbc;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@DynamicPropertySource
	static void properties(final DynamicPropertyRegistry registry) {
		registry.add("arrowhead.security.profile", () -> "development");
		registry.add("sysmonitor.workers.enabled", () -> "false");
		registry.add("spring.datasource.url", () -> System.getProperty("sysmonitor.it.db.url"));
		registry.add("spring.datasource.password", () -> System.getProperty("sysmonitor.it.db.password"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void managementAndHistoryAgainstMySql() throws Exception {
		// subscriptions with revisions
		mvc.perform(put("/sysmonitor/management/subscriptions/Pump").header("Authorization", MANAGER).contentType(MediaType.APPLICATION_JSON)
				.content("{\"observationTypes\":[\"status\",\"echo\"]}"))
				.andExpect(status().isOk())
				.andExpect(header().string("ETag", "1"))
				.andExpect(jsonPath("$.observationTypes[0]").value("echo"))
				.andExpect(jsonPath("$.updatedBy").value("LocalCloudManager"));
		mvc.perform(put("/sysmonitor/management/subscriptions/Pump").header("Authorization", MANAGER).header("If-Match", "7").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isConflict());
		mvc.perform(get("/sysmonitor/management/subscriptions").header("Authorization", MANAGER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.defaultSubscription").value("all"))
				.andExpect(jsonPath("$.list[0].providerSystem").value("Pump"))
				.andExpect(jsonPath("$.list[0].providerFound").value(false));
		mvc.perform(get("/sysmonitor/management/subscriptions").header("Authorization", "Bearer SYSTEM//AnySystem")).andExpect(status().isForbidden());

		// history
		final String source = Instant.now().minusSeconds(5).toString();
		store.enqueue(ObservationFormat.normalize(map("sourceTime", source, "value", map("status", "alive")), "TestCloud.sysmonitor.Pump.pump.echo", "TestCloud"));
		mvc.perform(get("/sysmonitor/history/systems").header("Authorization", "Bearer SYSTEM//AnySystem"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.systems[0].systemName").value("Pump"))
				.andExpect(jsonPath("$.systems[0].observationCount").value(1))
				.andExpect(jsonPath("$.retentionDays").value(30.0));
		mvc.perform(get("/sysmonitor/history/systems/Pump/observations?type=echo&count=10").header("Authorization", "Bearer SYSTEM//AnySystem"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.observations[0].sourceTime").value(source));
		mvc.perform(get("/sysmonitor/history/systems/Pump/latest").header("Authorization", "Bearer SYSTEM//AnySystem"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latest.echo.providerSystem").value("Pump"));
		assertEquals(1, store.pending().size());

		// status: degraded without brokers; generalManagement for the management whitelist only
		mvc.perform(get("/sysmonitor/management/status").header("Authorization", MANAGER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("degraded"))
				.andExpect(jsonPath("$.storage.stored").value(1))
				.andExpect(jsonPath("$.amqp.queue").value("SysMonitor.observations"))
				.andExpect(jsonPath("$.mqtt.output").value("arrowhead/TestCloud/sysmonitor/data"));
		mvc.perform(get("/sysmonitor/mgmt/general/status").header("Authorization", MANAGER)).andExpect(status().isServiceUnavailable());
		mvc.perform(get("/sysmonitor/mgmt/general/status").header("Authorization", "Bearer SYSTEM//AnySystem")).andExpect(status().isForbidden());
		mvc.perform(post("/sysmonitor/mgmt/general/logs").header("Authorization", MANAGER).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
		mvc.perform(get("/health").header("Authorization", "Bearer SYSTEM//AnySystem")).andExpect(status().isServiceUnavailable());

		mvc.perform(delete("/sysmonitor/management/subscriptions/Pump").header("Authorization", MANAGER)).andExpect(status().isNoContent());
		mvc.perform(delete("/sysmonitor/management/subscriptions/Pump").header("Authorization", MANAGER)).andExpect(status().isNotFound());
		assertEquals(2, (int) jdbc.queryForObject("SELECT COUNT(*) FROM audit", Integer.class));

		// retention prune
		assertEquals(1, store.prune(-1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void applicationAccountHasDataManipulationOnly() {
		final List<String> grants = jdbc.queryForList("SHOW GRANTS FOR CURRENT_USER()", String.class);
		assertTrue(grants.stream().anyMatch(g -> g.startsWith("GRANT SELECT, INSERT, UPDATE, DELETE ON")), grants.toString());
		assertThrows(Exception.class, () -> jdbc.execute("CREATE TABLE ddl_probe (id int)"));
		assertThrows(Exception.class, () -> jdbc.execute("DROP TABLE audit"));
		assertThrows(Exception.class, () -> jdbc.execute("ALTER TABLE audit ADD COLUMN probe int"));
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
