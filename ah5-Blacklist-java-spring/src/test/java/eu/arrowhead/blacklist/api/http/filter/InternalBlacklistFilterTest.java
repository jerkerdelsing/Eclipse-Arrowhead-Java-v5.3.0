/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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
package eu.arrowhead.blacklist.api.http.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.blacklist.service.DiscoveryService;
import eu.arrowhead.blacklist.service.normalization.Normalization;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpUtilities;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
public class InternalBlacklistFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private InternalBlacklistFilter filter = new InternalBlacklistFilterTestHelper(); // this is the trick

	@Mock
	private DiscoveryService discoveryService;

	@Mock
	private Normalization normalizer;

	@Mock
	private FilterChain chain;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalUnknownRequester() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/blacklist/test");

		try (MockedStatic<HttpUtilities> staticMock = Mockito.mockStatic(HttpUtilities.class)) {
			staticMock.when(() -> HttpUtilities.acquireName(request, "GET /blacklist/test")).thenThrow(InvalidParameterException.class);

			final ArrowheadException ex = assertThrows(
					AuthException.class,
					() -> filter.doFilterInternal(request, null, chain));

			assertEquals("Unknown requester system", ex.getMessage());
			assertEquals("GET /blacklist/test", ex.getOrigin());

			staticMock.verify(() -> HttpUtilities.acquireName(request, "GET /blacklist/test"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalForbidden1() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("");
		request.setAttribute("arrowhead.authenticated.system", "TestSystem");
		request.setAttribute("arrowhead.sysop.request", false);

		when(discoveryService.check("TestSystem", "GET http://localhost")).thenReturn(true);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("TestSystem system is blacklisted", ex.getMessage());
		assertEquals("GET http://localhost", ex.getOrigin());

		verify(discoveryService).check("TestSystem", "GET http://localhost");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalForbidden2() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/blacklist/check/OtherSystem");
		request.setAttribute("arrowhead.authenticated.system", "TestSystem");
		request.setAttribute("arrowhead.sysop.request", false);

		when(discoveryService.check("TestSystem", "GET /blacklist/check/OtherSystem")).thenReturn(true);
		when(normalizer.normalizeSystemName("OtherSystem")).thenReturn("OtherSystem");
		when(normalizer.normalizeSystemName("TestSystem")).thenReturn("TestSystem");

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("TestSystem system is blacklisted", ex.getMessage());
		assertEquals("GET /blacklist/check/OtherSystem", ex.getOrigin());

		verify(discoveryService).check("TestSystem", "GET /blacklist/check/OtherSystem");
		verify(normalizer).normalizeSystemName("OtherSystem");
		verify(normalizer).normalizeSystemName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalOk1() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/blacklist/check/OtherSystem");
		request.setAttribute("arrowhead.authenticated.system", "TestSystem");
		request.setAttribute("arrowhead.sysop.request", true);

		doNothing().when(chain).doFilter(request, null);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(discoveryService, never()).check(anyString(), anyString());
		verify(normalizer, never()).normalizeSystemName(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalOk2() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/blacklist/check/TestSystem");
		request.setAttribute("arrowhead.authenticated.system", "TestSystem");
		request.setAttribute("arrowhead.sysop.request", false);

		doNothing().when(chain).doFilter(request, null);
		when(normalizer.normalizeSystemName("TestSystem")).thenReturn("TestSystem");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(discoveryService, never()).check(anyString(), anyString());
		verify(normalizer, times(2)).normalizeSystemName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalOk3() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/blacklist/lookup");
		request.setAttribute("arrowhead.authenticated.system", "TestSystem");
		request.setAttribute("arrowhead.sysop.request", false);

		doNothing().when(chain).doFilter(request, null);
		when(normalizer.normalizeSystemName("lookup")).thenReturn("Lookup");
		when(normalizer.normalizeSystemName("TestSystem")).thenReturn("TestSystem");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(discoveryService, never()).check(anyString(), anyString());
		verify(normalizer).normalizeSystemName("lookup");
		verify(normalizer).normalizeSystemName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalOk4() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/blacklist/test");
		request.setAttribute("arrowhead.authenticated.system", "TestSystem");
		request.setAttribute("arrowhead.sysop.request", false);

		doNothing().when(chain).doFilter(request, null);
		when(normalizer.normalizeSystemName("test")).thenReturn("Test");
		when(normalizer.normalizeSystemName("TestSystem")).thenReturn("TestSystem");
		when(discoveryService.check("TestSystem", "GET /blacklist/test")).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(discoveryService).check("TestSystem", "GET /blacklist/test");
		verify(normalizer).normalizeSystemName("test");
		verify(normalizer).normalizeSystemName("TestSystem");
	}
}