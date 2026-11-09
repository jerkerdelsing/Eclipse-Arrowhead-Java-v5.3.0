/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.ArrowheadResponseEntityExceptionHandler;
import eu.arrowhead.common.http.filter.authentication.OutsourcedFilter;
import eu.arrowhead.common.service.ConfigService;
import eu.arrowhead.common.service.LogService;
import eu.arrowhead.common.service.normalization.NormalizationMode;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import eu.arrowhead.gatetunnel.TestPki;
import eu.arrowhead.gatetunnel.api.http.GateTunnelConnectionAPI;
import eu.arrowhead.gatetunnel.api.http.GateTunnelExceptionHandler;
import eu.arrowhead.gatetunnel.api.http.GateTunnelKeyAPI;
import eu.arrowhead.gatetunnel.api.http.GateTunnelManagementAPI;
import eu.arrowhead.gatetunnel.api.http.GeneralManagementAPI;
import eu.arrowhead.gatetunnel.api.http.MonitorAPI;
import eu.arrowhead.gatetunnel.security.GateTunnelCallerFilter;
import eu.arrowhead.gatetunnel.service.ControlSessionService;
import eu.arrowhead.gatetunnel.service.GateTunnelApiException;
import eu.arrowhead.gatetunnel.service.GateTunnelStatusService;
import eu.arrowhead.gatetunnel.service.InterCloudSessionService;

// Every method and path the native 5.2.2 GateTunnel served (taken from its route() and handle(): under /gatetunnel and the
// 4.x name /gateway), plus the unprefixed aliases, is called through the real authentication filter (OutsourcedFilter:
// identity token verified by Authentication and bound to the client certificate) and the GateTunnel caller filter:
// unauthenticated -> 401, the certificate of another system -> 401, a non-system certificate or a system without
// permission -> 403, an authorised
// caller -> the 5.2.2 success status.
public class GateTunnelRouteContractTest {

	//=================================================================================================
	// members

	private static final Map<String, X509Certificate> CERTIFICATES = new java.util.HashMap<>();

