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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.matching.AddressMatching;
import eu.arrowhead.serviceregistry.service.validation.SystemDiscoveryValidation;

@ExtendWith(MockitoExtension.class)
public class SystemDiscoveryServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SystemDiscoveryService service;

	@Mock
	private SystemDiscoveryValidation validator;

	@Mock
	private AddressMatching addressMatcher;

	@Mock
	private SystemDbService dbService;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private ServiceRegistrySystemInfo sysInfo;

	// expected error messages
	private static final String DIFFERENT_METADATA = "System with name: {} already exists, but provided metadata is not matching";
	private static final String DIFFERENT_VERSION = "System with name: {} already exists, but provided version is not matching";
	private static final String DIFFERENT_ADDRESS_LIST = "System with name: {} already exists, but provided address list is not matching";
	private static final String DIFFERENT_DEVICE = "System with name: {} already exists, but provided device name is not matching";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemOrigionNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.registerSystem(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemOrigionEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.registerSystem(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemDifferentMetadata() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 2\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerSystem(dto, "test origin"));
		assertEquals(DIFFERENT_METADATA.replace("{}", "TemperatureManager"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemDifferentVersion() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.1");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerSystem(dto, "test origin"));
		assertEquals(DIFFERENT_VERSION.replace("{}", "TemperatureManager"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemDifferentAddressList() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100", "greenhouse.com"), "DEVICE1");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO(
				"TemperatureManager",
				Map.of("priority", 1),
				"5.0.0",
				List.of(new AddressDTO("IPV4", "192.168.100.100"),
						new AddressDTO("HOSTNAME", "greenhouse.com")),
				"DEVICE1");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerSystem(dto, "test origin"));
		assertEquals(DIFFERENT_ADDRESS_LIST.replace("{}", "TemperatureManager"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(addressMatcher).isAddressListMatching(List.of(new AddressDTO("IPV4", "192.168.100.100")), List.of(new AddressDTO("IPV4", "192.168.100.100"), new AddressDTO("HOSTNAME", "greenhouse.com")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemDifferentInheritedAddressList() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(), "DEVICE1");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(), "DEVICE1");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress1 = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final DeviceAddress deviceAddress2 = new DeviceAddress(device, AddressType.HOSTNAME, "greenhouse.com"); // this one is inheritable
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress1, deviceAddress2)))));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerSystem(dto, "test origin"));
		assertEquals(DIFFERENT_ADDRESS_LIST.replace("{}", "TemperatureManager"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(addressMatcher).isAddressListMatching(List.of(new AddressDTO("IPV4", "192.168.100.100")), List.of(new AddressDTO("HOSTNAME", "greenhouse.com")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemNoAddressProvidedAndNoDeviceToInheritFrom() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(), "DEVICE1");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(), "DEVICE1");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> existingTriple = Triple.of(system, List.of(systemAddress), null);
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(existingTriple));

		when(addressMatcher.isAddressListMatching(eq(List.of(new AddressDTO("IPV4", "192.168.100.100"))), eq(List.of()))).thenReturn(false);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerSystem(dto, "test origin"));
		assertEquals(DIFFERENT_ADDRESS_LIST.replace("{}", "TemperatureManager"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(addressMatcher).isAddressListMatching(List.of(new AddressDTO("IPV4", "192.168.100.100")), List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemOnlyExistingDeviceNameIsNull() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(Triple.of(system, List.of(systemAddress), null)));

		when(addressMatcher.isAddressListMatching(any(), any())).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerSystem(dto, "test origin"));
		assertEquals(DIFFERENT_DEVICE.replace("{}", "TemperatureManager"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemOnlyNewDeviceNameIsNull() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), null);
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), null);
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));

		when(addressMatcher.isAddressListMatching(any(), any())).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerSystem(dto, "test origin"));
		assertEquals(DIFFERENT_DEVICE.replace("{}", "TemperatureManager"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemDifferentDeviceName() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE2");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE2");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));

		when(addressMatcher.isAddressListMatching(any(), any())).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerSystem(dto, "test origin"));
		assertEquals(DIFFERENT_DEVICE.replace("{}", "TemperatureManager"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemOk() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> existingTriple = Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)));
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(existingTriple));

		when(addressMatcher.isAddressListMatching(any(), any())).thenReturn(true);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		when(dtoConverter.convertSystemTripletToDTO(existingTriple)).thenReturn(response);

		final Entry<SystemResponseDTO, Boolean> actual = service.registerSystem(dto, "test origin");
		assertEquals(Map.entry(response, false), actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemExistingSystemOkNullDevices() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), null);
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), null);
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> existingTriple = Triple.of(system, List.of(systemAddress), null);
		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.of(existingTriple));

		when(addressMatcher.isAddressListMatching(any(), any())).thenReturn(true);

		final DeviceResponseDTO deviceResponse = null;
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		when(dtoConverter.convertSystemTripletToDTO(existingTriple)).thenReturn(response);

		final Entry<SystemResponseDTO, Boolean> actual = service.registerSystem(dto, "test origin");
		assertEquals(Map.entry(response, false), actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemNewSystemOk() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		when(dbService.getByName("TemperatureManager")).thenReturn(Optional.empty());

		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> createdTriple = Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)));
		when(dbService.createBulk(List.of(normalizedDto))).thenReturn(List.of(createdTriple));

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);

		when(dtoConverter.convertSystemTripletToDTO(createdTriple)).thenReturn(response);

		final Entry<SystemResponseDTO, Boolean> actual = service.registerSystem(dto, "test origin");
		assertEquals(Map.entry(response, true), actual);
		verify(dbService).createBulk(List.of(normalizedDto));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemThrowsInternalServerError() {

		// dtos
		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeRegisterSystem(dto, "test origin")).thenReturn(normalizedDto);

		when(dbService.getByName("TemperatureManager")).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.registerSystem(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemOrigionNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.lookupSystem(null, false, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemOrigionEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.lookupSystem(null, false, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemVerboseParamFalse() {

		final Triple<SystemLookupRequestDTO, SystemListResponseDTO, SystemListResponseDTO> setupResult = setupLookup();
		final SystemLookupRequestDTO dto = setupResult.getLeft();
		final SystemListResponseDTO verbose = setupResult.getMiddle();
		final SystemListResponseDTO expected = setupResult.getRight();

		final SystemListResponseDTO actual = service.lookupSystem(dto, false, "test origin");
		assertEquals(expected, actual);
		verify(dtoConverter).convertSystemListResponseDtoToTerse(verbose);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemDiscoveryVerboseFalse() {

		final Triple<SystemLookupRequestDTO, SystemListResponseDTO, SystemListResponseDTO> setupResult = setupLookup();
		final SystemLookupRequestDTO dto = setupResult.getLeft();
		final SystemListResponseDTO verbose = setupResult.getMiddle();
		final SystemListResponseDTO expected = setupResult.getRight();

		when(sysInfo.isDiscoveryVerbose()).thenReturn(false);

		final SystemListResponseDTO actual = service.lookupSystem(dto, true, "test origin");
		assertEquals(expected, actual);
		verify(dtoConverter).convertSystemListResponseDtoToTerse(verbose);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemVerboseEnabled() {

		final Triple<SystemLookupRequestDTO, SystemListResponseDTO, SystemListResponseDTO> setupResult = setupLookup();
		final SystemLookupRequestDTO dto = setupResult.getLeft();
		final SystemListResponseDTO verbose = setupResult.getMiddle();

		when(sysInfo.isDiscoveryVerbose()).thenReturn(true);

		final SystemListResponseDTO actual = service.lookupSystem(dto, true, "test origin");
		assertEquals(verbose, actual);
		verify(dtoConverter, never()).convertSystemListResponseDtoToTerse(verbose);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemAddressTypeIsNull() {

		// dto
		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "EQUALS", "value", 1));
		final SystemLookupRequestDTO dto = new SystemLookupRequestDTO(
				List.of("TemperatureManager"),
				List.of("192.168.100.100"),
				"",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("DEVICE1"));

		when(validator.validateAndNormalizeLookupSystem(dto, "test origin")).thenReturn(dto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> existingTriple = Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)));
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> pageOfTriple = new PageImpl<>(List.of(existingTriple));

		when(dbService.getPageByFilters(
				any(),
				eq(List.of("TemperatureManager")),
				eq(List.of("192.168.100.100")),
				eq(null),
				eq(List.of(requirement)),
				eq(List.of("5.0.0")),
				eq(List.of("DEVICE1")))).thenReturn(pageOfTriple);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(pageOfTriple)).thenReturn(result);

		when(sysInfo.isDiscoveryVerbose()).thenReturn(true);

		final SystemListResponseDTO actual = service.lookupSystem(dto, true, "test origin");
		assertEquals(new SystemListResponseDTO(List.of(response), 1), actual);
		verify(dtoConverter, never()).convertSystemListResponseDtoToTerse(result);
		verify(dbService).getPageByFilters(
				any(),
				eq(List.of("TemperatureManager")),
				eq(List.of("192.168.100.100")),
				eq(null),
				eq(List.of(requirement)),
				eq(List.of("5.0.0")),
				eq(List.of("DEVICE1")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemThrowsInternalServerError() {

		// dto
		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "EQUALS", "value", 1));
		final SystemLookupRequestDTO dto = new SystemLookupRequestDTO(
				List.of("TemperatureManager"),
				List.of("192.168.100.100"),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("DEVICE1"));

		when(validator.validateAndNormalizeLookupSystem(dto, "test origin")).thenReturn(dto);

		when(dbService.getPageByFilters(
				any(),
				eq(List.of("TemperatureManager")),
				eq(List.of("192.168.100.100")),
				eq(AddressType.IPV4),
				eq(List.of(requirement)),
				eq(List.of("5.0.0")),
				eq(List.of("DEVICE1")))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.lookupSystem(dto, true, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeSystemOrigionNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeSystem(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeSystemOrigionEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeSystem(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeSystemOk() {

		final String name = "TemperatureManager";
		when(validator.validateAndNormalizeRevokeSystem(name, "test origin")).thenReturn(name);
		when(dbService.deleteByName(name)).thenReturn(true);
		assertTrue(service.revokeSystem(name, "test origin"));
		verify(dbService).deleteByName(name);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeSystemThrowsInternalServerError() {

		final String name = "TemperatureManager";
		when(validator.validateAndNormalizeRevokeSystem(name, "test origin")).thenReturn(name);
		when(dbService.deleteByName(name)).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.revokeSystem(name, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Triple<SystemLookupRequestDTO, SystemListResponseDTO, SystemListResponseDTO> setupLookup() {

		// dto
		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "EQUALS", "value", 1));
		final SystemLookupRequestDTO dto = new SystemLookupRequestDTO(
				List.of("TemperatureManager"),
				List.of("192.168.100.100"),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("DEVICE1"));

		when(validator.validateAndNormalizeLookupSystem(dto, "test origin")).thenReturn(dto);

		// entities in the database
		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 1\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
		final Device device = new Device("DEVICE1", "{ }");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> existingTriple = Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)));
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> pageOfTriple = new PageImpl<>(List.of(existingTriple));

		when(dbService.getPageByFilters(
				any(),
				eq(List.of("TemperatureManager")),
				eq(List.of("192.168.100.100")),
				eq(AddressType.IPV4),
				eq(List.of(requirement)),
				eq(List.of("5.0.0")),
				eq(List.of("DEVICE1")))).thenReturn(pageOfTriple);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(pageOfTriple)).thenReturn(result);

		// convert to terse
		final DeviceResponseDTO deviceResponseTerse = new DeviceResponseDTO("DEVICE1", null, null, null, null);
		final SystemResponseDTO responseTerse = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponseTerse, null, null);
		final SystemListResponseDTO resultTerse = new SystemListResponseDTO(List.of(responseTerse), 1);
		lenient().when(dtoConverter.convertSystemListResponseDtoToTerse(result)).thenReturn(resultTerse);

		return Triple.of(dto, result, resultTerse);
	}
}
