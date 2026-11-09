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
package eu.arrowhead.common.http.filter.authentication;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;

@ExtendWith(MockitoExtension.class)
public class SelfDeclaredFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private final SelfDeclaredFilter filter = new SelfDeclaredFilterTestHelper(); // this is the trick

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private FilterChain chain;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalMissingAuthHeader() throws IOException, ServletException {
		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(new MockHttpServletRequest(), null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("No authorization header has been provided", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalEmptyAuthHeader() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, " ");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("No authorization header has been provided", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalAuthHeaderTooShort() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "oneword");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalAuthHeaderTooLong() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "first second third");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalAuthHeaderInvalidSchema() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "first second");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalAuthHeaderContentTooShort() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer systemName");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalAuthHeaderContentTooLong() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer prefix//systemName//other");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalAuthHeaderContentInvalidPrefix() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer prefix//systemName");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalOkSystem() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer SYSTEM//systemName");

		when(systemNameNormalizer.normalize("systemName")).thenReturn("SystemName");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("systemName");
		verify(chain).doFilter(any(ServletRequest.class), isNull());

		assertAll("doFilterInternal - ok (system)",
				() -> assertNotNull(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM)),
				() -> assertEquals("SystemName", request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM).toString()),
				() -> assertNotNull(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST)),
				() -> assertFalse((boolean) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalOkSysop() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer SYSTEM//sysop");

		when(systemNameNormalizer.normalize("sysop")).thenReturn("Sysop");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("sysop");
		verify(chain).doFilter(any(ServletRequest.class), isNull());

		assertAll("doFilterInternal - ok (system)",
				() -> assertNotNull(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM)),
				() -> assertEquals("Sysop", request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM).toString()),
				() -> assertNotNull(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST)),
				() -> assertTrue((boolean) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST)));
	}
}