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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

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
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;

@ExtendWith(MockitoExtension.class)
public class AuthorizationTokenManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationTokenManagementValidation validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private CloudIdentifierValidator cloudIdentifierValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	@Mock
	private EventTypeNameValidator eventTypeNameValidator;

	@Mock
	private AuthorizationScopeValidator scopeValidator;

	@Mock
	private AuthorizationTokenNormalizer tokenNormalizer;

	@Mock
	private PageValidator pageValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateTokenReferencesOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateTokenReferences(List.of("tokenRef"), null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateTokenReferencesListEmpty() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateTokenReferences(List.of(), "testOrigin"));

		assertEquals("Token references list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateTokenReferencesListContainsEmptyElement() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateTokenReferences(List.of(""), "testOrigin"));

		assertEquals("Token references list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateTokenReferencesOk() {
		assertDoesNotThrow(() -> validator.validateTokenReferences(List.of("tokenRef"), "testOrigin"));
	}

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
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("ProviderName");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemName("ProviderName", "testOrigin"));

		verify(systemNameNormalizer).normalize("ProviderName");
		verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameOk() {
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");

		final String result = validator.validateAndNormalizeSystemName("ProviderName", "testOrigin");

		final InOrder orderVerifier = Mockito.inOrder(systemNameNormalizer, systemNameValidator);
		orderVerifier.verify(systemNameNormalizer).normalize("ProviderName");
		orderVerifier.verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("ProviderName", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsOriginEmpty() {
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsEmptyList() {
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(
				List.of());

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsNullElement() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Request payload list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsMissingVariant() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						null,
						"service_def",
						null,
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						null,
						20));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Token variant is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsInvalidVariant() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"invalid",
						"service_def",
						null,
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						null,
						20));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Token variant is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsInvalidTargetType() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"USAGE_LIMITED_TOKEN_AUTH",
						"invalid",
						null,
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						null,
						20));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Target type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsMissingConsumer() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"USAGE_LIMITED_TOKEN_AUTH",
						"service_def",
						null,
						"",
						"ProviderName",
						"testService",
						"op",
						null,
						20));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Consumer system name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsMissingProvider() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"USAGE_LIMITED_TOKEN_AUTH",
						null,
						null,
						"ConsumerName",
						null,
						"testService",
						"op",
						null,
						20));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Provider system name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsMissingTarget() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"USAGE_LIMITED_TOKEN_AUTH",
						null,
						null,
						"ConsumerName",
						"ProviderName",
						"",
						"op",
						null,
						20));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Target is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsInvalidExpirationTime() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"TIME_LIMITED_TOKEN_AUTH",
						null,
						null,
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						"invalid",
						null));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Expiration time has an invalid time format", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsPastExpirationTime() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"TIME_LIMITED_TOKEN_AUTH",
						null,
						null,
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						"2025-07-31T10:00:00Z",
						null));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Expiration time is in the past", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsInvalidUsageLimit() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"USAGE_LIMITED_TOKEN_AUTH",
						null,
						null,
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						null,
						0));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		assertEquals("Usage limit is invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsInvalidVariant2() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"NONE",
						"SERVICE_DEF",
						"LOCAL",
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						"2100-05-02T10:00:00Z",
						null));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request)).thenReturn(request);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		verify(tokenNormalizer).normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request);

		assertEquals("Invalid token variant: NONE", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsNotOfferableVariant() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"TRANSLATION_BRIDGE_TOKEN_AUTH",
						"SERVICE_DEF",
						"LOCAL",
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						"2100-05-02T10:00:00Z",
						null));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request)).thenReturn(request);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		verify(tokenNormalizer).normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request);

		assertEquals("Invalid token variant: TRANSLATION_BRIDGE_TOKEN_AUTH", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsExceptionInAValidator() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"USAGE_LIMITED_TOKEN_AUTH",
						"SERVICE_DEF",
						"Invalid",
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						null,
						20));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request)).thenReturn(request);
		doThrow(new InvalidParameterException("test")).when(cloudIdentifierValidator).validateCloudIdentifier("Invalid");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin"));

		verify(tokenNormalizer).normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request);
		verify(cloudIdentifierValidator).validateCloudIdentifier("Invalid");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsOk1() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"USAGE_LIMITED_TOKEN_AUTH",
						"SERVICE_DEF",
						"LOCAL",
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						null,
						20));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request)).thenReturn(request);
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("ConsumerName");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(scopeValidator).validateScope("op");

		final AuthorizationTokenGenerationMgmtListRequestDTO result = validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(tokenNormalizer, cloudIdentifierValidator, systemNameValidator, serviceDefinitionNameValidator, scopeValidator);

		orderValidator.verify(tokenNormalizer).normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request);
		orderValidator.verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		orderValidator.verify(systemNameValidator).validateSystemName("ConsumerName");
		orderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		orderValidator.verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(eventTypeNameValidator, never()).validateEventTypeName(anyString());
		orderValidator.verify(scopeValidator).validateScope("op");

		assertEquals(request, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGenerateTokenRequestsOk2() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"USAGE_LIMITED_TOKEN_AUTH",
						"EVENT_TYPE",
						"LOCAL",
						"ConsumerName",
						"ProviderName",
						"testEvent",
						null,
						null,
						20));
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request)).thenReturn(request);
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("ConsumerName");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");

		final AuthorizationTokenGenerationMgmtListRequestDTO result = validator.validateAndNormalizeGenerateTokenRequests(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(tokenNormalizer, cloudIdentifierValidator, systemNameValidator, eventTypeNameValidator);

		orderValidator.verify(tokenNormalizer).normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request);
		orderValidator.verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		orderValidator.verify(systemNameValidator).validateSystemName("ConsumerName");
		orderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		verify(serviceDefinitionNameValidator, never()).validateServiceDefinitionName(anyString());
		orderValidator.verify(eventTypeNameValidator).validateEventTypeName("testEvent");
		verify(scopeValidator, never()).validateScope(anyString());

		assertEquals(request, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestOriginNull() {
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(null, null, null, null, null, null, null, null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeQueryTokensRequest(request, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryTokensRequest(null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestInvalidTokenType() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				"Requester",
				"invalid",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				"SERVICE_DEF",
				"testService");

		doNothing().when(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryTokensRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));

		assertEquals("Invalid token type: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestNotOfferableTokenType() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				"Requester",
				"TRANSLATION_BRIDGE_TOKEN",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				"SERVICE_DEF",
				"testService");

		doNothing().when(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		when(tokenNormalizer.normalizeAuthorizationTokenQueryRequestDTO(request)).thenReturn(request);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryTokensRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));

		assertEquals("Token type is invalid: TRANSLATION_BRIDGE_TOKEN", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestInvalidTargetType() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				"Requester",
				"TIME_LIMITED_TOKEN",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				"invalid",
				"testService");

		doNothing().when(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryTokensRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));

		assertEquals("Invalid target type: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestExceptionInAValidator() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				"Requester",
				"",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				"SERVICE_DEF",
				"testService");

		doNothing().when(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		when(tokenNormalizer.normalizeAuthorizationTokenQueryRequestDTO(request)).thenReturn(request);
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("Requester");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryTokensRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		verify(tokenNormalizer).normalizeAuthorizationTokenQueryRequestDTO(request);
		verify(systemNameValidator).validateSystemName("Requester");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestOk1() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				"Requester",
				"",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				null,
				"testService");

		doNothing().when(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		when(tokenNormalizer.normalizeAuthorizationTokenQueryRequestDTO(request)).thenReturn(request);
		doNothing().when(systemNameValidator).validateSystemName("Requester");
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("ConsumerName");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");

		final AuthorizationTokenQueryRequestDTO result = validator.validateAndNormalizeQueryTokensRequest(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(pageValidator, tokenNormalizer, systemNameValidator, cloudIdentifierValidator, serviceDefinitionNameValidator);

		orderValidator.verify(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		orderValidator.verify(tokenNormalizer).normalizeAuthorizationTokenQueryRequestDTO(request);
		orderValidator.verify(systemNameValidator).validateSystemName("Requester");
		orderValidator.verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		orderValidator.verify(systemNameValidator).validateSystemName("ConsumerName");
		orderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		orderValidator.verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(eventTypeNameValidator, never()).validateEventTypeName(anyString());

		assertEquals(request, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestOk2() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				null,
				"",
				null,
				"",
				"",
				"SERVICE_DEF",
				"testService");

		doNothing().when(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		when(tokenNormalizer.normalizeAuthorizationTokenQueryRequestDTO(request)).thenReturn(request);
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");

		final AuthorizationTokenQueryRequestDTO result = validator.validateAndNormalizeQueryTokensRequest(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(pageValidator, tokenNormalizer, serviceDefinitionNameValidator);

		orderValidator.verify(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		orderValidator.verify(tokenNormalizer).normalizeAuthorizationTokenQueryRequestDTO(request);
		verify(systemNameValidator, never()).validateSystemName(anyString());
		verify(cloudIdentifierValidator, never()).validateCloudIdentifier(anyString());
		orderValidator.verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(eventTypeNameValidator, never()).validateEventTypeName(anyString());

		assertEquals(request, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestOk3() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				null,
				"",
				null,
				"",
				"",
				"EVENT_TYPE",
				"testEvent");

		doNothing().when(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		when(tokenNormalizer.normalizeAuthorizationTokenQueryRequestDTO(request)).thenReturn(request);
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");

		final AuthorizationTokenQueryRequestDTO result = validator.validateAndNormalizeQueryTokensRequest(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(pageValidator, tokenNormalizer, eventTypeNameValidator);

		orderValidator.verify(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		orderValidator.verify(tokenNormalizer).normalizeAuthorizationTokenQueryRequestDTO(request);
		verify(systemNameValidator, never()).validateSystemName(anyString());
		verify(cloudIdentifierValidator, never()).validateCloudIdentifier(anyString());
		verify(serviceDefinitionNameValidator, never()).validateServiceDefinitionName(anyString());
		orderValidator.verify(eventTypeNameValidator).validateEventTypeName("testEvent");

		assertEquals(request, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestOk4() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				null,
				"",
				null,
				"",
				"",
				null,
				"");

		doNothing().when(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		when(tokenNormalizer.normalizeAuthorizationTokenQueryRequestDTO(request)).thenReturn(request);

		final AuthorizationTokenQueryRequestDTO result = validator.validateAndNormalizeQueryTokensRequest(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(pageValidator, tokenNormalizer);

		orderValidator.verify(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		orderValidator.verify(tokenNormalizer).normalizeAuthorizationTokenQueryRequestDTO(request);
		verify(systemNameValidator, never()).validateSystemName(anyString());
		verify(cloudIdentifierValidator, never()).validateCloudIdentifier(anyString());
		verify(serviceDefinitionNameValidator, never()).validateServiceDefinitionName(anyString());
		verify(eventTypeNameValidator, never()).validateEventTypeName(anyString());

		assertEquals(request, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryTokensRequestOk5() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO request = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				null,
				"SELF_CONTAINED_TOKEN",
				null,
				"",
				"",
				null,
				"");

		doNothing().when(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		when(tokenNormalizer.normalizeAuthorizationTokenQueryRequestDTO(request)).thenReturn(request);

		final AuthorizationTokenQueryRequestDTO result = validator.validateAndNormalizeQueryTokensRequest(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(pageValidator, tokenNormalizer);

		orderValidator.verify(pageValidator).validatePageParameter(eq(pageDTO), anyList(), eq("testOrigin"));
		orderValidator.verify(tokenNormalizer).normalizeAuthorizationTokenQueryRequestDTO(request);
		verify(systemNameValidator, never()).validateSystemName(anyString());
		verify(cloudIdentifierValidator, never()).validateCloudIdentifier(anyString());
		verify(serviceDefinitionNameValidator, never()).validateServiceDefinitionName(anyString());
		verify(eventTypeNameValidator, never()).validateEventTypeName(anyString());

		assertEquals(request, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestOriginEmpty() {
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(request, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestNullList() {
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(request, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestNullElement() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(request, "testOrigin"));

		assertEquals("Request payload list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestMissingSystem() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = List.of(
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO("", "aKey", "AES/ECB/PKCS5Padding"));

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(request, "testOrigin"));

		assertEquals("System name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestMissingKey() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = List.of(
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO("ProviderName", null, "AES/ECB/PKCS5Padding"));

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(request, "testOrigin"));

		assertEquals("Key is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestExceptionInSystemNameValidation() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = List.of(
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO("Invalid#SystemName", "aKey", "AES/ECB/PKCS5Padding"));

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request)).thenReturn(request);
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("Invalid#SystemName");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(request, "testOrigin"));

		verify(tokenNormalizer).normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request);
		verify(systemNameValidator).validateSystemName("Invalid#SystemName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestDuplicateSystem() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = List.of(
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO("ProviderName", "aVeryVeryVeryVeryLongKey", "AES/ECB/PKCS5Padding"),
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO("ProviderName", "anOtherKey", "AES/ECB/PKCS5Padding"));

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request)).thenReturn(request);
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(request, "testOrigin"));

		verify(tokenNormalizer).normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request);
		verify(systemNameValidator, times(2)).validateSystemName("ProviderName");

		assertEquals("Duplicate system name: ProviderName", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestUnsupportedAlgorithm() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = List.of(
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO("ProviderName", "aVeryVeryVeryVeryLongKey", "unsupported"));

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request)).thenReturn(request);
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(request, "testOrigin"));

		verify(tokenNormalizer).normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request);
		verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("Unsupported algorithm", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestShortKey() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = List.of(
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO("ProviderName", "shortKey", "AES/CBC/PKCS5Padding"));

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request)).thenReturn(request);
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAddEncryptionKeysRequest(request, "testOrigin"));

		verify(tokenNormalizer).normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request);
		verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("Key must be minimum 16 bytes long for system: ProviderName", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAddEncryptionKeysRequestOk() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = List.of(
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO("ProviderName", "aVeryVeryVeryVeryLongKey", "AES/CBC/PKCS5Padding"));

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		when(tokenNormalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request)).thenReturn(request);
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO result = validator.validateAndNormalizeAddEncryptionKeysRequest(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(tokenNormalizer, systemNameValidator);

		orderValidator.verify(tokenNormalizer).normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(request);
		orderValidator.verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals(request, result);
	}
}