/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.api;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.cert.X509Certificate;
import java.util.HashMap;
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
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.ArrowheadResponseEntityExceptionHandler;
import eu.arrowhead.common.http.filter.authentication.OutsourcedFilter;
import eu.arrowhead.common.http.filter.authorization.ManagementPolicy;
import eu.arrowhead.common.http.filter.authorization.ManagementServiceFilter;
import eu.arrowhead.common.service.ConfigService;
import eu.arrowhead.common.service.LogService;
import eu.arrowhead.common.service.normalization.NormalizationMode;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import eu.arrowhead.gatekeeper.TestPki;
import eu.arrowhead.gatekeeper.api.http.GatekeeperAPI;
import eu.arrowhead.gatekeeper.api.http.GatekeeperExceptionHandler;
import eu.arrowhead.gatekeeper.api.http.GeneralManagementAPI;
import eu.arrowhead.gatekeeper.api.http.ManagementAPI;
import eu.arrowhead.gatekeeper.security.CallerProfileFilter;
import eu.arrowhead.gatekeeper.security.GatekeeperAccessFilter;
import eu.arrowhead.gatekeeper.security.LivenessFilter;
import eu.arrowhead.gatekeeper.security.NativeResponseFilter;
import eu.arrowhead.gatekeeper.service.FederationService;
import eu.arrowhead.gatekeeper.service.ManagementService;
import eu.arrowhead.gatekeeper.service.StatusService;

// Every method and path the native 5.2.2 Gatekeeper served (its Service.route/handle registrations), through the deployed
// filter chain: native response filter, liveness, caller profile, the common OutsourcedFilter (identity token bound to the
// client certificate), the common management filter (whitelist policy) and the Gatekeeper caller rules. For each route:
// the authorised status, and the statuses of an unauthenticated call, a wrong certificate, a refused certificate profile,
// a missing client certificate and an unauthorised system. The native implementation had no unprefixed aliases.
public class GatekeeperRouteContractTest {

	//=================================================================================================
	// members

	private static TestPki pki;
	private static final Map<String, X509Certificate> SYSTEM_CERTS = new HashMap<>();
	private static final Map<String, X509Certificate> ONBOARDING_CERTS = new HashMap<>();

	private MockMvc mvc;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeAll
	public static void pki() throws Exception {
		pki = new TestPki();
		for (final String name : List.of("DynamicServiceOrchestration", "SimpleStoreServiceOrchestration", "LocalCloudManager", "Gatekeeper", "Sysop", "AnySystem")) {
			SYSTEM_CERTS.put(name, pki.issue(name, "sy"));
			ONBOARDING_CERTS.put(name, pki.issue(name, "on"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() throws Exception {
		final ObjectMapper mapper = new ObjectMapper();

		final FederationService federation = mock(FederationService.class);
		when(federation.gsd(any(), anyString())).thenReturn(m("results", List.of(), "unsuccessfulRequests", 0, "queriedClouds", 0));
		when(federation.icn(any(), anyString())).thenReturn(m("negotiated", false, "targetCloud", "OtherCloud", "reason", "Provider not found"));
		final ManagementService management = mock(ManagementService.class);
		when(management.clouds()).thenReturn(m("entries", List.of(), "count", 0));
		when(management.relays()).thenReturn(m("entries", List.of(), "count", 0));
		when(management.upsertClouds(any())).thenReturn(m("entries", List.of(), "count", 0));
		when(management.upsertRelays(any())).thenReturn(m("entries", List.of(), "count", 0));
		when(management.deleteCloud("Company/OtherCloud")).thenReturn(true);
		when(management.deleteCloud("OnlyOne")).thenThrow(new IllegalArgumentException("operator/name required"));
		final StatusService statusService = mock(StatusService.class);
		when(statusService.status()).thenReturn(m("status", "ok", "system", "Gatekeeper"));
		when(statusService.liveness()).thenReturn(m("status", "ok", "system", "Gatekeeper"));

		final GatekeeperAPI api = new GatekeeperAPI();
		ReflectionTestUtils.setField(api, "federationService", federation);
		final ManagementAPI managementApi = new ManagementAPI();
		ReflectionTestUtils.setField(managementApi, "managementService", management);
		ReflectionTestUtils.setField(managementApi, "statusService", statusService);
		final GeneralManagementAPI general = new GeneralManagementAPI();
		ReflectionTestUtils.setField(general, "statusService", statusService);
		ReflectionTestUtils.setField(general, "logService", mock(LogService.class));
		ReflectionTestUtils.setField(general, "configService", mock(ConfigService.class));

		final SystemInfo sysInfo = mock(SystemInfo.class);
		when(sysInfo.getSystemName()).thenReturn("Gatekeeper");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("LocalCloudManager", "Sysop"));

		// Authentication: "tok-<Name>" is a valid identity token of <Name>; Sysop is the system operator
		final ArrowheadHttpService httpService = mock(ArrowheadHttpService.class);
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_IDENTITY), eq(Constants.SERVICE_OP_IDENTITY_VERIFY), eq(IdentityVerifyResponseDTO.class), anyList()))
				.thenAnswer(invocation -> {
					final String token = ((List<?>) invocation.getArgument(3)).getFirst().toString();
					return token.startsWith("tok-")
							? new IdentityVerifyResponseDTO(true, token.substring(4), "tok-Sysop".equals(token), null, null)
							: new IdentityVerifyResponseDTO(false, null, false, null, null);
				});
		final SystemNameNormalizer normalizer = new SystemNameNormalizer();
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);

