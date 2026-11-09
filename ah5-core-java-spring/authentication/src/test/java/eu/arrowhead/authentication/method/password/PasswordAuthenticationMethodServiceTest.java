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
package eu.arrowhead.authentication.method.password;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class PasswordAuthenticationMethodServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private PasswordAuthenticationMethodService service;

	@Mock
	private PasswordEncoder encoder;

	@Mock
	private IdentityDbService identityDbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyCredentialsSystemNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.verifyCredentials(null, null));

		assertEquals("system is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyCredentialsCredentialsNull() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.verifyCredentials(sys, null));

		assertEquals("credentials is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyCredentialsNoPassword() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.verifyCredentials(sys, Map.of()));

		assertEquals("password field is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyCredentialsPasswordEmpty() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.verifyCredentials(sys, Map.of("password", "")));

		assertEquals("password field is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyCredentialsNoRecord() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(identityDbService.getPasswordAuthenticationBySystem(sys)).thenReturn(Optional.empty());

		final boolean result = service.verifyCredentials(sys, Map.of("password", "123456"));

		assertFalse(result);

		verify(identityDbService).getPasswordAuthenticationBySystem(sys);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyCredentialsOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final PasswordAuthentication pa = new PasswordAuthentication(sys, "encoded");

		when(identityDbService.getPasswordAuthenticationBySystem(sys)).thenReturn(Optional.of(pa));
		when(encoder.matches("123456", "encoded")).thenReturn(true);

		final boolean result = service.verifyCredentials(sys, Map.of("password", "123456"));

		assertTrue(result);

		verify(identityDbService).getPasswordAuthenticationBySystem(sys);
		verify(encoder).matches("123456", "encoded");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeCredentialsSystemNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.changeCredentials(null, null, null));

		assertEquals("system is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeCredentialsNewCredentialsNull() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.changeCredentials(sys, null, null));

		assertEquals("newCredentials is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeCredentialsNoNewPassword() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.changeCredentials(sys, Map.of(), Map.of()));

		assertEquals("new password field is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeCredentialsNewPasswordEmpty() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.changeCredentials(sys, Map.of(), Map.of("password", "")));

		assertEquals("new password field is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeCredentialsOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(encoder.encode("new")).thenReturn("newEncoded");
		doNothing().when(identityDbService).changePassword(sys, "newEncoded");

		assertDoesNotThrow(() -> service.changeCredentials(sys, Map.of("password", "old"), Map.of("password", "new")));

		verify(encoder).encode("new");
		verify(identityDbService).changePassword(sys, "newEncoded");
	}
}