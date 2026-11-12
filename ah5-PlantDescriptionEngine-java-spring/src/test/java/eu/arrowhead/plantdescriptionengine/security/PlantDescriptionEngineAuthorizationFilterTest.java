/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.plantdescriptionengine.PlantDescriptionEngineConstants;

// Release 5.3.0 negative-access review: the plantDescriptionMonitor operations refuse an unauthenticated caller (401),
// a caller whose certificate belongs to another system (401) and a caller without a ConsumerAuthorization grant (403).
public class PlantDescriptionEngineAuthorizationFilterTest {

	private static final String QUERY = "/plantdescriptionengine/monitor/plant-descriptions";

	private PlantDescriptionEngineAuthorizationFilter filter;
	private PlantDescriptionEngineAuthorizationService authorization;
	private SystemInfo sysInfo;

	@BeforeEach
	public void setUp() {
		authorization = mock(PlantDescriptionEngineAuthorizationService.class);
		when(authorization.isAuthorized("Operator", PlantDescriptionEngineConstants.SERVICE_DEF_PLANT_DESCRIPTION_MONITOR,
				PlantDescriptionEngineConstants.SERVICE_OP_QUERY_PLANT_DESCRIPTIONS)).thenReturn(true);
		sysInfo = mock(SystemInfo.class);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		filter = new PlantDescriptionEngineAuthorizationFilter();
		ReflectionTestUtils.setField(filter, "authorizationService", authorization);
		ReflectionTestUtils.setField(filter, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(filter, "mapper", new ObjectMapper());
	}

	@Test
	public void grantedSystemWithItsOwnCertificatePasses() throws Exception {
		assertEquals(200, run("Operator", "Operator", "GET", QUERY));
	}

	@Test
	public void callerWithoutAGrantIsForbidden() throws Exception {
		assertEquals(403, run("Intruder", "Intruder", "GET", QUERY));
	}

	@Test
	public void unauthenticatedCallerIsRefused() throws Exception {
		assertEquals(401, run(null, "Operator", "GET", QUERY));
		verify(authorization, never()).isAuthorized(anyString(), anyString(), anyString());
	}

	@Test
	public void certificateOfAnotherSystemIsRefused() throws Exception {
		assertEquals(401, run("Operator", "Intruder", "GET", QUERY));
		assertEquals(401, run("Operator", null, "GET", QUERY));
		verify(authorization, never()).isAuthorized(anyString(), anyString(), anyString());
	}

	@Test
	public void withoutSslTheCertificateIsNotCompared() throws Exception {
		when(sysInfo.isSslEnabled()).thenReturn(false);
		assertEquals(200, run("Operator", null, "GET", QUERY));
		assertEquals(403, run("Intruder", null, "GET", QUERY));
	}

	@Test
	public void pathsOutsideTheMonitorServiceAreLeftToOtherFilters() throws Exception {
		assertEquals(200, run(null, null, "GET", "/plantdescriptionengine/mgmt/plant-descriptions"));
		verify(authorization, never()).isAuthorized(anyString(), anyString(), anyString());
	}

	@Test
	public void systemNameIsTheFirstLabelOfTheCommonName() {
		assertEquals("Operator", PlantDescriptionEngineAuthorizationFilter.systemNameFromSubject("CN=Operator.TestCloud.Company.arrowhead.eu,O=Company"));
		assertNull(PlantDescriptionEngineAuthorizationFilter.systemNameFromSubject("O=Company"));
		assertNull(PlantDescriptionEngineAuthorizationFilter.systemNameFromSubject("not a DN"));
	}

	private int run(final String system, final String certificateSystem, final String method, final String path) throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		if (system != null) {
			request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, system);
		}
		if (certificateSystem != null) {
			final X509Certificate certificate = mock(X509Certificate.class);
			when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal("CN=" + certificateSystem + ".TestCloud.Company.arrowhead.eu"));
			request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[] { certificate });
		}
		final MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response.getStatus();
	}
}
