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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

import eu.arrowhead.common.testhelper.StandaloneModeInTests;
import eu.arrowhead.gatekeeper.GatekeeperMain;
import eu.arrowhead.gatekeeper.jpa.service.GatekeeperDbService;
import eu.arrowhead.gatekeeper.migrate.SqliteImport;

// The whole application (filters, management filter, controllers, JPA with ddl-auto=validate) against a fresh MySQL schema
// created by deployment/database/init.sql and restricted to DML by least-privilege.sh, plus the migrate-sqlite import.
// Run with -Dgk.it.db.url=... -Dgk.it.db.password=...; skipped otherwise. Development profile without TLS and with
// declared identities ("Bearer SYSTEM//<name>"); no relay, no registration.
@EnabledIfSystemProperty(named = "gk.it.db.url", matches = ".+")
@SpringBootTest(classes = { GatekeeperMain.class, MySqlEndToEndTest.Standalone.class }, properties = "spring.config.location=classpath:/application.properties")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MySqlEndToEndTest {

	//=================================================================================================
	// members

	@Autowired
	private MockMvc mvc;

	@Autowired
	private GatekeeperDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@DynamicPropertySource
	static void properties(final DynamicPropertyRegistry registry) {
		registry.add("arrowhead.security.profile", () -> "development");
		registry.add("spring.datasource.url", () -> System.getProperty("gk.it.db.url"));
		registry.add("spring.datasource.password", () -> System.getProperty("gk.it.db.password"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@Order(1)
	public void managementAgainstMySql() throws Exception {
		mvc.perform(post("/gatekeeper/mgmt/clouds").header("Authorization", "Bearer SYSTEM//LocalCloudManager").contentType(MediaType.APPLICATION_JSON)
				.content("[{\"name\":\"OtherCloud\",\"operator\":\"Company\",\"gatekeeperAddress\":\"gk.other\",\"gatekeeperPort\":8449,\"metadata\":{\"site\":\"lab\"}},"
						+ "{\"name\":\"LabCloud\",\"operator\":\"Lab\"}]"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.count").value(2))
				// ordered by operator, name
				.andExpect(jsonPath("$.entries[0].name").value("OtherCloud"))
				.andExpect(jsonPath("$.entries[0].metadata.site").value("lab"))
				.andExpect(jsonPath("$.entries[1].operator").value("Lab"));

		// an update keeps one row; an invalid item writes nothing
		mvc.perform(post("/gatekeeper/mgmt/clouds").header("Authorization", "Bearer SYSTEM//Sysop").contentType(MediaType.APPLICATION_JSON)
				.content("{\"list\":[{\"name\":\"OtherCloud\",\"operator\":\"Company\",\"address\":\"gk2\"}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.count").value(2))
				.andExpect(jsonPath("$.entries[0].gatekeeperAddress").value("gk2"));
		mvc.perform(post("/gatekeeper/mgmt/clouds").header("Authorization", "Bearer SYSTEM//Sysop").contentType(MediaType.APPLICATION_JSON)
				.content("[{\"name\":\"Third\",\"operator\":\"Company\"},{\"name\":\"NoOperator\"}]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Operator required"));
		mvc.perform(get("/gatekeeper/mgmt/clouds").header("Authorization", "Bearer SYSTEM//LocalCloudManager"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(2));

		mvc.perform(post("/gatekeeper/mgmt/relays").header("Authorization", "Bearer SYSTEM//LocalCloudManager").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"org-relay\",\"address\":\"relay.example\",\"port\":8883,\"secure\":false}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.entries[0].type").value("MQTT"))
				.andExpect(jsonPath("$.entries[0].secure").value(false))
				.andExpect(jsonPath("$.entries[0].createdAt").isNumber());

		mvc.perform(delete("/gatekeeper/mgmt/clouds/Lab/LabCloud").header("Authorization", "Bearer SYSTEM//LocalCloudManager"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.removed").value(true));
		mvc.perform(delete("/gatekeeper/mgmt/clouds/Lab/LabCloud").header("Authorization", "Bearer SYSTEM//LocalCloudManager"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.removed").value(false));

		// status, general management, refusals, liveness
		mvc.perform(get("/gatekeeper/mgmt/status").header("Authorization", "Bearer SYSTEM//LocalCloudManager"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.implementation").value("java-spring"))
				.andExpect(jsonPath("$.relay.connected").value(false));
		mvc.perform(get("/gatekeeper/mgmt/general/get-config").param("keys", "gatekeeper.federation.profile").header("Authorization", "Bearer SYSTEM//Sysop"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.map['gatekeeper.federation.profile']").value("legacy-mtls"));
		mvc.perform(get("/gatekeeper/mgmt/clouds").header("Authorization", "Bearer SYSTEM//DynamicServiceOrchestration")).andExpect(status().isForbidden());
		mvc.perform(get("/gatekeeper/echo").header("Authorization", "Bearer SYSTEM//AnySystem")).andExpect(status().isForbidden());
		mvc.perform(get("/gatekeeper/echo").header("Authorization", "Bearer SYSTEM//DynamicServiceOrchestration"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.system").value("Gatekeeper"));
		mvc.perform(get("/gatekeeper/mgmt/clouds")).andExpect(status().isUnauthorized());
		mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@Order(2)
	public void migrateSqliteImportsCloudsAndRelaysOnce() throws Exception {
		final Path file = Files.createTempDirectory("gk-migrate-").resolve("gatekeeper.db");
		try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement s = c.createStatement()) {
			// the native 5.2.2 schema
			s.execute("CREATE TABLE clouds(name TEXT NOT NULL,operator TEXT NOT NULL,gatekeeper_address TEXT,gatekeeper_port INTEGER,metadata TEXT NOT NULL DEFAULT '{}',created_at REAL NOT NULL,updated_at REAL NOT NULL,PRIMARY KEY(name,operator))");
			s.execute("CREATE TABLE relays(name TEXT PRIMARY KEY,address TEXT NOT NULL,port INTEGER NOT NULL,type TEXT NOT NULL,secure INTEGER NOT NULL DEFAULT 1,metadata TEXT NOT NULL,created_at REAL NOT NULL)");
			s.execute("INSERT INTO clouds VALUES('OtherCloud','Company','old',1,'{}',1,1)");
			s.execute("INSERT INTO clouds VALUES('GoCloud','Company','gk.go',8449,'{\"v\":2}',1,1)");
			s.execute("INSERT INTO relays VALUES('legacy-relay','relay.legacy',8883,'MQTT',0,'{}',1700000000.25)");
		}

		final SqliteImport source = SqliteImport.read(file);
		assertEquals(2, source.getClouds().size());
		// OtherCloud already exists and is kept
		assertEquals(2, dbService.importRows(source.getClouds(), source.getRelays(), source.getRelayCreatedAt()));
		assertEquals(0, dbService.importRows(source.getClouds(), source.getRelays(), source.getRelayCreatedAt()));

		mvc.perform(get("/gatekeeper/mgmt/clouds").header("Authorization", "Bearer SYSTEM//Sysop"))
				.andExpect(jsonPath("$.count").value(2))
				.andExpect(jsonPath("$.entries[0].name").value("GoCloud"))
				.andExpect(jsonPath("$.entries[0].metadata.v").value(2))
				.andExpect(jsonPath("$.entries[1].gatekeeperAddress").value("gk2"));
		mvc.perform(get("/gatekeeper/mgmt/relays").header("Authorization", "Bearer SYSTEM//Sysop"))
				.andExpect(jsonPath("$.entries[0].name").value("legacy-relay"))
				.andExpect(jsonPath("$.entries[0].secure").value(false))
				.andExpect(jsonPath("$.entries[0].createdAt").value(1700000000.25));
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
