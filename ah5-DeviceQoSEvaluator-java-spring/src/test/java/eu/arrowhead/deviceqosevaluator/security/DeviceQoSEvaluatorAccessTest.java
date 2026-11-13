/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.deviceqosevaluator.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.authentication.OutsourcedFilter;
import eu.arrowhead.common.http.filter.authorization.ManagementPolicy;
import eu.arrowhead.common.http.filter.authorization.ManagementServiceFilter;
import eu.arrowhead.common.service.normalization.NormalizationMode;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;

/**
 * Negative-access tests for DeviceQoSEvaluator. The system has no access rule of its own: every HTTP request passes the
 * common library's identity-token filter (authentication.policy=outsourced), and its management paths
 * (/deviceqosevaluator/mgmt/... and /deviceqosevaluator/general/mgmt/...) the common management filter. These tests run
 * those filters, in the order the security configuration installs them, with DeviceQoSEvaluator's own SystemInfo,
 * service models and request paths, and check which callers reach the API.
 */
public class DeviceQoSEvaluatorAccessTest {

	//=================================================================================================
	// members

	private static final String FILTER = DeviceQoSEvaluatorConstants.HTTP_API_QUALITY_EVALUATION_PATH + DeviceQoSEvaluatorConstants.HTTP_API_OP_FILTER_PATH;
	private static final String SORT = DeviceQoSEvaluatorConstants.HTTP_API_QUALITY_EVALUATION_PATH + DeviceQoSEvaluatorConstants.HTTP_API_OP_SORT_PATH;
	private static final String QUERY = DeviceQoSEvaluatorConstants.HTTP_API_DEVICE_QUALITY_DATA_MANAGEMENT_PATH + DeviceQoSEvaluatorConstants.HTTP_API_OP_QUERY_PATH;
	private static final String RELOAD = DeviceQoSEvaluatorConstants.HTTP_API_DEVICE_QUALITY_DATA_MANAGEMENT_PATH + DeviceQoSEvaluatorConstants.HTTP_API_OP_RELOAD_PATH;
	private static final String LOGS = DeviceQoSEvaluatorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + Constants.HTTP_API_OP_LOGS_PATH;
	private static final String GET_CONFIG = DeviceQoSEvaluatorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + Constants.HTTP_API_OP_GET_CONFIG_PATH;

	private static final List<String[]> EVALUATION_OPERATIONS = List.of(new String[] { "POST", FILTER }, new String[] { "POST", SORT });
	private static final List<String[]> MANAGEMENT_OPERATIONS = List.of(
			new String[] { "POST", QUERY },
			new String[] { "POST", RELOAD },
			new String[] { "POST", LOGS },
			new String[] { "GET", GET_CONFIG });

	private static final String CONSUMER = "SysMonitor";
	private static final String OPERATOR = "Sysop";

	private final ArrowheadHttpService httpService = mock(ArrowheadHttpService.class);
	private DeviceQoSEvaluatorSystemInfo sysInfo;
	private ServiceOperationNameNormalizer operationNameNormalizer;
	private OutsourcedFilter authenticationFilter;
	private ManagementServiceFilter managementFilter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		sysInfo = spy(new DeviceQoSEvaluatorSystemInfo());
		doReturn(true).when(sysInfo).isSslEnabled();
		doReturn(false).when(sysInfo).isMqttApiEnabled();
		doReturn("device-qos-evaluator").when(sysInfo).getDomainAddress();
		doReturn(8472).when(sysInfo).getServerPort();
		doReturn(List.of()).when(sysInfo).getManagementWhitelist();
		doReturn(ManagementPolicy.SYSOP_ONLY).when(sysInfo).getManagementPolicy(); // config/application.properties

