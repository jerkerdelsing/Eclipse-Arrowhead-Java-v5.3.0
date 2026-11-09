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
package eu.arrowhead.authentication.http.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthenticationMethod;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
public class InternalAuthenticationFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private InternalAuthenticationFilter filter = new InternalAuthenticationFilterTestHelper(); // this is the trick

	@Mock
	private IdentityDbService dbService;

	@Mock
	private FilterChain chain;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalNoNeedForTokenCheck() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/identity/login");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalAuthHeaderNull() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("No authorization header has been provided", ex.getMessage());

		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalAuthHeaderEmpty() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");
		request.addHeader("Authorization", "");

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("No authorization header has been provided", ex.getMessage());

		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalAuthHeaderInvalid1() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");
		request.addHeader("Authorization", "SYSTEM//wrong");

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("Invalid authorization header", ex.getMessage());

		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalAuthHeaderInvalid2() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");
		request.addHeader("Authorization", "WrongPrefix SYSTEM//wrong");

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("Invalid authorization header", ex.getMessage());

		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalAuthHeaderInvalid3() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");
		request.addHeader("Authorization", "Bearer SYSTEM|wrong");

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("Invalid authorization header", ex.getMessage());

		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalAuthHeaderInvalid4() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");
		request.addHeader("Authorization", "Bearer SYSTEM//wrong");

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("Invalid authorization header", ex.getMessage());

		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalSessionProblem() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");
		request.addHeader("Authorization", "Bearer IDENTITY-TOKEN//token");

		when(dbService.getSessionByToken("token")).thenThrow(InternalServerError.class);

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("Session problem", ex.getMessage());

		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalNoSession() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");
		request.addHeader("Authorization", "Bearer IDENTITY-TOKEN//token");

		when(dbService.getSessionByToken("token")).thenReturn(Optional.empty());

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("Invalid identity token", ex.getMessage());

		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalExpiredSession() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");
		request.addHeader("Authorization", "Bearer IDENTITY-TOKEN//token");
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, true, "AdminSystem");
		final ActiveSession session = new ActiveSession(
				sys,
				"token",
				ZonedDateTime.of(2025, 11, 21, 8, 15, 13, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2025, 11, 21, 8, 20, 13, 0, ZoneId.of(Constants.UTC)));

		when(dbService.getSessionByToken("token")).thenReturn(Optional.of(session));

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		assertEquals("Invalid identity token", ex.getMessage());

		verify(chain, never()).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoFilterInternalOk() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8444);
		request.setRequestURI("/authentication/mgmt/identities");
		request.addHeader("Authorization", "Bearer IDENTITY-TOKEN//token");
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, true, "AdminSystem");
		final ActiveSession session = new ActiveSession(
				sys,
				"token",
				ZonedDateTime.of(2025, 11, 21, 8, 15, 13, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2125, 11, 21, 8, 20, 13, 0, ZoneId.of(Constants.UTC))); // this only works for 100 years

		when(dbService.getSessionByToken("token")).thenReturn(Optional.of(session));

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));
		assertEquals("TestSystem", request.getAttribute("arrowhead.authenticated.system"));
		assertTrue(Boolean.valueOf(request.getAttribute("arrowhead.sysop.request").toString()));

		verify(chain).doFilter(request, null);
	}
}