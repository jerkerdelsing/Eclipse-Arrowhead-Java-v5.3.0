/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.api.http;

import static eu.arrowhead.sysmonitor.service.Json.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.cert.X509Certificate;
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
import eu.arrowhead.sysmonitor.TestCertificates;
import eu.arrowhead.sysmonitor.api.http.filter.CallerProfileFilter;
import eu.arrowhead.sysmonitor.api.http.filter.SysMonitorAccessFilter;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;
import eu.arrowhead.sysmonitor.service.HistoryAuthorizationService;
import eu.arrowhead.sysmonitor.service.MonitoringService;
import eu.arrowhead.sysmonitor.service.StatusService;
import eu.arrowhead.sysmonitor.service.SubscriptionConflictException;

// Every method and path the native 5.2.2 SysMonitor served (taken from its route() and the generalManagement and health
// handlers of its Service base class) behind the deployed filter chain: the common OutsourcedFilter (identity token bound
// to the client certificate), the caller-profile and access filters of this system and the common management filter.
// Each path is called unauthenticated, with a wrong certificate and authorised.
public class SysMonitorRouteContractTest {

	//=================================================================================================
	// members

	private static TestCertificates pki;
	private static X509Certificate localCloudManager;
	private static X509Certificate sysop;
	private static X509Certificate anySystem;
	private static X509Certificate onboardingProfile;