		final OutsourcedFilter authentication = new OutsourcedFilter();
		ReflectionTestUtils.setField(authentication, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(authentication, "httpService", httpService);
		ReflectionTestUtils.setField(authentication, "systemNameNormalizer", normalizer);
		ReflectionTestUtils.setField(authentication, "mapper", mapper);
		final ManagementServiceFilter managementFilter = new ManagementServiceFilter();
		ReflectionTestUtils.setField(managementFilter, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(managementFilter, "mapper", mapper);
		final LivenessFilter liveness = new LivenessFilter();
		ReflectionTestUtils.setField(liveness, "statusService", statusService);
		ReflectionTestUtils.setField(liveness, "mapper", mapper);
		final CallerProfileFilter profile = new CallerProfileFilter();
		ReflectionTestUtils.setField(profile, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(profile, "mapper", mapper);
		final GatekeeperAccessFilter access = new GatekeeperAccessFilter();
		ReflectionTestUtils.setField(access, "mapper", mapper);

		mvc = MockMvcBuilders.standaloneSetup(api, managementApi, general)
				.setControllerAdvice(new GatekeeperExceptionHandler(), new ArrowheadResponseEntityExceptionHandler())
				// the deployed order: native response (outermost), liveness 10, profile 14, authentication 15, management 25, access 30
				.addFilters(new NativeResponseFilter(), liveness, profile, authentication, managementFilter, access)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	// method, path, authorised status, an authorised caller
	static Stream<Arguments> nativeRoutes() {
		return Stream.of(
				Arguments.of("POST", "/gatekeeper/init_gsd", 200, "DynamicServiceOrchestration"),
				Arguments.of("POST", "/gatekeeper/init_multi_gsd", 200, "SimpleStoreServiceOrchestration"),
				Arguments.of("POST", "/gatekeeper/init_icn", 200, "DynamicServiceOrchestration"),
				Arguments.of("GET", "/gatekeeper/echo", 200, "Gatekeeper"),
				Arguments.of("GET", "/gatekeeper/monitor/echo", 200, "LocalCloudManager"),
				Arguments.of("GET", "/gatekeeper/mgmt/status", 200, "LocalCloudManager"),
				Arguments.of("GET", "/gatekeeper/mgmt/clouds", 200, "LocalCloudManager"),
				Arguments.of("POST", "/gatekeeper/mgmt/clouds", 201, "LocalCloudManager"),
				Arguments.of("DELETE", "/gatekeeper/mgmt/clouds/Company/OtherCloud", 200, "LocalCloudManager"),
				Arguments.of("GET", "/gatekeeper/mgmt/relays", 200, "Sysop"),
				Arguments.of("POST", "/gatekeeper/mgmt/relays", 201, "Sysop"),
				Arguments.of("GET", "/gatekeeper/mgmt/general/status", 200, "LocalCloudManager"));
	}

	//-------------------------------------------------------------------------------------------------
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("nativeRoutes")
	public void nativeRouteKeepsItsStatuses(final String method, final String path, final int authorised, final String caller) throws Exception {
		// authorised (also the system operator)
		mvc.perform(call(method, path, SYSTEM_CERTS.get(caller), caller)).andExpect(status().is(authorised));
		mvc.perform(call(method, path, SYSTEM_CERTS.get("Sysop"), "Sysop")).andExpect(status().is(authorised));

		// unauthenticated: no identity token
		mvc.perform(call(method, path, SYSTEM_CERTS.get(caller), null)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").exists());
		// wrong certificate: the token of the caller presented with another system's certificate
		mvc.perform(call(method, path, SYSTEM_CERTS.get("AnySystem"), caller)).andExpect(status().isUnauthorized());
		// invalid token
		mvc.perform(call(method, path, SYSTEM_CERTS.get(caller), null).header("Authorization", "Bearer IDENTITY-TOKEN//forged")).andExpect(status().isUnauthorized());
		// refused certificate profile (on-boarding), with or without a token
		mvc.perform(call(method, path, ONBOARDING_CERTS.get(caller), caller)).andExpect(status().isForbidden());
		mvc.perform(call(method, path, ONBOARDING_CERTS.get(caller), null)).andExpect(status().isForbidden());
		// no client certificate (the TLS handshake refuses it in deployment)
		mvc.perform(call(method, path, null, caller)).andExpect(status().isUnauthorized());
		// an authenticated system outside the allowlist
		mvc.perform(call(method, path, SYSTEM_CERTS.get("AnySystem"), "AnySystem")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").exists());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void managementPathsAreForLocalCloudManagerAndTheOperatorOnly() throws Exception {
		for (final String caller : List.of("DynamicServiceOrchestration", "SimpleStoreServiceOrchestration", "Gatekeeper")) {
			mvc.perform(call("GET", "/gatekeeper/mgmt/clouds", SYSTEM_CERTS.get(caller), caller)).andExpect(status().isForbidden());
			mvc.perform(call("GET", "/gatekeeper/mgmt/general/status", SYSTEM_CERTS.get(caller), caller)).andExpect(status().isForbidden());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void livenessIsTokenFreeAndDisclosesOnlyStatus() throws Exception {
		for (final String path : List.of("/health", "/certificate-authority/health")) {
			mvc.perform(call("GET", path, SYSTEM_CERTS.get("AnySystem"), null))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("ok"))
					.andExpect(jsonPath("$.system").value("Gatekeeper"))
					.andExpect(jsonPath("$.registration").doesNotExist());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void onlyOrchestratorsManagerAndOperatorMayActForAnotherConsumer() throws Exception {
		final String body = "{\"consumerSystem\":\"SomeConsumer\",\"serviceRequirement\":{\"serviceDefinition\":\"t\"}}";
		mvc.perform(call("POST", "/gatekeeper/init_gsd", SYSTEM_CERTS.get("Gatekeeper"), "Gatekeeper").content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("Caller cannot act for another consumer"));
		mvc.perform(call("POST", "/gatekeeper/init_gsd", SYSTEM_CERTS.get("DynamicServiceOrchestration"), "DynamicServiceOrchestration").content(body)).andExpect(status().isOk());
		mvc.perform(call("POST", "/gatekeeper/init_icn", SYSTEM_CERTS.get("LocalCloudManager"), "LocalCloudManager").content(body)).andExpect(status().isOk());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void nativeErrorsAndHeaders() throws Exception {
		// unknown cloud: 404 {"removed": false}; wrong segment count: 400
		mvc.perform(call("DELETE", "/gatekeeper/mgmt/clouds/Company/Unknown", SYSTEM_CERTS.get("Sysop"), "Sysop"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.removed").value(false));
		mvc.perform(call("DELETE", "/gatekeeper/mgmt/clouds/OnlyOne", SYSTEM_CERTS.get("Sysop"), "Sysop"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("operator/name required"));
		// invalid JSON: 400; response headers of the native implementation
		mvc.perform(call("POST", "/gatekeeper/init_gsd", SYSTEM_CERTS.get("Sysop"), "Sysop").content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid JSON"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
		// more than 1 MiB: 413
		mvc.perform(call("POST", "/gatekeeper/mgmt/clouds", SYSTEM_CERTS.get("Sysop"), "Sysop").content("[" + " ".repeat(1024 * 1024) + "]"))
				.andExpect(status().isPayloadTooLarge());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static MockHttpServletRequestBuilder call(final String method, final String path, final X509Certificate certificate, final String tokenOf) {
		final MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path).secure(true).contentType(MediaType.APPLICATION_JSON);
		if (!"GET".equals(method) && !"DELETE".equals(method)) {
			builder.content("{}");
		}

		if (certificate != null) {
			builder.requestAttr(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[] { certificate, pki.root });
		}

		if (tokenOf != null) {
			builder.header("Authorization", "Bearer IDENTITY-TOKEN//tok-" + tokenOf);
		}

		return builder;
	}
}
