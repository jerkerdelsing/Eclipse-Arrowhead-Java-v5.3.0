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
package eu.arrowhead.authentication.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.method.IAuthenticationMethodService;
import eu.arrowhead.authentication.service.dto.IdentityLoginData;
import eu.arrowhead.authentication.service.validation.IdentityValidation;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.IdentityChangeRequestDTO;
import eu.arrowhead.dto.IdentityLoginResponseDTO;
import eu.arrowhead.dto.IdentityRequestDTO;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class IdentityServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private IdentityService service;

	@Mock
	private IdentityValidation validator;

	@Mock
	private IdentityDbService dbService;

	@Mock
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.loginOperation(null, false, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.loginOperation(null, false, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationInternalServerError1() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.loginOperation(dto, false, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationUnknownSystem() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.empty());

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> service.loginOperation(dto, false, "origin"));

		assertEquals("Invalid name and/or credentials", ex.getMessage());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationAuthenticationMethodNotFound() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.loginOperation(dto, false, "origin"));

		assertEquals("Authentication method implementation not found: PASSWORD", ex.getMessage());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationInternalServerError() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.loginOperation(dto, false, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationExternalServerError() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenThrow(new ExternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> service.loginOperation(dto, false, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationSystemNotVerified() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(false);

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> service.loginOperation(dto, false, "origin"));

		assertEquals("Invalid name and/or credentials", ex.getMessage());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationSystemVerifiedNoSession() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);

		final IdentityLoginData result = service.loginOperation(dto, true, "origin");

		assertNotNull(result);
		assertEquals(dto, result.normalizedRequest());
		assertEquals(system, result.system());
		assertNull(result.response());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationSystemInternalServerError3() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);
		final UUID testToken = UUID.randomUUID();

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);

		try (MockedStatic<UUID> staticMock = Mockito.mockStatic(UUID.class)) {
			staticMock.when(() -> UUID.randomUUID()).thenReturn(testToken);
			when(dbService.createOrUpdateSession(system, testToken.toString())).thenThrow(new InternalServerError("test"));
			doNothing().when(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");

			final ArrowheadException ex = assertThrows(
					InternalServerError.class,
					() -> service.loginOperation(dto, false, "origin"));

			assertEquals("test", ex.getMessage());
			assertEquals("origin", ex.getOrigin());

			verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
			verify(dbService).getSystemByName("TestSystem");
			verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
			verify(methods).method(AuthenticationMethod.PASSWORD);
			verify(methodMock, times(2)).service();
			verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
			staticMock.verify(() -> UUID.randomUUID());
			verify(dbService).createOrUpdateSession(system, testToken.toString());
			verify(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoginOperationSystemVerifiedWithSession() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);
		final UUID testToken = UUID.randomUUID();
		final ZonedDateTime expirationTime = ZonedDateTime.of(2200, 11, 13, 10, 8, 53, 0, ZoneId.of(Constants.UTC));
		final ActiveSession session = new ActiveSession(system, testToken.toString(), Utilities.utcNow(), expirationTime);
		final IdentityLoginResponseDTO response = new IdentityLoginResponseDTO(testToken.toString(), "2200-11-13T10:08:53Z");

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);

		try (MockedStatic<UUID> staticMock = Mockito.mockStatic(UUID.class)) {
			staticMock.when(() -> UUID.randomUUID()).thenReturn(testToken);
			when(dbService.createOrUpdateSession(system, testToken.toString())).thenReturn(session);

			final IdentityLoginData result = service.loginOperation(dto, false, "origin");

			assertNotNull(result);
			assertEquals(dto, result.normalizedRequest());
			assertEquals(system, result.system());
			assertEquals(response, result.response());

			verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
			verify(dbService).getSystemByName("TestSystem");
			verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
			verify(methods).method(AuthenticationMethod.PASSWORD);
			verify(methodMock).service();
			verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
			staticMock.verify(() -> UUID.randomUUID());
			verify(dbService).createOrUpdateSession(system, testToken.toString());
			verify(serviceMock, never()).rollbackCredentialsVerification(any(System.class), anyMap(), anyString());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLogoutOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.logoutOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLogoutOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.logoutOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLogoutOperationInternalServerError() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);
		doThrow(new InternalServerError("test")).when(dbService).removeSession("TestSystem");

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.logoutOperation(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods, times(2)).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
		verify(dbService).removeSession("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLogoutOperationOk() {
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);
		doNothing().when(dbService).removeSession("TestSystem");
		doNothing().when(serviceMock).logout(system, Map.of("password", "123456"));

		assertDoesNotThrow(() -> service.logoutOperation(dto, "origin"));

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods, times(2)).method(AuthenticationMethod.PASSWORD);
		verify(methodMock, times(2)).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
		verify(dbService).removeSession("TestSystem");
		verify(serviceMock).logout(system, Map.of("password", "123456"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.changeOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.changeOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeOperationExceptionDuringValidation() {
		final IdentityChangeRequestDTO changeDto = new IdentityChangeRequestDTO("TestSystem", Map.of("password", "123456"), Map.of("password", "abcdef"));
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);
		when(validator.validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin")).thenThrow(new InvalidParameterException("test", "testOrigin"));
		doNothing().when(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.changeOperation(changeDto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods, times(2)).method(AuthenticationMethod.PASSWORD);
		verify(methodMock, times(2)).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
		verify(validator).validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin");
		verify(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testChangeOperationInvalidParameterExceptionDuringChange() {
		final IdentityChangeRequestDTO changeDto = new IdentityChangeRequestDTO("TestSystem", Map.of("password", "123456"), Map.of("password", "abcdef"));
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);
		when(validator.validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(changeDto);
		doThrow(new InvalidParameterException("test")).when(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		doNothing().when(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.changeOperation(changeDto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods, times(2)).method(AuthenticationMethod.PASSWORD);
		verify(methodMock, times(3)).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
		verify(validator).validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin");
		verify(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		verify(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testChangeOperationInternalServerErrorDuringChange() {
		final IdentityChangeRequestDTO changeDto = new IdentityChangeRequestDTO("TestSystem", Map.of("password", "123456"), Map.of("password", "abcdef"));
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);
		when(validator.validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(changeDto);
		doThrow(new InternalServerError("test")).when(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		doNothing().when(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.changeOperation(changeDto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods, times(2)).method(AuthenticationMethod.PASSWORD);
		verify(methodMock, times(3)).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
		verify(validator).validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin");
		verify(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		verify(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testChangeOperationExternalServerErrorDuringChange() {
		final IdentityChangeRequestDTO changeDto = new IdentityChangeRequestDTO("TestSystem", Map.of("password", "123456"), Map.of("password", "abcdef"));
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);
		when(validator.validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(changeDto);
		doThrow(new ExternalServerError("test")).when(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		doNothing().when(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> service.changeOperation(changeDto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods, times(2)).method(AuthenticationMethod.PASSWORD);
		verify(methodMock, times(3)).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
		verify(validator).validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin");
		verify(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		verify(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testChangeOperationExceptionThatShouldNeverHappenDuringChange() {
		final IdentityChangeRequestDTO changeDto = new IdentityChangeRequestDTO("TestSystem", Map.of("password", "123456"), Map.of("password", "abcdef"));
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);
		when(validator.validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(changeDto);
		doThrow(new ArrowheadException("test")).when(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		doNothing().when(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");

		final ArrowheadException ex = assertThrows(
				ArrowheadException.class,
				() -> service.changeOperation(changeDto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods, times(2)).method(AuthenticationMethod.PASSWORD);
		verify(methodMock, times(3)).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
		verify(validator).validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin");
		verify(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		verify(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "123456"), "test");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testChangeOperationOk() {
		final IdentityChangeRequestDTO changeDto = new IdentityChangeRequestDTO("TestSystem", Map.of("password", "123456"), Map.of("password", "abcdef"));
		final IdentityRequestDTO dto = new IdentityRequestDTO("TestSystem", Map.of("password", "123456"));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodService serviceMock = Mockito.mock(IAuthenticationMethodService.class);

		when(validator.validateAndNormalizeLoginServicePhase1(dto, "origin")).thenReturn(dto);
		when(dbService.getSystemByName("TestSystem")).thenReturn(Optional.of(system));
		when(validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(dto);
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.service()).thenReturn(serviceMock);
		when(serviceMock.verifyCredentials(system, Map.of("password", "123456"))).thenReturn(true);
		when(validator.validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin")).thenReturn(changeDto);
		doNothing().when(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		doNothing().when(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "abcdef"), "Credentials changed");

		assertDoesNotThrow(() -> service.changeOperation(changeDto, "origin"));

		verify(validator).validateAndNormalizeLoginServicePhase1(dto, "origin");
		verify(dbService).getSystemByName("TestSystem");
		verify(validator).validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");
		verify(methods, times(2)).method(AuthenticationMethod.PASSWORD);
		verify(methodMock, times(3)).service();
		verify(serviceMock).verifyCredentials(system, Map.of("password", "123456"));
		verify(validator).validateAndNormalizeChangeServicePhase2(changeDto, AuthenticationMethod.PASSWORD, "origin");
		verify(serviceMock).changeCredentials(system, Map.of("password", "123456"), Map.of("password", "abcdef"));
		verify(serviceMock).rollbackCredentialsVerification(system, Map.of("password", "abcdef"), "Credentials changed");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.verifyOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.verifyOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyOperationInternalServerError() {
		when(validator.validateAndNormalizeIdentityToken("token", "origin")).thenReturn("token");
		when(dbService.getSessionByToken("token")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.verifyOperation("token", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeIdentityToken("token", "origin");
		verify(dbService).getSessionByToken("token");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyOperationNoSession() {
		final IdentityVerifyResponseDTO response = new IdentityVerifyResponseDTO(false, null, null, null, null);

		when(validator.validateAndNormalizeIdentityToken("token", "origin")).thenReturn("token");
		when(dbService.getSessionByToken("token")).thenReturn(Optional.empty());

		final IdentityVerifyResponseDTO result = service.verifyOperation("token", "origin");

		assertEquals(response, result);

		verify(validator).validateAndNormalizeIdentityToken("token", "origin");
		verify(dbService).getSessionByToken("token");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyOperationOk() {
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final ActiveSession session = new ActiveSession(
				system,
				"token",
				ZonedDateTime.of(2025, 11, 14, 10, 12, 14, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2200, 11, 14, 10, 12, 14, 0, ZoneId.of(Constants.UTC)));
		final IdentityVerifyResponseDTO response = new IdentityVerifyResponseDTO(
				true,
				"TestSystem",
				false,
				"2025-11-14T10:12:14Z",
				"2200-11-14T10:12:14Z");

		when(validator.validateAndNormalizeIdentityToken("token", "origin")).thenReturn("token");
		when(dbService.getSessionByToken("token")).thenReturn(Optional.of(session));

		final IdentityVerifyResponseDTO result = service.verifyOperation("token", "origin");

		assertEquals(response, result);

		verify(validator).validateAndNormalizeIdentityToken("token", "origin");
		verify(dbService).getSessionByToken("token");
	}
}