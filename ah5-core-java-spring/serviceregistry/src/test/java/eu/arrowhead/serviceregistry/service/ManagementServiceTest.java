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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceCreateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListResponseDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateQueryRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateResponseDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.service.DeviceDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceDefinitionDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.model.ServiceLookupFilterModel;
import eu.arrowhead.serviceregistry.service.validation.ManagementValidation;

@SuppressWarnings("checkstyle:magicnumber")
@ExtendWith(MockitoExtension.class)
public class ManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementService service;

	@Mock
	private ManagementValidation validator;

	@Mock
	private PageService pageService;

	@Mock
	private DeviceDbService deviceDbService;

	@Mock
	private ServiceDefinitionDbService serviceDefinitionDbService;

	@Mock
	private SystemDbService systemDbService;

	@Mock
	private ServiceInterfaceTemplateDbService interfaceTemplateDbService;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private ServiceInstanceDbService instanceDbService;

	//=================================================================================================
	// methods

	// DEVICES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDevicesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createDevices(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDevicesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createDevices(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDevicesOk() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeCreateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		final Device device = new Device("ALARM", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		when(deviceDbService.createBulk(List.of(normalizedDeviceRequest))).thenReturn(List.of(Map.entry(device, List.of(deviceAddress))));

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final DeviceListResponseDTO expected = new DeviceListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(List.of(Map.entry(device, List.of(deviceAddress))), 1)).thenReturn(expected);

		final DeviceListResponseDTO actual = service.createDevices(dto, "test origin");
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDevicesThrowsInvalidParameterException() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeCreateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		when(deviceDbService.createBulk(List.of(normalizedDeviceRequest))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createDevices(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDevicesThrowsInternalServerError() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeCreateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		when(deviceDbService.createBulk(List.of(normalizedDeviceRequest))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createDevices(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDevicesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateDevices(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDevicesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateDevices(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDevicesOk() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeUpdateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		final Device device = new Device("ALARM", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		when(deviceDbService.updateBulk(List.of(normalizedDeviceRequest))).thenReturn(List.of(Map.entry(device, List.of(deviceAddress))));

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final DeviceListResponseDTO expected = new DeviceListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(List.of(Map.entry(device, List.of(deviceAddress))), 1)).thenReturn(expected);

		final DeviceListResponseDTO actual = service.updateDevices(dto, "test origin");
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDevicesThrowsInvalidParameterException() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeUpdateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		when(deviceDbService.updateBulk(List.of(normalizedDeviceRequest))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateDevices(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDevicesThrowsInternalServerError() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeUpdateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		when(deviceDbService.updateBulk(List.of(normalizedDeviceRequest))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.updateDevices(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDevicesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryDevices(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDevicesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryDevices(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDevicesAddressTypeNotEmpty() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), "MAC", List.of(requirement));
		when(validator.validateAndNormalizeQueryDevices(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final Device device = new Device("ALARM", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		final Page<Entry<Device, List<DeviceAddress>>> expectedPage = new PageImpl<>(List.of(Map.entry(device, List.of(deviceAddress))));
		when(deviceDbService.getPage(pageRequest, List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), AddressType.MAC, List.of(requirement))).thenReturn(expectedPage);

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final DeviceListResponseDTO expected = new DeviceListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(expectedPage, 1)).thenReturn(expected);

		final DeviceListResponseDTO actual = service.queryDevices(dto, "test origin");
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDevicesAddressTypeEmpty() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), "", List.of(requirement));
		when(validator.validateAndNormalizeQueryDevices(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final Device device = new Device("ALARM", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		final Page<Entry<Device, List<DeviceAddress>>> expectedPage = new PageImpl<>(List.of(Map.entry(device, List.of(deviceAddress))));
		when(deviceDbService.getPage(pageRequest, List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), null, List.of(requirement))).thenReturn(expectedPage);

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final DeviceListResponseDTO expected = new DeviceListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(expectedPage, 1)).thenReturn(expected);

		final DeviceListResponseDTO actual = service.queryDevices(dto, "test origin");
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDevicesThrowsInternalServerError() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), "MAC", List.of(requirement));
		when(validator.validateAndNormalizeQueryDevices(dto, "test origin")).thenReturn(dto);
		when(pageService.getPageRequest(any(), any(), any(), any(), any())).thenReturn(PageRequest.of(10, 20, Direction.ASC, "id"));

		when(deviceDbService.getPage(any(), any(), any(), any(), any())).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.queryDevices(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeDevices(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeDevices(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesOk() {

		final List<String> names = List.of("ALARM");
		when(validator.validateAndNormalizeRemoveDevices(names, "test origin")).thenReturn(names);

		service.removeDevices(names, "test origin");
		verify(deviceDbService).deleteByNameList(List.of("ALARM"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesThrowsInvalidParameterException() {

		final List<String> names = List.of("ALARM");
		when(validator.validateAndNormalizeRemoveDevices(names, "test origin")).thenReturn(names);
		doThrow(new InvalidParameterException("Invalid parameter")).when(deviceDbService).deleteByNameList(names);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.removeDevices(names, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesThrowsLockedException() {

		final List<String> names = List.of("ALARM");
		when(validator.validateAndNormalizeRemoveDevices(names, "test origin")).thenReturn(names);
		doThrow(new LockedException("Locked entity")).when(deviceDbService).deleteByNameList(names);

		final LockedException ex = assertThrows(LockedException.class, () -> service.removeDevices(names, "test origin"));
		assertEquals("Locked entity", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesThrowsInternalServerError() {

		final List<String> names = List.of("ALARM");
		when(validator.validateAndNormalizeRemoveDevices(names, "test origin")).thenReturn(names);
		doThrow(new InternalServerError("Database error")).when(deviceDbService).deleteByNameList(names);

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeDevices(names, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SERVICE DEFINITIONS

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetServiceDefinitionsOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getServiceDefinitions(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetServiceDefinitionsOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getServiceDefinitions(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetServiceDefinitionsOk() {

		final PageDTO dto = new PageDTO(10, 20, "ASC", "id");
		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(dto), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final ServiceDefinition serviceDefinition = new ServiceDefinition("alertService");
		final Page<ServiceDefinition> entitiesFromDb = new PageImpl<>(List.of(serviceDefinition));
		when(serviceDefinitionDbService.getPage(pageRequest)).thenReturn(entitiesFromDb);

		final ServiceDefinitionResponseDTO reponseEntity = new ServiceDefinitionResponseDTO("alertService", null, null);
		final ServiceDefinitionListResponseDTO expectedResponse = new ServiceDefinitionListResponseDTO(List.of(reponseEntity), 1);
		when(dtoConverter.convertServiceDefinitionEntityPageToDTO(entitiesFromDb)).thenReturn(expectedResponse);

		final ServiceDefinitionListResponseDTO actual = service.getServiceDefinitions(dto, "test origin");
		assertEquals(expectedResponse, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetServiceDefinitionsThrowsInternalServerError() {

		final PageDTO dto = new PageDTO(10, 20, "ASC", "id");
		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(dto), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		when(serviceDefinitionDbService.getPage(pageRequest)).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getServiceDefinitions(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateServiceDefinitionsOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createServiceDefinitions(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateServiceDefinitionsOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createServiceDefinitions(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCeateServiceDefinitionsOk() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("alertService"));
		when(validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin")).thenReturn(List.of("alertService"));

		final ServiceDefinition serviceDefinition = new ServiceDefinition("alertService");
		when(serviceDefinitionDbService.createBulk(List.of("alertService"))).thenReturn(List.of(serviceDefinition));

		final ServiceDefinitionResponseDTO reponseEntity = new ServiceDefinitionResponseDTO("alertService", null, null);
		final ServiceDefinitionListResponseDTO expectedResponse = new ServiceDefinitionListResponseDTO(List.of(reponseEntity), 1);
		when(dtoConverter.convertServiceDefinitionEntityListToDTO(List.of(serviceDefinition))).thenReturn(expectedResponse);

		final ServiceDefinitionListResponseDTO actual = service.createServiceDefinitions(dto, "test origin");
		assertEquals(expectedResponse, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCeateServiceDefinitionsThrowsInvalidParameterException() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("alertService"));
		when(validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin")).thenReturn(List.of("alertService"));

		when(serviceDefinitionDbService.createBulk(List.of("alertService"))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createServiceDefinitions(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCeateServiceDefinitionsThrowsInternalServerError() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("alertService"));
		when(validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin")).thenReturn(List.of("alertService"));

		when(serviceDefinitionDbService.createBulk(List.of("alertService"))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createServiceDefinitions(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceDefinitionsOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeServiceDefinitions(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceDefinitionsOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeServiceDefinitions(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceDefinitionsOk() {

		final List<String> names = List.of("alertService");
		when(validator.validateAndNormalizeRemoveServiceDefinitions(names, "test origin")).thenReturn(names);

		service.removeServiceDefinitions(names, "test origin");
		verify(serviceDefinitionDbService).removeBulk(names);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceDefinitionsThrowsInternalServerError() {

		final List<String> names = List.of("alertService");
		when(validator.validateAndNormalizeRemoveServiceDefinitions(names, "test origin")).thenReturn(names);
		doThrow(new InternalServerError("Database error")).when(serviceDefinitionDbService).removeBulk(names);

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeServiceDefinitions(names, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SYSTEMS

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSystemsOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createSystems(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSystemsOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createSystems(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSystemsOk() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeCreateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 2\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> dbResult = List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress))));
		when(systemDbService.createBulk(List.of(normalizedDto))).thenReturn(dbResult);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletListToDTO(dbResult)).thenReturn(result);

		final SystemListResponseDTO actual = service.createSystems(dto, "test origin");
		assertEquals(result, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSystemsThrowsInvalidParameterException() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeCreateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		when(systemDbService.createBulk(List.of(normalizedDto))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createSystems(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSystemsThrowsInternalServerError() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeCreateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		when(systemDbService.createBulk(List.of(normalizedDto))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createSystems(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.querySystems(null, false, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.querySystems(null, false, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsAddressTypeEmpty() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTripletPage = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));
		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenReturn(systemTripletPage);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(systemTripletPage)).thenReturn(result);

		try (MockedStatic<Utilities> utilitiesMock = mockStatic(Utilities.class)) {
			utilitiesMock.when(() -> Utilities.isEmpty("")).thenReturn(true);

			service.querySystems(dto, true, "test origin");
			utilitiesMock.verify(() -> Utilities.isEmpty(""));
		}
		verify(systemDbService).getPageByFilters(pageRequest, List.of("TemperatureManager"), List.of("192.168.100.100"), null, List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsAddressTypeNotEmpty() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTripletPage = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));
		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenReturn(systemTripletPage);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(systemTripletPage)).thenReturn(result);

		service.querySystems(dto, true, "test origin");
		verify(systemDbService).getPageByFilters(pageRequest, List.of("TemperatureManager"), List.of("192.168.100.100"), AddressType.IPV4, List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsVerboseTrue() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTripletPage = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));
		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenReturn(systemTripletPage);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(systemTripletPage)).thenReturn(result);

		final SystemListResponseDTO actual = service.querySystems(dto, true, "test origin");
		assertEquals(result, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsVerboseFalse() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTripletPage = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));
		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenReturn(systemTripletPage);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(systemTripletPage)).thenReturn(result);

		final DeviceResponseDTO deviceTerse = new DeviceResponseDTO("DEVICE1", null, null, null, null);
		final SystemResponseDTO responseTerse = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceTerse, null, null);
		final SystemListResponseDTO resultTerse = new SystemListResponseDTO(List.of(responseTerse), 1);
		when(dtoConverter.convertSystemListResponseDtoToTerse(result)).thenReturn(resultTerse);

		final SystemListResponseDTO actual = service.querySystems(dto, false, "test origin");
		assertEquals(resultTerse, actual);
		verify(dtoConverter).convertSystemListResponseDtoToTerse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsThrowsInternalServerError() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenThrow(new InternalServerError("Database error", "test origin"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.querySystems(dto, true, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateSystemsOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateSystems(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateSystemsOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateSystems(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateSystemsOk() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeUpdateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 2\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> dbResult = List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress))));
		when(systemDbService.updateBulk(List.of(normalizedDto))).thenReturn(dbResult);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletListToDTO(dbResult)).thenReturn(result);

		final SystemListResponseDTO actual = service.updateSystems(dto, "test origin");
		assertEquals(result, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateSystemsThrowsInvalidParameterException() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeUpdateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		when(systemDbService.updateBulk(List.of(normalizedDto))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateSystems(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateSystemsThrowsInternalServerError() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeUpdateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		when(systemDbService.updateBulk(List.of(normalizedDto))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.updateSystems(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSystemsOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeSystems(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSystemsOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeSystems(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSystemsOk() {

		final List<String> names = List.of("TemperatureManager");
		when(validator.validateAndNormalizeRemoveSystems(names, "test origin")).thenReturn(names);

		assertDoesNotThrow(() -> service.removeSystems(names, "test origin"));
		verify(systemDbService).deleteByNameList(names);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSystemsThrowsInternalServerError() {

		final List<String> names = List.of("TemperatureManager");
		when(validator.validateAndNormalizeRemoveSystems(names, "test origin")).thenReturn(names);
		doThrow(new InternalServerError("Database error", "test origin")).when(systemDbService).deleteByNameList(names);

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeSystems(names, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SERVICE INSTANCES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateServiceInstancesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createServiceInstances(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateServiceInstancesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createServiceInstances(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateServiceInstancesOk() {

		final ServiceInstanceRequestDTO instanceRequest = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080))));
		final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instanceRequest));

		when(validator.validateAndNormalizeCreateServiceInstances(dto, "test origin")).thenReturn(List.of(instanceRequest));

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
		when(instanceDbService.createBulk(List.of(instanceRequest))).thenReturn(List.of(instanceEntry));

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.1");
		final Device device = new Device("TEST_DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTriplet = Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)));
		when(systemDbService.getByNameList(List.of("TemperatureManager"))).thenReturn(List.of(systemTriplet));

		// responses after conversion
		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("TEST_DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO systemResponse = new SystemResponseDTO("TemperatureManager", Map.of(), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.100.1")), deviceResponse, null, null);
		final ServiceInstanceInterfaceResponseDTO interfaceResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		final ServiceInstanceResponseDTO responseInstance = new ServiceInstanceResponseDTO(
				"TemperatureManager|temperatureManagement|5.0.0",
				systemResponse,
				new ServiceDefinitionResponseDTO("temperatureInfo", null, null),
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(interfaceResponse),
				null,
				null);
		final ServiceInstanceListResponseDTO response = new ServiceInstanceListResponseDTO(List.of(responseInstance), 1);
		when(dtoConverter.convertServiceInstanceListToDTO(List.of(instanceEntry), List.of(systemTriplet))).thenReturn(response);

		final ServiceInstanceListResponseDTO actual = assertDoesNotThrow(() -> service.createServiceInstances(dto, "test origin"));
		assertEquals(response, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateServiceInstancesThrowsInvalidParameterException() {

		final ServiceInstanceRequestDTO instanceRequest = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080))));
		final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instanceRequest));

		when(validator.validateAndNormalizeCreateServiceInstances(dto, "test origin")).thenReturn(List.of(instanceRequest));

		// entities in the database
		when(instanceDbService.createBulk(List.of(instanceRequest))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createServiceInstances(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateServiceInstancesThrowsInternalServerError() {

		final ServiceInstanceRequestDTO instanceRequest = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080))));
		final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instanceRequest));

		when(validator.validateAndNormalizeCreateServiceInstances(dto, "test origin")).thenReturn(List.of(instanceRequest));

		// entities in the database
		when(instanceDbService.createBulk(List.of(instanceRequest))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createServiceInstances(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateServiceInstancesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateServiceInstances(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateServiceInstancesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateServiceInstances(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateServiceInstancesOk() {

		final ServiceInstanceUpdateRequestDTO instanceRequest = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080))));
		final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instanceRequest));

		when(validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin")).thenReturn(List.of(instanceRequest));

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
		when(instanceDbService.updateBulk(List.of(instanceRequest))).thenReturn(List.of(instanceEntry));

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.1");
		final Device device = new Device("TEST_DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTriplet = Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)));
		when(systemDbService.getByNameList(List.of("TemperatureManager"))).thenReturn(List.of(systemTriplet));

		// responses after conversion
		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("TEST_DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO systemResponse = new SystemResponseDTO("TemperatureManager", Map.of(), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.100.1")), deviceResponse, null, null);
		final ServiceInstanceInterfaceResponseDTO interfaceResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		final ServiceInstanceResponseDTO responseInstance = new ServiceInstanceResponseDTO(
				"TemperatureManager|temperatureManagement|5.0.0",
				systemResponse,
				new ServiceDefinitionResponseDTO("temperatureInfo", null, null),
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(interfaceResponse),
				null,
				null);
		final ServiceInstanceListResponseDTO response = new ServiceInstanceListResponseDTO(List.of(responseInstance), 1);
		when(dtoConverter.convertServiceInstanceListToDTO(List.of(instanceEntry), List.of(systemTriplet))).thenReturn(response);

		final ServiceInstanceListResponseDTO actual = assertDoesNotThrow(() -> service.updateServiceInstances(dto, "test origin"));
		assertEquals(response, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateServiceInstancesThrowsInvalidParameterException() {

		final ServiceInstanceUpdateRequestDTO instanceRequest = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080))));
		final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instanceRequest));

		when(validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin")).thenReturn(List.of(instanceRequest));

		// entities in the database
		when(instanceDbService.updateBulk(List.of(instanceRequest))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateServiceInstances(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateServiceInstancesThrowsInternalServerError() {

		final ServiceInstanceUpdateRequestDTO instanceRequest = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080))));
		final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instanceRequest));

		when(validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin")).thenReturn(List.of(instanceRequest));

		// entities in the database
		when(instanceDbService.updateBulk(List.of(instanceRequest))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.updateServiceInstances(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceInstancesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeServiceInstances(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceInstancesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeServiceInstances(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceInstancesOk() {

		final List<String> ids = List.of("TemperatureManager|temperatureManagement|5.0.0");
		when(validator.validateAndNormalizeRemoveServiceInstances(ids, "test origin")).thenReturn(ids);

		assertDoesNotThrow(() -> service.removeServiceInstances(ids, "test origin"));
		verify(instanceDbService).deleteByInstanceIds(ids);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceInstancesThrowsInternalServerError() {

		final List<String> ids = List.of("TemperatureManager|temperatureManagement|5.0.0");
		when(validator.validateAndNormalizeRemoveServiceInstances(ids, "test origin")).thenReturn(ids);
		doThrow(new InternalServerError("Database error")).when(instanceDbService).deleteByInstanceIds(ids);

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeServiceInstances(ids, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryServiceInstancesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryServiceInstances(null, false, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryServiceInstancesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryServiceInstances(null, false, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryServiceInstancesVerboseTrue() {

		// dto
		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("management", false);

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
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
		when(validator.validateAndNormalizeQueryServiceInstances(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		// filter model
		final ServiceLookupFilterModel filterModel = new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE")));
		when(dtoConverter.convertServiceInstanceQueryRequestDtoToFilterModel(dto)).thenReturn(filterModel);

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
		final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> servicesWithInterfaces = new PageImpl<>(List.of(instanceEntry));
		when(instanceDbService.getPageByFilters(pageRequest, filterModel)).thenReturn(servicesWithInterfaces);

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.1");
		final Device device = new Device("TEST_DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTriplet = Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)));
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemsWithDevices = new PageImpl<>(List.of(systemTriplet));
		when(systemDbService.getPageByFilters(
				PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, System.DEFAULT_SORT_FIELD),
				List.of("TemperatureManager"),
				null, null, null, null, null)).thenReturn(systemsWithDevices);

		// responses after conversion
		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("TEST_DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO systemResponse = new SystemResponseDTO("TemperatureManager", Map.of(), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.100.1")), deviceResponse, null, null);
		final ServiceInstanceInterfaceResponseDTO interfaceResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		final ServiceInstanceResponseDTO instanceResponse = new ServiceInstanceResponseDTO(
				"TemperatureManager|temperatureManagement|5.0.0",
				systemResponse,
				new ServiceDefinitionResponseDTO("temperatureInfo", null, null),
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(interfaceResponse),
				null,
				null);
		final ServiceInstanceListResponseDTO response = new ServiceInstanceListResponseDTO(List.of(instanceResponse), 1);
		when(dtoConverter.convertServiceInstancePageToDTO(servicesWithInterfaces, systemsWithDevices)).thenReturn(response);

		final ServiceInstanceListResponseDTO actual = assertDoesNotThrow(() -> service.queryServiceInstances(dto, true, "test origin"));
		assertEquals(response, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryServiceInstancesVerboseFalse() {

		// dto
		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("management", false);

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
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
		when(validator.validateAndNormalizeQueryServiceInstances(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		// filter model
		final ServiceLookupFilterModel filterModel = new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE")));
		when(dtoConverter.convertServiceInstanceQueryRequestDtoToFilterModel(dto)).thenReturn(filterModel);

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
		final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> servicesWithInterfaces = new PageImpl<>(List.of(instanceEntry));
		when(instanceDbService.getPageByFilters(pageRequest, filterModel)).thenReturn(servicesWithInterfaces);

		// responses after conversion
		final SystemResponseDTO systemResponse = new SystemResponseDTO("TemperatureManager", Map.of(), "1.0.0", null, null, null, null);
		final ServiceInstanceInterfaceResponseDTO interfaceResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		final ServiceInstanceResponseDTO instanceResponse = new ServiceInstanceResponseDTO(
				"TemperatureManager|temperatureManagement|5.0.0",
				systemResponse,
				new ServiceDefinitionResponseDTO("temperatureInfo", null, null),
				"5.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("priority", 2),
				List.of(interfaceResponse),
				null,
				null);
		final ServiceInstanceListResponseDTO response = new ServiceInstanceListResponseDTO(List.of(instanceResponse), 1);
		when(dtoConverter.convertServiceInstancePageToDTO(servicesWithInterfaces, null)).thenReturn(response);

		final ServiceInstanceListResponseDTO actual = assertDoesNotThrow(() -> service.queryServiceInstances(dto, false, "test origin"));
		assertEquals(response, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryServiceInstancesThrowsInternalServerError() {

		// dto
		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("management", false);

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
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
		when(validator.validateAndNormalizeQueryServiceInstances(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		// filter model
		final ServiceLookupFilterModel filterModel = new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(
				List.of("AlertProvider|alertService|16.4.3"),
				List.of("AlertProvider"),
				List.of("alertService"),
				List.of("16.4.3"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE")));
		when(dtoConverter.convertServiceInstanceQueryRequestDtoToFilterModel(dto)).thenReturn(filterModel);

		when(instanceDbService.getPageByFilters(pageRequest, filterModel)).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.queryServiceInstances(dto, true, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// INTERFACE TEMPLATES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInterfaceTemplatesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createInterfaceTemplates(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInterfaceTemplatesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createInterfaceTemplates(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInterfaceTemplatesOk() {

		// dto
		final ServiceInterfaceTemplatePropertyDTO property = new ServiceInterfaceTemplatePropertyDTO("accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST", List.of());
		final ServiceInterfaceTemplateRequestDTO templateRequest = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(property));
		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(templateRequest));
		when(validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin")).thenReturn(dto);

		// db entities
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty templateProperty = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");
		final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> entries = Map.of(template, List.of(templateProperty));
		when(interfaceTemplateDbService.createBulk(List.of(templateRequest))).thenReturn(entries);

		// responses after conversion
		final ServiceInterfaceTemplateResponseDTO templateResponse = new ServiceInterfaceTemplateResponseDTO(
				"generic_http",
				"http",
				List.of(new ServiceInterfaceTemplatePropertyDTO("accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST", List.of())),
				"2024-11-04T01:53:02Z",
				"2024-11-04T01:53:02Z");
		final ServiceInterfaceTemplateListResponseDTO response = new ServiceInterfaceTemplateListResponseDTO(List.of(templateResponse), 1);
		when(dtoConverter.convertInterfaceTemplateEntriesToDTO(entries.entrySet(), 1)).thenReturn(response);

		final ServiceInterfaceTemplateListResponseDTO actual = assertDoesNotThrow(() -> service.createInterfaceTemplates(dto, "test origin"));
		assertEquals(response, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInterfaceTemplatesThrowsInvalidParameterException() {

		// dto
		final ServiceInterfaceTemplatePropertyDTO property = new ServiceInterfaceTemplatePropertyDTO("accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST", List.of());
		final ServiceInterfaceTemplateRequestDTO templateRequest = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(property));
		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(templateRequest));
		when(validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin")).thenReturn(dto);

		when(interfaceTemplateDbService.createBulk(List.of(templateRequest))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createInterfaceTemplates(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInterfaceTemplatesThrowsInternalServerError() {

		// dto
		final ServiceInterfaceTemplatePropertyDTO property = new ServiceInterfaceTemplatePropertyDTO("accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST", List.of());
		final ServiceInterfaceTemplateRequestDTO templateRequest = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(property));
		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(templateRequest));
		when(validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin")).thenReturn(dto);

		when(interfaceTemplateDbService.createBulk(List.of(templateRequest))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createInterfaceTemplates(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryInterfaceTemplatesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryInterfaceTemplates(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryInterfaceTemplatesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryInterfaceTemplates(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryInterfaceTemplatesOk() {

		final ServiceInterfaceTemplateQueryRequestDTO dto = new ServiceInterfaceTemplateQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("generic_http"), List.of("http"));
		when(validator.validateAndNormalizeQueryInterfaceTemplates(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty templateProperty = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> entries = new PageImpl<>(List.of(Map.entry(template, List.of(templateProperty))));
		when(interfaceTemplateDbService.getPageByFilters(pageRequest, List.of("generic_http"), List.of("http"))).thenReturn(entries);

		final ServiceInterfaceTemplateResponseDTO templateResponse = new ServiceInterfaceTemplateResponseDTO(
				"generic_http",
				"http",
				List.of(new ServiceInterfaceTemplatePropertyDTO("accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST", List.of())),
				"2024-11-04T01:53:02Z",
				"2024-11-04T01:53:02Z");
		final ServiceInterfaceTemplateListResponseDTO response = new ServiceInterfaceTemplateListResponseDTO(List.of(templateResponse), 1);
		when(dtoConverter.convertInterfaceTemplateEntriesToDTO(List.of(Map.entry(template, List.of(templateProperty))), 1)).thenReturn(response);

		final ServiceInterfaceTemplateListResponseDTO actual = assertDoesNotThrow(() -> service.queryInterfaceTemplates(dto, "test origin"));
		assertEquals(response, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryInterfaceTemplatesThrowsInternalServerError() {

		final ServiceInterfaceTemplateQueryRequestDTO dto = new ServiceInterfaceTemplateQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("generic_http"), List.of("http"));
		when(validator.validateAndNormalizeQueryInterfaceTemplates(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		when(interfaceTemplateDbService.getPageByFilters(pageRequest, List.of("generic_http"), List.of("http"))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.queryInterfaceTemplates(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveInterfaceTemplatesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeInterfaceTemplates(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveInterfaceTemplatesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeInterfaceTemplates(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveInterfaceTemplatesOk() {

		final List<String> names = List.of("generic_http");
		when(validator.validateAndNormalizeRemoveInterfaceTemplates(names, "test origin")).thenReturn(names);

		assertDoesNotThrow(() -> service.removeInterfaceTemplates(names, "test origin"));
		verify(interfaceTemplateDbService).deleteByTemplateNameList(names);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveInterfaceTemplatesThrowsInternalServerError() {

		final List<String> names = List.of("generic_http");
		when(validator.validateAndNormalizeRemoveInterfaceTemplates(names, "test origin")).thenReturn(names);
		doThrow(new InternalServerError("Database error")).when(interfaceTemplateDbService).deleteByTemplateNameList(names);

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeInterfaceTemplates(names, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}
}