		final SystemNameNormalizer systemNameNormalizer = new SystemNameNormalizer();
		final ServiceDefinitionNameNormalizer serviceDefNameNormalizer = new ServiceDefinitionNameNormalizer();
		operationNameNormalizer = new ServiceOperationNameNormalizer();
		for (final Object normalizer : List.of(systemNameNormalizer, serviceDefNameNormalizer, operationNameNormalizer)) {
			ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);
		}

		final ObjectMapper mapper = new ObjectMapper();

		authenticationFilter = new OutsourcedFilter();
		ReflectionTestUtils.setField(authenticationFilter, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(authenticationFilter, "httpService", httpService);
		ReflectionTestUtils.setField(authenticationFilter, "systemNameNormalizer", systemNameNormalizer);
		ReflectionTestUtils.setField(authenticationFilter, "mapper", mapper);

		managementFilter = new ManagementServiceFilter();
		ReflectionTestUtils.setField(managementFilter, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(managementFilter, "httpService", httpService);
		ReflectionTestUtils.setField(managementFilter, "serviceDefNameNormalizer", serviceDefNameNormalizer);
		ReflectionTestUtils.setField(managementFilter, "operationNameNormalizer", operationNameNormalizer);
		ReflectionTestUtils.setField(managementFilter, "mapper", mapper);

		identity("token-of-sysmonitor", CONSUMER, false);
		identity("token-of-sysop", OPERATOR, true);
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_IDENTITY), eq(Constants.SERVICE_OP_IDENTITY_VERIFY), eq(IdentityVerifyResponseDTO.class), eq(List.of("forged-token"))))
				.thenReturn(new IdentityVerifyResponseDTO(false, null, null, null, null));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void theManagementFilterIsSwitchedOnInThePackagedConfiguration() throws IOException {
		// CommonBeanConfig creates the management filter only when enable.management.filter is true
		final Properties packaged = new Properties();
		try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
			packaged.load(in);
		}

		assertEquals("true", packaged.getProperty(Constants.ENABLE_MANAGEMENT_FILTER));
		// and the management filter runs after authentication, as in call() below
		assertTrue(Constants.REQUEST_FILTER_ORDER_AUTHENTICATION < Constants.REQUEST_FILTER_ORDER_AUTHORIZATION_MGMT_SERVICE);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void everyOperationIsUnauthorizedWithoutAnIdentityToken() throws Exception {
		for (final String[] op : all()) {
			final Result result = call(op[0], op[1], null, CONSUMER);
			assertEquals(HttpStatus.UNAUTHORIZED.value(), result.status(), op[1]);
			assertFalse(result.reachedApi(), op[1]);
		}

		verifyNoInteractions(httpService); // not even the identity service is asked
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void forgedMalformedAndAuthenticatorKeyHeadersAreUnauthorized() throws Exception {
		for (final String[] op : all()) {
			assertEquals(HttpStatus.UNAUTHORIZED.value(), call(op[0], op[1], bearer("forged-token"), CONSUMER).status(), op[1]);
			assertEquals(HttpStatus.UNAUTHORIZED.value(), call(op[0], op[1], "Bearer token-of-sysmonitor", CONSUMER).status(), op[1]);
			assertEquals(HttpStatus.UNAUTHORIZED.value(), call(op[0], op[1], "Basic c3lzb3A6c3lzb3A=", OPERATOR).status(), op[1]);
			// DeviceQoSEvaluator has no authenticator keys: a key header is refused whatever it carries
			assertEquals(HttpStatus.UNAUTHORIZED.value(),
					call(op[0], op[1], Constants.AUTHENTICATION_SCHEMA + " " + Constants.AUTHENTICATION_PREFIX_AUTHENTICATOR_KEY + "//" + OPERATOR + "//anything", OPERATOR).status(),
					op[1]);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void aTokenIsRefusedWithAnotherSystemsCertificateOrWithoutOne() throws Exception {
		for (final String[] op : all()) {
			// the operator's token presented with SysMonitor's certificate
			final Result stolen = call(op[0], op[1], bearer("token-of-sysop"), CONSUMER);
			assertEquals(HttpStatus.UNAUTHORIZED.value(), stolen.status(), op[1]);
			assertFalse(stolen.reachedApi(), op[1]);

			final Result noCertificate = call(op[0], op[1], bearer("token-of-sysmonitor"), null);
			assertEquals(HttpStatus.UNAUTHORIZED.value(), noCertificate.status(), op[1]);
			assertFalse(noCertificate.reachedApi(), op[1]);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void managementIsForbiddenToOrdinarySystemsUnderTheDeployedSysopOnlyPolicy() throws Exception {
		for (final String[] op : MANAGEMENT_OPERATIONS) {
			final Result result = call(op[0], op[1], bearer("token-of-sysmonitor"), CONSUMER);
			assertEquals(HttpStatus.FORBIDDEN.value(), result.status(), op[1]);
			assertFalse(result.reachedApi(), op[1]);

			assertTrue(call(op[0], op[1], bearer("token-of-sysop"), OPERATOR).reachedApi(), op[1]);
		}

		// sysop-only does not ask ConsumerAuthorization, so a grant cannot open management
		verify(httpService, never()).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void managementNeedsAGrantUnderTheAuthorizationPolicyAndFailsClosed() throws Exception {
		doReturn(ManagementPolicy.AUTHORIZATION).when(sysInfo).getManagementPolicy(); // src/main/resources/application.properties

		grant(false);
		for (final String[] op : MANAGEMENT_OPERATIONS) {
			final Result result = call(op[0], op[1], bearer("token-of-sysmonitor"), CONSUMER);
			assertEquals(HttpStatus.FORBIDDEN.value(), result.status(), op[1]);
			assertFalse(result.reachedApi(), op[1]);
		}

		reset(httpService);
		identity("token-of-sysmonitor", CONSUMER, false);
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenThrow(new ExternalServerError("ConsumerAuthorization is unreachable"));
		for (final String[] op : MANAGEMENT_OPERATIONS) {
			assertEquals(HttpStatus.FORBIDDEN.value(), call(op[0], op[1], bearer("token-of-sysmonitor"), CONSUMER).status(), op[1]);
		}

		// a path or method that matches no published operation is refused without asking
		reset(httpService);
		identity("token-of-sysmonitor", CONSUMER, false);
		grant(true);
		assertEquals(HttpStatus.FORBIDDEN.value(), call("GET", QUERY, bearer("token-of-sysmonitor"), CONSUMER).status());
		assertEquals(HttpStatus.FORBIDDEN.value(), call("POST", DeviceQoSEvaluatorConstants.HTTP_API_DEVICE_QUALITY_DATA_MANAGEMENT_PATH + "/unpublished", bearer("token-of-sysmonitor"), CONSUMER).status());
		verify(httpService, never()).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));

		// with a grant the call goes through, and the question asked is about DeviceQoSEvaluator's own service
		assertTrue(call("POST", QUERY, bearer("token-of-sysmonitor"), CONSUMER).reachedApi());
		final ArgumentCaptor<AuthorizationVerifyRequestDTO> asked = ArgumentCaptor.forClass(AuthorizationVerifyRequestDTO.class);
		verify(httpService).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), asked.capture());
		assertEquals(Constants.SYS_NAME_DEVICE_QOS_EVALUATOR, asked.getValue().provider());
		assertEquals(CONSUMER, asked.getValue().consumer());
		assertEquals(Constants.SERVICE_DEF_DEVICE_QUALITY_DATA_MANAGEMENT, asked.getValue().target());
		assertEquals(operationNameNormalizer.normalize(Constants.SERVICE_OP_QUERY), asked.getValue().scope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void qualityEvaluationIsOpenToAuthenticatedSystemsOnly() throws Exception {
		for (final String[] op : EVALUATION_OPERATIONS) {
			assertEquals(HttpStatus.UNAUTHORIZED.value(), call(op[0], op[1], null, CONSUMER).status(), op[1]);
			assertTrue(call(op[0], op[1], bearer("token-of-sysmonitor"), CONSUMER).reachedApi(), op[1]);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private record Result(int status, boolean reachedApi) {
	}

	//-------------------------------------------------------------------------------------------------
	private static List<String[]> all() {
		return java.util.stream.Stream.concat(EVALUATION_OPERATIONS.stream(), MANAGEMENT_OPERATIONS.stream()).toList();
	}

	//-------------------------------------------------------------------------------------------------
	private static String bearer(final String token) {
		return Constants.AUTHENTICATION_SCHEMA + " " + Constants.AUTHENTICATION_PREFIX_IDENTITY_TOKEN + Constants.AUTHENTICATION_KEY_DELIMITER + token;
	}

	//-------------------------------------------------------------------------------------------------
	private void identity(final String token, final String systemName, final boolean sysop) {
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_IDENTITY), eq(Constants.SERVICE_OP_IDENTITY_VERIFY), eq(IdentityVerifyResponseDTO.class), eq(List.of(token))))
				.thenReturn(new IdentityVerifyResponseDTO(true, systemName, sysop, null, null));
	}

	//-------------------------------------------------------------------------------------------------
	private void grant(final boolean granted) {
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenReturn(granted);
	}

	//-------------------------------------------------------------------------------------------------
	private Result call(final String method, final String path, final String authorization, final String certificateSystem) throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setSecure(true);
		request.setScheme("https");
		request.setServerName("device-qos-evaluator");
		request.setServerPort(8472);
		request.setContentType("application/json");
		request.setContent("{}".getBytes());
		if (authorization != null) {
			request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
		}
		if (certificateSystem != null) {
			final X509Certificate certificate = mock(X509Certificate.class);
			when(certificate.getSubjectX500Principal())
					.thenReturn(new X500Principal("CN=" + certificateSystem + ".TestCloud.Company.arrowhead.eu, OID.2.5.4.46=sy, O=Company"));
			request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[] { certificate });
		}

		final AtomicBoolean reached = new AtomicBoolean(false);
		final HttpServlet api = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			public void service(final ServletRequest req, final ServletResponse res) {
				reached.set(true);
			}
		};

		final MockHttpServletResponse response = new MockHttpServletResponse();
		new MockFilterChain(api, authenticationFilter, managementFilter).doFilter(request, response);

		return new Result(response.getStatus(), reached.get());
	}
}
