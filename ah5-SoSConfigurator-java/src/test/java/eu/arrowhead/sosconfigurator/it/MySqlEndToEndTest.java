/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import eu.arrowhead.common.testhelper.StandaloneModeInTests;
import eu.arrowhead.sosconfigurator.SoSConfiguratorMain;
import eu.arrowhead.sosconfigurator.colony.ColonyClient;
import eu.arrowhead.sosconfigurator.console.ConsolePasswordHash;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;
import eu.arrowhead.sosconfigurator.service.SosBackend;

// The whole application on a real embedded Tomcat with both connectors, the real common filter chain and JPA against a fresh
// MySQL schema (deployment/database/init.sql, restricted by least-privilege.sh). ColonyOS is replaced by a stub backend.
// Run with -Dsos.it.db.url=... -Dsos.it.db.password=...; skipped otherwise.
@EnabledIfSystemProperty(named = "sos.it.db.url", matches = ".+")
@SpringBootTest(classes = { SoSConfiguratorMain.class, MySqlEndToEndTest.Fixtures.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
		properties = "spring.config.location=classpath:/application.properties")
public class MySqlEndToEndTest {

	//=================================================================================================
	// members

	private static final int API_PORT = 18494;
	private static final int CONSOLE_PORT = 18495;
	private static final String PASSWORD = "integration console password";

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@DynamicPropertySource
	static void properties(final DynamicPropertyRegistry registry) {
		registry.add("arrowhead.security.profile", () -> "development");
		registry.add("spring.datasource.url", () -> System.getProperty("sos.it.db.url"));
		registry.add("spring.datasource.password", () -> System.getProperty("sos.it.db.password"));
		registry.add("server.port", () -> API_PORT);
		registry.add("console.port", () -> CONSOLE_PORT);
		registry.add("console.password.hash", () -> ConsolePasswordHash.create(PASSWORD));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void consoleAndArrowheadConnectorsAgainstMySql() throws Exception {
		final String console = "http://127.0.0.1:" + CONSOLE_PORT;
		final String api = "http://127.0.0.1:" + API_PORT;

		// console: UI is public, the API needs the password session
		assertEquals(302, send(HttpRequest.newBuilder(URI.create(console + "/")).GET()).statusCode());
		assertEquals(200, send(HttpRequest.newBuilder(URI.create(console + "/index.html")).GET()).statusCode());
		assertEquals(401, send(HttpRequest.newBuilder(URI.create(console + "/api/catalog")).GET()).statusCode());
		final HttpResponse<String> login = send(HttpRequest.newBuilder(URI.create(console + "/api/session")).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"password\":\"" + PASSWORD + "\"}")));
		assertEquals(200, login.statusCode());
		final String cookie = login.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];

		assertEquals(200, send(HttpRequest.newBuilder(URI.create(console + "/api/catalog")).header("Cookie", cookie).GET()).statusCode());
		final HttpResponse<String> submitted = send(HttpRequest.newBuilder(URI.create(console + "/api/deployments")).header("Cookie", cookie)
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(Files.readString(Path.of("requests/testcloud-gocloud.json")))));
		assertEquals(202, submitted.statusCode(), submitted.body());
		final HttpResponse<String> listed = send(HttpRequest.newBuilder(URI.create(console + "/api/deployments")).header("Cookie", cookie).GET());
		assertEquals(200, listed.statusCode());
		assertTrue(listed.body().contains("\"submittedBy\":\"console\""), listed.body());

		// Arrowhead connector: the common authentication filter applies; the console cookie and console paths do not exist there
		assertEquals(401, send(HttpRequest.newBuilder(URI.create(api + "/api/catalog")).header("Cookie", cookie).GET()).statusCode());
		assertEquals(404, send(HttpRequest.newBuilder(URI.create(api + "/index.html")).GET()).statusCode());
		// declared identities in the development profile: Sysop passes the access filter, another system needs a grant
		assertEquals(200, send(HttpRequest.newBuilder(URI.create(api + "/api/catalog")).header("Authorization", "Bearer SYSTEM//Sysop").GET()).statusCode());
		final HttpResponse<String> plan = send(HttpRequest.newBuilder(URI.create(api + "/api/plan")).header("Authorization", "Bearer SYSTEM//Sysop")
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(Files.readString(Path.of("requests/refused-rustdev.json")))));
		assertEquals(200, plan.statusCode());
		assertTrue(plan.body().contains("\"allowed\":false"));

		// generalManagement is sysop-only and not on the console
		assertEquals(200, send(HttpRequest.newBuilder(URI.create(api + "/sosconfigurator/general/mgmt/get-config?keys=sos.catalog.file")).header("Authorization", "Bearer SYSTEM//Sysop").GET()).statusCode());
		assertEquals(403, send(HttpRequest.newBuilder(URI.create(api + "/sosconfigurator/general/mgmt/get-config?keys=sos.catalog.file")).header("Authorization", "Bearer SYSTEM//DataManager").GET()).statusCode());
		assertEquals(404, send(HttpRequest.newBuilder(URI.create(console + "/sosconfigurator/general/mgmt/get-config")).header("Cookie", cookie).GET()).statusCode());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private HttpResponse<String> send(final HttpRequest.Builder builder) throws IOException, InterruptedException {
		return http.send(builder.timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
	}

	//-------------------------------------------------------------------------------------------------
	@TestConfiguration
	static class Fixtures {

		//-------------------------------------------------------------------------------------------------
		@Bean
		StandaloneModeInTests standaloneMode() {
			return new StandaloneModeInTests() {
			};
		}

		//-------------------------------------------------------------------------------------------------
		@Bean
		@Primary
		SosBackend stubBackend() {
			return new SosBackend() {
				@Override
				public String submit(final Planner.Plan plan, final SosRequest request) {
					return (Long.toHexString(System.nanoTime()) + "b".repeat(64)).substring(0, 64);
				}

				@Override
				public ColonyClient.Deployment deployment(final String id) {
					return new ColonyClient.Deployment(id, "success", List.of());
				}

				@Override
				public String status(final String role, final Duration timeout) {
					return "{\"state\":\"ready\"}";
				}
			};
		}
	}
}
