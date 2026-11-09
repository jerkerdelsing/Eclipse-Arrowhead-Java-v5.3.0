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
package eu.arrowhead.serviceregistry.service.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.ZonedDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListResponseDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateResponseDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
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
import eu.arrowhead.serviceregistry.service.model.ServiceLookupFilterModel;

@SuppressWarnings("checkstyle:magicnumber")
public class DTOConverterTest {

	//=================================================================================================
	// members

	private final DTOConverter converter = new DTOConverter();

	private static MockedStatic<Utilities> utilitiesMock; // needed to mock the creation time

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertDeviceAndDeviceAddressEntriesToDTO() {

		final Device device = new Device("TEST_DEVICE", "{ }");
		device.onCreate();
		final DeviceAddress address1 = new DeviceAddress(device, AddressType.IPV4, "192.168.2.2");
		final DeviceAddress address2 = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:5e");

		final DeviceResponseDTO expectedDTO = new DeviceResponseDTO(
				"TEST_DEVICE",
				Map.of(),
				List.of(new AddressDTO("IPV4", "192.168.2.2"), new AddressDTO("MAC", "00:1a:2b:3c:4d:5e")),
				"2024-11-04T01:53:02Z",
				"2024-11-04T01:53:02Z");

		DeviceListResponseDTO converted = converter.convertDeviceAndDeviceAddressEntriesToDTO(List.of(Map.entry(device, List.of(address1, address2))), 1);
		assertEquals(new DeviceListResponseDTO(List.of(expectedDTO), 1), converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testConvertDeviceAndDeviceAddressEntriesToDTODeviceNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertDeviceEntityToDeviceResponseDTO(null, null));

		assertEquals("device entity is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	@Test
	void testConvertDeviceAndDeviceAddressEntriesToDTOAddressesNull() {
		utilitiesMock.when(() -> Utilities.isEmpty((Collection) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertDeviceEntityToDeviceResponseDTO(new Device(), null));

		assertEquals("device address entities is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testConvertDeviceAndDeviceAddressEntriesToDTOAddressesEmpty() {
		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertDeviceEntityToDeviceResponseDTO(new Device(), List.of()));

		assertEquals("device address entities is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testConvertDeviceAndDeviceAddressEntriesToDTOAddressesContainsNull() {
		final List<DeviceAddress> list = new ArrayList<>(1);
		list.add(null);

		utilitiesMock.when(() -> Utilities.containsNull(list)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertDeviceEntityToDeviceResponseDTO(new Device(), list));

		assertEquals("device address entities contains null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertDeviceEntityToDeviceResponseDTO() {

		final Device device = new Device("TEST_DEVICE", "{ }");
		device.onCreate();
		final DeviceAddress address1 = new DeviceAddress(device, AddressType.IPV4, "192.168.2.2");
		final DeviceAddress address2 = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:5e");

		final DeviceResponseDTO expected = new DeviceResponseDTO(
				"TEST_DEVICE",
				Map.of(),
				List.of(new AddressDTO("IPV4", "192.168.2.2"), new AddressDTO("MAC", "00:1a:2b:3c:4d:5e")),
				"2024-11-04T01:53:02Z",
				"2024-11-04T01:53:02Z");

		DeviceResponseDTO converted = converter.convertDeviceEntityToDeviceResponseDTO(device, List.of(address1, address2));
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceDefinitionEntityListToDTO() {

		final ServiceDefinition entity = new ServiceDefinition("temperatureInfo");
		entity.onCreate();

		final ServiceDefinitionResponseDTO expectedDTO = new ServiceDefinitionResponseDTO("temperatureInfo", "2024-11-04T01:53:02Z", "2024-11-04T01:53:02Z");
		final ServiceDefinitionListResponseDTO expected = new ServiceDefinitionListResponseDTO(List.of(expectedDTO), 1);

		final ServiceDefinitionListResponseDTO converted = converter.convertServiceDefinitionEntityListToDTO(List.of(entity));
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceDefinitionEntityPageToDTO() {

		final ServiceDefinition entity = new ServiceDefinition("temperatureInfo");
		entity.onCreate();

		final ServiceDefinitionResponseDTO expectedDTO = new ServiceDefinitionResponseDTO("temperatureInfo", "2024-11-04T01:53:02Z", "2024-11-04T01:53:02Z");
		final ServiceDefinitionListResponseDTO expected = new ServiceDefinitionListResponseDTO(List.of(expectedDTO), 10);

		final ServiceDefinitionListResponseDTO converted = converter.convertServiceDefinitionEntityPageToDTO(new PageImpl<>(List.of(entity), PageRequest.of(0, 1), 10));
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceDefinitionEntityToDTONullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertServiceDefinitionEntityToDTO(null));

		assertEquals("entity is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceDefinitionEntityToDTONameNull() {
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertServiceDefinitionEntityToDTO(new ServiceDefinition(null)));

		assertEquals("name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceDefinitionEntityToDTONameEmpty() {
		utilitiesMock.when(() -> Utilities.isEmpty("")).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertServiceDefinitionEntityToDTO(new ServiceDefinition("")));

		assertEquals("name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceDefinitionEntityToDTO() {

		final ServiceDefinition entity = new ServiceDefinition("temperatureInfo");
		entity.onCreate();

		final ServiceDefinitionResponseDTO expected = new ServiceDefinitionResponseDTO("temperatureInfo", "2024-11-04T01:53:02Z", "2024-11-04T01:53:02Z");

		final ServiceDefinitionResponseDTO converted = converter.convertServiceDefinitionEntityToDTO(entity);
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemTripletPageToDTO() {

		// system with device

		final Entry<System, SystemAddress> systemDetails1 = createSystem1();
		final Entry<Device, DeviceAddress> deviceDetails1 = createDevice1();

		// system without device

		final Entry<System, SystemAddress> systemDetails2 = createSystem2();

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> toConvert = new PageImpl<>(List.of(
				Triple.of(systemDetails1.getKey(), List.of(systemDetails1.getValue()), Map.entry(deviceDetails1.getKey(), List.of(deviceDetails1.getValue()))),
				Triple.of(systemDetails2.getKey(), List.of(systemDetails2.getValue()), null)),
				PageRequest.of(0, 3),
				10);

		// expected dtos
		final DeviceResponseDTO expectedDevice1 = createResponseDevice1();
		final SystemResponseDTO expectedDTO1 = createResponseSystem1(expectedDevice1, true);
		final SystemResponseDTO expectedDTO2 = createResponseSystem2(null, true);

		final SystemListResponseDTO expected = new SystemListResponseDTO(List.of(expectedDTO1, expectedDTO2), 10);

		final SystemListResponseDTO converted = converter.convertSystemTripletPageToDTO(toConvert);
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemTripletListToDTONullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertSystemTripletToDTO(null));

		assertEquals("entity is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemTripletListToDTOLeftNull() {
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity = ImmutableTriple.of(null, null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertSystemTripletToDTO(entity));

		assertEquals("the System in the triplet is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	@Test
	public void testConvertSystemTripletListToDTOMiddleNull() {
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity = ImmutableTriple.of(
				new System(),
				null,
				null);

		utilitiesMock.when(() -> Utilities.isEmpty((Collection) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertSystemTripletToDTO(entity));

		assertEquals("the system address list in the triplet is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemTripletListToDTOMiddleEmpty() {
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity = ImmutableTriple.of(
				new System(),
				List.of(),
				null);

		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertSystemTripletToDTO(entity));

		assertEquals("the system address list in the triplet is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	@Test
	public void testConvertSystemTripletListToDTORightNullList() {
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity = ImmutableTriple.of(
				new System(),
				List.of(new SystemAddress()),
				new SimpleEntry<>(new Device(), null));

		utilitiesMock.when(() -> Utilities.isEmpty((Collection) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertSystemTripletToDTO(entity));

		assertEquals("the device data list in the triplet is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemTripletListToDTORightEmptyList() {
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity = ImmutableTriple.of(
				new System(),
				List.of(new SystemAddress()),
				new SimpleEntry<>(new Device(), List.of()));

		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertSystemTripletToDTO(entity));

		assertEquals("the device data list in the triplet is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemTripletListToDTO() {

		// system with device

		final Entry<System, SystemAddress> systemDetails1 = createSystem1();
		final Entry<Device, DeviceAddress> deviceDetails1 = createDevice1();

		// system without device

		final Entry<System, SystemAddress> systemDetails2 = createSystem2();

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> toConvert = List.of(
				Triple.of(systemDetails1.getKey(), List.of(systemDetails1.getValue()), Map.entry(deviceDetails1.getKey(), List.of(deviceDetails1.getValue()))),
				Triple.of(systemDetails2.getKey(), List.of(systemDetails2.getValue()), null));

		// expected dtos
		final DeviceResponseDTO expectedDevice1 = createResponseDevice1();
		final SystemResponseDTO expectedDTO1 = createResponseSystem1(expectedDevice1, true);
		final SystemResponseDTO expectedDTO2 = createResponseSystem2(null, true);

		final SystemListResponseDTO expected = new SystemListResponseDTO(List.of(expectedDTO1, expectedDTO2), 2);

		final SystemListResponseDTO converted = converter.convertSystemTripletListToDTO(toConvert);
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemTripletDTODeviceNotNull() {

		// system with device

		final Entry<System, SystemAddress> systemDetails = createSystem1();
		final Entry<Device, DeviceAddress> deviceDetails = createDevice1();

		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> toConvert = Triple.of(
				systemDetails.getKey(),
				List.of(systemDetails.getValue()),
				Map.entry(deviceDetails.getKey(), List.of(deviceDetails.getValue())));

		// expected dtos
		final DeviceResponseDTO expectedDevice = createResponseDevice1();
		final SystemResponseDTO expectedDTO = createResponseSystem1(expectedDevice, true);

		final SystemResponseDTO converted = converter.convertSystemTripletToDTO(toConvert);
		assertEquals(expectedDTO, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemTripletDTODeviceNull() {
		// system without device

		final Entry<System, SystemAddress> systemDetails = createSystem1();

		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> toConvert = Triple.of(
				systemDetails.getKey(),
				List.of(systemDetails.getValue()),
				null);

		// expected dto
		final SystemResponseDTO expectedDTO = createResponseSystem1(null, true);

		final SystemResponseDTO converted = converter.convertSystemTripletToDTO(toConvert);
		assertEquals(expectedDTO, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemListResponseDtoToTerseDeviceNotNull() {

		// system to convert
		final DeviceResponseDTO deviceToConvert = createResponseDevice1();
		final SystemResponseDTO systemToConvert = createResponseSystem1(deviceToConvert, true);

		// expected dtos
		final DeviceResponseDTO expectedDevice = new DeviceResponseDTO(deviceToConvert.name(), null, null, null, null);
		final SystemResponseDTO expectedSystem = createResponseSystem1(expectedDevice, true);

		final SystemListResponseDTO converted = converter.convertSystemListResponseDtoToTerse(new SystemListResponseDTO(List.of(systemToConvert), 1));
		assertEquals(new SystemListResponseDTO(List.of(expectedSystem), 1), converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSystemListResponseDtoToTerseDeviceNull() {
		final SystemResponseDTO systemResponse = createResponseSystem1(null, true);

		final SystemListResponseDTO converted = converter.convertSystemListResponseDtoToTerse(new SystemListResponseDTO(List.of(systemResponse), 1));
		assertEquals(new SystemListResponseDTO(List.of(systemResponse), 1), converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstanceEntityToDTOInstanceNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertServiceInstanceEntityToDTO(null, null));

		assertEquals("instance is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	@Test
	public void testConvertServiceInstanceEntityToDTOInterfacesNull() {
		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntry = new SimpleEntry<>(
				new ServiceInstance(),
				null);

		utilitiesMock.when(() -> Utilities.isEmpty((Collection) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertServiceInstanceEntityToDTO(instanceEntry, null));

		assertEquals("instance interfaces list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstanceEntityToDTOInterfacesEmpty() {
		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntry = new SimpleEntry<>(
				new ServiceInstance(),
				List.of());

		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertServiceInstanceEntityToDTO(instanceEntry, null));

		assertEquals("instance interfaces list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstanceEntityToDTOInterfacesListContainsNull() {
		final List<ServiceInstanceInterface> list = new ArrayList<>(1);
		list.add(null);

		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntry = new SimpleEntry<>(
				new ServiceInstance(),
				list);

		utilitiesMock.when(() -> Utilities.containsNull(list)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertServiceInstanceEntityToDTO(instanceEntry, null));

		assertEquals("instance interfaces list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstanceEntityToDTOSystemTripletNotNull() {
		final DTOConverter converterSpy = spy(new DTOConverter());

		// entities to convert

		final Entry<System, SystemAddress> systemDetails = createSystem1();
		final Entry<Device, DeviceAddress> deviceDetails = createDevice1();
		final ServiceDefinition serviceDefinition = createServiceDefinition();
		final ServiceInstance instance = createServiceInstance(systemDetails.getKey(), serviceDefinition);
		final ServiceInstanceInterface instanceInterface = createServiceInstanceInterface(instance);

		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert = Map.entry(instance, List.of(instanceInterface));
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTripletToConvert = Triple.of(
				systemDetails.getKey(),
				List.of(systemDetails.getValue()),
				Map.entry(deviceDetails.getKey(), List.of(deviceDetails.getValue())));

		// expected dtos

		final DeviceResponseDTO expectedDevice = createResponseDevice1();
		final SystemResponseDTO expectedSystem = createResponseSystem1(expectedDevice, true);

		final ServiceInstanceResponseDTO expected = createServiceInstanceResponse(expectedSystem);

		final ServiceInstanceResponseDTO converted = converterSpy.convertServiceInstanceEntityToDTO(instanceEntryToConvert, systemTripletToConvert);
		assertEquals(expected, converted);
		verify(converterSpy).convertSystemTripletToDTO(systemTripletToConvert);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstanceEntityToDTOSystemTripletNull() {
		// entities to convert

		final Entry<System, SystemAddress> systemDetails = createSystem1();
		final ServiceDefinition serviceDefinition = createServiceDefinition();
		final ServiceInstance instance = createServiceInstance(systemDetails.getKey(), serviceDefinition);
		final ServiceInstanceInterface instanceInterface = createServiceInstanceInterface(instance);

		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert = Map.entry(instance, List.of(instanceInterface));

		// expected dtos

		final SystemResponseDTO expectedSystem = createResponseSystem1(null, false);

		final ServiceInstanceResponseDTO expected = createServiceInstanceResponse(expectedSystem);

		final ServiceInstanceResponseDTO converted = converter.convertServiceInstanceEntityToDTO(instanceEntryToConvert, null);
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstancePageToDTOSystemsWithDevicesAreNotNull() {

		final DTOConverter converterSpy = spy(new DTOConverter());

		// entities to convert

		final Entry<System, SystemAddress> systemDetails1 = createSystem1();
		final Entry<System, SystemAddress> systemDetails2 = createSystem2();

		final ServiceDefinition serviceDefinition = createServiceDefinition();

		final ServiceInstance instance1 = createServiceInstance(systemDetails1.getKey(), serviceDefinition);
		final ServiceInstance instance2 = createServiceInstance(systemDetails2.getKey(), serviceDefinition);

		final ServiceInstanceInterface instanceInterface1 = createServiceInstanceInterface(instance1);
		final ServiceInstanceInterface instanceInterface2 = createServiceInstanceInterface(instance2);

		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert1 = Map.entry(instance1, List.of(instanceInterface1));
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTripletToConvert1 = Triple.of(systemDetails1.getKey(), List.of(systemDetails1.getValue()), null);
		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert2 = Map.entry(instance2, List.of(instanceInterface2));
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTripletToConvert2 = Triple.of(systemDetails2.getKey(), List.of(systemDetails2.getValue()), null);

		// expected dtos

		final SystemResponseDTO expectedSystem1 = createResponseSystem1(null, true);
		final SystemResponseDTO expectedSystem2 = createResponseSystem2(null, true);

		final ServiceInstanceResponseDTO expected1 = createServiceInstanceResponse(expectedSystem1);
		final ServiceInstanceResponseDTO expected2 = createServiceInstanceResponse(expectedSystem2);

		final ServiceInstanceListResponseDTO expected = new ServiceInstanceListResponseDTO(List.of(expected1, expected2), 10);

		final ServiceInstanceListResponseDTO converted = converterSpy.convertServiceInstancePageToDTO(
				new PageImpl<>(List.of(instanceEntryToConvert1, instanceEntryToConvert2), PageRequest.of(0, 2), 10),
				List.of(systemTripletToConvert2, systemTripletToConvert1));
		assertEquals(expected, converted);
		verify(converterSpy, times(2)).convertServiceInstanceEntityToDTO(any(), any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstancePageToDTOSystemsWithDevicesAreNull() {

		final DTOConverter converterSpy = spy(new DTOConverter());

		// entities to convert

		final Entry<System, SystemAddress> systemDetails = createSystem1();
		final ServiceDefinition serviceDefinition = createServiceDefinition();
		final ServiceInstance instance = createServiceInstance(systemDetails.getKey(), serviceDefinition);
		final ServiceInstanceInterface instanceInterface = createServiceInstanceInterface(instance);

		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert = Map.entry(instance, List.of(instanceInterface));

		// expected dtos

		final SystemResponseDTO expectedSystem = createResponseSystem1(null, false);
		final ServiceInstanceResponseDTO expectedDTO = createServiceInstanceResponse(expectedSystem);

		final ServiceInstanceListResponseDTO expected = new ServiceInstanceListResponseDTO(List.of(expectedDTO), 10);

		final ServiceInstanceListResponseDTO converted = converterSpy.convertServiceInstancePageToDTO(
				new PageImpl<>(List.of(instanceEntryToConvert), PageRequest.of(0, 1), 10),
				null);
		assertEquals(expected, converted);
		verify(converterSpy, times(1)).convertServiceInstanceEntityToDTO(any(), any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstancePageToDTOSystemsWithDevicesAreEmpty() {

		final DTOConverter converterSpy = spy(new DTOConverter());

		// entities to convert

		final Entry<System, SystemAddress> systemDetails1 = createSystem1();
		final ServiceDefinition serviceDefinition = createServiceDefinition();
		final ServiceInstance instance = createServiceInstance(systemDetails1.getKey(), serviceDefinition);
		final ServiceInstanceInterface instanceInterface = createServiceInstanceInterface(instance);

		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert = Map.entry(instance, List.of(instanceInterface));

		// expected dtos

		final SystemResponseDTO expectedSystem = createResponseSystem1(null, false);
		final ServiceInstanceResponseDTO expectedDTO = createServiceInstanceResponse(expectedSystem);
		final ServiceInstanceListResponseDTO expected = new ServiceInstanceListResponseDTO(List.of(expectedDTO), 10);

		final ServiceInstanceListResponseDTO converted = converterSpy.convertServiceInstancePageToDTO(
				new PageImpl<>(List.of(instanceEntryToConvert), PageRequest.of(0, 1), 10),
				List.of());
		assertEquals(expected, converted);
		verify(converterSpy, times(1)).convertServiceInstanceEntityToDTO(any(), any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstanceListToDTOSystemsWithDevicesAreNotNull() {

		final DTOConverter converterSpy = spy(new DTOConverter());
		// entities to convert

		final Entry<System, SystemAddress> systemDetails1 = createSystem1();
		final Entry<System, SystemAddress> systemDetails2 = createSystem2();

		final ServiceDefinition serviceDefinition = createServiceDefinition();

		final ServiceInstance instance1 = createServiceInstance(systemDetails1.getKey(), serviceDefinition);
		final ServiceInstance instance2 = createServiceInstance(systemDetails2.getKey(), serviceDefinition);

		final ServiceInstanceInterface instanceInterface1 = createServiceInstanceInterface(instance1);
		final ServiceInstanceInterface instanceInterface2 = createServiceInstanceInterface(instance2);

		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert1 = Map.entry(instance1, List.of(instanceInterface1));
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTripletToConvert1 = Triple.of(systemDetails1.getKey(), List.of(systemDetails1.getValue()), null);
		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert2 = Map.entry(instance2, List.of(instanceInterface2));
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTripletToConvert2 = Triple.of(systemDetails2.getKey(), List.of(systemDetails2.getValue()), null);

		// expected dtos

		final SystemResponseDTO expectedSystem1 = createResponseSystem1(null, true);
		final SystemResponseDTO expectedSystem2 = createResponseSystem2(null, true);

		final ServiceInstanceResponseDTO expected1 = createServiceInstanceResponse(expectedSystem1);
		final ServiceInstanceResponseDTO expected2 = createServiceInstanceResponse(expectedSystem2);

		final ServiceInstanceListResponseDTO expected = new ServiceInstanceListResponseDTO(List.of(expected1, expected2), 2);

		final ServiceInstanceListResponseDTO converted = converterSpy.convertServiceInstanceListToDTO(
				List.of(instanceEntryToConvert1, instanceEntryToConvert2),
				List.of(systemTripletToConvert2, systemTripletToConvert1));
		assertEquals(expected, converted);
		verify(converterSpy, times(2)).convertServiceInstanceEntityToDTO(any(), any());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstanceListToDTOSystemsWithDevicesAreNull() {

		final DTOConverter converterSpy = spy(new DTOConverter());
		// entities to convert

		final Entry<System, SystemAddress> systemDetails1 = createSystem1();
		final Entry<System, SystemAddress> systemDetails2 = createSystem2();

		final ServiceDefinition serviceDefinition = createServiceDefinition();

		final ServiceInstance instance1 = createServiceInstance(systemDetails1.getKey(), serviceDefinition);
		final ServiceInstance instance2 = createServiceInstance(systemDetails2.getKey(), serviceDefinition);

		final ServiceInstanceInterface instanceInterface1 = createServiceInstanceInterface(instance1);
		final ServiceInstanceInterface instanceInterface2 = createServiceInstanceInterface(instance2);

		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert1 = Map.entry(instance1, List.of(instanceInterface1));
		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert2 = Map.entry(instance2, List.of(instanceInterface2));

		// expected dtos

		final SystemResponseDTO expectedSystem1 = createResponseSystem1(null, false);
		final SystemResponseDTO expectedSystem2 = createResponseSystem2(null, false);

		final ServiceInstanceResponseDTO expected1 = createServiceInstanceResponse(expectedSystem1);
		final ServiceInstanceResponseDTO expected2 = createServiceInstanceResponse(expectedSystem2);

		final ServiceInstanceListResponseDTO expected = new ServiceInstanceListResponseDTO(List.of(expected1, expected2), 2);

		final ServiceInstanceListResponseDTO converted = converterSpy.convertServiceInstanceListToDTO(
				List.of(instanceEntryToConvert1, instanceEntryToConvert2),
				null);
		assertEquals(expected, converted);
		verify(converterSpy, times(2)).convertServiceInstanceEntityToDTO(any(), any());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstanceListToDTOSystemsWithDevicesAreEmpty() {

		final DTOConverter converterSpy = spy(new DTOConverter());
		// entities to convert

		final Entry<System, SystemAddress> systemDetails1 = createSystem1();
		final Entry<System, SystemAddress> systemDetails2 = createSystem2();

		final ServiceDefinition serviceDefinition = createServiceDefinition();

		final ServiceInstance instance1 = createServiceInstance(systemDetails1.getKey(), serviceDefinition);
		final ServiceInstance instance2 = createServiceInstance(systemDetails2.getKey(), serviceDefinition);

		final ServiceInstanceInterface instanceInterface1 = createServiceInstanceInterface(instance1);
		final ServiceInstanceInterface instanceInterface2 = createServiceInstanceInterface(instance2);

		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert1 = Map.entry(instance1, List.of(instanceInterface1));
		final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntryToConvert2 = Map.entry(instance2, List.of(instanceInterface2));

		// expected dtos

		final SystemResponseDTO expectedSystem1 = createResponseSystem1(null, false);
		final SystemResponseDTO expectedSystem2 = createResponseSystem2(null, false);

		final ServiceInstanceResponseDTO expected1 = createServiceInstanceResponse(expectedSystem1);
		final ServiceInstanceResponseDTO expected2 = createServiceInstanceResponse(expectedSystem2);

		final ServiceInstanceListResponseDTO expected = new ServiceInstanceListResponseDTO(List.of(expected1, expected2), 2);

		final ServiceInstanceListResponseDTO converted = converterSpy.convertServiceInstanceListToDTO(
				List.of(instanceEntryToConvert1, instanceEntryToConvert2),
				List.of());
		assertEquals(expected, converted);
		verify(converterSpy, times(2)).convertServiceInstanceEntityToDTO(any(), any());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertInterfaceTemplateEntriesToDTOEntriesNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertInterfaceTemplateEntriesToDTO(null, 0));

		assertEquals("entry collection is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	@Test
	public void testConvertInterfaceTemplateEntriesToDTOPropertyListNull() {
		final Collection<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> entries = List.of(new SimpleEntry<>(
				new ServiceInterfaceTemplate(),
				null));

		utilitiesMock.when(() -> Utilities.isEmpty((Collection) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertInterfaceTemplateEntriesToDTO(entries, 1));

		assertEquals("ServiceInterfaceTemplateProperty list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertInterfaceTemplateEntriesToDTOPropertyListEmpty() {
		final Collection<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> entries = List.of(new SimpleEntry<>(
				new ServiceInterfaceTemplate(),
				List.of()));

		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertInterfaceTemplateEntriesToDTO(entries, 1));

		assertEquals("ServiceInterfaceTemplateProperty list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertInterfaceTemplateEntriesToDTOPropertyListContainsNull() {
		final List<ServiceInterfaceTemplateProperty> list = new ArrayList<>(1);
		list.add(null);

		final Collection<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> entries = List.of(new SimpleEntry<>(
				new ServiceInterfaceTemplate(),
				list));

		utilitiesMock.when(() -> Utilities.containsNull(list)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertInterfaceTemplateEntriesToDTO(entries, 1));

		assertEquals("ServiceInterfaceTemplateProperty list contains null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertInterfaceTemplateEntriesToDTO() {
		final DTOConverter converterSpy = spy(new DTOConverter());

		final ServiceInterfaceTemplate interfaceTemplate = new ServiceInterfaceTemplate("generic_http", "http");
		interfaceTemplate.onCreate();
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(interfaceTemplate, "maxQos", true, "MINMAX|2|4");
		final Collection<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> toConvert = List.of(Map.entry(interfaceTemplate, List.of(property)));

		final ServiceInterfaceTemplateResponseDTO expectedDTO = new ServiceInterfaceTemplateResponseDTO(
				"generic_http",
				"http",
				List.of(new ServiceInterfaceTemplatePropertyDTO("maxQos", true, "MINMAX", List.of("2", "4"))),
				"2024-11-04T01:53:02Z",
				"2024-11-04T01:53:02Z");

		final ServiceInterfaceTemplateListResponseDTO converted = converterSpy.convertInterfaceTemplateEntriesToDTO(toConvert, 1);
		assertEquals(new ServiceInterfaceTemplateListResponseDTO(List.of(expectedDTO), 1), converted);
		verify(converterSpy, times(1)).convertServiceInterfaceTemplatePropertytoDTO(property);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInterfaceTemplatePropertytoDTOWithValidatorParams() {
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(
				new ServiceInterfaceTemplate("generic_http", "http"),
				"maxQos",
				true,
				"MINMAX|2|4");

		final ServiceInterfaceTemplatePropertyDTO expected = new ServiceInterfaceTemplatePropertyDTO("maxQos", true, "MINMAX", List.of("2", "4"));

		final ServiceInterfaceTemplatePropertyDTO converted = converter.convertServiceInterfaceTemplatePropertytoDTO(property);
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInterfaceTemplatePropertytoDTOWithoutValidatorParams() {
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(
				new ServiceInterfaceTemplate("generic_http", "http"),
				"acccessPort",
				true,
				"PORT");

		final ServiceInterfaceTemplatePropertyDTO expected = new ServiceInterfaceTemplatePropertyDTO("acccessPort", true, "PORT", new ArrayList<>());

		final ServiceInterfaceTemplatePropertyDTO converted = converter.convertServiceInterfaceTemplatePropertytoDTO(property);
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInterfaceTemplatePropertytoDTOWithoutValidator() {

		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(
				new ServiceInterfaceTemplate("generic_http", "http"),
				"basePath",
				true,
				null);

		final ServiceInterfaceTemplatePropertyDTO expected = new ServiceInterfaceTemplatePropertyDTO("basePath", true, null, null);

		final ServiceInterfaceTemplatePropertyDTO converted = converter.convertServiceInterfaceTemplatePropertytoDTO(property);
		assertEquals(expected, converted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceInstanceQueryRequestDtoToFilterModel() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

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

		final ServiceLookupFilterModel expected = new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(
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

		final ServiceLookupFilterModel converted = converter.convertServiceInstanceQueryRequestDtoToFilterModel(dto);
		assertThat(converted)
				.usingRecursiveComparison()
				.isEqualTo(expected);
	}

	//=================================================================================================
	// assistant methods

	@SuppressWarnings("unchecked")
	//-------------------------------------------------------------------------------------------------
	@BeforeAll
	private static void initializeUtilitiesMock() {

		utilitiesMock = mockStatic(Utilities.class);
		utilitiesMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.parse("2024-11-04T01:53:02Z"));
		utilitiesMock.when(() -> Utilities.convertZonedDateTimeToUTCString(eq(ZonedDateTime.parse("2024-11-04T01:53:02Z")))).thenReturn("2024-11-04T01:53:02Z");
		utilitiesMock.when(() -> Utilities.convertZonedDateTimeToUTCString(eq(ZonedDateTime.parse("2025-11-04T01:53:02Z")))).thenReturn("2025-11-04T01:53:02Z");
		utilitiesMock.when(() -> Utilities.fromJson(eq("{ }"), any(TypeReference.class))).thenReturn(Map.of());
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEnumValue("IPV4", AddressType.class)).thenReturn(true);
	}

	//-------------------------------------------------------------------------------------------------
	@AfterAll
	private static void closeUtilitiesMock() {
		utilitiesMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	private Entry<System, SystemAddress> createSystem1() {
		final System system = new System("TemperatureProvider1", "{ }", "1.0.1");
		system.onCreate();
		system.setId(0);
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.1");
		return Map.entry(system, systemAddress);
	}

	//-------------------------------------------------------------------------------------------------
	private Entry<System, SystemAddress> createSystem2() {
		final System system = new System("TemperatureProvider2", "{ }", "1.0.1");
		system.onCreate();
		system.setId(1);
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.2");
		return Map.entry(system, systemAddress);
	}

	//-------------------------------------------------------------------------------------------------
	private Entry<Device, DeviceAddress> createDevice1() {
		final Device device = new Device("TEST_DEVICE1", "{ }");
		device.onCreate();
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		return Map.entry(device, deviceAddress);
	}

	//-------------------------------------------------------------------------------------------------
	private DeviceResponseDTO createResponseDevice1() {
		return new DeviceResponseDTO(
				"TEST_DEVICE1",
				Map.of(),
				List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")),
				"2024-11-04T01:53:02Z",
				"2024-11-04T01:53:02Z");
	}

	//-------------------------------------------------------------------------------------------------
	private SystemResponseDTO createResponseSystem1(final DeviceResponseDTO device, final boolean addressDetails) {
		return new SystemResponseDTO(
				"TemperatureProvider1",
				Map.of(),
				"1.0.1",
				addressDetails ? List.of(new AddressDTO("IPV4", "192.168.100.1")) : null,
				device,
				"2024-11-04T01:53:02Z",
				"2024-11-04T01:53:02Z");
	}

	//-------------------------------------------------------------------------------------------------
	private SystemResponseDTO createResponseSystem2(final DeviceResponseDTO device, final boolean addressDetails) {
		return new SystemResponseDTO(
				"TemperatureProvider2",
				Map.of(),
				"1.0.1",
				addressDetails ? List.of(new AddressDTO("IPV4", "192.168.100.2")) : null,
				device,
				"2024-11-04T01:53:02Z",
				"2024-11-04T01:53:02Z");
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceDefinition createServiceDefinition() {
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureInfo");
		serviceDefinition.onCreate();
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstance createServiceInstance(final System system, final ServiceDefinition serviceDefinition) {
		final ServiceInstance instance = new ServiceInstance(
				system.getName() + "|" + serviceDefinition.getName() + "|" + "1.1.1",
				system,
				serviceDefinition,
				"1.1.1",
				ZonedDateTime.parse("2025-11-04T01:53:02Z"),
				"{ }");
		instance.onCreate();
		return instance;
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceInterface createServiceInstanceInterface(final ServiceInstance instance) {
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfacePolicy policy = ServiceInterfacePolicy.CERT_AUTH;
		return new ServiceInstanceInterface(instance, template, "{ }", policy);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO createServiceInstanceResponse(final SystemResponseDTO systemResponse) {

		final ServiceInstanceInterfaceResponseDTO expectedInterface = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "CERT_AUTH", Map.of());

		return new ServiceInstanceResponseDTO(
				systemResponse.name() + "|temperatureInfo|1.1.1",
				systemResponse,
				new ServiceDefinitionResponseDTO("temperatureInfo", "2024-11-04T01:53:02Z", "2024-11-04T01:53:02Z"),
				"1.1.1",
				"2025-11-04T01:53:02Z",
				Map.of(),
				List.of(expectedInterface),
				"2024-11-04T01:53:02Z",
				"2024-11-04T01:53:02Z");
	}
}
