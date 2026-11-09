/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.api.http.SosConfigurationAPI;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.colony.ColonyClient;
import eu.arrowhead.sosconfigurator.console.ConsoleAuth;
import eu.arrowhead.sosconfigurator.console.ConsoleController;
import eu.arrowhead.sosconfigurator.console.ConsoleFilter;
import eu.arrowhead.sosconfigurator.console.ConsolePasswordHash;
import eu.arrowhead.sosconfigurator.jpa.repository.SosDeploymentRepository;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;
import eu.arrowhead.sosconfigurator.service.SosBackend;
import eu.arrowhead.sosconfigurator.service.SosConfigurationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// The console connector (password session) and the Arrowhead connector (stood in for by a filter that demands an identity, like
// the common authentication filter) in front of the native 5.2.2 API behaviour.
public class SosConsoleAndApiTest {

	//=================================================================================================
	// members

	private static final int CONSOLE_PORT = 8495;
	private static final int API_PORT = 8494;
	private static final String PASSWORD = "correct horse battery";

	private final AtomicInteger submitted = new AtomicInteger();
	private MockMvc mvc;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() throws IOException {
		final Catalog catalog = Catalog.load(Path.of("catalogs/sos.json"));
		final SosBackend backend = new SosBackend() {
			@Override
			public String submit(final Planner.Plan plan, final SosRequest request) {
				submitted.incrementAndGet();
				return "a".repeat(64);
			}

			@Override
			public ColonyClient.Deployment deployment(final String id) {
				return new ColonyClient.Deployment(id, "success", List.of());
			}

			@Override
			public String status(final String role, final Duration timeout) throws IOException {
				if (role.equals("cloud:RustDev")) {
					throw new IOException("no executor answered");
				}
				return "{\"state\":\"ready\"}";
			}
		};
		final SosDeploymentRepository repository = mock(SosDeploymentRepository.class);
		when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
		when(repository.findTop100ByOrderBySubmittedAtDesc()).thenReturn(List.of());

		final SosConfigurationAPI api = new SosConfigurationAPI();
		ReflectionTestUtils.setField(api, "service", new SosConfigurationService(catalog, new Planner(catalog), backend, repository, 1));

		final ConsoleAuth auth = new ConsoleAuth(ConsolePasswordHash.create(PASSWORD), "127.0.0.1,localhost,[::1]", 3600, System::currentTimeMillis);
		final ConsoleController console = new ConsoleController();
		ReflectionTestUtils.setField(console, "auth", auth);

		final ArrowheadFilter identity = new ArrowheadFilter() {
			@Override
			protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, jakarta.servlet.ServletException {
				if (request.getHeader("Authorization") == null) {
					response.sendError(401);
					return;
				}
				request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, "Sysop");
				chain.doFilter(request, response);
			}
		};
		final ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeansOfType(ArrowheadFilter.class)).thenReturn(Map.of("authenticationPolicyFilter", identity));
		final ConsoleFilter consoleFilter = new ConsoleFilter();
		ReflectionTestUtils.setField(consoleFilter, "auth", auth);
		ReflectionTestUtils.setField(consoleFilter, "context", context);
		ReflectionTestUtils.setField(consoleFilter, "consolePort", CONSOLE_PORT);

		mvc = MockMvcBuilders.standaloneSetup(api, console)
				.addFilter(consoleFilter)
				.addFilter(new NamedFilter(identity, "authenticationPolicyFilter"))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void consoleRequiresPasswordSessionAndLocalHost() throws Exception {
		mvc.perform(console(get("/healthz"))).andExpect(status().isOk());
		mvc.perform(console(get("/"))).andExpect(status().isFound());
		mvc.perform(console(get("/index.html"))).andExpect(status().isOk());
		mvc.perform(console(get("/app.js"))).andExpect(status().isOk());
		mvc.perform(console(get("/api/catalog"))).andExpect(status().isUnauthorized());
		mvc.perform(console(post("/api/session").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"wrong\"}"))).andExpect(status().isUnauthorized());

		final Cookie session = login();
		mvc.perform(console(get("/api/catalog")).cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.colony").exists());
		mvc.perform(console(get("/api/catalog"), "attacker.example:" + CONSOLE_PORT).cookie(session)).andExpect(status().isForbidden());
		mvc.perform(console(get("/api/catalog")).cookie(session).header("Origin", "http://attacker.example")).andExpect(status().isForbidden());
		mvc.perform(console(get("/sosconfigurator/general/mgmt/get-config")).cookie(session)).andExpect(status().isNotFound());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void arrowheadConnectorNeedsAnIdentityAndHasNoConsolePaths() throws Exception {
		mvc.perform(arrowhead(get("/api/catalog"))).andExpect(status().isUnauthorized());
		mvc.perform(arrowhead(get("/api/catalog")).header("Authorization", "Bearer IDENTITY-TOKEN//x")).andExpect(status().isOk());
		mvc.perform(arrowhead(get("/index.html")).header("Authorization", "Bearer IDENTITY-TOKEN//x")).andExpect(status().isNotFound());
		mvc.perform(arrowhead(post("/api/session").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"" + PASSWORD + "\"}"))).andExpect(status().isNotFound());
		// a console cookie is worthless on the Arrowhead connector
		mvc.perform(arrowhead(get("/api/catalog")).cookie(login())).andExpect(status().isUnauthorized());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void refusedSoSIsNotSubmitted() throws Exception {
		final MvcResult result = mvc.perform(console(post("/api/deployments")).cookie(login()).contentType(MediaType.APPLICATION_JSON)
				.content(Files.readString(Path.of("requests/refused-rustdev.json"))))
				.andExpect(status().isUnprocessableEntity()).andReturn();
		assertEquals(0, submitted.get());
		assertTrue(result.getResponse().getContentAsString().contains("S4"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void allowedSoSIsSubmitted() throws Exception {
		final MvcResult result = mvc.perform(arrowhead(post("/api/deployments")).header("Authorization", "Bearer IDENTITY-TOKEN//x").contentType(MediaType.APPLICATION_JSON)
				.content(Files.readString(Path.of("requests/testcloud-gocloud.json"))))
				.andExpect(status().isAccepted()).andReturn();
		assertEquals(1, submitted.get());
		assertEquals(11, Json.LENIENT.readTree(result.getResponse().getContentAsString()).path("plan").path("steps").size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void rejectsUnknownFieldsAndBadIds() throws Exception {
		final Cookie session = login();
		mvc.perform(console(post("/api/plan")).cookie(session).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\",\"clouds\":[],\"grantEverything\":true}"))
				.andExpect(status().isBadRequest());
		mvc.perform(console(get("/api/deployments/" + "z".repeat(64))).cookie(session)).andExpect(status().isNotFound());
		mvc.perform(console(get("/api/deployments/" + "a".repeat(64))).cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("success"));
		mvc.perform(console(post("/api/plan")).cookie(session).contentType(MediaType.APPLICATION_JSON).content("{\"x\":\"" + "y".repeat(70000) + "\"}"))
				.andExpect(status().isPayloadTooLarge());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void statusMarksSilentExecutorsOffline() throws Exception {
		final MvcResult result = mvc.perform(console(get("/api/status")).cookie(login())).andExpect(status().isOk()).andReturn();
		final var state = Json.LENIENT.readTree(result.getResponse().getContentAsString());
		assertEquals("ready", state.path("relay").path("state").asText());
		assertEquals("offline", state.path("clouds").path("RustDev").path("state").asText());
		assertEquals("ready", state.path("clouds").path("GoCloud").path("state").asText());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Cookie login() throws Exception {
		final MvcResult result = mvc.perform(console(post("/api/session")).contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isOk()).andReturn();
		final String header = result.getResponse().getHeader("Set-Cookie");
		assertNotNull(header);
		assertTrue(header.contains("HttpOnly") && header.contains("SameSite=Strict"));
		final String value = header.split(";", 2)[0].split("=", 2)[1];
		return new Cookie("ah_sos_console", value);
	}

	//-------------------------------------------------------------------------------------------------
	private static MockHttpServletRequestBuilder console(final MockHttpServletRequestBuilder builder) {
		return console(builder, "127.0.0.1:" + CONSOLE_PORT);
	}

	//-------------------------------------------------------------------------------------------------
	private static MockHttpServletRequestBuilder console(final MockHttpServletRequestBuilder builder, final String host) {
		return builder.header("Host", host).with(request -> {
			request.setLocalPort(CONSOLE_PORT);
			return request;
		});
	}

	//-------------------------------------------------------------------------------------------------
	private static MockHttpServletRequestBuilder arrowhead(final MockHttpServletRequestBuilder builder) {
		return builder.with(request -> {
			request.setLocalPort(API_PORT);
			return request;
		});
	}

	//-------------------------------------------------------------------------------------------------
	// registers a filter under a bean-like name, as Spring Boot does, so that OncePerRequestFilter uses "<name>.FILTERED"
	private static final class NamedFilter extends OncePerRequestFilter {
		private final OncePerRequestFilter delegate;
		private final String name;

		NamedFilter(final OncePerRequestFilter delegate, final String name) {
			this.delegate = delegate;
			this.name = name;
		}

		@Override
		protected String getAlreadyFilteredAttributeName() {
			return name + ALREADY_FILTERED_SUFFIX;
		}

		@Override
		protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, jakarta.servlet.ServletException {
			delegate.doFilter(request, response, chain);
		}
	}
}
