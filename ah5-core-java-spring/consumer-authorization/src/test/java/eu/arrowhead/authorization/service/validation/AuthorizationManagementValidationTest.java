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

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedQueryRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyInstanceIdentifierNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyRequestNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationScopeNormalizer;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@ExtendWith(MockitoExtension.class)
public class AuthorizationManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationManagementValidation validator;

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
	private AuthorizationScopeValidator scopeValidator;

	@Mock
	private AuthorizationScopeNormalizer scopeNormalizer;

	@Mock
	private AuthorizationPolicyRequestValidator policyRequestValidator;

	@Mock
	private AuthorizationPolicyRequestNormalizer policyRequestNormalizer;

	@Mock
	private AuthorizationPolicyInstanceIdentifierValidator instanceIdValidator;

	@Mock
	private AuthorizationPolicyInstanceIdentifierNormalizer instanceIdNormalizer;

	@Mock
	private PageValidator pageValidator;

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
	public void testValidateAndNormalizeGrantListRequestOriginEmpty() {
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestEmptyList() {
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(List.of());

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestNullElement() {
		final List<AuthorizationMgmtGrantRequestDTO> list = new ArrayList<>(1);
		list.add(null);
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Request payload list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestMissingTargetType() {
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						"LOCAL",
						"ProviderName",
						"",
						"testService",
						"description",
						new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
								.build(),
						Map.of()));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Target type is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestInvalidTargetType() {
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						"LOCAL",
						"ProviderName",
						"invalid",
						"testService",
						"description",
						new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
								.build(),
						Map.of()));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Target type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestMissingTarget() {
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						"LOCAL",
						"ProviderName",
						"SERVICE_DEF",
						null,
						"description",
						new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
								.build(),
						Map.of()));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Target is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestEmptyScopeKey() {
		final AuthorizationPolicyRequestDTO defaultPolicy = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.BLACKLIST)
				.addBlacklistElement("BadSystem")
				.build();
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						"LOCAL",
						"ProviderName",
						"SERVICE_DEF",
						"testService",
						"description",
						defaultPolicy,
						Map.of("", new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL).build())));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		doNothing().when(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		verify(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");

		assertEquals("Scope is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestExceptionInAValidator() {
		final AuthorizationPolicyRequestDTO defaultPolicy = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.BLACKLIST)
				.addBlacklistElement("BadSystem")
				.build();
		final AuthorizationPolicyRequestDTO scopedPolicy = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL).build();
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						"LOCAL",
						"ProviderName",
						"SERVICE_DEF",
						"testService",
						"description",
						defaultPolicy,
						Map.of("op", scopedPolicy)));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final NormalizedAuthorizationPolicyRequest nDefaultPolicy = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.BLACKLIST, List.of("BadSystem"), null);
		final NormalizedAuthorizationPolicyRequest nScopedPolicy = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null);

		doNothing().when(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		doNothing().when(policyRequestValidator).validateAuthorizationPolicy(scopedPolicy, false, "testOrigin");
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(scopeNormalizer.normalize("op")).thenReturn("op");
		when(policyRequestNormalizer.normalize(scopedPolicy)).thenReturn(nScopedPolicy);
		when(policyRequestNormalizer.normalize(defaultPolicy)).thenReturn(nDefaultPolicy);
		doThrow(new InvalidParameterException("test")).when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		verify(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		verify(policyRequestValidator).validateAuthorizationPolicy(scopedPolicy, false, "testOrigin");
		verify(cloudIdentifierNormalizer).normalize("LOCAL");
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(scopeNormalizer).normalize("op");
		verify(policyRequestNormalizer).normalize(scopedPolicy);
		verify(policyRequestNormalizer).normalize(defaultPolicy);
		verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestOk1() {
		final AuthorizationPolicyRequestDTO defaultPolicy = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
				.build();
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						null,
						"ProviderName",
						"SERVICE_DEF",
						"testService",
						"description",
						defaultPolicy,
						Map.of()));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final NormalizedAuthorizationPolicyRequest nDefaultPolicy = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null);

		doNothing().when(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(policyRequestNormalizer.normalize(defaultPolicy)).thenReturn(nDefaultPolicy);
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(scopeValidator).validateScope("*");
		doNothing().when(policyRequestValidator).validateNormalizedAuthorizationPolicy(nDefaultPolicy);

		final List<NormalizedGrantRequest> result = validator.validateAndNormalizeGrantListRequest(request, "testOrigin");

		final InOrder prOrderValidator = Mockito.inOrder(policyRequestValidator, policyRequestNormalizer, policyRequestValidator);
		final InOrder systemOrderValidator = Mockito.inOrder(systemNameNormalizer, systemNameValidator);
		final InOrder serviceDefOrderValidator = Mockito.inOrder(serviceDefNameNormalizer, serviceDefNameValidator);

		prOrderValidator.verify(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		systemOrderValidator.verify(systemNameNormalizer).normalize("ProviderName");
		serviceDefOrderValidator.verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		prOrderValidator.verify(policyRequestNormalizer).normalize(defaultPolicy);
		verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		serviceDefOrderValidator.verify(serviceDefNameValidator).validateServiceDefinitionName("testService");
		verify(scopeValidator).validateScope("*");
		prOrderValidator.verify(policyRequestValidator).validateNormalizedAuthorizationPolicy(nDefaultPolicy);

		assertEquals(1, result.size());
		assertEquals(AuthorizationLevel.MGMT, result.get(0).level());
		assertEquals("LOCAL", result.get(0).cloud());
		assertEquals("ProviderName", result.get(0).provider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.get(0).targetType());
		assertEquals("testService", result.get(0).target());
		assertEquals("description", result.get(0).description());
		assertEquals(1, result.get(0).policies().size());
		assertEquals(AuthorizationPolicyType.ALL, result.get(0).policies().get("*").policyType());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestOk2() {
		final AuthorizationPolicyRequestDTO defaultPolicy = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
				.build();
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						null,
						"ProviderName",
						"EVENT_TYPE",
						"testEvent",
						"description",
						defaultPolicy,
						Map.of()));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final NormalizedAuthorizationPolicyRequest nDefaultPolicy = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null);

		doNothing().when(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");
		when(policyRequestNormalizer.normalize(defaultPolicy)).thenReturn(nDefaultPolicy);
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");
		doNothing().when(scopeValidator).validateScope("*");
		doNothing().when(policyRequestValidator).validateNormalizedAuthorizationPolicy(nDefaultPolicy);

		final List<NormalizedGrantRequest> result = validator.validateAndNormalizeGrantListRequest(request, "testOrigin");

		final InOrder prOrderValidator = Mockito.inOrder(policyRequestValidator, policyRequestNormalizer, policyRequestValidator);
		final InOrder systemOrderValidator = Mockito.inOrder(systemNameNormalizer, systemNameValidator);
		final InOrder eventTypeOrderValidator = Mockito.inOrder(eventTypeNameNormalizer, eventTypeNameValidator);

		prOrderValidator.verify(policyRequestValidator).validateAuthorizationPolicy(defaultPolicy, true, "testOrigin");
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		systemOrderValidator.verify(systemNameNormalizer).normalize("ProviderName");
		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		eventTypeOrderValidator.verify(eventTypeNameNormalizer).normalize("testEvent");
		prOrderValidator.verify(policyRequestNormalizer).normalize(defaultPolicy);
		verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		eventTypeOrderValidator.verify(eventTypeNameValidator).validateEventTypeName("testEvent");
		verify(scopeValidator).validateScope("*");
		prOrderValidator.verify(policyRequestValidator).validateNormalizedAuthorizationPolicy(nDefaultPolicy);

		assertEquals(1, result.size());
		assertEquals(AuthorizationLevel.MGMT, result.get(0).level());
		assertEquals("LOCAL", result.get(0).cloud());
		assertEquals("ProviderName", result.get(0).provider());
		assertEquals(AuthorizationTargetType.EVENT_TYPE, result.get(0).targetType());
		assertEquals("testEvent", result.get(0).target());
		assertEquals("description", result.get(0).description());
		assertEquals(1, result.get(0).policies().size());
		assertEquals(AuthorizationPolicyType.ALL, result.get(0).policies().get("*").policyType());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokePoliciesInputOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRevokePoliciesInput(List.of("instanceId"), null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokePoliciesInputListNull() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRevokePoliciesInput(null, "testOrigin"));

		assertEquals("Instance id list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokePoliciesInputEmptyElement() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRevokePoliciesInput(List.of(" "), "testOrigin"));

		assertEquals("Instance id list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokePoliciesInputExceptionInValidator() {
		when(instanceIdNormalizer.normalize("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo")).thenReturn("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		doThrow(new InvalidParameterException("test")).when(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRevokePoliciesInput(List.of("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo"), "testOrigin"));

		verify(instanceIdNormalizer).normalize("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		verify(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokePoliciesInputOk() {
		when(instanceIdNormalizer.normalize("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo")).thenReturn("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		doNothing().when(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");

		final List<String> result = validator.validateAndNormalizeRevokePoliciesInput(List.of("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo"), "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(instanceIdNormalizer, instanceIdValidator);

		orderValidator.verify(instanceIdNormalizer).normalize("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		orderValidator.verify(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");

		assertEquals(1, result.size());
		assertEquals("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo", result.get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestOriginEmpty() {
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(null, null, null, null, null, null, null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestMissingRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestMissingLevel() {
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				new PageDTO(0, 11, "ASC", "id"),
				"",
				null,
				null,
				null,
				null,
				null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		assertEquals("Level is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestInvalidLevel() {
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				new PageDTO(0, 11, "ASC", "id"),
				"invalid",
				null,
				null,
				null,
				null,
				null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		assertEquals("Level is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestExceptionInPageValidatorWithOrigin() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"MGMT",
				null,
				null,
				null,
				null,
				null);

		doThrow(new InvalidParameterException("test", "otherOrigin")).when(pageValidator).validatePageParameter(pageDTO, AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(pageDTO, AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		assertEquals("test", ex.getMessage());
		assertEquals("otherOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestExceptionInPageValidatorWithoutOrigin() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				null,
				null,
				null,
				null,
				null);

		doThrow(new InvalidParameterException("test")).when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestEmptyProvider() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				List.of(""),
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		assertEquals("Provider list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestEmptyInstanceId() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				List.of("TemperatureProvider2"),
				List.of(""),
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		assertEquals("Instance id list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestInvalidTargetType() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				null,
				List.of("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo"),
				null,
				null,
				"invalid");

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		assertEquals("Target type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestEmptyTarget() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				null,
				null,
				null,
				List.of(""),
				"SERVICE_DEF");

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		assertEquals("Target names list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestMissingTargetTypeWhenMandatory() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				null,
				null,
				null,
				List.of("kelvinInfo"),
				"");

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		assertEquals("If target names list is specified then a valid target type is mandatory", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestEmptyCloudIdentifier() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				null,
				null,
				List.of(""),
				null,
				"");

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		assertEquals("Cloud identifiers list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestExceptionInAValidator() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				List.of("TemperatureProvider2"),
				List.of("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo"),
				List.of("LOCAL"),
				List.of("kelvinInfo"),
				"SERVICE_DEF");

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");
		when(systemNameNormalizer.normalize("TemperatureProvider2")).thenReturn("TemperatureProvider2");
		when(instanceIdNormalizer.normalize("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo")).thenReturn("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		when(serviceDefNameNormalizer.normalize("kelvinInfo")).thenReturn("kelvinInfo");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("TemperatureProvider2");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(request, "testOrigin"));

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");
		verify(systemNameNormalizer).normalize("TemperatureProvider2");
		verify(instanceIdNormalizer).normalize("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		verify(cloudIdentifierNormalizer).normalize("LOCAL");
		verify(serviceDefNameNormalizer).normalize("kelvinInfo");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameValidator).validateSystemName("TemperatureProvider2");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestOk1() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				List.of("TemperatureProvider2"),
				List.of("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo"),
				List.of("LOCAL"),
				List.of("kelvinInfo"),
				"SERVICE_DEF");

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");
		when(systemNameNormalizer.normalize("TemperatureProvider2")).thenReturn("TemperatureProvider2");
		when(instanceIdNormalizer.normalize("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo")).thenReturn("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		when(serviceDefNameNormalizer.normalize("kelvinInfo")).thenReturn("kelvinInfo");
		doNothing().when(systemNameValidator).validateSystemName("TemperatureProvider2");
		doNothing().when(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("kelvinInfo");

		final NormalizedQueryRequest result = validator.validateAndNormalizeQueryRequest(request, "testOrigin");

		final InOrder systemOrderValidator = Mockito.inOrder(systemNameNormalizer, systemNameValidator);
		final InOrder instanceOrderValidator = Mockito.inOrder(instanceIdNormalizer, instanceIdValidator);
		final InOrder cloudOrderValidator = Mockito.inOrder(cloudIdentifierNormalizer, cloudIdentifierValidator);
		final InOrder serviceDefOrderValidator = Mockito.inOrder(serviceDefNameNormalizer, serviceDefNameValidator);

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");
		systemOrderValidator.verify(systemNameNormalizer).normalize("TemperatureProvider2");
		instanceOrderValidator.verify(instanceIdNormalizer).normalize("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		cloudOrderValidator.verify(cloudIdentifierNormalizer).normalize("LOCAL");
		serviceDefOrderValidator.verify(serviceDefNameNormalizer).normalize("kelvinInfo");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		systemOrderValidator.verify(systemNameValidator).validateSystemName("TemperatureProvider2");
		instanceOrderValidator.verify(instanceIdValidator).validateInstanceIdentifier("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo");
		cloudOrderValidator.verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		serviceDefOrderValidator.verify(serviceDefNameValidator).validateServiceDefinitionName("kelvinInfo");

		assertEquals(AuthorizationLevel.PROVIDER, result.level());
		assertEquals(1, result.providers().size());
		assertEquals("TemperatureProvider2", result.providers().get(0));
		assertEquals(1, result.instanceIds().size());
		assertEquals("PR|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo", result.instanceIds().get(0));
		assertEquals(1, result.cloudIdentifiers().size());
		assertEquals("LOCAL", result.cloudIdentifiers().get(0));
		assertEquals(1, result.targetNames().size());
		assertEquals("kelvinInfo", result.targetNames().get(0));
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestOk2() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				null,
				null,
				null,
				List.of("testEvent"),
				"EVENT_TYPE");

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");
		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");

		final NormalizedQueryRequest result = validator.validateAndNormalizeQueryRequest(request, "testOrigin");

		final InOrder orderValidator = Mockito.inOrder(eventTypeNameNormalizer, eventTypeNameValidator);

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");
		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(instanceIdNormalizer, never()).normalize(anyString());
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		orderValidator.verify(eventTypeNameNormalizer).normalize("testEvent");
		orderValidator.verify(eventTypeNameValidator).validateEventTypeName("testEvent");

		assertEquals(AuthorizationLevel.PROVIDER, result.level());
		assertNull(result.providers());
		assertNull(result.instanceIds());
		assertNull(result.cloudIdentifiers());
		assertEquals(1, result.targetNames().size());
		assertEquals("testEvent", result.targetNames().get(0));
		assertEquals(AuthorizationTargetType.EVENT_TYPE, result.targetType());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestOk3() {
		final PageDTO pageDTO = new PageDTO(0, 11, "ASC", "id");
		final AuthorizationQueryRequestDTO request = new AuthorizationQueryRequestDTO(
				pageDTO,
				"PROVIDER",
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");

		final NormalizedQueryRequest result = validator.validateAndNormalizeQueryRequest(request, "testOrigin");

		verify(pageValidator).validatePageParameter(pageDTO, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, "testOrigin");
		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(instanceIdNormalizer, never()).normalize(anyString());
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		verify(eventTypeNameNormalizer, never()).normalize(anyString());

		assertEquals(AuthorizationLevel.PROVIDER, result.level());
		assertNull(result.providers());
		assertNull(result.instanceIds());
		assertNull(result.cloudIdentifiers());
		assertNull(result.targetNames());
		assertNull(result.targetType());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestOriginNull() {
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(request, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestEmptyList() {
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(List.of());

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(request, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestNullElement() {
		final List<AuthorizationVerifyRequestDTO> list = new ArrayList<>(1);
		list.add(null);
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(request, "testOrigin"));

		assertEquals("Request payload list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestMissingProvider() {
		final List<AuthorizationVerifyRequestDTO> list = List.of(
				new AuthorizationVerifyRequestDTO(
						null,
						"ConsumerName",
						"LOCAL",
						"SERVICE_DEF",
						"testService",
						"op"));
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(request, "testOrigin"));

		assertEquals("Provider is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestMissingConsumer() {
		final List<AuthorizationVerifyRequestDTO> list = List.of(
				new AuthorizationVerifyRequestDTO(
						"ProviderName",
						"",
						"LOCAL",
						"SERVICE_DEF",
						"testService",
						"op"));
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(request, "testOrigin"));

		assertEquals("Consumer is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestMissingTargetType() {
		final List<AuthorizationVerifyRequestDTO> list = List.of(
				new AuthorizationVerifyRequestDTO(
						"ProviderName",
						"ConsumerName",
						"LOCAL",
						null,
						"testService",
						"op"));
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(request, "testOrigin"));

		assertEquals("Target type is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestInvalidTargetType() {
		final List<AuthorizationVerifyRequestDTO> list = List.of(
				new AuthorizationVerifyRequestDTO(
						"ProviderName",
						"ConsumerName",
						"LOCAL",
						"invalid",
						"testService",
						"op"));
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(request, "testOrigin"));

		assertEquals("Target type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestMissingTarget() {
		final List<AuthorizationVerifyRequestDTO> list = List.of(
				new AuthorizationVerifyRequestDTO(
						"ProviderName",
						"ConsumerName",
						"LOCAL",
						"SERVICE_DEF",
						"",
						"op"));
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(request, "testOrigin"));

		assertEquals("Target is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestExceptionInAValidator() {
		final List<AuthorizationVerifyRequestDTO> list = List.of(
				new AuthorizationVerifyRequestDTO(
						"ProviderName",
						"ConsumerName",
						"LOCAL",
						"SERVICE_DEF",
						"testService",
						"op"));
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(list);

		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(systemNameNormalizer.normalize("ConsumerName")).thenReturn("ConsumerName");
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		when(scopeNormalizer.normalize("op")).thenReturn("op");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("ProviderName");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeVerifyListRequest(request, "testOrigin"));

		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(systemNameNormalizer).normalize("ConsumerName");
		verify(cloudIdentifierNormalizer).normalize("LOCAL");
		verify(scopeNormalizer).normalize("op");
		verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestOk1() {
		final List<AuthorizationVerifyRequestDTO> list = List.of(
				new AuthorizationVerifyRequestDTO(
						"ProviderName",
						"ConsumerName",
						"LOCAL",
						"SERVICE_DEF",
						"testService",
						"op"));
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(list);

		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(systemNameNormalizer.normalize("ConsumerName")).thenReturn("ConsumerName");
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		when(scopeNormalizer.normalize("op")).thenReturn("op");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(systemNameValidator).validateSystemName("ConsumerName");
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(scopeValidator).validateScope("op");

		final List<NormalizedVerifyRequest> result = validator.validateAndNormalizeVerifyListRequest(request, "testOrigin");

		final InOrder serviceDefOrderValidator = Mockito.inOrder(serviceDefNameNormalizer, serviceDefNameValidator);
		final InOrder systemOrderValidator = Mockito.inOrder(systemNameNormalizer, systemNameValidator);
		final InOrder cloudOrderValidator = Mockito.inOrder(cloudIdentifierNormalizer, cloudIdentifierValidator);
		final InOrder scopeOrderValidator = Mockito.inOrder(scopeNormalizer, scopeValidator);

		serviceDefOrderValidator.verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		systemOrderValidator.verify(systemNameNormalizer).normalize("ProviderName");
		systemOrderValidator.verify(systemNameNormalizer).normalize("ConsumerName");
		cloudOrderValidator.verify(cloudIdentifierNormalizer).normalize("LOCAL");
		scopeOrderValidator.verify(scopeNormalizer).normalize("op");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ConsumerName");
		cloudOrderValidator.verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		serviceDefOrderValidator.verify(serviceDefNameValidator).validateServiceDefinitionName("testService");
		scopeOrderValidator.verify(scopeValidator).validateScope("op");

		assertEquals(1, result.size());
		assertEquals("ProviderName", result.get(0).provider());
		assertEquals("ConsumerName", result.get(0).consumer());
		assertEquals("LOCAL", result.get(0).cloud());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.get(0).targetType());
		assertEquals("testService", result.get(0).target());
		assertEquals("op", result.get(0).scope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeVerifyListRequestOk2() {
		final List<AuthorizationVerifyRequestDTO> list = List.of(
				new AuthorizationVerifyRequestDTO(
						"ProviderName",
						"ConsumerName",
						null,
						"EVENT_TYPE",
						"testEvent",
						""));
		final AuthorizationVerifyListRequestDTO request = new AuthorizationVerifyListRequestDTO(list);

		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(systemNameNormalizer.normalize("ConsumerName")).thenReturn("ConsumerName");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");
		doNothing().when(systemNameValidator).validateSystemName("ConsumerName");
		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");

		final List<NormalizedVerifyRequest> result = validator.validateAndNormalizeVerifyListRequest(request, "testOrigin");

		final InOrder eventTypeOrderValidator = Mockito.inOrder(eventTypeNameNormalizer, eventTypeNameValidator);
		final InOrder systemOrderValidator = Mockito.inOrder(systemNameNormalizer, systemNameValidator);

		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		eventTypeOrderValidator.verify(eventTypeNameNormalizer).normalize("testEvent");
		systemOrderValidator.verify(systemNameNormalizer).normalize("ProviderName");
		systemOrderValidator.verify(systemNameNormalizer).normalize("ConsumerName");
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		verify(scopeNormalizer, never()).normalize(anyString());
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ProviderName");
		systemOrderValidator.verify(systemNameValidator).validateSystemName("ConsumerName");
		verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		eventTypeOrderValidator.verify(eventTypeNameValidator).validateEventTypeName("testEvent");

		assertEquals(1, result.size());
		assertEquals("ProviderName", result.get(0).provider());
		assertEquals("ConsumerName", result.get(0).consumer());
		assertEquals("LOCAL", result.get(0).cloud());
		assertEquals(AuthorizationTargetType.EVENT_TYPE, result.get(0).targetType());
		assertEquals("testEvent", result.get(0).target());
		assertNull(result.get(0).scope());
	}
}