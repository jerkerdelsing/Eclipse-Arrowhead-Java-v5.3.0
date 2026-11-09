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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
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
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.TranslationDiscoveryRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationNegotiationRequestDTO;

@ExtendWith(MockitoExtension.class)
public class TranslationBridgeValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslationBridgeValidation validator;

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
	public void testValidateAndNormalizeDiscoveryRequestOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestRequestNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", null, "origin"));

		assertEquals("Request is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidatesNull() {
		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("candidates list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidatesListEmpty() {
		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("candidates list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidatesListContainsNull() {
		final List<ServiceInstanceResponseDTO> list = new ArrayList<>(1);
		list.add(null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				list,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("candidates list contains null element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInstanceIdNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInstanceIdEmpty() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateProviderNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateProviderNameNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateProviderNameEmpty() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateServiceDefinitionNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service definition name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateServiceDefinitionNameNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service definition name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateServiceDefinitionNameEmpty() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service definition name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceListNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceListEmpty() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceListContainsNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface list contains null element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceTemplateNameNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Template name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceTemplateNameEmpty() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Template name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePolicyNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Policy is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePolicyEmpty() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Policy is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePolicyInvalid() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Policy is invalid: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePropertiesNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface properties are missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePropertiesEmpty() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface properties are missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestOperationNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("operation is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestOperationEmpty() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"",
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("operation is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestInterfaceTempateNamesListNull() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestInterfaceTempateNamesListEmpty() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				List.of(),
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestInterfaceTempateNamesListContainsNull() {
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

		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				list,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestInterfaceTempateNamesListContainsEmptyElement() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				List.of(""),
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestDifferentServiceDefinitions() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate, candidate2),
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider2|testService2|1.0.0")).thenReturn("TestProvider2|testService2|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider2")).thenReturn("TestProvider2");
		when(serviceDefinitionNameNormalizer.normalize("testService2")).thenReturn("testService2");
		when(operationNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(interfaceTemplateNameNormalizer.normalize("generic_mqtt")).thenReturn("generic_mqtt");
		when(dataModelIdentifierNormalizer.normalize(null)).thenReturn(null);
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider2|testService2|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider2");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService2");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("All candidates must contain the same service definition name", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(interfaceTemplateNameNormalizer, times(2)).normalize("generic_http");
		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider2|testService2|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider2");
		verify(serviceDefinitionNameNormalizer).normalize("testService2");
		verify(operationNormalizer).normalize("test-operation");
		verify(interfaceTemplateNameNormalizer).normalize("generic_mqtt");
		verify(dataModelIdentifierNormalizer, times(2)).normalize(null);
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
	public void testValidateAndNormalizeDiscoveryRequestOk1() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		when(operationNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(interfaceTemplateNameNormalizer.normalize("generic_mqtt")).thenReturn("generic_mqtt");
		when(dataModelIdentifierNormalizer.normalize(null)).thenReturn(null);
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		doNothing().when(operationValidator).validateServiceOperationName("test-operation");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");

		final NormalizedTranslationDiscoveryRequestDTO normalized = validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin");

		assertNotNull(normalized);
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

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(interfaceTemplateNameNormalizer).normalize("generic_http");
		verify(operationNormalizer).normalize("test-operation");
		verify(interfaceTemplateNameNormalizer).normalize("generic_mqtt");
		verify(dataModelIdentifierNormalizer, times(2)).normalize(null);
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		verify(operationValidator).validateServiceOperationName("test-operation");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");
		verify(dataModelIdentifierValidator, never()).validateDataModelIdentifier(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestOk2() {
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

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				List.of("generic_mqtt"),
				"testXml",
				"testJson");

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		when(operationNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(interfaceTemplateNameNormalizer.normalize("generic_mqtt")).thenReturn("generic_mqtt");
		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		when(dataModelIdentifierNormalizer.normalize("testJson")).thenReturn("testJson");
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		doNothing().when(operationValidator).validateServiceOperationName("test-operation");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testJson");

		final NormalizedTranslationDiscoveryRequestDTO normalized = validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin");

		assertNotNull(normalized);
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
		assertEquals("testXml", normalized.inputDataModelId());
		assertEquals("testJson", normalized.outputDataModelId());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(interfaceTemplateNameNormalizer).normalize("generic_http");
		verify(operationNormalizer).normalize("test-operation");
		verify(interfaceTemplateNameNormalizer).normalize("generic_mqtt");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierNormalizer).normalize("testJson");
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		verify(operationValidator).validateServiceOperationName("test-operation");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testJson");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeNegotiationRequest(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeNegotiationRequest(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestRequestNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationRequest(null, "origin"));

		assertEquals("Request is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestTargetNull() {
		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				null,
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationRequest(dto, "origin"));

		assertEquals("Target is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestWithBridgeIdBridgeIdInvalid() {
		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				"invalid",
				new ServiceInstanceResponseDTO(
						null,
						null,
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
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationRequest(dto, "origin"));

		assertEquals("Bridge id is invalid: invalid", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestWithBridgeIdServiceInstanceIdNull() {
		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				"581fd924-d8b0-4548-8cf8-4334e9f3cba2",
				new ServiceInstanceResponseDTO(
						null,
						null,
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
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationRequest(dto, "origin"));

		assertEquals("Service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestWithBridgeIdServiceInstanceIdEmpty() {
		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				"581fd924-d8b0-4548-8cf8-4334e9f3cba2",
				new ServiceInstanceResponseDTO(
						"",
						null,
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
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationRequest(dto, "origin"));

		assertEquals("Service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestWithBridgeIdInvalidParameterException() {
		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				"581fd924-d8b0-4548-8cf8-4334e9f3cba2",
				new ServiceInstanceResponseDTO(
						"TestProvider/testService/1.0.0",
						null,
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
				null);

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider/testService/1.0.0")).thenReturn("TestProvider/testService/1.0.0");
		when(operationNormalizer.normalize(null)).thenReturn(null);
		when(interfaceTemplateNameNormalizer.normalize(null)).thenReturn(null);
		when(dataModelIdentifierNormalizer.normalize(null)).thenReturn(null);
		doThrow(new InvalidParameterException("test")).when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider/testService/1.0.0");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationRequest(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider/testService/1.0.0");
		verify(operationNormalizer).normalize(null);
		verify(interfaceTemplateNameNormalizer).normalize(null);
		verify(dataModelIdentifierNormalizer, times(2)).normalize(null);
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider/testService/1.0.0");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestWithBridgeIdOk() {
		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				"581fd924-d8b0-4548-8cf8-4334e9f3cba2",
				new ServiceInstanceResponseDTO(
						"TestProvider|testService|1.0.0",
						null,
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
				null);

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		when(operationNormalizer.normalize(null)).thenReturn(null);
		when(interfaceTemplateNameNormalizer.normalize(null)).thenReturn(null);
		when(dataModelIdentifierNormalizer.normalize(null)).thenReturn(null);
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");

		final NormalizedTranslationNegotiationRequestDTO normalized = validator.validateAndNormalizeNegotiationRequest(dto, "origin");

		assertNotNull(normalized);
		assertEquals(UUID.fromString("581fd924-d8b0-4548-8cf8-4334e9f3cba2"), normalized.bridgeId());
		assertNotNull(normalized.target());
		assertEquals("TestProvider|testService|1.0.0", normalized.target().instanceId());
		assertNull(normalized.operation());
		assertNull(normalized.interfaceTemplateName());
		assertNull(normalized.inputDataModelId());
		assertNull(normalized.outputDataModelId());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(operationNormalizer).normalize(null);
		verify(interfaceTemplateNameNormalizer).normalize(null);
		verify(dataModelIdentifierNormalizer, times(2)).normalize(null);
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestWithoutBridgeIdEmbeddedDiscovery() {
		// we test this with getting an InvalidParameterException because of missing provider (this one is not checked when bridge id is specified)

		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				null,
				new ServiceInstanceResponseDTO(
						"TestProvider|testService|1.0.0",
						null,
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
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeNegotiationRequest(dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestWithoutBridgeIdOk1() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				null,
				new ServiceInstanceResponseDTO(
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
						null,
						List.of(intf),
						null,
						null),
				"test-operation",
				"generic_mqtt",
				null,
				null);

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		when(operationNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(interfaceTemplateNameNormalizer.normalize("generic_mqtt")).thenReturn("generic_mqtt");
		when(dataModelIdentifierNormalizer.normalize(null)).thenReturn(null);
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		doNothing().when(operationValidator).validateServiceOperationName("test-operation");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");

		final NormalizedTranslationNegotiationRequestDTO normalized = validator.validateAndNormalizeNegotiationRequest(dto, "origin");

		assertNotNull(normalized);
		assertNull(normalized.bridgeId());
		assertNotNull(normalized.target());
		assertEquals("TestProvider|testService|1.0.0", normalized.target().instanceId());
		assertEquals("TestProvider", normalized.target().provider());
		assertEquals("testService", normalized.target().serviceDefinition());
		assertNotNull(normalized.target().interfaces());
		assertEquals(1, normalized.target().interfaces().size());
		assertEquals("generic_http", normalized.target().interfaces().get(0).templateName());
		assertEquals("test-operation", normalized.operation());
		assertEquals("generic_mqtt", normalized.interfaceTemplateName());
		assertNull(normalized.inputDataModelId());
		assertNull(normalized.outputDataModelId());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(interfaceTemplateNameNormalizer).normalize("generic_http");
		verify(operationNormalizer).normalize("test-operation");
		verify(interfaceTemplateNameNormalizer).normalize("generic_mqtt");
		verify(dataModelIdentifierNormalizer, times(2)).normalize(null);
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		verify(operationValidator).validateServiceOperationName("test-operation");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");
		verify(dataModelIdentifierValidator, never()).validateDataModelIdentifier(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNegotiationRequestWithoutBridgeIdOk2() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				null,
				new ServiceInstanceResponseDTO(
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
						null,
						List.of(intf),
						null,
						null),
				"test-operation",
				"generic_mqtt",
				"testXml",
				"testJson");

		when(serviceInstanceIdentifierNormalizer.normalize("TestProvider|testService|1.0.0")).thenReturn("TestProvider|testService|1.0.0");
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(serviceDefinitionNameNormalizer.normalize("testService")).thenReturn("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		when(operationNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(interfaceTemplateNameNormalizer.normalize("generic_mqtt")).thenReturn("generic_mqtt");
		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		when(dataModelIdentifierNormalizer.normalize("testJson")).thenReturn("testJson");
		doNothing().when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		doNothing().when(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		doNothing().when(operationValidator).validateServiceOperationName("test-operation");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");

		final NormalizedTranslationNegotiationRequestDTO normalized = validator.validateAndNormalizeNegotiationRequest(dto, "origin");

		assertNotNull(normalized);
		assertNull(normalized.bridgeId());
		assertNotNull(normalized.target());
		assertEquals("TestProvider|testService|1.0.0", normalized.target().instanceId());
		assertEquals("TestProvider", normalized.target().provider());
		assertEquals("testService", normalized.target().serviceDefinition());
		assertNotNull(normalized.target().interfaces());
		assertEquals(1, normalized.target().interfaces().size());
		assertEquals("generic_http", normalized.target().interfaces().get(0).templateName());
		assertEquals("test-operation", normalized.operation());
		assertEquals("generic_mqtt", normalized.interfaceTemplateName());
		assertEquals("testXml", normalized.inputDataModelId());
		assertEquals("testJson", normalized.outputDataModelId());

		verify(serviceInstanceIdentifierNormalizer).normalize("TestProvider|testService|1.0.0");
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(serviceDefinitionNameNormalizer).normalize("testService");
		verify(interfaceTemplateNameNormalizer).normalize("generic_http");
		verify(operationNormalizer).normalize("test-operation");
		verify(interfaceTemplateNameNormalizer).normalize("generic_mqtt");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierNormalizer).normalize("testJson");
		verify(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TestProvider|testService|1.0.0");
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(serviceDefinitionNameValidator).validateServiceDefinitionName("testService");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		verify(operationValidator).validateServiceOperationName("test-operation");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testJson");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBridgeIdOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeBridgeId(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBridgeIdOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeBridgeId(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBridgeIdIdNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeBridgeId(null, "origin"));

		assertEquals("Bridge identifier is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBridgeIdIdEmpty() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeBridgeId("", "origin"));

		assertEquals("Bridge identifier is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBridgeIdIdInvalid() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeBridgeId("invalid", "origin"));

		assertEquals("Bridge identifier is invalid: invalid", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBridgeIdIdOk() {
		final UUID uuid = validator.validateAndNormalizeBridgeId("fcd92344-e643-4c3f-9dc1-9f5cdff6262a", "origin");

		assertEquals(UUID.fromString("fcd92344-e643-4c3f-9dc1-9f5cdff6262a"), uuid);
	}
}