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
package eu.arrowhead.authorization.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authorization.service.normalization.AuthorizationTokenNormalizer;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;

@ExtendWith(MockitoExtension.class)
public class AuthorizationTokenValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationTokenValidation validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	@Mock
	private EventTypeNameValidator eventTypeNameValidator;

	@Mock
	private AuthorizationScopeValidator scopeValidator;

	@Mock
	private AuthorizationTokenNormalizer normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeSystemName("ProviderName", null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameEmpty() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemName("", "testOrigin"));

		assertEquals("System name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameExceptionInValidator() {
		when(normalizer.normalizeSystemName("ConsumerName")).thenReturn("ConsumerName");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("ConsumerName");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemName("ConsumerName", "testOrigin"));

		verify(normalizer).normalizeSystemName("ConsumerName");
		verify(systemNameValidator).validateSystemName("ConsumerName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameOk() {
		when(normalizer.normalizeSystemName("ConsumerName")).thenReturn("ConsumerName");
		doNothing().when(systemNameValidator).validateSystemName("ConsumerName");

		final String result = validator.validateAndNormalizeSystemName("ConsumerName", "testOrigin");

		final InOrder systemOrderValidator = Mockito.inOrder(normalizer, systemNameValidator);

		systemOrderValidator.verify(normalizer).normalizeSystemName("ConsumerName");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ConsumerName");

		assertEquals("ConsumerName", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeTokenOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeToken("aToken", ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeTokenNullToken() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeToken(null, "testOrigin"));

		assertEquals("Token is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeTokenOk() {
		when(normalizer.normalizeToken("aToken")).thenReturn("aNormalizedToken");

		final String result = validator.validateAndNormalizeToken("aToken", "testOrigin");

		verify(normalizer).normalizeToken("aToken");

		assertEquals("aNormalizedToken", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestOriginNull() {
		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(null, null, null, null, null);
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeGenerateRequest(request, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestRequestNull() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateRequest(null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestVariantEmpty() {
		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"",
				"ProviderName",
				"service_def",
				"testService",
				"op");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateRequest(request, "testOrigin"));

		assertEquals("Token variant is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestInvalidVariant() {
		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"invalid",
				"ProviderName",
				"service_def",
				"testService",
				"op");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateRequest(request, "testOrigin"));

		assertEquals("Token variant is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestProviderMissing() {
		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"TIME_LIMITED_TOKEN_AUTH",
				null,
				"service_def",
				"testService",
				"op");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateRequest(request, "testOrigin"));

		assertEquals("Provider system is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestInvalidTargetType() {
		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"TIME_LIMITED_TOKEN_AUTH",
				"ProviderName",
				"invalid",
				"testService",
				"op");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateRequest(request, "testOrigin"));

		assertEquals("Target type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestTargetIsMissing() {
		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"TIME_LIMITED_TOKEN_AUTH",
				"ProviderName",
				"service_def",
				"",
				"op");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateRequest(request, "testOrigin"));

		assertEquals("Target is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestInvalidVariant2() {

		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"NONE",
				"ProviderName",
				null,
				"testService",
				"op");

		when(normalizer.normalizeAuthorizationTokenGenerationRequestDTO(request)).thenReturn(request);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateRequest(request, "testOrigin"));

		verify(normalizer).normalizeAuthorizationTokenGenerationRequestDTO(request);

		assertEquals("Token variant is invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestNotOfferableVariant() {

		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"TRANSLATION_BRIDGE_TOKEN_AUTH",
				"ProviderName",
				null,
				"testService",
				"op");

		when(normalizer.normalizeAuthorizationTokenGenerationRequestDTO(request)).thenReturn(request);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateRequest(request, "testOrigin"));

		verify(normalizer).normalizeAuthorizationTokenGenerationRequestDTO(request);

		assertEquals("Token variant is invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestExceptionInAValidator() {
		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"TIME_LIMITED_TOKEN_AUTH",
				"ProviderName",
				"SERVICE_DEF",
				"testService",
				"op");

		when(normalizer.normalizeAuthorizationTokenGenerationRequestDTO(request)).thenReturn(request);
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("ProviderName");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateRequest(request, "testOrigin"));

		verify(normalizer).normalizeAuthorizationTokenGenerationRequestDTO(request);
		verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestOk() {
		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"TIME_LIMITED_TOKEN_AUTH",
				"ProviderName",
				"SERVICE_DEF",
				"testService",
				"op");

		when(normalizer.normalizeAuthorizationTokenGenerationRequestDTO(request)).thenReturn(request);
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(scopeValidator).validateScope("op");

		final AuthorizationTokenGenerationRequestDTO result = validator.validateAndNormalizeGenerateRequest(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(normalizer, systemNameValidator, serviceDefinitionNameValidator, scopeValidator);

		orderValidator.verify(normalizer).normalizeAuthorizationTokenGenerationRequestDTO(request);
		orderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		orderValidator.verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(eventTypeNameValidator, never()).validateEventTypeName(anyString());
		orderValidator.verify(scopeValidator).validateScope("op");

		assertEquals(request, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateRequestOk2() {
		final AuthorizationTokenGenerationRequestDTO request = new AuthorizationTokenGenerationRequestDTO(
				"TIME_LIMITED_TOKEN_AUTH",
				"ProviderName",
				"EVENT_TYPE",
				"testEvent",
				null);

		when(normalizer.normalizeAuthorizationTokenGenerationRequestDTO(request)).thenReturn(request);
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");

		final AuthorizationTokenGenerationRequestDTO result = validator.validateAndNormalizeGenerateRequest(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(normalizer, systemNameValidator, eventTypeNameValidator);

		orderValidator.verify(normalizer).normalizeAuthorizationTokenGenerationRequestDTO(request);
		orderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		verify(serviceDefinitionNameValidator, never()).validateServiceDefinitionName("testEvent");
		orderValidator.verify(eventTypeNameValidator).validateEventTypeName("testEvent");

		assertEquals(request, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterEncryptionKeyRequestOriginEmpty() {
		final AuthorizationEncryptionKeyRegistrationRequestDTO request = new AuthorizationEncryptionKeyRegistrationRequestDTO(null, null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRegisterEncryptionKeyRequest(request, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterEncryptionKeyRequestNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRegisterEncryptionKeyRequest(null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterEncryptionKeyRequestNullKey() {
		final AuthorizationEncryptionKeyRegistrationRequestDTO request = new AuthorizationEncryptionKeyRegistrationRequestDTO(null, null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRegisterEncryptionKeyRequest(request, "testOrigin"));

		assertEquals("Key is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterEncryptionKeyRequestUnsupportedAlgorithm() {
		final AuthorizationEncryptionKeyRegistrationRequestDTO request = new AuthorizationEncryptionKeyRegistrationRequestDTO(
				"aKey",
				"invalid");

		when(normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(request)).thenReturn(request);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRegisterEncryptionKeyRequest(request, "testOrigin"));

		verify(normalizer).normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(request);

		assertEquals("Unsupported algorithm", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterEncryptionKeyRequestShortKey() {
		final AuthorizationEncryptionKeyRegistrationRequestDTO request = new AuthorizationEncryptionKeyRegistrationRequestDTO(
				"aKey",
				"AES/ECB/PKCS5Padding");

		when(normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(request)).thenReturn(request);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRegisterEncryptionKeyRequest(request, "testOrigin"));

		verify(normalizer).normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(request);

		assertEquals("Key must be minimum 16 bytes long", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterEncryptionKeyRequestOk() {
		final AuthorizationEncryptionKeyRegistrationRequestDTO request = new AuthorizationEncryptionKeyRegistrationRequestDTO(
				"aVeryVeryVeryLongKey",
				"AES/CBC/PKCS5Padding");

		when(normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(request)).thenReturn(request);

		final AuthorizationEncryptionKeyRegistrationRequestDTO result = validator.validateAndNormalizeRegisterEncryptionKeyRequest(request, "testOrigin");

		verify(normalizer).normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(request);

		assertEquals(request, result);
	}
}