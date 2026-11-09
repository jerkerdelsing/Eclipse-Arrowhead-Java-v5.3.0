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
package eu.arrowhead.serviceorchestration.service.model.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.QoSRequirementDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@ExtendWith(MockitoExtension.class)
public class OrchestrationFormValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationFormValidation validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private ServiceOperationNameValidator serviceOpNameValidator;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private OrchestrationFormNormalization normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormBasic() {
		final String origin = "test.origin";
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, null, null, null);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequestDTO);

		assertDoesNotThrow(() -> validator.validateOrchestrationForm(orchestrationForm, origin));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormComplex() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestManager", "TestConsumer", orchestrationRequestDTO);

		assertDoesNotThrow(() -> validator.validateOrchestrationForm(orchestrationForm, origin));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormNullInput() {
		final String origin = "test.origin";

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(null, origin));

		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormMissingRequesterSystem() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Requester system name is empty", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormMissingTargetSystem() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Target system name is empty", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormMissingServiceDefinition() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Service definition is empty", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormEmptyOperation() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write", ""), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Operation list contains empty element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormEmptyVersion() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0", ""),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Version list contains empty element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormNullFlagValue() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final Map<String, Boolean> flags = new HashMap<>();
		flags.put(OrchestrationFlag.MATCHMAKING.name(), null);
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, flags, null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Orchestration flag map contains null value", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormInvalidDuration() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 0);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Exclusivity duration must be greater than 0", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormInvalidAlivesAt() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				"2025-12-15 14:05", List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Alives at time has an invalid time format", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormNullMetatadatRequirement() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final List<MetadataRequirementDTO> metadataReqList = new ArrayList<>();
		metadataReqList.add(metadataReq);
		metadataReqList.add(null);
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), metadataReqList, List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Metadata requirement list contains null element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormEmptyInterfaceTemplateName() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http", ""), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Interface template name list contains empty element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormEmptyAddressType() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4", ""), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Interface address type list contains empty element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormNullInterfacePropRequirement() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final List<MetadataRequirementDTO> interfacePropsReqList = new ArrayList<>();
		interfacePropsReqList.add(interfacePropsReq);
		interfacePropsReqList.add(null);
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), interfacePropsReqList,
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Interface property requirement list contains null element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormEmptySecurityPolicy() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH", ""), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Security policy list contains empty element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormEmptyPreferredProvider() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", ""));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Preferred provider list contains empty element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormEmptyQoSPrefernceType() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final List<QoSRequirementDTO> qosPref = List.of(new QoSRequirementDTO("  ", "FILTER", null));

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), qosPref, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("QoS type is empty", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormEmptyQoSOperation() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final List<QoSRequirementDTO> qosPref = List.of(new QoSRequirementDTO("test-qos", "  ", null));

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), qosPref, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("QoS operation is empty", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationFormInvalidQoSOperation() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final List<QoSRequirementDTO> qosPref = List.of(new QoSRequirementDTO("test-qos", "fhfdh", null));

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), qosPref, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("RequesterSystem", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationForm(orchestrationForm, origin));

		assertEquals("Invalid QoS operation", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationFormBasic() {
		final String origin = "test.origin";
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, null, null, null);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequestDTO);

		assertDoesNotThrow(() -> validator.validateAndNormalizeOrchestrationForm(orchestrationForm, false, origin));

		verify(normalizer).normalizeOrchestrationForm(eq(orchestrationForm));
		verify(systemNameValidator, times(2)).validateSystemName(eq("TestConsumer"));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq("testService"));
		verify(serviceOpNameValidator, never()).validateServiceOperationName(eq("read"));
		verify(serviceOpNameValidator, never()).validateServiceOperationName(eq("write"));
		verify(interfaceTemplateNameValidator, never()).validateInterfaceTemplateName(eq("generic_http"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider1"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider2"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationFormComplex() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true),
				List.of(new QoSRequirementDTO("test-qos", "FILTER", Map.of("something", "xyz"))), 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestManager", "TestConsumer", orchestrationRequestDTO);

		assertDoesNotThrow(() -> validator.validateAndNormalizeOrchestrationForm(orchestrationForm, false, origin));

		verify(normalizer).normalizeOrchestrationForm(eq(orchestrationForm));
		verify(systemNameValidator).validateSystemName(eq("TestManager"));
		verify(systemNameValidator).validateSystemName(eq("TestConsumer"));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq("testService"));
		verify(serviceOpNameValidator).validateServiceOperationName(eq("read"));
		verify(serviceOpNameValidator).validateServiceOperationName(eq("write"));
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName(eq("generic_http"));
		verify(systemNameValidator).validateSystemName(eq("PreferredProvider1"));
		verify(systemNameValidator).validateSystemName(eq("PreferredProvider2"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationFormInvalidFlag() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of("NOTAFLAG", true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestManager", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeOrchestrationForm(orchestrationForm, false, origin));

		verify(normalizer).normalizeOrchestrationForm(eq(orchestrationForm));
		verify(systemNameValidator).validateSystemName(eq("TestManager"));
		verify(systemNameValidator).validateSystemName(eq("TestConsumer"));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq("testService"));
		verify(serviceOpNameValidator, never()).validateServiceOperationName(eq("read"));
		verify(serviceOpNameValidator, never()).validateServiceOperationName(eq("write"));
		verify(interfaceTemplateNameValidator, never()).validateInterfaceTemplateName(eq("generic_http"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider1"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider2"));

		assertEquals("Invalid orchestration flag: NOTAFLAG", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationFormInvalidAddressType() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV10"), List.of(interfacePropsReq),
				List.of("CERT_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestManager", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeOrchestrationForm(orchestrationForm, false, origin));

		verify(normalizer).normalizeOrchestrationForm(eq(orchestrationForm));
		verify(systemNameValidator).validateSystemName(eq("TestManager"));
		verify(systemNameValidator).validateSystemName(eq("TestConsumer"));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq("testService"));
		verify(serviceOpNameValidator).validateServiceOperationName(eq("read"));
		verify(serviceOpNameValidator).validateServiceOperationName(eq("write"));
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName(eq("generic_http"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider1"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider2"));

		assertEquals("Invalid interface address type: IPV10", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationFormInvalidSecurity() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("INVALID_SECURITY"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestManager", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeOrchestrationForm(orchestrationForm, true, origin));

		verify(normalizer).normalizeOrchestrationForm(eq(orchestrationForm));
		verify(systemNameValidator).validateSystemName(eq("TestManager"));
		verify(systemNameValidator).validateSystemName(eq("TestConsumer"));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq("testService"));
		verify(serviceOpNameValidator).validateServiceOperationName(eq("read"));
		verify(serviceOpNameValidator).validateServiceOperationName(eq("write"));
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName(eq("generic_http"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider1"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider2"));

		assertEquals("Invalid security policy: INVALID_SECURITY", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationFormNotOfferableSecurity() {
		final String origin = "test.origin";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO("testService", List.of("read", "write"), List.of("1.0.0"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of("generic_http"), List.of("IPV4"), List.of(interfacePropsReq),
				List.of("TRANSLATION_BRIDGE_TOKEN_AUTH"), List.of("PreferredProvider1", "PreferredProvider2"));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestManager", "TestConsumer", orchestrationRequestDTO);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeOrchestrationForm(orchestrationForm, true, origin));

		verify(normalizer).normalizeOrchestrationForm(eq(orchestrationForm));
		verify(systemNameValidator).validateSystemName(eq("TestManager"));
		verify(systemNameValidator).validateSystemName(eq("TestConsumer"));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq("testService"));
		verify(serviceOpNameValidator).validateServiceOperationName(eq("read"));
		verify(serviceOpNameValidator).validateServiceOperationName(eq("write"));
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName(eq("generic_http"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider1"));
		verify(systemNameValidator, never()).validateSystemName(eq("PreferredProvider2"));

		assertEquals("Invalid security policy: TRANSLATION_BRIDGE_TOKEN_AUTH", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}
}
