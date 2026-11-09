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
package eu.arrowhead.serviceregistry.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.common.service.validation.MetadataRequirementsMatcher;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceSystemConnector;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceSystemConnectorRepository;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;

@ExtendWith(MockitoExtension.class)
public class DeviceDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	@Spy
	private DeviceDbService service;

	@Mock
	private DeviceRepository deviceRepo;

	@Mock
	private DeviceAddressRepository addressRepo;

	@Mock
	private DeviceSystemConnectorRepository connectorRepo;

	private static final String DB_ERROR_MSG = "Database operation error";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByFilters() {

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDeviceAndAddressIn(any(), any())).thenReturn(List.of(deviceAddress));

		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(device)));
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final List<Entry<Device, List<DeviceAddress>>> expected = List.of(Map.entry(device, List.of(deviceAddress)));
		final List<Entry<Device, List<DeviceAddress>>> actual = service.getByFilters(List.of(), List.of("3a:7f:12:b4:9c:e1"), null, List.of());

		assertEquals(expected, actual);
		verify(service).getPage(any(), eq(List.of()), eq(List.of("3a:7f:12:b4:9c:e1")), eq(null), eq(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageWithoutFilterParameters() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of()));

		service.getPage(pageRequest);
		verify(deviceRepo).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageShouldReturnMatchingAddressesToo() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(device)));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDevice(device)).thenReturn(List.of(deviceAddress));

		final Page<Entry<Device, List<DeviceAddress>>> expected = new PageImpl<Entry<Device, List<DeviceAddress>>>(List.of(Map.entry(device, List.of(deviceAddress))), pageRequest, 1);

		final Page<Entry<Device, List<DeviceAddress>>> actual = service.getPage(pageRequest, List.of(), List.of(), null, List.of());
		verify(addressRepo).findAllByDevice(device);
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageNotEmptyFilters() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDeviceAndAddressIn(any(), any())).thenReturn(List.of(deviceAddress));

		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(device)));
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		service.getPage(pageRequest, List.of("DEVICE"), List.of("3a:7f:12:b4:9c:e1"), null, List.of());
		verify(deviceRepo).findAllByNameIn(List.of("DEVICE"), pageRequest);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void testGetPageEmptyFiltersOnly() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of()));

		try (MockedStatic<Utilities> utilitiesMock = mockStatic(Utilities.class)) {
			utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);

			service.getPage(pageRequest, List.of(), List.of(), null, List.of());

			utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), times(3));
		}
		verify(deviceRepo).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByName() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.getPage(pageRequest, List.of("DEVICE"), List.of(), null, List.of());
		verify(deviceRepo).findAllByNameIn(List.of("DEVICE"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByAddressButNoMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));
		when(addressRepo.findAllByDeviceAndAddressIn(any(), any())).thenReturn(List.of());
		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.getPage(pageRequest, List.of(), List.of("3a:7f:12:b4:9c:e1"), null, List.of());
		verify(addressRepo).findAllByDeviceAndAddressIn(device, List.of("3a:7f:12:b4:9c:e1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByAddressAndMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDeviceAndAddressIn(any(), any())).thenReturn(List.of(deviceAddress));

		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(device)));
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final Page<Entry<Device, List<DeviceAddress>>> expected = new PageImpl<Entry<Device, List<DeviceAddress>>>(List.of(Map.entry(device, List.of(deviceAddress))), pageRequest, 1);

		final Page<Entry<Device, List<DeviceAddress>>> actual = service.getPage(pageRequest, List.of(), List.of("3a:7f:12:b4:9c:e1"), null, List.of());
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByAddressTypeButNoMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));
		when(addressRepo.findAllByDeviceAndAddressType(any(), any())).thenReturn(List.of());
		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.getPage(pageRequest, List.of(), List.of(), AddressType.MAC, List.of());
		verify(addressRepo).findAllByDeviceAndAddressType(device, AddressType.MAC);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByAddressTypeAndMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDeviceAndAddressType(any(), any())).thenReturn(List.of(deviceAddress));

		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(device)));
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final Page<Entry<Device, List<DeviceAddress>>> expected = new PageImpl<Entry<Device, List<DeviceAddress>>>(List.of(Map.entry(device, List.of(deviceAddress))), pageRequest, 1);

		final Page<Entry<Device, List<DeviceAddress>>> actual = service.getPage(pageRequest, List.of(), List.of(), AddressType.MAC, List.of());
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByMetadataAndMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));

		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(device)));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final Page<Entry<Device, List<DeviceAddress>>> expected = new PageImpl<Entry<Device, List<DeviceAddress>>>(List.of(Map.entry(device, List.of(deviceAddress))), pageRequest, 1);

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", true);
		final Page<Entry<Device, List<DeviceAddress>>> actual = service.getPage(pageRequest, List.of(), List.of(), null, List.of(requirement));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByMetadataButNoMatch() {

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : false\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));
		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", true);

		try (MockedStatic<MetadataRequirementsMatcher> metaMatcherMock = mockStatic(MetadataRequirementsMatcher.class)) {
			metaMatcherMock.when(() -> MetadataRequirementsMatcher.isMetadataMatch(any(), any())).thenReturn(false);

			service.getPage(pageRequest, List.of(), List.of(), null, List.of(requirement));
			metaMatcherMock.verify(() -> MetadataRequirementsMatcher.isMetadataMatch(Map.of("indoor", false), requirement));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageThrowsInternalServerError() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAll(pageRequest)).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getPage(pageRequest, List.of(), List.of(), null, List.of()));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByName(null));

		assertEquals("device name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByName(""));

		assertEquals("device name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameEmptyResult() {

		when(deviceRepo.findByName(any())).thenReturn(Optional.empty());

		final Optional<Entry<Device, List<DeviceAddress>>> result = service.getByName("DEVICE");
		assertEquals(Optional.empty(), result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNamePresentResult() {

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findByName(any())).thenReturn(Optional.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final Optional<Entry<Device, List<DeviceAddress>>> result = service.getByName("DEVICE");
		assertEquals(Optional.of(Map.entry(device, List.of(deviceAddress))), result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameThrowsInternalServerError() {

		when(deviceRepo.findByName(any())).thenThrow(new LockedException("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getByName("DEVICE"));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createBulk(null));

		assertEquals("device candidate list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createBulk(List.of()));

		assertEquals("device candidate list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkExistingDevicesThrowsInvalidParameterException() {

		final NormalizedDeviceRequestDTO dto1 = new NormalizedDeviceRequestDTO("DEVICE1", Map.of("indoor", false), List.of());
		final NormalizedDeviceRequestDTO dto2 = new NormalizedDeviceRequestDTO("DEVICE2", Map.of("indoor", true), List.of());
		final NormalizedDeviceRequestDTO dto3 = new NormalizedDeviceRequestDTO("DEVICE3", Map.of("indoor", true), List.of());

		final Device device1 = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");
		final Device device3 = new Device("DEVICE3", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of(device1, device3));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto1, dto2, dto3)));
		assertEquals("Device with names already exists: DEVICE1, DEVICE3", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkOk() {

		final List<AddressDTO> addresses = new ArrayList<AddressDTO>(2);
		addresses.add(new AddressDTO("MAC", "3a:7c:91:ef:2d:b6"));
		addresses.add(new AddressDTO("MAC", "56:3e:c9:7a:11:84"));

		final NormalizedDeviceRequestDTO deviceWithAddresses = new NormalizedDeviceRequestDTO("DEVICE1", Map.of("indoor", false), addresses);
		final NormalizedDeviceRequestDTO deviceWithoutAddresses = new NormalizedDeviceRequestDTO("DEVICE2", Map.of("indoor", false), List.of());

		final Device deviceEntity1 = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");
		final Device deviceEntity2 = new Device("DEVICE2", "{\r\n  \"indoor\" : false\r\n}");
		when(deviceRepo.saveAllAndFlush(List.of(deviceEntity1, deviceEntity2))).thenReturn(List.of(deviceEntity1, deviceEntity2));

		final DeviceAddress deviceAddress1 = new DeviceAddress(deviceEntity1, AddressType.MAC, "3a:7c:91:ef:2d:b6");
		final DeviceAddress deviceAddress2 = new DeviceAddress(deviceEntity1, AddressType.MAC, "56:3e:c9:7a:11:84");
		when(addressRepo.findAllByDevice(same(deviceEntity1))).thenReturn(List.of(deviceAddress1, deviceAddress2));
		when(addressRepo.findAllByDevice(same(deviceEntity2))).thenReturn(List.of());

		final List<Entry<Device, List<DeviceAddress>>> expected = List.of(Map.entry(deviceEntity1, List.of(deviceAddress1, deviceAddress2)), Map.entry(deviceEntity2, List.of()));
		final List<Entry<Device, List<DeviceAddress>>> actual = service.createBulk(List.of(deviceWithAddresses, deviceWithoutAddresses));
		assertEquals(expected, actual);
		verify(deviceRepo).saveAllAndFlush(List.of(deviceEntity1, deviceEntity2));
		verify(addressRepo).saveAllAndFlush(List.of(deviceAddress1, deviceAddress2));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkThrowsInternalServerError() {

		final NormalizedDeviceRequestDTO dto = new NormalizedDeviceRequestDTO("DEVICE1", Map.of("indoor", false), List.of());
		when(deviceRepo.findAllByNameIn(any())).thenThrow(new LockedException("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createBulk(List.of(dto)));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.create(null));

		assertEquals("device candidate is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateNullName() {
		final NormalizedDeviceRequestDTO candidate = new NormalizedDeviceRequestDTO(null, null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.create(candidate));

		assertEquals("device candidate name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateEmptyName() {
		final NormalizedDeviceRequestDTO candidate = new NormalizedDeviceRequestDTO("", null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.create(candidate));

		assertEquals("device candidate name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExistingDeviceThrowsInvalidParameterException() {

		final NormalizedDeviceRequestDTO dto = new NormalizedDeviceRequestDTO("DEVICE", Map.of("indoor", true), List.of());
		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findByName(any())).thenReturn(Optional.of(device));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.create(dto));
		assertEquals("Device with name 'DEVICE' already exists", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceWithAddress() {

		final List<AddressDTO> addresses = List.of(new AddressDTO("MAC", "3a:7c:91:ef:2d:b6"));
		final NormalizedDeviceRequestDTO dto = new NormalizedDeviceRequestDTO("DEVICE", Map.of("indoor", false), addresses);

		when(deviceRepo.findByName(any())).thenReturn(Optional.empty());

		final Device device = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");
		when(deviceRepo.saveAndFlush(device)).thenReturn(device);

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7c:91:ef:2d:b6");
		when(addressRepo.saveAllAndFlush(List.of(deviceAddress))).thenReturn(List.of(deviceAddress));

		final Entry<Device, List<DeviceAddress>> actual = service.create(dto);
		assertEquals(Map.entry(device, List.of(deviceAddress)), actual);
		verify(deviceRepo).saveAndFlush(device);
		verify(addressRepo).saveAllAndFlush(List.of(deviceAddress));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceWithoutAddress() {

		final NormalizedDeviceRequestDTO dto = new NormalizedDeviceRequestDTO("DEVICE", Map.of("indoor", false), List.of());

		when(deviceRepo.findByName(any())).thenReturn(Optional.empty());

		final Device device = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");
		when(deviceRepo.saveAndFlush(device)).thenReturn(device);

		final Entry<Device, List<DeviceAddress>> actual = service.create(dto);
		assertEquals(Map.entry(device, List.of()), actual);
		verify(deviceRepo).saveAndFlush(device);
		verify(addressRepo, never()).saveAllAndFlush(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateThrowsInternalServerError() {

		final NormalizedDeviceRequestDTO dto = new NormalizedDeviceRequestDTO("DEVICE", Map.of("indoor", false), List.of());

		when(deviceRepo.findByName(any())).thenThrow(new LockedException("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.create(dto));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateBulk(null));

		assertEquals("candidate list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateBulk(List.of()));

		assertEquals("candidate list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNotExistingDeviceThrowsInvalidParameterException() {

		final NormalizedDeviceRequestDTO dto1 = new NormalizedDeviceRequestDTO("DEVICE1", Map.of("indoor", false), List.of());
		final NormalizedDeviceRequestDTO dto2 = new NormalizedDeviceRequestDTO("DEVICE2", Map.of("indoor", false), List.of());
		final NormalizedDeviceRequestDTO dto3 = new NormalizedDeviceRequestDTO("DEVICE3", Map.of("indoor", false), List.of());
		final Device device = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of(device));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateBulk(List.of(dto1, dto2, dto3)));
		assertEquals("Device(s) not exists: DEVICE2, DEVICE3", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkOk() {

		final List<AddressDTO> addresses = new ArrayList<AddressDTO>(2);
		addresses.add(new AddressDTO("MAC", "3a:7c:91:ef:2d:b6"));
		addresses.add(new AddressDTO("MAC", "56:3e:c9:7a:11:84"));

		final NormalizedDeviceRequestDTO deviceWithAddresses = new NormalizedDeviceRequestDTO("DEVICE1", Map.of("indoor", false), addresses);
		final NormalizedDeviceRequestDTO deviceWithoutAddresses = new NormalizedDeviceRequestDTO("DEVICE2", Map.of("indoor", false), List.of());

		final Device deviceEntity1 = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");
		final Device deviceEntity2 = new Device("DEVICE2", "{\r\n  \"indoor\" : false\r\n}");
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of(deviceEntity1, deviceEntity2));
		when(deviceRepo.saveAllAndFlush(List.of(deviceEntity1, deviceEntity2))).thenReturn(List.of(deviceEntity1, deviceEntity2));

		final DeviceAddress deviceAddress1 = new DeviceAddress(deviceEntity1, AddressType.MAC, "3a:7c:91:ef:2d:b6");
		final DeviceAddress deviceAddress2 = new DeviceAddress(deviceEntity1, AddressType.MAC, "56:3e:c9:7a:11:84");
		when(addressRepo.deleteAllByDeviceIn(any())).thenReturn(List.of(deviceAddress1, deviceAddress2));
		when(addressRepo.saveAllAndFlush(any())).thenReturn(List.of(deviceAddress1, deviceAddress2));
		when(addressRepo.findAllByDevice(same(deviceEntity1))).thenReturn(List.of(deviceAddress1, deviceAddress2));
		when(addressRepo.findAllByDevice(same(deviceEntity2))).thenReturn(List.of());

		final List<Entry<Device, List<DeviceAddress>>> expected = List.of(Map.entry(deviceEntity1, List.of(deviceAddress1, deviceAddress2)), Map.entry(deviceEntity2, List.of()));
		final List<Entry<Device, List<DeviceAddress>>> actual = service.updateBulk(List.of(deviceWithAddresses, deviceWithoutAddresses));
		assertEquals(expected, actual);
		verify(deviceRepo).saveAllAndFlush(List.of(deviceEntity1, deviceEntity2));
		verify(addressRepo).deleteAllByDeviceIn(List.of(deviceEntity1, deviceEntity2));
		verify(addressRepo).saveAllAndFlush(List.of(deviceAddress1, deviceAddress2));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkThrowsInternalServerError() {

		final NormalizedDeviceRequestDTO dto = new NormalizedDeviceRequestDTO("DEVICE", Map.of("indoor", false), List.of());
		when(deviceRepo.findAllByNameIn(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.updateBulk(List.of(dto)));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByNameList(null));

		assertEquals("device name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByNameList(List.of()));

		assertEquals("device name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListASystemIsAssignedThrowsLockedException() {

		final Device device = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");
		final System system = new System("SYSTEM1", "{\r\n  \"indoor\" : false\r\n}", "5.0.0");
		final DeviceSystemConnector connection = new DeviceSystemConnector(device, system);

		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of(device));
		when(connectorRepo.findByDeviceIn(any())).thenReturn(List.of(connection));

		final LockedException ex = assertThrows(LockedException.class, () -> service.deleteByNameList(List.of("DEVICE1")));
		assertEquals("At least one system is assigned to these devices", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListOk() {

		final Device device = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");

		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of(device));
		when(connectorRepo.findByDeviceIn(any())).thenReturn(List.of());

		service.deleteByNameList(List.of("DEVICE1"));
		verify(deviceRepo).deleteAll(List.of(device));
		verify(deviceRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListThrowsInternalServerError() {

		when(deviceRepo.findAllByNameIn(any())).thenThrow(new RuntimeException("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.deleteByNameList(List.of("DEVICE1")));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByName(null));

		assertEquals("device name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByName(""));

		assertEquals("device name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameASystemIsAssignedThrowsLockedException() {

		final Device device = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");
		final System system = new System("SYSTEM1", "{\r\n  \"indoor\" : false\r\n}", "5.0.0");
		final DeviceSystemConnector connection = new DeviceSystemConnector(device, system);

		when(deviceRepo.findByName(any())).thenReturn(Optional.of(device));
		when(connectorRepo.findByDevice(any())).thenReturn(List.of(connection));

		final LockedException ex = assertThrows(LockedException.class, () -> service.deleteByName("DEVICE1"));
		assertEquals("At least one system is assigned to this device", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameExistingDevice() {

		final Device device = new Device("DEVICE1", "{\r\n  \"indoor\" : false\r\n}");

		when(deviceRepo.findByName(any())).thenReturn(Optional.of(device));
		when(connectorRepo.findByDevice(any())).thenReturn(List.of());

		final boolean result = service.deleteByName("DEVICE1");
		assertTrue(result);
		verify(deviceRepo).delete(device);
		verify(deviceRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameNotExistingDevice() {

		when(deviceRepo.findByName(any())).thenReturn(Optional.empty());

		final boolean result = service.deleteByName("DEVICE1");
		assertFalse(result);
		verify(deviceRepo, never()).delete(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameThrowsInternalServerError() {

		when(deviceRepo.findByName(any())).thenThrow(new RuntimeException("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.deleteByName("DEVICE1"));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}
}
