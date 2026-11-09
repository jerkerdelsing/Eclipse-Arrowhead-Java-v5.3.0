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
package eu.arrowhead.serviceregistry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.model.ServiceLookupFilterModel;
import eu.arrowhead.serviceregistry.service.validation.ServiceDiscoveryValidation;

@SuppressWarnings("checkstyle:magicnumber")
@ExtendWith(MockitoExtension.class)
public class ServiceDiscoveryServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceDiscoveryService service;

	@Mock
	private ServiceRegistrySystemInfo sysInfo;

	@Mock
	private ServiceDiscoveryValidation validator;

	@Mock
	private ServiceInstanceDbService instanceDbService;

	@Mock
	private SystemDbService systemDbService;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private ReservedServiceDefinitions reservedDefinitions;

	// expected error messages
	private static final String VERBOSE_NOT_ALLOWED = "Verbose is not allowed";
	private static final String INVALID_REVOKE = "Revoking other systems' service is forbidden";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.registerService(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.registerService(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceOk() {

		// dto
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080))));
		when(validator.validateAndNormalizeRegisterService(dto, "test origin")).thenReturn(dto);

		// entities in the database
		final System system = new System("TemperatureManager", "{ }", "1.0.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInstance instance = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.0.0",
				system,
				serviceDefinition,
				"5.0.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"priority\" : 2\r\n}");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfacePolicy policy = ServiceInterfacePolicy.NONE;
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(instance, template, "{ }", policy);
		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntry = Map.entry(instance, List.of(instanceInterface));
		when(instanceDbService.createBulk(List.of(dto))).thenReturn(List.of(instanceEntry));

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.1");
		final Device device = new Device("TEST_DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTriplet = Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)));
		when(systemDbService.getByName("TemperatureManager")).thenReturn(Optional.of(systemTriplet));

		// responses after conversion
		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("TEST_DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO systemResponse = new SystemResponseDTO("TemperatureManager", Map.of(), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.100.1")), deviceResponse, null, null);
		final ServiceInstanceInterfaceResponseDTO interfaceResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		final ServiceInstanceResponseDTO response = new ServiceInstanceResponseDTO(
				"TemperatureManager|temperatureManagement|5.0.0",
				systemResponse,
				new ServiceDefinitionResponseDTO("temperatureInfo", null, null),
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(interfaceResponse),
				null,
				null);
		when(dtoConverter.convertServiceInstanceEntityToDTO(instanceEntry, systemTriplet)).thenReturn(response);

		final ServiceInstanceResponseDTO actual = service.registerService(dto, "test origin");
		assertEquals(response, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceOkThrowsInvalidParameterException() {

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080))));
		when(validator.validateAndNormalizeRegisterService(dto, "test origin")).thenReturn(dto);

		when(instanceDbService.createBulk(List.of(dto))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerService(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceOkThrowsInternalServerError() {

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080))));
		when(validator.validateAndNormalizeRegisterService(dto, "test origin")).thenReturn(dto);

		when(instanceDbService.createBulk(List.of(dto))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.registerService(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServicesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.lookupServices(null, false, false, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServicesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.lookupServices(null, false, false, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceRestrictedEmptyMetadataRequirements() {

		// dto
		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				new ArrayList<>(),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));
		when(validator.validateAndNormalizeLookupService(dto, "test origin")).thenReturn(dto);

		// expected argument for getPageByFilters
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("unrestrictedDiscovery", true);
		final ServiceInstanceLookupRequestDTO dtoRestricred = new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));
		final ServiceLookupFilterModel expected = new ServiceLookupFilterModel(dtoRestricred);

		service.lookupServices(dto, false, true, "test origin");
		ArgumentCaptor<ServiceLookupFilterModel> filterModelCaptor = ArgumentCaptor.forClass(ServiceLookupFilterModel.class);
		verify(instanceDbService).getPageByFilters(any(), filterModelCaptor.capture());
		ServiceLookupFilterModel actual = filterModelCaptor.getValue();
		assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceRestrictedNotEmptyMetadataRequirements() {

		// dto
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));
		when(validator.validateAndNormalizeLookupService(dto, "test origin")).thenReturn(dto);

		// expected argument for getPageByFilters
		metadataReq.put("unrestrictedDiscovery", true);
		final ServiceInstanceLookupRequestDTO dtoRestricred = new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));
		final ServiceLookupFilterModel expected = new ServiceLookupFilterModel(dtoRestricred);

		service.lookupServices(dto, false, true, "test origin");
		ArgumentCaptor<ServiceLookupFilterModel> filterModelCaptor = ArgumentCaptor.forClass(ServiceLookupFilterModel.class);
		verify(instanceDbService).getPageByFilters(any(), filterModelCaptor.capture());
		ServiceLookupFilterModel actual = filterModelCaptor.getValue();
		assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceVerboseTrueButNotAllowed() {

		// dto
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));
		when(validator.validateAndNormalizeLookupService(dto, "test origin")).thenReturn(dto);

		final ForbiddenException ex = assertThrows(ForbiddenException.class, () -> service.lookupServices(dto, true, false, "test origin"));
		assertEquals(VERBOSE_NOT_ALLOWED, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testLookupServiceVerboseTrueAndAllowed() {

		// dto
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));
		when(validator.validateAndNormalizeLookupService(dto, "test origin")).thenReturn(dto);
		when(sysInfo.isDiscoveryVerbose()).thenReturn(true);

		// entities in the database
		final System system = new System("AlertProvider", "{\r\n  \"priority\" : 2\r\n}", "16.4.3");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("alertService");
		final ServiceInstance instance = new ServiceInstance(
				"AlertProvider|alertService|16.4.3",
				system,
				serviceDefinition,
				"16.4.3",
				ZonedDateTime.of(2025, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"priority\" : 2\r\n}");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfacePolicy policy = ServiceInterfacePolicy.NONE;
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(instance, template, "{ }", policy);
		final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> servicesWithInterfaces = new PageImpl<>(List.of(Map.entry(instance, List.of(instanceInterface))));
		when(instanceDbService.getPageByFilters(any(), any())).thenReturn(servicesWithInterfaces);

		// entities for verbose
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.1");
		final Device device = new Device("TEST_DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemsWithDevices = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));
		when(systemDbService.getPageByFilters(any(), any(), any(), any(), any(), any(), any())).thenReturn(systemsWithDevices);

		service.lookupServices(dto, true, false, "test origin");

		// check if the right filter was used
		ArgumentCaptor<ServiceLookupFilterModel> filterModelCaptor = ArgumentCaptor.forClass(ServiceLookupFilterModel.class);
		verify(instanceDbService).getPageByFilters(any(), filterModelCaptor.capture());
		ServiceLookupFilterModel actualFilterModel = filterModelCaptor.getValue();
		assertThat(actualFilterModel).usingRecursiveComparison().isEqualTo(new ServiceLookupFilterModel(dto));

		// check if the expected entities were returned
		ArgumentCaptor<Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>>> verboseEntitiesCaptor = ArgumentCaptor.forClass(Page.class);
		verify(dtoConverter).convertServiceInstanceListToDTO(any(), verboseEntitiesCaptor.capture());
		Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actualVerboseEntities = verboseEntitiesCaptor.getValue();
		assertThat(actualVerboseEntities).usingRecursiveComparison().isEqualTo(systemsWithDevices);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testLookupServiceVerboseFalse() {

		// dto
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));
		when(validator.validateAndNormalizeLookupService(dto, "test origin")).thenReturn(dto);
		when(sysInfo.isDiscoveryVerbose()).thenReturn(true);

		// entities in the database
		final System system = new System("AlertProvider", "{\r\n  \"priority\" : 2\r\n}", "16.4.3");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("alertService");
		final ServiceInstance instance = new ServiceInstance(
				"AlertProvider|alertService|16.4.3",
				system,
				serviceDefinition,
				"16.4.3",
				ZonedDateTime.of(2025, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"priority\" : 2\r\n}");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfacePolicy policy = ServiceInterfacePolicy.NONE;
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(instance, template, "{ }", policy);
		final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> servicesWithInterfaces = new PageImpl<>(List.of(Map.entry(instance, List.of(instanceInterface))));
		when(instanceDbService.getPageByFilters(any(), any())).thenReturn(servicesWithInterfaces);

		service.lookupServices(dto, true, false, "test origin");

		// check if the verbose related parameter is null
		ArgumentCaptor<Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>>> verboseEntitiesCaptor = ArgumentCaptor.forClass(Page.class);
		verify(dtoConverter).convertServiceInstanceListToDTO(any(), verboseEntitiesCaptor.capture());
		Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actualVerboseEntities = verboseEntitiesCaptor.getValue();
		assertThat(actualVerboseEntities).usingRecursiveComparison().isEqualTo(null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceThrowsInternalServerError() {

		// dto
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));
		when(validator.validateAndNormalizeLookupService(dto, "test origin")).thenReturn(dto);
		when(instanceDbService.getPageByFilters(any(), any())).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.lookupServices(dto, true, false, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeServiceNameNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeService(null, null, null));

		assertEquals("identifiedSystemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeServiceNameEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeService(null, null, null));

		assertEquals("identifiedSystemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeServiceOrigionNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeService("SystemName", null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeServiceOrigionEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeService("SystemName", null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeServiceTryingToRevokeSomebodyElse() {

		final String instanceId = "AlertProvider|alertService|16.4.3";
		final String identifiedName = "EvilSystem";
		when(validator.validateAndNormalizeRevokeService(identifiedName, instanceId, "test origin")).thenReturn(Map.entry(identifiedName, instanceId));
		final ForbiddenException ex = assertThrows(ForbiddenException.class, () -> service.revokeService(identifiedName, instanceId, "test origin"));
		assertEquals(INVALID_REVOKE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeServiceOk() {

		final String instanceId = "AlertProvider|alertService|16.4.3";
		final String identifiedName = "AlertProvider";
		when(validator.validateAndNormalizeRevokeService(identifiedName, instanceId, "test origin")).thenReturn(Map.entry(identifiedName, instanceId));
		when(instanceDbService.deleteByInstanceId(instanceId)).thenReturn(true);
		assertTrue(service.revokeService(identifiedName, instanceId, "test origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeServiceThrowsInternalServerError() {

		final String instanceId = "AlertProvider|alertService|16.4.3";
		final String identifiedName = "AlertProvider";
		when(validator.validateAndNormalizeRevokeService(identifiedName, instanceId, "test origin")).thenReturn(Map.entry(identifiedName, instanceId));
		when(instanceDbService.deleteByInstanceId(instanceId)).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.revokeService(identifiedName, instanceId, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}
}
