/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.serviceregistry.api.http.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
public class ServiceLookupFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceLookupFilter filter = new ServiceLookupFilterTestHelper(); // this is the trick

	@Mock
	private ServiceRegistrySystemInfo sysInfo;

	@Mock
	private FilterChain chain;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalTargetMismatch() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/register");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		assertNull(request.getAttribute("restricted.service.lookup"));

		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterException() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");

		when(sysInfo.getServiceDiscoveryPolicy()).thenThrow(new InvalidParameterException("impossible"));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("impossible", ex.getMessage());

		verify(sysInfo).getServiceDiscoveryPolicy();
		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalSysop() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setAttribute("arrowhead.sysop.request", true);
		request.setAttribute("arrowhead.authenticated.system", "TestConsumer");

		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.RESTRICTED);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		assertNotNull(request.getAttribute("restricted.service.lookup"));
		assertFalse(Boolean.valueOf(request.getAttribute("restricted.service.lookup").toString()));

		verify(sysInfo).getServiceDiscoveryPolicy();
		verify(sysInfo, never()).hasClientDirectAccess(anyString());
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalDirectAccessAllowed() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "TestConsumer");

		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.RESTRICTED);
		when(sysInfo.hasClientDirectAccess("TestConsumer")).thenReturn(true);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		assertNotNull(request.getAttribute("restricted.service.lookup"));
		assertFalse(Boolean.valueOf(request.getAttribute("restricted.service.lookup").toString()));

		verify(sysInfo).getServiceDiscoveryPolicy();
		verify(sysInfo).hasClientDirectAccess("TestConsumer");
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalRestrictedAccess() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "TestConsumer");

		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.RESTRICTED);
		when(sysInfo.hasClientDirectAccess("TestConsumer")).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		assertNotNull(request.getAttribute("restricted.service.lookup"));
		assertTrue(Boolean.valueOf(request.getAttribute("restricted.service.lookup").toString()));

		verify(sysInfo).getServiceDiscoveryPolicy();
		verify(sysInfo).hasClientDirectAccess("TestConsumer");
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalPolicyOpen() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");

		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.OPEN);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		assertFalse(Boolean.valueOf(request.getAttribute("restricted.service.lookup").toString()));

		verify(sysInfo).getServiceDiscoveryPolicy();
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalPolicyUnknown() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");

		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(null);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		assertNull(request.getAttribute("restricted.service.lookup"));

		verify(sysInfo).getServiceDiscoveryPolicy();
		verify(chain).doFilter(request, null);
	}
}