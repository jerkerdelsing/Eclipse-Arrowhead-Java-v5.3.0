/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;

public class SosAccessFilterTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void mapsEveryApiPathToItsOperation() {
		assertEquals("catalog", SosAccessFilter.operation("GET", "/api/catalog"));
		assertEquals("status", SosAccessFilter.operation("GET", "/api/status"));
		assertEquals("plan", SosAccessFilter.operation("POST", "/api/plan"));
		assertEquals("submit-deployment", SosAccessFilter.operation("POST", "/api/deployments"));
		assertEquals("list-deployments", SosAccessFilter.operation("GET", "/api/deployments"));
		assertEquals("get-deployment", SosAccessFilter.operation("GET", "/api/deployments/" + "a".repeat(64)));
		assertNull(SosAccessFilter.operation("GET", "/api/deployments/a/b"));
		assertNull(SosAccessFilter.operation("GET", "/sosconfigurator/general/mgmt/get-config"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void sysopAndWhitelistPassOthersNeedAGrant() throws Exception {
		final SosAuthorizationService authorization = mock(SosAuthorizationService.class);
		when(authorization.isAuthorized(eq("PlantDescriptionEngine"), eq("sosConfiguration"), eq("status"))).thenReturn(true);
		final SosAccessFilter filter = new SosAccessFilter("LocalCloudManager");
		ReflectionTestUtils.setField(filter, "authorizationService", authorization);
		ReflectionTestUtils.setField(filter, "authorizationEnabled", true);
		ReflectionTestUtils.setField(filter, "mapper", new ObjectMapper());

		assertEquals(200, run(filter, "Sysop", true, "/api/plan"));
		assertEquals(200, run(filter, "LocalCloudManager", false, "/api/plan"));
		assertEquals(200, run(filter, "PlantDescriptionEngine", false, "/api/status"));
		assertEquals(403, run(filter, "PlantDescriptionEngine", false, "/api/catalog"));
		assertEquals(401, run(filter, Constants.UNKNOWN, false, "/api/catalog"));
		verify(authorization, never()).isAuthorized(eq("Sysop"), anyString(), anyString());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static int run(final SosAccessFilter filter, final String system, final boolean sysop, final String path) throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest(path.equals("/api/plan") ? "POST" : "GET", path);
		request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, system);
		request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST, sysop);
		final MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response.getStatus();
	}
}
