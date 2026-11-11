/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.security;

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

public class SolcAccessFilterTest {

	private static final String ID = "0123abcd-0123-4567-89ab-0123456789ab";

	@Test
	public void mapsEveryApiPathToItsOperation() {
		assertEquals("catalog", SolcAccessFilter.operation("GET", "/api/catalog"));
		assertEquals("list-lc-designs", SolcAccessFilter.operation("GET", "/api/lc-designs"));
		assertEquals("list-designs", SolcAccessFilter.operation("GET", "/api/designs"));
		assertEquals("create-design", SolcAccessFilter.operation("POST", "/api/designs"));
		assertEquals("get-design", SolcAccessFilter.operation("GET", "/api/designs/" + ID));
		assertEquals("update-design", SolcAccessFilter.operation("PUT", "/api/designs/" + ID));
		assertEquals("remove-design", SolcAccessFilter.operation("DELETE", "/api/designs/" + ID));
		assertEquals("plan", SolcAccessFilter.operation("POST", "/api/plan"));
		assertEquals("list-packages", SolcAccessFilter.operation("GET", "/api/packages"));
		assertEquals("build-package", SolcAccessFilter.operation("POST", "/api/packages"));
		assertEquals("get-package", SolcAccessFilter.operation("GET", "/api/packages/" + ID));
		assertEquals("remove-package", SolcAccessFilter.operation("DELETE", "/api/packages/" + ID));
		assertEquals("", SolcAccessFilter.operation("GET", "/api/plan"));
		assertEquals("", SolcAccessFilter.operation("GET", "/api/designs/../x"));
		assertEquals("", SolcAccessFilter.operation("PUT", "/api/packages/" + ID));
		assertNull(SolcAccessFilter.operation("GET", "/solocalcloudconfigurator/general/mgmt/get-config"));
	}

	@Test
	public void sysopAndWhitelistPassOthersNeedAGrant() throws Exception {
		final SolcAuthorizationService authorization = mock(SolcAuthorizationService.class);
		when(authorization.isAuthorized(eq("LocalCloudManager"), eq("soLocalCloudConfiguration"), eq("plan"))).thenReturn(true);
		final SolcAccessFilter filter = new SolcAccessFilter("Operator");
		ReflectionTestUtils.setField(filter, "authorizationService", authorization);
		ReflectionTestUtils.setField(filter, "authorizationEnabled", true);
		ReflectionTestUtils.setField(filter, "mapper", new ObjectMapper());

		assertEquals(200, run(filter, "Sysop", true, "POST", "/api/plan"));
		assertEquals(200, run(filter, "Operator", false, "POST", "/api/plan"));
		assertEquals(200, run(filter, "LocalCloudManager", false, "POST", "/api/plan"));
		assertEquals(403, run(filter, "LocalCloudManager", false, "POST", "/api/packages"));
		assertEquals(403, run(filter, "LocalCloudManager", false, "GET", "/api/unknown"));
		assertEquals(401, run(filter, Constants.UNKNOWN, false, "GET", "/api/catalog"));
		verify(authorization, never()).isAuthorized(eq("Sysop"), anyString(), anyString());
	}

	private static int run(final SolcAccessFilter filter, final String system, final boolean sysop, final String method, final String path) throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, system);
		request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST, sysop);
		final MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response.getStatus();
	}
}