	private MockMvc mvc;
	private ControlSessionService controlSessions;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeAll
	public static void certificates() throws Exception {
		final Path folder = Files.createTempDirectory("gatetunnel-contract-pki");
		final TestPki pki = new TestPki(folder);
		for (final String name : List.of("Gatekeeper", "LocalCloudManager", "Sysop", "AnySystem", "DynamicServiceOrchestration")) {
			CERTIFICATES.put(name, pki.issue(name, "sy").certificate());
		}
		CERTIFICATES.put("Gatekeeper-on", pki.issue("Gatekeeper", "on").certificate());
	}

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() throws Exception {
		controlSessions = mock(ControlSessionService.class);
		final InterCloudSessionService interCloud = mock(InterCloudSessionService.class);
		final GateTunnelStatusService statusService = mock(GateTunnelStatusService.class);
		when(controlSessions.connectProvider(any())).thenReturn(Map.of("sessionId", "s", "queueId", "q", "peerName", "p", "serverPort", 8000, "state", "PROVIDER_READY"));
		when(controlSessions.connectConsumer(any())).thenReturn(Map.of("sessionId", "s", "serverPort", 8000, "state", "ACTIVE"));
		when(controlSessions.closeSessions(any())).thenReturn(List.of());
		when(controlSessions.list(any())).thenReturn(Map.of("entries", List.of(), "count", 0));
		when(controlSessions.close(any())).thenReturn(true);
		when(interCloud.openProvider(any())).thenReturn(Map.of("sessionId", "s"));
		when(interCloud.openConsumer(any())).thenReturn(Map.of("address", "gatetunnel", "port", 8200, "sessionId", "s"));
		when(statusService.status()).thenReturn(Map.of("status", "ok", "system", "GateTunnel"));
		when(statusService.liveness()).thenReturn(Map.of("status", "ok", "system", "GateTunnel"));
		when(statusService.echo()).thenReturn(Map.of("status", "ok", "system", "GateTunnel", "version", "5.2.2"));
		when(statusService.publicKey()).thenReturn(Map.of("algorithm", "EC", "format", "X.509", "publicKey", "AAAA"));

		final GateTunnelConnectionAPI connection = new GateTunnelConnectionAPI();
		ReflectionTestUtils.setField(connection, "controlSessions", controlSessions);
		ReflectionTestUtils.setField(connection, "interCloud", interCloud);
		final GateTunnelKeyAPI key = new GateTunnelKeyAPI();
		ReflectionTestUtils.setField(key, "statusService", statusService);
		final GateTunnelManagementAPI management = new GateTunnelManagementAPI();
		ReflectionTestUtils.setField(management, "controlSessions", controlSessions);
		ReflectionTestUtils.setField(management, "statusService", statusService);
		final MonitorAPI monitor = new MonitorAPI();
		ReflectionTestUtils.setField(monitor, "statusService", statusService);
		final GeneralManagementAPI general = new GeneralManagementAPI();
		ReflectionTestUtils.setField(general, "statusService", statusService);
		ReflectionTestUtils.setField(general, "logService", mock(LogService.class));
		ReflectionTestUtils.setField(general, "configService", mock(ConfigService.class));

		// identity tokens are "token-<SystemName>"; Authentication verifies them as that system
		final ArrowheadHttpService httpService = mock(ArrowheadHttpService.class);
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_IDENTITY), eq(Constants.SERVICE_OP_IDENTITY_VERIFY), eq(IdentityVerifyResponseDTO.class), any(List.class)))
				.thenAnswer(i -> {
					final String token = ((List<?>) i.getArgument(3)).getFirst().toString();
					if (!token.startsWith("token-")) {
						throw new AuthException("Invalid token");
					}
					return new IdentityVerifyResponseDTO(true, token.substring("token-".length()), false, null, null);
				});
		final SystemInfo sysInfo = mock(SystemInfo.class);
		when(sysInfo.getSystemName()).thenReturn("GateTunnel");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		final SystemNameNormalizer normalizer = new SystemNameNormalizer();
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.SIMPLE);

		final OutsourcedFilter authentication = new OutsourcedFilter();
		ReflectionTestUtils.setField(authentication, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(authentication, "collector", mock(ServiceCollector.class));
		ReflectionTestUtils.setField(authentication, "httpService", httpService);
		ReflectionTestUtils.setField(authentication, "systemNameNormalizer", normalizer);
		ReflectionTestUtils.setField(authentication, "mapper", new ObjectMapper());
		final GateTunnelCallerFilter callers = new GateTunnelCallerFilter();
		ReflectionTestUtils.setField(callers, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(callers, "mapper", new ObjectMapper());

		mvc = MockMvcBuilders.standaloneSetup(connection, key, management, monitor, general)
				.setControllerAdvice(new GateTunnelExceptionHandler(), new ArrowheadResponseEntityExceptionHandler())
				.addFilters(authentication, callers)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	// method, path without base, 5.2.2 success status, an authorised caller, base paths it is served under
	static Stream<Arguments> nativeRoutes() {
		final List<Object[]> operations = List.of(
				new Object[] { "POST", "/connect_provider", 201, "Gatekeeper" },
				new Object[] { "POST", "/connect_consumer", 201, "DynamicServiceOrchestration" },
				new Object[] { "POST", "/close_sessions", 200, "Gatekeeper" },
				new Object[] { "POST", "/intercloud/provider", 201, "Gatekeeper" },
				new Object[] { "POST", "/intercloud/consumer", 201, "Gatekeeper" },
				new Object[] { "GET", "/publickey", 200, "LocalCloudManager" },
				new Object[] { "GET", "/mgmt/sessions", 200, "LocalCloudManager" },
				new Object[] { "POST", "/mgmt/sessions/close", 200, "Sysop" },
				new Object[] { "GET", "/mgmt/status", 200, "LocalCloudManager" },
				new Object[] { "GET", "/echo", 200, "Gatekeeper" },
				new Object[] { "GET", "/monitor/echo", 200, "Gatekeeper" });
		final List<Arguments> routes = new ArrayList<>();
		for (final String base : List.of("/gatetunnel", "/gateway", "")) {
			for (final Object[] op : operations) {
				routes.add(Arguments.of(op[0], base + op[1], op[2], op[3]));
			}
		}
		routes.add(Arguments.of("GET", "/gatetunnel/mgmt/general/status", 200, "LocalCloudManager"));

		return routes.stream();
	}

	//-------------------------------------------------------------------------------------------------
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("nativeRoutes")
	public void authorisedCallerGetsTheNativeStatus(final String method, final String path, final int expected, final String caller) throws Exception {
		mvc.perform(call(method, path, caller, caller)).andExpect(status().is(expected));
	}

	//-------------------------------------------------------------------------------------------------
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("nativeRoutes")
	public void unauthenticatedCallIsRefused(final String method, final String path, final int expected, final String caller) throws Exception {
		mvc.perform(call(method, path, null, caller)).andExpect(status().isUnauthorized());
		mvc.perform(call(method, path, "malformed", caller).header("Authorization", "Bearer SYSTEM//Sysop")).andExpect(status().isUnauthorized());
	}

	//-------------------------------------------------------------------------------------------------
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("nativeRoutes")
	public void tokenWithTheCertificateOfAnotherSystemIsRefused(final String method, final String path, final int expected, final String caller) throws Exception {
		mvc.perform(call(method, path, caller, "AnySystem")).andExpect(status().isUnauthorized());
		// the authorised system's name with a non-system (on-boarding) certificate: 403, as in 5.2.2
		mvc.perform(call(method, path, "Gatekeeper", "Gatekeeper-on")).andExpect(status().isForbidden());
	}

	//-------------------------------------------------------------------------------------------------
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("nativeRoutes")
	public void systemWithoutPermissionIsForbidden(final String method, final String path, final int expected, final String caller) throws Exception {
		mvc.perform(call(method, path, "AnySystem", "AnySystem")).andExpect(status().isForbidden());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void onlyTheGatekeeperOpensInterCloudSessions() throws Exception {
		mvc.perform(call("POST", "/gatetunnel/intercloud/provider", "LocalCloudManager", "LocalCloudManager"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorMessage").value("Only the local Gatekeeper opens relay sessions"));
		mvc.perform(call("GET", "/gatetunnel/mgmt/sessions", "Gatekeeper", "Gatekeeper")).andExpect(status().isForbidden());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void healthNeedsAuthenticationOnly() throws Exception {
		for (final String path : List.of("/health", "/certificate-authority/health")) {
			mvc.perform(call("GET", path, "AnySystem", "AnySystem")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
			mvc.perform(call("GET", path, null, "AnySystem")).andExpect(status().isUnauthorized());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void errorsKeepTheNativeStatusCodes() throws Exception {
		when(controlSessions.connectConsumer(any())).thenThrow(new GateTunnelApiException(404, "Session not found"));
		mvc.perform(call("POST", "/gatetunnel/connect_consumer", "Gatekeeper", "Gatekeeper")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorMessage").value("Session not found"));
		when(controlSessions.connectProvider(any())).thenThrow(new GateTunnelApiException(503, "No control ports available"));
		mvc.perform(call("POST", "/gatetunnel/connect_provider", "Gatekeeper", "Gatekeeper")).andExpect(status().is(503));
		when(controlSessions.close(any())).thenReturn(false);
		mvc.perform(call("POST", "/gatetunnel/mgmt/sessions/close", "Sysop", "Sysop")).andExpect(status().isNotFound()).andExpect(jsonPath("$.closed").value(false));
		// invalid JSON body, and a body over 1 MiB
		mvc.perform(call("POST", "/gatetunnel/close_sessions", "Gatekeeper", "Gatekeeper").content("{not json")).andExpect(status().isBadRequest());
		mvc.perform(call("POST", "/gatetunnel/close_sessions", "Gatekeeper", "Gatekeeper").content(new byte[1024 * 1024 + 1])).andExpect(status().is(413));
		// a body without a JSON content type is accepted, as in 5.2.2
		mvc.perform(call("POST", "/gatetunnel/close_sessions", "Gatekeeper", "Gatekeeper").contentType(MediaType.TEXT_PLAIN).content("[8000]")).andExpect(status().isOk());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static MockHttpServletRequestBuilder call(final String method, final String path, final String tokenSystem, final String certificate) {
		final MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path).secure(true).contentType(MediaType.APPLICATION_JSON);
		if ("POST".equals(method)) {
			builder.content("{}");
		}
		if (tokenSystem != null) {
			builder.header("Authorization", "Bearer IDENTITY-TOKEN//token-" + tokenSystem);
		}
		builder.requestAttr(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[] { CERTIFICATES.get(certificate) });

		return builder;
	}
}