	private MonitorDbService store;
	private MonitoringService monitoring;
	private HistoryAuthorizationService authorization;
	private MockMvc mvc;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeAll
	public static void certificates() throws Exception {
		pki = new TestCertificates();
		localCloudManager = pki.issue("LocalCloudManager", "sy");
		sysop = pki.issue("Sysop", "op");
		anySystem = pki.issue("AnySystem", "sy");
		onboardingProfile = pki.issue("LocalCloudManager", "on");
	}

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		final SystemInfo sysInfo = mock(SystemInfo.class);
		when(sysInfo.getSystemName()).thenReturn("SysMonitor");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("LocalCloudManager"));

		// Authentication's identity-verify: token -> system
		final ArrowheadHttpService http = mock(ArrowheadHttpService.class);
		when(http.consumeService(eq(Constants.SERVICE_DEF_IDENTITY), eq(Constants.SERVICE_OP_IDENTITY_VERIFY), eq(IdentityVerifyResponseDTO.class), anyList())).thenAnswer(call -> {
			final String token = ((List<?>) call.getArgument(3)).get(0).toString();
			return switch (token) {
			case "lcm-token" -> new IdentityVerifyResponseDTO(true, "LocalCloudManager", false, null, null);
			case "sysop-token" -> new IdentityVerifyResponseDTO(true, "Sysop", true, null, null);
			case "any-token" -> new IdentityVerifyResponseDTO(true, "AnySystem", false, null, null);
			default -> new IdentityVerifyResponseDTO(false, null, null, null, null);
			};
		});

		final SystemNameNormalizer normalizer = new SystemNameNormalizer();
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);

		final OutsourcedFilter authentication = new OutsourcedFilter();
		ReflectionTestUtils.setField(authentication, "mapper", mapper);
		ReflectionTestUtils.setField(authentication, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(authentication, "collector", mock(ServiceCollector.class));
		ReflectionTestUtils.setField(authentication, "httpService", http);
		ReflectionTestUtils.setField(authentication, "systemNameNormalizer", normalizer);

		final CallerProfileFilter profile = new CallerProfileFilter();
		ReflectionTestUtils.setField(profile, "mapper", mapper);
		ReflectionTestUtils.setField(profile, "sysInfo", sysInfo);

		authorization = mock(HistoryAuthorizationService.class);
		when(authorization.isAuthorized(anyString(), anyString())).thenReturn(true);
		final SysMonitorAccessFilter access = new SysMonitorAccessFilter();
		ReflectionTestUtils.setField(access, "mapper", mapper);
		ReflectionTestUtils.setField(access, "authorizationService", authorization);
		ReflectionTestUtils.setField(access, "authorizationEnabled", true);

		final ManagementServiceFilter management = new ManagementServiceFilter();
		ReflectionTestUtils.setField(management, "mapper", mapper);
		ReflectionTestUtils.setField(management, "sysInfo", sysInfo);

		store = mock(MonitorDbService.class);
		when(store.systems()).thenReturn(List.of(map("systemName", "Pump", "observationCount", 1L, "firstObservationTime", "t", "lastObservationTime", "t")));
		when(store.latest("Pump")).thenReturn(Map.of("echo", map("observationId", "1")));
		when(store.history(anyString(), anyList(), any(), any(), anyInt())).thenReturn(List.of(map("observationId", "1")));
		when(store.put(anyString(), anyList(), anyBoolean(), anyString(), any())).thenReturn(map("providerSystem", "Pump", "observationTypes", List.of("*"), "enabled", true,
				"revision", 3, "updatedAt", "t", "updatedBy", "LocalCloudManager"));
		when(store.delete("Pump")).thenReturn(true);

		monitoring = mock(MonitoringService.class);
		when(monitoring.getDefaultSubscription()).thenReturn("all");
		when(monitoring.getRetentionDays()).thenReturn(30.0);
		when(monitoring.providersView()).thenReturn(List.of());
		when(monitoring.subscriptionsView()).thenReturn(List.of());

		final StatusService statusService = mock(StatusService.class);
		when(statusService.healthCode()).thenReturn(200);
		when(statusService.liveness()).thenReturn(map("status", "ok", "system", "SysMonitor"));
		when(statusService.status()).thenReturn(map("status", "running", "system", "SysMonitor"));

		final HistoryAPI history = new HistoryAPI();
		ReflectionTestUtils.setField(history, "store", store);
		ReflectionTestUtils.setField(history, "monitoring", monitoring);
		final ManagementAPI managementApi = new ManagementAPI();
		ReflectionTestUtils.setField(managementApi, "store", store);
		ReflectionTestUtils.setField(managementApi, "monitoring", monitoring);
		ReflectionTestUtils.setField(managementApi, "statusService", statusService);
		final GeneralManagementAPI general = new GeneralManagementAPI();
		ReflectionTestUtils.setField(general, "logService", mock(LogService.class));
		ReflectionTestUtils.setField(general, "configService", mock(ConfigService.class));
		ReflectionTestUtils.setField(general, "statusService", statusService);
		final HealthAPI health = new HealthAPI();
		ReflectionTestUtils.setField(health, "statusService", statusService);

		mvc = MockMvcBuilders.standaloneSetup(history, managementApi, general, health)
				.setControllerAdvice(new ArrowheadResponseEntityExceptionHandler())
				.addFilters(authentication, profile, access, management)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	// method, path, status of an authorised call (LocalCloudManager), status for an ordinary consumer with a valid identity
	static Stream<Arguments> nativeRoutes() {
		return Stream.of(
				Arguments.of("GET", "/health", 200, 200),
				Arguments.of("GET", "/sysmonitor/history/systems", 200, 200),
				Arguments.of("GET", "/sysmonitor/history/systems/Pump/latest", 200, 200),
				Arguments.of("GET", "/sysmonitor/history/systems/Pump/observations", 200, 200),
				Arguments.of("GET", "/sysmonitor/management/status", 200, 403),
				Arguments.of("GET", "/sysmonitor/management/providers", 200, 403),
				Arguments.of("GET", "/sysmonitor/management/subscriptions", 200, 403),
				Arguments.of("PUT", "/sysmonitor/management/subscriptions/Pump", 200, 403),
				Arguments.of("DELETE", "/sysmonitor/management/subscriptions/Pump", 204, 403),
				Arguments.of("POST", "/sysmonitor/management/reconcile", 202, 403),
				Arguments.of("GET", "/sysmonitor/mgmt/general/status", 200, 403));
	}

	//-------------------------------------------------------------------------------------------------
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("nativeRoutes")
	public void unauthenticatedCallIsRefused(final String method, final String path, final int authorised, final int consumer) throws Exception {
		mvc.perform(call(method, path, localCloudManager, null)).andExpect(status().isUnauthorized());
		mvc.perform(call(method, path, localCloudManager, "unknown-token")).andExpect(status().isUnauthorized());
	}

	//-------------------------------------------------------------------------------------------------
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("nativeRoutes")
	public void wrongCertificateIsRefused(final String method, final String path, final int authorised, final int consumer) throws Exception {
		// token of LocalCloudManager presented with another system's certificate
		mvc.perform(call(method, path, anySystem, "lcm-token")).andExpect(status().isUnauthorized());
		// matching name, but an on-boarding certificate
		mvc.perform(call(method, path, onboardingProfile, "lcm-token")).andExpect(status().isForbidden());
	}

	//-------------------------------------------------------------------------------------------------
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("nativeRoutes")
	public void authorisedCallIsServed(final String method, final String path, final int authorised, final int consumer) throws Exception {
		mvc.perform(call(method, path, localCloudManager, "lcm-token")).andExpect(status().is(authorised));
		mvc.perform(call(method, path, sysop, "sysop-token")).andExpect(status().is(authorised));
		mvc.perform(call(method, path, anySystem, "any-token")).andExpect(status().is(consumer));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void historyIsDecidedByConsumerAuthorization() throws Exception {
		when(authorization.isAuthorized("AnySystem", "query")).thenReturn(false);
		mvc.perform(call("GET", "/sysmonitor/history/systems/Pump/observations", anySystem, "any-token")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorMessage").value("History operation not authorized"));
		mvc.perform(call("GET", "/sysmonitor/history/systems/Pump/latest", anySystem, "any-token")).andExpect(status().isOk());
		verify(authorization).isAuthorized("AnySystem", "latest");
		// not a history operation: no authorization decision, 404
		mvc.perform(call("GET", "/sysmonitor/history/other", anySystem, "any-token")).andExpect(status().isNotFound());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void responseShapes() throws Exception {
		mvc.perform(call("GET", "/health", anySystem, "any-token")).andExpect(jsonPath("$.status").value("ok")).andExpect(jsonPath("$.system").value("SysMonitor"));
		mvc.perform(call("GET", "/sysmonitor/history/systems", anySystem, "any-token"))
				.andExpect(jsonPath("$.systems[0].systemName").value("Pump"))
				.andExpect(jsonPath("$.retentionDays").value(30.0));
		mvc.perform(call("GET", "/sysmonitor/history/systems/Pump/latest", anySystem, "any-token"))
				.andExpect(jsonPath("$.systemName").value("Pump"))
				.andExpect(jsonPath("$.latest.echo.observationId").value("1"));
		mvc.perform(call("GET", "/sysmonitor/history/systems/Pump/observations?count=5&type=echo,status&type=power&from=2026-10-15T12:00:00Z", anySystem, "any-token"))
				.andExpect(jsonPath("$.systemName").value("Pump"))
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.observations[0].observationId").value("1"));
		verify(store).history(eq("Pump"), eq(List.of("echo", "status", "power")), eq(1792065600.0), isNull(), eq(5));
		mvc.perform(call("GET", "/sysmonitor/management/providers", localCloudManager, "lcm-token"))
				.andExpect(jsonPath("$.defaultSubscription").value("all"))
				.andExpect(jsonPath("$.list").isArray());
		mvc.perform(call("GET", "/sysmonitor/management/subscriptions", localCloudManager, "lcm-token"))
				.andExpect(jsonPath("$.defaultSubscription").value("all"))
				.andExpect(jsonPath("$.list").isArray());
		mvc.perform(call("PUT", "/sysmonitor/management/subscriptions/Pump", localCloudManager, "lcm-token").header("If-Match", "\"2\""))
				.andExpect(header().string("ETag", "3"))
				.andExpect(jsonPath("$.revision").value(3));
		verify(store).put("Pump", List.of("*"), true, "LocalCloudManager", 2);
		mvc.perform(call("POST", "/sysmonitor/management/reconcile", localCloudManager, "lcm-token")).andExpect(jsonPath("$.jobId").exists());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void errorStatuses() throws Exception {
		when(store.latest("Empty")).thenReturn(Map.of());
		mvc.perform(call("GET", "/sysmonitor/history/systems/Empty/latest", anySystem, "any-token")).andExpect(status().isNotFound());
		mvc.perform(call("GET", "/sysmonitor/history/systems/Pump/observations?count=0", anySystem, "any-token")).andExpect(status().isBadRequest());
		mvc.perform(call("GET", "/sysmonitor/history/systems/Pump/observations?count=1001", anySystem, "any-token")).andExpect(status().isBadRequest());
		mvc.perform(call("GET", "/sysmonitor/history/systems/Pump/observations?from=yesterday", anySystem, "any-token")).andExpect(status().isBadRequest());
		when(store.delete("Unknown")).thenReturn(false);
		mvc.perform(call("DELETE", "/sysmonitor/management/subscriptions/Unknown", localCloudManager, "lcm-token")).andExpect(status().isNotFound());
		when(store.put(eq("Conflict"), anyList(), anyBoolean(), anyString(), any())).thenThrow(new SubscriptionConflictException("Subscription revision conflict"));
		mvc.perform(call("PUT", "/sysmonitor/management/subscriptions/Conflict", localCloudManager, "lcm-token").header("If-Match", "1"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorMessage").value("Subscription revision conflict"));
		when(store.put(eq("Bad.Name"), anyList(), anyBoolean(), anyString(), any())).thenThrow(new IllegalArgumentException("Valid providerSystem required"));
		mvc.perform(call("PUT", "/sysmonitor/management/subscriptions/Bad.Name", localCloudManager, "lcm-token")).andExpect(status().isBadRequest());
		mvc.perform(call("PUT", "/sysmonitor/management/subscriptions/Pump", localCloudManager, "lcm-token").content("not json")).andExpect(status().isBadRequest());
		mvc.perform(call("PUT", "/sysmonitor/management/subscriptions/Pump", localCloudManager, "lcm-token").header("If-Match", "x")).andExpect(status().isBadRequest());
		// unknown management path: 403 for an ordinary consumer, 404 for a manager (as before)
		mvc.perform(call("GET", "/sysmonitor/management/other", anySystem, "any-token")).andExpect(status().isForbidden());
		mvc.perform(call("GET", "/sysmonitor/management/other", localCloudManager, "lcm-token")).andExpect(status().isNotFound());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static MockHttpServletRequestBuilder call(final String method, final String path, final X509Certificate certificate, final String token) {
		final MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path).secure(true).contentType(MediaType.APPLICATION_JSON);
		builder.requestAttr(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[] { certificate });
		if (token != null) {
			builder.header("Authorization", "Bearer IDENTITY-TOKEN//" + token);
		}

		if ("PUT".equals(method)) {
			builder.content("{}");
		}

		return builder;
	}
}
