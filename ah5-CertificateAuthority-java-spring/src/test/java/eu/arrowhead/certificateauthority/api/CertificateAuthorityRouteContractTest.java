/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

import eu.arrowhead.certificateauthority.api.http.CertificateAuthorityAPI;
import eu.arrowhead.certificateauthority.api.http.GeneralManagementAPI;
import eu.arrowhead.certificateauthority.service.CertificateAuthorityStatusService;
import eu.arrowhead.certificateauthority.service.CertificateManagementService;
import eu.arrowhead.certificateauthority.service.CertificateSigningService;
import eu.arrowhead.certificateauthority.service.CertificateValidationService;
import eu.arrowhead.certificateauthority.service.CrlService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadResponseEntityExceptionHandler;

// Every method and path the native 5.2.2 CertificateAuthority served (with and without the base path) is still served, with
// the same success status. Authentication (identity token bound to the client certificate) is the common filter chain's job.
public class CertificateAuthorityRouteContractTest {

	//=================================================================================================
	// members

	private CertificateSigningService signing;
	private CertificateValidationService validation;
	private CertificateManagementService management;
	private CrlService crl;
	private MockMvc mvc;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		signing = mock(CertificateSigningService.class);
		validation = mock(CertificateValidationService.class);
		management = mock(CertificateManagementService.class);
		crl = mock(CrlService.class);
		final CertificateAuthorityStatusService statusService = mock(CertificateAuthorityStatusService.class);

		when(signing.sign(any(), anyString(), anyString())).thenReturn(Map.of("id", 1));
		when(validation.checkCertificate(any(), anyString())).thenReturn(Map.of("status", "GOOD"));
		when(validation.checkTrustedKey(any(), anyString())).thenReturn(Map.of("trusted", true));
		when(crl.crl()).thenReturn("-----BEGIN X509 CRL-----".getBytes());
		when(management.listCertificates(anyString(), anyString())).thenReturn(List.of());
		when(management.listKeys(anyString(), anyString())).thenReturn(List.of());
		when(management.revokeById(anyString(), anyLong(), anyString())).thenReturn(true);
		when(management.revokeByContent(anyString(), any(), anyString())).thenReturn(Map.of("revoked", true));
		when(management.addKey(anyString(), any(), anyString())).thenReturn(3L);
		when(management.deleteKey(anyString(), anyLong(), anyString())).thenReturn(true);
		when(statusService.liveness()).thenReturn(Map.of("status", "ok"));
		when(statusService.status()).thenReturn(Map.of("status", "ok", "system", "CertificateAuthority"));

		final CertificateAuthorityAPI api = new CertificateAuthorityAPI();
		ReflectionTestUtils.setField(api, "signingService", signing);
		ReflectionTestUtils.setField(api, "validationService", validation);
		ReflectionTestUtils.setField(api, "managementService", management);
		ReflectionTestUtils.setField(api, "crlService", crl);
		ReflectionTestUtils.setField(api, "statusService", statusService);

		final GeneralManagementAPI general = new GeneralManagementAPI();
		ReflectionTestUtils.setField(general, "managementService", management);
		ReflectionTestUtils.setField(general, "statusService", statusService);

		mvc = MockMvcBuilders.standaloneSetup(api, general).setControllerAdvice(new ArrowheadResponseEntityExceptionHandler()).build();
	}

	//-------------------------------------------------------------------------------------------------
	static Stream<Arguments> nativeRoutes() {
		final List<Object[]> routes = List.of(
				new Object[] { "POST", "/sign", 201 },
				new Object[] { "POST", "/checkCertificate", 200 },
				new Object[] { "POST", "/checkTrustedKey", 200 },
				new Object[] { "GET", "/crl", 200 },
				new Object[] { "GET", "/mgmt/certificates", 200 },
				new Object[] { "DELETE", "/mgmt/certificates/5", 200 },
				new Object[] { "POST", "/mgmt/revocations", 200 },
				new Object[] { "GET", "/mgmt/keys", 200 },
				new Object[] { "PUT", "/mgmt/keys", 201 },
				new Object[] { "DELETE", "/mgmt/keys/5", 200 },
				new Object[] { "GET", "/health", 200 });

		return Stream.concat(
				routes.stream().map(r -> Arguments.of(r[0], "/certificate-authority" + r[1], r[2])),
				routes.stream().map(r -> Arguments.of(r[0], r[1], r[2])));
	}

	//-------------------------------------------------------------------------------------------------
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("nativeRoutes")
	public void nativeRouteIsServed(final String method, final String path, final int expected) throws Exception {
		mvc.perform(call(method, path, "Sysop")).andExpect(status().is(expected));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void generalManagementStatusKeepsItsNativePath() throws Exception {
		mvc.perform(call("GET", "/certificate-authority/mgmt/general/status", "Sysop"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.system").value("CertificateAuthority"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void unchangedNothingReturns404WithSuccessFalse() throws Exception {
		when(management.deleteKey(anyString(), eq(9L), anyString())).thenReturn(false);
		mvc.perform(call("DELETE", "/certificate-authority/mgmt/keys/9", "Sysop"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void crlIsServedAsPkixCrl() throws Exception {
		mvc.perform(call("GET", "/certificate-authority/crl", "Sysop"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/pkix-crl"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void forbiddenAndUnauthenticatedCallsUseArrowheadErrors() throws Exception {
		doThrow(new ForbiddenException("Caller is not authorized for this operation")).when(management).requireManager(eq("LocalCloudManager"), anyString());
		mvc.perform(call("GET", "/certificate-authority/mgmt/general/status", "LocalCloudManager"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorMessage").value("Caller is not authorized for this operation"));

		// no authenticated system on the request
		mvc.perform(call("POST", "/certificate-authority/sign", null)).andExpect(status().isUnauthorized());
		mvc.perform(call("POST", "/certificate-authority/sign", Constants.UNKNOWN)).andExpect(status().isUnauthorized());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void callerNameReachesTheServices() throws Exception {
		mvc.perform(call("POST", "/sign", "Onboarding")).andExpect(status().isCreated());
		org.mockito.Mockito.verify(signing).sign(any(), eq("Onboarding"), eq("POST /sign"));
		assertEquals(1, org.mockito.Mockito.mockingDetails(signing).getInvocations().size());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static MockHttpServletRequestBuilder call(final String method, final String path, final String caller) {
		final MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON);
		if (!"GET".equals(method) && !"DELETE".equals(method)) {
			builder.content("{}");
		}

		if (caller != null) {
			builder.requestAttr(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, caller);
		}

		return builder;
	}
}
