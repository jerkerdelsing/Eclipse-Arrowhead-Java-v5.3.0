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
package eu.arrowhead.translationmanager.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.TranslationDiscoveryMgmtRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationMgmtRequestDTO;
import eu.arrowhead.dto.TranslationQueryRequestDTO;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationQueryRequestDTO;

@ExtendWith(MockitoExtension.class)
public class TranslationBridgeMgmtValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslationBridgeMgmtValidation validator;

	@Mock
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer;

	@Mock
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefinitionNameNormalizer;

	@Mock
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	@Mock
	private ServiceOperationNameNormalizer operationNormalizer;

	@Mock
	private ServiceOperationNameValidator operationValidator;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private DataModelIdentifierNormalizer dataModelIdentifierNormalizer;

	@Mock
	private DataModelIdentifierValidator dataModelIdentifierValidator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private PageService pageService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester("Consumer", null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester("Consumer", ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester(null, "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterEmpty() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("", "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterInvalid() {
		when(systemNameNormalizer.normalize("inv@lid")).thenReturn("Inv@lid");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("Inv@lid");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("inv@lid", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("inv@lid");
		verify(systemNameValidator).validateSystemName("Inv@lid");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterOk() {
		when(systemNameNormalizer.normalize("consumer")).thenReturn("Consumer");
		doNothing().when(systemNameValidator).validateSystemName("Consumer");

		final String result = validator.validateAndNormalizeRequester("consumer", "origin");

		assertEquals("Consumer", result);

		verify(systemNameNormalizer).normalize("consumer");
		verify(systemNameValidator).validateSystemName("Consumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestRequestNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", null, "origin"));

		assertEquals("Request is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidatesNull() {
		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("candidates list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidatesListEmpty() {
		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("candidates list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidatesListContainsNull() {
		final List<ServiceInstanceResponseDTO> list = new ArrayList<>(1);
		list.add(null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				list,
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("candidates list contains null element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInstanceIdNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInstanceIdEmpty() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateProviderNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateProviderNameNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						null,
						null,
						null,
						null,
						null,
						null,
						null),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateProviderNameEmpty() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"",
						null,
						null,
						null,
						null,
						null,
						null),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateServiceDefinitionNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Service definition name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateServiceDefinitionNameNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO(null, null, null),
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Service definition name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateServiceDefinitionNameEmpty() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("", null, null),
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Service definition name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfaceListNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				null,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("interfaces list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfaceListEmpty() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("interfaces list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfaceListContainsNull() {
		final List<ServiceInstanceInterfaceResponseDTO> intfList = new ArrayList<>(1);
		intfList.add(null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				intfList,
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("interfaces list contains null element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfaceTemplateNameNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				null,
				null,
				null,
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Template name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfaceTemplateNameEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"",
				null,
				null,
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Template name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfacePolicyNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				null,
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Policy is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfacePolicyEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"",
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Policy is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfacePolicyInvalid() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"INVALID",
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Policy is invalid: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfacePropertiesNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Interface properties are missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestCandidateInterfacePropertiesEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Interface properties are missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestConsumerNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("consumer is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestConsumerEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"",
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("consumer is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestOperationNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("operation is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestOperationEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"",
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("operation is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestInterfaceListNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"test-operation",
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestInterfaceListEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"test-operation",
				List.of(),
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestInterfaceListContainsNull() {
		final List<String> intfNames = new ArrayList<>(1);
		intfNames.add(null);

		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"test-operation",
				intfNames,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestInterfaceListContainsEmptyElement() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"test-operation",
				List.of(""),
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestFlagNameNull() {
		final Map<String, Boolean> flagMap = new HashMap<>(1);
		flagMap.put(null, true);

		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testXml",
				flagMap);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Flag name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestFlagNameEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testXml",
				Map.of("", true));

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Flag name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestFlagNameInvalid() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testXml",
				Map.of("INVALID", true));

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("Flag is invalid: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestDifferentServiceDefinitions() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceInterfaceResponseDTO intf2 = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				null,
				"NONE",
				Map.of("accessPort", 12346));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final ServiceInstanceResponseDTO candidate2 = new ServiceInstanceResponseDTO(
				"TestProvider2|testService2|1.0.0",
				new SystemResponseDTO(
						"TestProvider2",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService2", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf2),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate, candidate2),
				"Consumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testXml",
				Map.of("CONSUMER_BLACKLIST_CHECK", true));

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider2|testService2|1.0.0")).thenReturn("TestProvider2|testService2|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider2")).thenReturn("TestProvider2");
		when(serviceDefinitionNameNormalizer.normalize("testService2")).thenReturn("testService2");
		when(systemNameNormalizer.normalize("Consumer")).thenReturn("Consumer");
		when(operationNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(interfaceTemplateNameNormalizer.normalize("generic_mqtt")).thenReturn("generic_mqtt");
		when(dataModelIdentifierNormalizer.normalize("testJson")).thenReturn("testJson");
		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider2|testService2|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider2");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService2");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin"));

		assertEquals("All candidates must contain the same service definition name", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(interfaceTemplateNameNormalizer, times(2)).normalize("generic_http");
		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider2|testService2|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider2");
		verify(serviceDefinitionNameNormalizer).normalize("testService2");
		verify(systemNameNormalizer).normalize("Consumer");
		verify(operationNormalizer).normalize("test-operation");
		verify(interfaceTemplateNameNormalizer).normalize("generic_mqtt");
		verify(dataModelIdentifierNormalizer).normalize("testJson");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider2|testService2|1.0.0");
		verify(systemNameValidator).validateSystemName("TestProvider2");
		verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService2");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestOk1() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final Map<String, Boolean> flags = new HashMap<>(1);
		flags.put("CONSUMER_BLACKLIST_CHECK", null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testXml",
				flags);

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		when(systemNameNormalizer.normalize("Consumer")).thenReturn("Consumer");
		when(operationNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(interfaceTemplateNameNormalizer.normalize("generic_mqtt")).thenReturn("generic_mqtt");
		when(dataModelIdentifierNormalizer.normalize("testJson")).thenReturn("testJson");
		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		doNothing().when(systemNameValidator).validateSystemName("Consumer");
		doNothing().when(operationValidator).validateServiceOperationName("test-operation");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testJson");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");

		final Pair<NormalizedTranslationDiscoveryRequestDTO, Map<TranslationDiscoveryFlag, Boolean>> resultPair = validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin");

		assertNotNull(resultPair);
		final NormalizedTranslationDiscoveryRequestDTO normalized = resultPair.getFirst();
		assertEquals("Consumer", normalized.createdBy());
		assertNotNull(normalized.candidates());
		assertFalse(normalized.candidates().isEmpty());
		assertEquals(1, normalized.candidates().size());
		final NormalizedServiceInstanceDTO nCandidate = normalized.candidates().get(0);
		assertNotNull(nCandidate);
		assertEquals("TestProvider|testService|1.0.0", nCandidate.instanceId());
		assertEquals("TestProvider", nCandidate.provider());
		assertEquals("testService", nCandidate.serviceDefinition());
		assertNotNull(nCandidate.interfaces());
		assertEquals(1, nCandidate.interfaces().size());
		assertEquals(intf, nCandidate.interfaces().get(0));
		assertEquals("Consumer", normalized.consumer());
		assertEquals("test-operation", normalized.operation());
		assertEquals(List.of("generic_mqtt"), normalized.interfaceTemplateNames());
		assertEquals("testJson", normalized.inputDataModelId());
		assertEquals("testXml", normalized.outputDataModelId());
		assertTrue(resultPair.getSecond().isEmpty());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(interfaceTemplateNameNormalizer).normalize("generic_http");
		verify(systemNameNormalizer).normalize("Consumer");
		verify(operationNormalizer).normalize("test-operation");
		verify(interfaceTemplateNameNormalizer).normalize("generic_mqtt");
		verify(dataModelIdentifierNormalizer).normalize("testJson");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		verify(systemNameValidator).validateSystemName("Consumer");
		verify(operationValidator).validateServiceOperationName("test-operation");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testJson");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryMgmtRequestOk2() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryMgmtRequestDTO dto = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"Consumer",
				"test-operation",
				List.of("generic_mqtt"),
				"",
				"",
				null);

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		when(systemNameNormalizer.normalize("Consumer")).thenReturn("Consumer");
		when(operationNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(interfaceTemplateNameNormalizer.normalize("generic_mqtt")).thenReturn("generic_mqtt");
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		doNothing().when(systemNameValidator).validateSystemName("Consumer");
		doNothing().when(operationValidator).validateServiceOperationName("test-operation");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");

		final Pair<NormalizedTranslationDiscoveryRequestDTO, Map<TranslationDiscoveryFlag, Boolean>> resultPair = validator.validateAndNormalizeDiscoveryMgmtRequest("Consumer", dto, "origin");

		assertNotNull(resultPair);
		final NormalizedTranslationDiscoveryRequestDTO normalized = resultPair.getFirst();
		assertEquals("Consumer", normalized.createdBy());
		assertNotNull(normalized.candidates());
		assertFalse(normalized.candidates().isEmpty());
		assertEquals(1, normalized.candidates().size());
		final NormalizedServiceInstanceDTO nCandidate = normalized.candidates().get(0);
		assertNotNull(nCandidate);
		assertEquals("TestProvider|testService|1.0.0", nCandidate.instanceId());
		assertEquals("TestProvider", nCandidate.provider());
		assertEquals("testService", nCandidate.serviceDefinition());
		assertNotNull(nCandidate.interfaces());
		assertEquals(1, nCandidate.interfaces().size());
		assertEquals(intf, nCandidate.interfaces().get(0));
		assertEquals("Consumer", normalized.consumer());
		assertEquals("test-operation", normalized.operation());
		assertEquals(List.of("generic_mqtt"), normalized.interfaceTemplateNames());
		assertNull(normalized.inputDataModelId());
		assertNull(normalized.outputDataModelId());
		assertTrue(resultPair.getSecond().isEmpty());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(interfaceTemplateNameNormalizer).normalize("generic_http");
		verify(systemNameNormalizer).normalize("Consumer");
		verify(operationNormalizer).normalize("test-operation");
		verify(interfaceTemplateNameNormalizer).normalize("generic_mqtt");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("");
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		verify(systemNameValidator).validateSystemName("Consumer");
		verify(operationValidator).validateServiceOperationName("test-operation");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");
		verify(dataModelIdentifierValidator, never()).validateDataModelIdentifier(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeNegotiationMgmtRequest(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeNegotiationMgmtRequest(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestDtoNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationMgmtRequest(null, "origin"));

		assertEquals("Request is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestBridgeIdNull() {
		final TranslationNegotiationMgmtRequestDTO dto = new TranslationNegotiationMgmtRequestDTO(null, null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationMgmtRequest(dto, "origin"));

		assertEquals("Bridge id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestBridgeIdEmpty() {
		final TranslationNegotiationMgmtRequestDTO dto = new TranslationNegotiationMgmtRequestDTO("", null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationMgmtRequest(dto, "origin"));

		assertEquals("Bridge id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestBridgeIdInvalid() {
		final TranslationNegotiationMgmtRequestDTO dto = new TranslationNegotiationMgmtRequestDTO("invalid", null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationMgmtRequest(dto, "origin"));

		assertEquals("Bridge id is invalid: invalid", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestTargetInstanceIdNull() {
		final TranslationNegotiationMgmtRequestDTO dto = new TranslationNegotiationMgmtRequestDTO("2240efa3-fde4-4f81-a625-04f1234acee7", null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationMgmtRequest(dto, "origin"));

		assertEquals("Target service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestTargetInstanceIdEmpty() {
		final TranslationNegotiationMgmtRequestDTO dto = new TranslationNegotiationMgmtRequestDTO("2240efa3-fde4-4f81-a625-04f1234acee7", "");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationMgmtRequest(dto, "origin"));

		assertEquals("Target service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestTargetInstanceIdInvalid() {
		final TranslationNegotiationMgmtRequestDTO dto = new TranslationNegotiationMgmtRequestDTO("2240efa3-fde4-4f81-a625-04f1234acee7", "TestProvider$testService$1.0.0");

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider$testService$1.0.0")).thenReturn("TestProvider$testService$1.0.0");
		doThrow(new InvalidParameterException("test")).when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider$testService$1.0.0");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationMgmtRequest(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider$testService$1.0.0");
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider$testService$1.0.0");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationMgmtRequestOk() {
		final TranslationNegotiationMgmtRequestDTO dto = new TranslationNegotiationMgmtRequestDTO("2240efa3-fde4-4f81-a625-04f1234acee7", "TestProvider|testService|1.0.0");

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");

		final Pair<UUID, String> result = validator.validateAndNormalizeNegotiationMgmtRequest(dto, "origin");

		assertEquals(UUID.fromString("2240efa3-fde4-4f81-a625-04f1234acee7"), result.getFirst());
		assertEquals("TestProvider|testService|1.0.0", result.getSecond());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAbortMgmtRequestOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeAbortMgmtRequest(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAbortMgmtRequestOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeAbortMgmtRequest(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAbortMgmtRequestListNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAbortMgmtRequest(null, "origin"));

		assertEquals("Bridge id list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAbortMgmtRequestListEmpty() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAbortMgmtRequest(List.of(), "origin"));

		assertEquals("Bridge id list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAbortMgmtRequestListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAbortMgmtRequest(list, "origin"));

		assertEquals("Bridge id list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAbortMgmtRequestListContainsEmptyElement() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAbortMgmtRequest(List.of(""), "origin"));

		assertEquals("Bridge id list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAbortMgmtRequestIdentifierInvalid() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeAbortMgmtRequest(List.of("invalid"), "origin"));

		assertEquals("Bridge identifier is invalid: invalid", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeAbortMgmtRequestOk() {
		final List<UUID> result = validator.validateAndNormalizeAbortMgmtRequest(List.of("2240efa3-fde4-4f81-a625-04f1234acee7"), "origin");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(UUID.fromString("2240efa3-fde4-4f81-a625-04f1234acee7"), result.get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestBridgeIdListContainsNull() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				list,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Bridge id list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestBridgeIdListContainsEmptyElement() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				List.of(""),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Bridge id list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestBridgeIdInvalid() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				List.of("invalid"),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Bridge id is invalid: invalid", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestCreatorListContainsNull() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				List.of("2240efa3-fde4-4f81-a625-04f1234acee7"),
				list,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Creator list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestCreatorListContainsEmptyElement() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				List.of(""),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Creator list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestStatusListContainsNull() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				List.of("AdminSystem"),
				list,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Status list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestStatusListContainsEmptyElement() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				List.of(""),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Status list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestStatusInvalid() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				List.of("INVALID"),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Invalid status: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestConsumerListContainsNull() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				List.of("USED"),
				list,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Consumer list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestConsumerListContainsEmptyElement() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				List.of(""),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Consumer list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestProviderListContainsNull() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				List.of("TestConsumer"),
				list,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Provider list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestProviderListContainsEmptyElement() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				List.of(""),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Provider list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestServiceDefinitionListContainsNull() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				List.of("TestProvider"),
				list,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Service definition list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestServiceDefinitionListContainsEmptyElement() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				List.of(""),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Service definition list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestInterfaceTranslatorListContainsNull() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				List.of("testService"),
				list,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Interface translator list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestInterfaceTranslatorListContainsEmptyElement() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				List.of(""),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Interface translator list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestDataModelTranslatorListContainsNull() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				List.of("TestInterfaceTranslator"),
				list,
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Data model translator list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestDataModelTranslatorListEmptyElement() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				List.of(""),
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Data model translator list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestInvalidCreationFrom() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				List.of("TestDataModelTranslator"),
				"invalid",
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Minimum creation time has an invalid time format", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestInvalidCreationTo() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"2026-01-12T10:00:00Z",
				"invalid",
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Maximum creation time has an invalid time format", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestEmptyCreationInterval() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"2026-01-12T10:00:00Z",
				"2026-01-12T09:00:00Z",
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Empty creation time interval", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestInvalidAliveFrom() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"2026-01-12T10:00:00Z",
				"2026-01-12T12:00:00Z",
				"invalid",
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Minimum alive time has an invalid time format", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestInvalidAliveTo() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"2026-01-12T10:00:00Z",
				null,
				"2026-01-12T12:00:00Z",
				"invalid",
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Maximum alive time has an invalid time format", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestEmptyAliveInterval() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"2026-01-12T12:00:00Z",
				"2026-01-12T10:00:00Z",
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Empty alive time interval", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestInvalidMinUsage() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"2026-01-12T10:00:00Z",
				"2026-01-12T12:00:00Z",
				-1,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Minimum usage number must be a non-negative number", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestInvalidMaxUsage() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"2026-01-12T10:00:00Z",
				null,
				null,
				-1);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Maximum usage number must be a non-negative number", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestEmptyUsageInterval() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "serviceDefinition");

		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDto,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				10,
				5);

		doNothing().when(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("Empty usage interval", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, BridgeDetails.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestNoDTOOk() {
		final PageRequest pageRequest = PageRequest.of(0, 10, Direction.DESC, "header_createdAt");
		when(pageService.getPageRequest(null, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(pageRequest);

		final NormalizedTranslationQueryRequestDTO result = validator.validateAndNormalizeQueryMgmtRequest(null, "origin");

		assertNotNull(result);
		assertEquals(pageRequest, result.pageRequest());
		assertNull(result.bridgeIds());
		assertNull(result.creators());
		assertNull(result.statuses());
		assertNull(result.consumers());
		assertNull(result.providers());
		assertNull(result.serviceDefinitions());
		assertNull(result.interfaceTranslators());
		assertNull(result.dataModelTranslators());
		assertNull(result.creationFrom());
		assertNull(result.creationTo());
		assertNull(result.alivesFrom());
		assertNull(result.alivesTo());
		assertNull(result.minUsage());
		assertNull(result.maxUsage());

		verify(pageService).getPageRequest(null, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestNullPaginationOk() {
		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				5,
				10);

		final PageRequest pageRequest = PageRequest.of(0, 10, Direction.DESC, "header_createdAt");
		when(pageService.getPageRequest(null, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(pageRequest);

		final NormalizedTranslationQueryRequestDTO result = validator.validateAndNormalizeQueryMgmtRequest(dto, "origin");

		assertNotNull(result);
		assertEquals(pageRequest, result.pageRequest());
		assertNull(result.bridgeIds());
		assertNull(result.creators());
		assertNull(result.statuses());
		assertNull(result.consumers());
		assertNull(result.providers());
		assertNull(result.serviceDefinitions());
		assertNull(result.interfaceTranslators());
		assertNull(result.dataModelTranslators());
		assertNull(result.creationFrom());
		assertNull(result.creationTo());
		assertNull(result.alivesFrom());
		assertNull(result.alivesTo());
		assertEquals(5, result.minUsage());
		assertEquals(10, result.maxUsage());

		verify(pageService).getPageRequest(null, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestNullSortFieldOk() {
		final PageDTO pageDTO = new PageDTO(0, 10, "DESC", null);
		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDTO,
				List.of("2240efa3-fde4-4f81-a625-04f1234acee7"),
				List.of("Creator"),
				List.of("USED"),
				List.of("TestConsumer"),
				List.of("TestProvider"),
				List.of("testService"),
				List.of("TestInterfaceTranslator"),
				List.of("TestDataModelTranslator"),
				"2026-01-12T10:00:00Z",
				"2026-01-12T12:00:00Z",
				"2026-01-12T12:00:00Z",
				"2026-01-12T13:00:00Z",
				null,
				10);

		final PageRequest pageRequest = PageRequest.of(0, 10, Direction.DESC, "header_createdAt");

		when(pageService.getPageRequest(pageDTO, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(pageRequest);
		when(systemNameNormalizer.normalize("Creator")).thenReturn("Creator");
		when(systemNameNormalizer.normalize("TestConsumer")).thenReturn("TestConsumer");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(systemNameNormalizer.normalize("TestInterfaceTranslator")).thenReturn("TestInterfaceTranslator");
		when(systemNameNormalizer.normalize("TestDataModelTranslator")).thenReturn("TestDataModelTranslator");
		doNothing().when(systemNameValidator).validateSystemName("Creator");
		doNothing().when(systemNameValidator).validateSystemName("TestConsumer");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(systemNameValidator).validateSystemName("TestInterfaceTranslator");
		doNothing().when(systemNameValidator).validateSystemName("TestDataModelTranslator");

		final NormalizedTranslationQueryRequestDTO result = validator.validateAndNormalizeQueryMgmtRequest(dto, "origin");

		assertNotNull(result);
		assertEquals(pageRequest, result.pageRequest());
		assertEquals(List.of(UUID.fromString("2240efa3-fde4-4f81-a625-04f1234acee7")), result.bridgeIds());
		assertEquals(List.of("Creator"), result.creators());
		assertEquals(List.of(TranslationBridgeStatus.USED), result.statuses());
		assertEquals(List.of("TestConsumer"), result.consumers());
		assertEquals(List.of("TestProvider"), result.providers());
		assertEquals(List.of("testService"), result.serviceDefinitions());
		assertEquals(List.of("TestInterfaceTranslator"), result.interfaceTranslators());
		assertEquals(List.of("TestDataModelTranslator"), result.dataModelTranslators());
		assertEquals(Utilities.parseUTCStringToZonedDateTime("2026-01-12T10:00:00Z"), result.creationFrom());
		assertEquals(Utilities.parseUTCStringToZonedDateTime("2026-01-12T12:00:00Z"), result.creationTo());
		assertEquals(Utilities.parseUTCStringToZonedDateTime("2026-01-12T12:00:00Z"), result.alivesFrom());
		assertEquals(Utilities.parseUTCStringToZonedDateTime("2026-01-12T13:00:00Z"), result.alivesTo());
		assertNull(result.minUsage());
		assertEquals(10, result.maxUsage());

		verify(pageService).getPageRequest(pageDTO, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter");
		verify(systemNameNormalizer).normalize("Creator");
		verify(systemNameNormalizer).normalize("TestConsumer");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(systemNameNormalizer).normalize("TestInterfaceTranslator");
		verify(systemNameNormalizer).normalize("TestDataModelTranslator");
		verify(systemNameValidator).validateSystemName("Creator");
		verify(systemNameValidator).validateSystemName("TestConsumer");
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(systemNameValidator).validateSystemName("TestInterfaceTranslator");
		verify(systemNameValidator).validateSystemName("TestDataModelTranslator");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testValidateAndNormalizeQueryMgmtRequestInvalidParamterException() {
		final PageDTO pageDTO = new PageDTO(0, 10, "DESC", "createdAt");
		final PageDTO pageDTO2 = new PageDTO(0, 10, "DESC", "header_createdAt");
		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				pageDTO,
				List.of("2240efa3-fde4-4f81-a625-04f1234acee7"),
				List.of("Cre@tor"),
				List.of("USED"),
				List.of("TestConsumer"),
				List.of("TestProvider"),
				List.of("testService"),
				List.of("TestInterfaceTranslator"),
				List.of("TestDataModelTranslator"),
				null,
				null,
				null,
				null,
				5,
				null);

		final PageRequest pageRequest = PageRequest.of(0, 10, Direction.DESC, "header_createdAt");

		when(pageService.getPageRequest(pageDTO2, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(pageRequest);
		when(systemNameNormalizer.normalize("Cre@tor")).thenReturn("Cre@tor");
		when(systemNameNormalizer.normalize("TestConsumer")).thenReturn("TestConsumer");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(systemNameNormalizer.normalize("TestInterfaceTranslator")).thenReturn("TestInterfaceTranslator");
		when(systemNameNormalizer.normalize("TestDataModelTranslator")).thenReturn("TestDataModelTranslator");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("Cre@tor");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryMgmtRequest(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageService).getPageRequest(pageDTO2, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter");
		verify(systemNameNormalizer).normalize("Cre@tor");
		verify(systemNameNormalizer).normalize("TestConsumer");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(systemNameNormalizer).normalize("TestInterfaceTranslator");
		verify(systemNameNormalizer).normalize("TestDataModelTranslator");
		verify(systemNameValidator).validateSystemName("Cre@tor");
	}
}