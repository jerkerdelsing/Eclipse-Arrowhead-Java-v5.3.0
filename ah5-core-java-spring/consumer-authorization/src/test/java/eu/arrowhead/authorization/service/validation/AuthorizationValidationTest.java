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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedLookupRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyInstanceIdentifierNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyRequestNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationScopeNormalizer;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationLookupRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@ExtendWith(MockitoExtension.class)
public class AuthorizationValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationValidation validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private CloudIdentifierValidator cloudIdentifierValidator;

	@Mock
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private EventTypeNameValidator eventTypeNameValidator;

	@Mock
	private EventTypeNameNormalizer eventTypeNameNormalizer;

	@Mock
	private AuthorizationPolicyRequestValidator policyRequestValidator;

	@Mock
	private AuthorizationPolicyRequestNormalizer policyRequestNormalizer;

	@Mock
	private AuthorizationScopeValidator scopeValidator;

	@Mock
	private AuthorizationScopeNormalizer scopeNormalizer;

	@Mock
	private AuthorizationPolicyInstanceIdentifierValidator instanceIdValidator;

	@Mock
	private AuthorizationPolicyInstanceIdentifierNormalizer instanceIdNormalizer;

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
	public void testValidateAndNormalizeGrantRequestOriginNull() {
		final AuthorizationGrantRequestDTO request = new AuthorizationGrantRequestDTO(null, null, null, null, null, null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeGrantRequest("ProviderName", request, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantRequestNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantRequest("ProviderName", null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantRequestEmptyTargetType() {
		final AuthorizationGrantRequestDTO request = new AuthorizationGrantRequestDTO(null, null, null, null, null, null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantRequest("ProviderName", request, "testOrigin"));

		assertEquals("Target type is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantRequestInvalidTargetType() {
		final AuthorizationGrantRequestDTO request = new AuthorizationGrantRequestDTO(null, "invalid", null, null, null, null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantRequest("ProviderName", request, "testOrigin"));

		assertEquals("Target type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantRequestTargetNull() {
		final AuthorizationGrantRequestDTO request = new AuthorizationGrantRequestDTO.Builder(AuthorizationTargetType.SERVICE_DEF)
				.build();

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantRequest("ProviderName", request, "testOrigin"));

		assertEquals("Target is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantRequestExceptionInAValidator() {
		final AuthorizationPolicyRequestDTO defaultPolicy = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
				.build();
		final AuthorizationGrantRequestDTO request = new AuthorizationGrantRequestDTO.Builder(AuthorizationTargetType.SERVICE_DEF)
				.cloud("invalid")
				.target("testService")
				.defaultPolicy(defaultPolicy)
				.build();

		doNothing().when(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		when(cloudIdentifierNormalizer.normalize("invalid")).thenReturn("Invalid");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(policyRequestNormalizer.normalize(defaultPolicy)).thenReturn(new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null));
		doThrow(new InvalidParameterException("test")).when(cloudIdentifierValidator).validateCloudIdentifier("Invalid");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantRequest("ProviderName", request, "testOrigin"));

		final InOrder policyRequestOrderValidator = Mockito.inOrder(policyRequestValidator, policyRequestNormalizer);
		final InOrder cloudOrderValidator = Mockito.inOrder(cloudIdentifierNormalizer, cloudIdentifierValidator);

		policyRequestOrderValidator.verify(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		cloudOrderValidator.verify(cloudIdentifierNormalizer).normalize("invalid");
		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize("testService");
		policyRequestOrderValidator.verify(policyRequestNormalizer).normalize(defaultPolicy);
		cloudOrderValidator.verify(cloudIdentifierValidator).validateCloudIdentifier("Invalid");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantRequestServiceDefOk() {
		final AuthorizationPolicyRequestDTO defaultPolicy = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
				.build();
		final AuthorizationPolicyRequestDTO scopedPolicy = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.BLACKLIST)
				.addBlacklistElement("BadSystem")
				.build();
		final AuthorizationGrantRequestDTO request = new AuthorizationGrantRequestDTO(
				null,
				"SERVICE_DEF",
				"testService",
				"description",
				defaultPolicy,
				Map.of("op", scopedPolicy));

		final NormalizedAuthorizationPolicyRequest nScopedPolicy = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.BLACKLIST, List.of("BadSystem"), null);
		final NormalizedAuthorizationPolicyRequest nDefaultPolicy = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null);

		doNothing().when(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		doNothing().when(policyRequestValidator).validateAuthorizationPolicy(scopedPolicy, false, "testOrigin");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(scopeNormalizer.normalize("op")).thenReturn("op");
		when(policyRequestNormalizer.normalize(scopedPolicy)).thenReturn(nScopedPolicy);
		when(policyRequestNormalizer.normalize(defaultPolicy)).thenReturn(nDefaultPolicy);
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(scopeValidator).validateScope("op");
		doNothing().when(policyRequestValidator).validateNormalizedAuthorizationPolicy(nScopedPolicy);
		doNothing().when(scopeValidator).validateScope("*");
		doNothing().when(policyRequestValidator).validateNormalizedAuthorizationPolicy(nDefaultPolicy);

		final NormalizedGrantRequest result = validator.validateAndNormalizeGrantRequest("ProviderName", request, "testOrigin");

		final InOrder policyRequestOrderValidator = Mockito.inOrder(policyRequestValidator, policyRequestNormalizer);
		final InOrder serviceDefOrderValidator = Mockito.inOrder(serviceDefNameNormalizer, serviceDefNameValidator);
		final InOrder scopeOrderValidator = Mockito.inOrder(scopeNormalizer, scopeValidator);

		policyRequestOrderValidator.verify(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		policyRequestOrderValidator.verify(policyRequestValidator).validateAuthorizationPolicy(scopedPolicy, false, "testOrigin");
		serviceDefOrderValidator.verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize("testService");
		scopeOrderValidator.verify(scopeNormalizer).normalize("op");
		policyRequestOrderValidator.verify(policyRequestNormalizer).normalize(scopedPolicy);
		policyRequestOrderValidator.verify(policyRequestNormalizer).normalize(defaultPolicy);
		verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		verify(systemNameValidator).validateSystemName("ProviderName");
		serviceDefOrderValidator.verify(serviceDefNameValidator).validateServiceDefinitionName("testService");
		verify(eventTypeNameValidator, never()).validateEventTypeName("testService");
		scopeOrderValidator.verify(scopeValidator).validateScope("op");
		policyRequestOrderValidator.verify(policyRequestValidator).validateNormalizedAuthorizationPolicy(nScopedPolicy);
		scopeOrderValidator.verify(scopeValidator).validateScope("*");
		policyRequestOrderValidator.verify(policyRequestValidator).validateNormalizedAuthorizationPolicy(nDefaultPolicy);

		assertEquals(AuthorizationLevel.PROVIDER, result.level());
		assertEquals("LOCAL", result.cloud());
		assertEquals("ProviderName", result.provider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
		assertEquals("testService", result.target());
		assertEquals("description", result.description());
		assertEquals(2, result.policies().size());
		assertEquals(nDefaultPolicy, result.policies().get("*"));
		assertEquals(nScopedPolicy, result.policies().get("op"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantRequestEventTypOk() {
		final AuthorizationPolicyRequestDTO defaultPolicy = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
				.build();
		final AuthorizationGrantRequestDTO request = new AuthorizationGrantRequestDTO.Builder(AuthorizationTargetType.EVENT_TYPE)
				.target("testEvent")
				.defaultPolicy(defaultPolicy)
				.build();
		final NormalizedAuthorizationPolicyRequest nDefaultPolicy = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null);

		doNothing().when(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");
		when(policyRequestNormalizer.normalize(defaultPolicy)).thenReturn(nDefaultPolicy);
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");
		doNothing().when(scopeValidator).validateScope("*");
		doNothing().when(policyRequestValidator).validateNormalizedAuthorizationPolicy(nDefaultPolicy);

		final NormalizedGrantRequest result = validator.validateAndNormalizeGrantRequest("ProviderName", request, "testOrigin");

		final InOrder policyRequestOrderValidator = Mockito.inOrder(policyRequestValidator, policyRequestNormalizer);
		final InOrder eventTypeOrderValidator = Mockito.inOrder(eventTypeNameNormalizer, eventTypeNameValidator);

		policyRequestOrderValidator.verify(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		verify(serviceDefNameNormalizer, never()).normalize("testEvent");
		eventTypeOrderValidator.verify(eventTypeNameNormalizer).normalize("testEvent");
		policyRequestOrderValidator.verify(policyRequestNormalizer).normalize(defaultPolicy);
		verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		verify(systemNameValidator).validateSystemName("ProviderName");
		verify(serviceDefNameValidator, never()).validateServiceDefinitionName("testEvent");
		eventTypeOrderValidator.verify(eventTypeNameValidator).validateEventTypeName("testEvent");
		verify(scopeValidator).validateScope("*");
		policyRequestOrderValidator.verify(policyRequestValidator).validateNormalizedAuthorizationPolicy(nDefaultPolicy);

		assertEquals(AuthorizationLevel.PROVIDER, result.level());
		assertEquals("LOCAL", result.cloud());
		assertEquals("ProviderName", result.provider());
		assertEquals(AuthorizationTargetType.EVENT_TYPE, result.targetType());
		assertEquals("testEvent", result.target());
		assertEquals(1, result.policies().size());
		assertEquals(nDefaultPolicy, result.policies().get("*"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokeInputOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRevokeInput("ProviderName", "PR|LOCAL|ProviderName|SERVICE_DEF|testService", ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokeInputInstanceIdNull() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRevokeInput("ProviderName", null, "testOrigin"));

		assertEquals("Instance id is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokeInputExceptionInAValidator() {
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(instanceIdNormalizer.normalize("PR|LOCAL|ProviderName|SERVICE_DEF|testService")).thenReturn("PR|LOCAL|ProviderName|SERVICE_DEF|testService");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("ProviderName");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRevokeInput("ProviderName", "PR|LOCAL|ProviderName|SERVICE_DEF|testService", "testOrigin"));

		verify(systemNameNormalizer).normalize("ProviderName");
		verify(instanceIdNormalizer).normalize("PR|LOCAL|ProviderName|SERVICE_DEF|testService");
		verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokeInputOk() {
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(instanceIdNormalizer.normalize("PR|LOCAL|ProviderName|SERVICE_DEF|testService")).thenReturn("PR|LOCAL|ProviderName|SERVICE_DEF|testService");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|ProviderName|SERVICE_DEF|testService");

		final Pair<String, String> result = validator.validateAndNormalizeRevokeInput("ProviderName", "PR|LOCAL|ProviderName|SERVICE_DEF|testService", "testOrigin");

		final InOrder systemNameOrderValidator = Mockito.inOrder(systemNameNormalizer, systemNameValidator);
		final InOrder instanceIdOrderValidator = Mockito.inOrder(instanceIdNormalizer, instanceIdValidator);

		systemNameOrderValidator.verify(systemNameNormalizer).normalize("ProviderName");
		instanceIdOrderValidator.verify(instanceIdNormalizer).normalize("PR|LOCAL|ProviderName|SERVICE_DEF|testService");
		systemNameOrderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		instanceIdOrderValidator.verify(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|ProviderName|SERVICE_DEF|testService");

		assertNotNull(result);
		assertEquals("ProviderName", result.getFirst());
		assertEquals("PR|LOCAL|ProviderName|SERVICE_DEF|testService", result.getSecond());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestOriginNull() {
		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(null, null, null, null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeLookupRequest("ProviderName", request, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeLookupRequest("ProviderName", null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestMissingMandatoryFilter() {
		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(null, null, null, null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin"));

		assertEquals("One of the following filters must be used: 'instanceIds', 'targetNames', 'cloudIdentifiers'", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestInstanceIdFilterContainsNull() {
		final List<String> instanceIds = new ArrayList<>(1);
		instanceIds.add(null);

		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(
				instanceIds,
				null,
				null,
				null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin"));

		assertEquals("Instance id list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestTargetNameFilterContainsEmpty() {
		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(
				null,
				null,
				List.of(""),
				null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin"));

		assertEquals("Target names list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestTargetTypeIsMandatoryCase() {
		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(
				List.of("PR|LOCAL|ProviderName|SERVICE_DEF|testService"),
				null,
				List.of("testService"),
				null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin"));

		assertEquals("If target names list is specified then a valid target type is mandatory", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestCloudIdentifiersFilterContainsNull() {
		final List<String> cloudIds = new ArrayList<>(1);
		cloudIds.add(null);

		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(
				null,
				cloudIds,
				List.of("testService"),
				"SERVICE_DEF");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin"));

		assertEquals("Cloud identifiers list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestInvalidTargetType() {
		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(
				null,
				List.of("LOCAL"),
				null,
				"invalid");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin"));

		assertEquals("Target type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestExceptionInAValidator() {
		when(instanceIdNormalizer.normalize("PR|LOCAL|ProviderName|SERVICE_DEF|testService")).thenReturn("PR|LOCAL|ProviderName|SERVICE_DEF|testService");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("ProviderName");

		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(
				List.of("PR|LOCAL|ProviderName|SERVICE_DEF|testService"),
				null,
				List.of("testService"),
				"service_def");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin"));

		verify(instanceIdNormalizer).normalize("PR|LOCAL|ProviderName|SERVICE_DEF|testService");
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestOk1() {
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");

		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(
				null,
				List.of("LOCAL"),
				null,
				null);

		final NormalizedLookupRequest result = validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin");

		final InOrder cloudOrderValidator = Mockito.inOrder(cloudIdentifierNormalizer, cloudIdentifierValidator);

		verify(instanceIdNormalizer, never()).normalize(anyString());
		cloudOrderValidator.verify(cloudIdentifierNormalizer).normalize("LOCAL");
		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameValidator).validateSystemName("ProviderName");
		cloudOrderValidator.verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");

		assertEquals(1, result.providers().size());
		assertEquals("ProviderName", result.providers().get(0));
		assertNull(result.instanceIds());
		assertEquals(1, result.cloudIdentifiers().size());
		assertEquals("LOCAL", result.cloudIdentifiers().get(0));
		assertNull(result.targetNames());
		assertNull(result.targetType());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestOk2() {
		when(instanceIdNormalizer.normalize("PR|LOCAL|ProviderName|EVENT_TYPE|testEvent")).thenReturn("PR|LOCAL|ProviderName|EVENT_TYPE|testEvent");
		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|ProviderName|EVENT_TYPE|testEvent");
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");

		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(
				List.of("PR|LOCAL|ProviderName|EVENT_TYPE|testEvent"),
				null,
				List.of("testEvent"),
				"EVENT_TYPE");

		final NormalizedLookupRequest result = validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin");

		final InOrder instanceOrderValidator = Mockito.inOrder(instanceIdNormalizer, instanceIdValidator);
		final InOrder eventTypeOrderValidator = Mockito.inOrder(eventTypeNameNormalizer, eventTypeNameValidator);

		instanceOrderValidator.verify(instanceIdNormalizer).normalize("PR|LOCAL|ProviderName|EVENT_TYPE|testEvent");
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		eventTypeOrderValidator.verify(eventTypeNameNormalizer).normalize("testEvent");
		verify(systemNameValidator).validateSystemName("ProviderName");
		instanceOrderValidator.verify(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|ProviderName|EVENT_TYPE|testEvent");
		eventTypeOrderValidator.verify(eventTypeNameValidator).validateEventTypeName("testEvent");

		assertEquals(1, result.providers().size());
		assertEquals("ProviderName", result.providers().get(0));
		assertEquals(1, result.instanceIds().size());
		assertEquals("PR|LOCAL|ProviderName|EVENT_TYPE|testEvent", result.instanceIds().get(0));
		assertNull(result.cloudIdentifiers());
		assertEquals(1, result.targetNames().size());
		assertEquals("testEvent", result.targetNames().get(0));
		assertEquals(AuthorizationTargetType.EVENT_TYPE, result.targetType());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupRequestOk3() {
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("testService");

		final AuthorizationLookupRequestDTO request = new AuthorizationLookupRequestDTO(
				null,
				null,
				List.of("testService"),
				"SERVICE_DEF");

		final NormalizedLookupRequest result = validator.validateAndNormalizeLookupRequest("ProviderName", request, "testOrigin");

		final InOrder serviceDefOrderValidator = Mockito.inOrder(serviceDefNameNormalizer, serviceDefNameValidator);

		verify(instanceIdNormalizer, never()).normalize(anyString());
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		serviceDefOrderValidator.verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameValidator).validateSystemName("ProviderName");
		serviceDefOrderValidator.verify(serviceDefNameValidator).validateServiceDefinitionName("testService");

		assertEquals(1, result.providers().size());
		assertEquals("ProviderName", result.providers().get(0));
		assertNull(result.instanceIds());
		assertNull(result.cloudIdentifiers());
		assertEquals(1, result.targetNames().size());
		assertEquals("testService", result.targetNames().get(0));
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestOriginEmpty() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(null, null, null, null, null, null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeVerifyRequest("ProviderName", request, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyRequest("ProviderName", null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestProviderOrConsumerIsMandatory() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(null, null, null, null, null, null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyRequest("ProviderName", request, "testOrigin"));

		assertEquals("Either provider or consumer must be specified", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestMissingTargetType() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"ProviderName",
				"ConsumerName",
				null,
				null,
				"testService",
				"op");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyRequest("ProviderName", request, "testOrigin"));

		assertEquals("Target type is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestInvalidTargetType() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				null,
				"ConsumerName",
				null,
				"invalid",
				"testService",
				"op");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyRequest("ProviderName", request, "testOrigin"));

		assertEquals("Target type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestMissingTarget() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"ProviderName",
				null,
				null,
				"service_def",
				"",
				"op");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyRequest("ProviderName", request, "testOrigin"));

		assertEquals("Target is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestNotRelatedRequester() {
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(systemNameNormalizer.normalize("ConsumerName")).thenReturn("ConsumerName");

		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"ProviderName",
				"ConsumerName",
				null,
				"service_def",
				"testService",
				"op");

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> validator.validateAndNormalizeVerifyRequest("Requester", request, "testOrigin"));

		verify(systemNameNormalizer).normalize("ProviderName");
		verify(systemNameNormalizer).normalize("ConsumerName");

		assertEquals("Only the related provider or consumer can use this operation", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestExceptionInAValidator() {
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(scopeNormalizer.normalize("op")).thenReturn("op");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("ProviderName");

		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"ProviderName",
				null,
				null,
				"service_def",
				"testService",
				"op");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyRequest("Requester", request, "testOrigin"));

		verify(systemNameNormalizer).normalize("ProviderName");
		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		verify(scopeNormalizer).normalize("op");
		verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestOk1() {
		when(systemNameNormalizer.normalize("ConsumerName")).thenReturn("ConsumerName");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		when(scopeNormalizer.normalize("op")).thenReturn("op");
		doNothing().when(systemNameValidator).validateSystemName("Requester");
		doNothing().when(systemNameValidator).validateSystemName("ConsumerName");
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(scopeValidator).validateScope("op");

		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				null,
				"ConsumerName",
				"LOCAL",
				"service_def",
				"testService",
				"op");

		final NormalizedVerifyRequest result = validator.validateAndNormalizeVerifyRequest("Requester", request, "testOrigin");

		final InOrder systemOrderValidator = Mockito.inOrder(systemNameNormalizer, systemNameValidator);
		final InOrder serviceDefOrderValidator = Mockito.inOrder(serviceDefNameNormalizer, serviceDefNameValidator);
		final InOrder cloudOrderValidator = Mockito.inOrder(cloudIdentifierNormalizer, cloudIdentifierValidator);
		final InOrder scopeOrderValidator = Mockito.inOrder(scopeNormalizer, scopeValidator);

		systemOrderValidator.verify(systemNameNormalizer).normalize("ConsumerName");
		serviceDefOrderValidator.verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		cloudOrderValidator.verify(cloudIdentifierNormalizer).normalize("LOCAL");
		scopeOrderValidator.verify(scopeNormalizer).normalize("op");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("Requester");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ConsumerName");
		cloudOrderValidator.verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		serviceDefOrderValidator.verify(serviceDefNameValidator).validateServiceDefinitionName("testService");
		scopeOrderValidator.verify(scopeValidator).validateScope("op");

		assertEquals("Requester", result.provider());
		assertEquals("ConsumerName", result.consumer());
		assertEquals("LOCAL", result.cloud());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
		assertEquals("testService", result.target());
		assertEquals("op", result.scope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyRequestOk2() {
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(systemNameNormalizer.normalize("ConsumerName")).thenReturn("ConsumerName");
		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(systemNameValidator).validateSystemName("ConsumerName");
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");

		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"ProviderName",
				"ConsumerName",
				null,
				"event_type",
				"testEvent",
				null);

		final NormalizedVerifyRequest result = validator.validateAndNormalizeVerifyRequest("ProviderName", request, "testOrigin");

		final InOrder systemOrderValidator = Mockito.inOrder(systemNameNormalizer, systemNameValidator);
		final InOrder eventOrderValidator = Mockito.inOrder(eventTypeNameNormalizer, eventTypeNameValidator);

		systemOrderValidator.verify(systemNameNormalizer).normalize("ProviderName");
		systemOrderValidator.verify(systemNameNormalizer).normalize("ConsumerName");
		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		eventOrderValidator.verify(eventTypeNameNormalizer).normalize("testEvent");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ConsumerName");
		eventOrderValidator.verify(eventTypeNameValidator).validateEventTypeName("testEvent");

		assertEquals("ProviderName", result.provider());
		assertEquals("ConsumerName", result.consumer());
		assertEquals("LOCAL", result.cloud());
		assertEquals(AuthorizationTargetType.EVENT_TYPE, result.targetType());
		assertEquals("testEvent", result.target());
		assertNull(result.scope());
	}
}