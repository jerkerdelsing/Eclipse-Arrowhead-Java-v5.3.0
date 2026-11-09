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
package eu.arrowhead.authentication.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.method.IAuthenticationMethodInputNormalizer;
import eu.arrowhead.authentication.method.IAuthenticationMethodInputValidator;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.IdentityChangeRequestDTO;
import eu.arrowhead.dto.IdentityRequestDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class IdentityValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private IdentityValidation validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase1OriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeLoginServicePhase1(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase1OriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeLoginServicePhase1(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase1DtoNull() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeLoginServicePhase1(null, "origin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase1SystemNameNull() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				null,
				null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeLoginServicePhase1(dto, "origin"));

		assertEquals("System name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase1SystemNameEmpty() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"",
				null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeLoginServicePhase1(dto, "origin"));

		assertEquals("System name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase1SystemNameInvalid() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"1SystemName",
				Map.of("key", "value"));

		when(systemNameNormalizer.normalize("1SystemName")).thenReturn("1SystemName");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1SystemName");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeLoginServicePhase1(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("1SystemName");
		verify(systemNameValidator).validateSystemName("1SystemName");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase1Ok() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"system-name",
				Map.of("key", "value"));

		when(systemNameNormalizer.normalize("system-name")).thenReturn("SystemName");
		doNothing().when(systemNameValidator).validateSystemName("SystemName");

		final IdentityRequestDTO result = validator.validateAndNormalizeLoginServicePhase1(dto, "origin");

		assertEquals("SystemName", result.systemName());
		assertEquals(Map.of("key", "value"), result.credentials());

		verify(systemNameNormalizer).normalize("system-name");
		verify(systemNameValidator).validateSystemName("SystemName");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase2DTONull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeLoginServicePhase2(null, null, null));

		assertEquals("dto is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase2AuthenticationMethodNull() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"SystemName",
				Map.of("key", "value"));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeLoginServicePhase2(dto, null, null));

		assertEquals("authentication method is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase2OriginNull() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"SystemName",
				Map.of("key", "value"));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase2OriginEmpty() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"SystemName",
				Map.of("key", "value"));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase2UnsupportedAuthenticationMethod() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"SystemName",
				Map.of("key", "value"));

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(null);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin"));

		assertEquals("Unsupported authentication method: PASSWORD", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase2InternalServerError() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"SystemName",
				Map.of("key", "value"));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodInputNormalizer normalizerMock = Mockito.mock(IAuthenticationMethodInputNormalizer.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.normalizer()).thenReturn(normalizerMock);
		when(normalizerMock.normalizeCredentials(Map.of("key", "value"))).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).normalizer();
		verify(normalizerMock).normalizeCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase2InvalidParameterException() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"SystemName",
				Map.of("key", "value"));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodInputNormalizer normalizerMock = Mockito.mock(IAuthenticationMethodInputNormalizer.class);
		final IAuthenticationMethodInputValidator validatorMock = Mockito.mock(IAuthenticationMethodInputValidator.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.normalizer()).thenReturn(normalizerMock);
		when(normalizerMock.normalizeCredentials(Map.of("key", "value"))).thenReturn(Map.of("key", "value"));
		when(methodMock.validator()).thenReturn(validatorMock);
		doThrow(new InvalidParameterException("test")).when(validatorMock).validateCredentials(Map.of("key", "value"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).normalizer();
		verify(normalizerMock).normalizeCredentials(Map.of("key", "value"));
		verify(methodMock).validator();
		verify(validatorMock).validateCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLoginServicePhase2Ok() {
		final IdentityRequestDTO dto = new IdentityRequestDTO(
				"SystemName",
				Map.of("key", "value"));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodInputNormalizer normalizerMock = Mockito.mock(IAuthenticationMethodInputNormalizer.class);
		final IAuthenticationMethodInputValidator validatorMock = Mockito.mock(IAuthenticationMethodInputValidator.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.normalizer()).thenReturn(normalizerMock);
		when(normalizerMock.normalizeCredentials(Map.of("key", "value"))).thenReturn(Map.of("key", "value"));
		when(methodMock.validator()).thenReturn(validatorMock);
		doNothing().when(validatorMock).validateCredentials(Map.of("key", "value"));

		final IdentityRequestDTO result = validator.validateAndNormalizeLoginServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");

		assertEquals(dto, result);

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).normalizer();
		verify(normalizerMock).normalizeCredentials(Map.of("key", "value"));
		verify(methodMock).validator();
		verify(validatorMock).validateCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeChangeServicePhase2DTONull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeChangeServicePhase2(null, null, null));

		assertEquals("dto is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeChangeServicePhase2AuthenticationMethodNull() {
		final IdentityChangeRequestDTO dto = new IdentityChangeRequestDTO(
				"SystemName",
				Map.of("key", "value"),
				Map.of("key", "newValue"));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeChangeServicePhase2(dto, null, null));

		assertEquals("authentication method is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeChangeServicePhase2OriginNull() {
		final IdentityChangeRequestDTO dto = new IdentityChangeRequestDTO(
				"SystemName",
				Map.of("key", "value"),
				Map.of("key", "newValue"));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeChangeServicePhase2(dto, AuthenticationMethod.PASSWORD, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeChangeServicePhase2OriginEmpty() {
		final IdentityChangeRequestDTO dto = new IdentityChangeRequestDTO(
				"SystemName",
				Map.of("key", "value"),
				Map.of("key", "newValue"));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeChangeServicePhase2(dto, AuthenticationMethod.PASSWORD, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeChangeServicePhase2UnsupportedAuthenticationMethod() {
		final IdentityChangeRequestDTO dto = new IdentityChangeRequestDTO(
				"SystemName",
				Map.of("key", "value"),
				Map.of("key", "newValue"));

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(null);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> validator.validateAndNormalizeChangeServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin"));

		assertEquals("Unsupported authentication method: PASSWORD", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeChangeServicePhase2InternalServerError() {
		final IdentityChangeRequestDTO dto = new IdentityChangeRequestDTO(
				"SystemName",
				Map.of("key", "value"),
				Map.of("key", "newValue"));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodInputNormalizer normalizerMock = Mockito.mock(IAuthenticationMethodInputNormalizer.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.normalizer()).thenReturn(normalizerMock);
		when(normalizerMock.normalizeCredentials(Map.of("key", "newValue"))).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> validator.validateAndNormalizeChangeServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).normalizer();
		verify(normalizerMock).normalizeCredentials(Map.of("key", "newValue"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeChangeServicePhase2InvalidParameterException() {
		final IdentityChangeRequestDTO dto = new IdentityChangeRequestDTO(
				"SystemName",
				Map.of("key", "value"),
				Map.of("key", "newValue"));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodInputNormalizer normalizerMock = Mockito.mock(IAuthenticationMethodInputNormalizer.class);
		final IAuthenticationMethodInputValidator validatorMock = Mockito.mock(IAuthenticationMethodInputValidator.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.normalizer()).thenReturn(normalizerMock);
		when(normalizerMock.normalizeCredentials(Map.of("key", "newValue"))).thenReturn(Map.of("key", "newValue"));
		when(methodMock.validator()).thenReturn(validatorMock);
		doThrow(new InvalidParameterException("test")).when(validatorMock).validateCredentials(Map.of("key", "newValue"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeChangeServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).normalizer();
		verify(normalizerMock).normalizeCredentials(Map.of("key", "newValue"));
		verify(methodMock).validator();
		verify(validatorMock).validateCredentials(Map.of("key", "newValue"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeChangeServicePhase2Ok() {
		final IdentityChangeRequestDTO dto = new IdentityChangeRequestDTO(
				"SystemName",
				Map.of("key", "value"),
				Map.of("key", "newValue"));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodInputNormalizer normalizerMock = Mockito.mock(IAuthenticationMethodInputNormalizer.class);
		final IAuthenticationMethodInputValidator validatorMock = Mockito.mock(IAuthenticationMethodInputValidator.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(methodMock.normalizer()).thenReturn(normalizerMock);
		when(normalizerMock.normalizeCredentials(Map.of("key", "newValue"))).thenReturn(Map.of("key", "newValue"));
		when(methodMock.validator()).thenReturn(validatorMock);
		doNothing().when(validatorMock).validateCredentials(Map.of("key", "newValue"));

		final IdentityChangeRequestDTO result = validator.validateAndNormalizeChangeServicePhase2(dto, AuthenticationMethod.PASSWORD, "origin");

		assertEquals(dto, result);

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(methodMock).normalizer();
		verify(normalizerMock).normalizeCredentials(Map.of("key", "newValue"));
		verify(methodMock).validator();
		verify(validatorMock).validateCredentials(Map.of("key", "newValue"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityTokenOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeIdentityToken(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityTokenOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeIdentityToken(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityTokenNullToken() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeIdentityToken(null, "origin"));

		assertEquals("Token is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityTokenEmptyToken() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeIdentityToken(" ", "origin"));

		assertEquals("Token is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityTokenOk() {
		final String result = validator.validateAndNormalizeIdentityToken(" token characters ", "origin");

		assertEquals("token characters", result);
	}
}