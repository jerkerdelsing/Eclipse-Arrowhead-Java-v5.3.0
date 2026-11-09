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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

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
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceSystemConnectorRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemRepository;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;

@ExtendWith(MockitoExtension.class)
public class SystemDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	@Spy
	private SystemDbService service;

	@Mock
	private SystemRepository systemRepo;

	@Mock
	private SystemAddressRepository systemAddressRepo;

	@Mock
	private DeviceAddressRepository deviceAddressRepo;

	@Mock
	private DeviceRepository deviceRepo;

	@Mock
	private DeviceSystemConnectorRepository deviceSystemConnectorRepo;

	private static final String DB_ERROR_MSG = "Database operation error";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createBulk(null));

		assertEquals("system candidate list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createBulk(List.of()));

		assertEquals("system candidate list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkExistingSystemNamesThowsInvalidParameterException() {

		final NormalizedSystemRequestDTO dto1 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE1");
		final NormalizedSystemRequestDTO dto2 = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), "TEST_DEVICE2");

		final System system1 = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		final System system2 = new System("TemperatureManager", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system1, system2));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto1, dto2)));
		assertEquals("Systems with names already exist: TemperatureConsumer, TemperatureManager", ex.getMessage());
		verify(systemRepo).findAllByNameIn(List.of("TemperatureConsumer", "TemperatureManager"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNotExistingDeviceNamesThowsInvalidParameterException() {

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());

		final NormalizedSystemRequestDTO dto1 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE1");
		final NormalizedSystemRequestDTO dto2 = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), "TEST_DEVICE2");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto1, dto2)));
		assertEquals("Device names do not exist: TEST_DEVICE1, TEST_DEVICE2", ex.getMessage());
		verify(deviceRepo).findAllByNameIn(List.of("TEST_DEVICE1", "TEST_DEVICE2"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNoAddressesThrowsInvalidParameterException() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "DEVICE");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(List.of("DEVICE"))).thenReturn(List.of(device));
		when(deviceRepo.findByName("DEVICE")).thenReturn(Optional.of(device));

		// no inheritable address
		when(deviceAddressRepo.findAllByDeviceAndAddressTypeIn(any(), any())).thenReturn(List.of());

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto)));
		assertEquals("At least one system address is needed for every system", ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkInheritedSystemAddress() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "DEVICE");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(List.of("DEVICE"))).thenReturn(List.of(device));
		when(deviceRepo.findByName("DEVICE")).thenReturn(Optional.of(device));

		final DeviceAddress deviceAddress1 = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		final DeviceAddress deviceAddress2 = new DeviceAddress(device, AddressType.IPV4, "192.168.4.4");
		when(deviceAddressRepo.findAllByDevice(device)).thenReturn(List.of(deviceAddress1, deviceAddress2));

		when(deviceAddressRepo.findAllByDeviceAndAddressTypeIn(any(), any())).thenReturn(List.of(deviceAddress2));

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findByName("TemperatureConsumer")).thenReturn(Optional.of(system));

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");

		when(deviceSystemConnectorRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(
				Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress1, deviceAddress2))));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.createBulk(List.of(dto));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNotInheritedSystemAddresses() {

		final NormalizedSystemRequestDTO systemWithDevice = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "DEVICE");
		final NormalizedSystemRequestDTO systemWithoutDevice = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), null);

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(List.of("DEVICE"))).thenReturn(List.of(device));
		when(deviceRepo.findByName("DEVICE")).thenReturn(Optional.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(deviceAddressRepo.findAllByDevice(device)).thenReturn(List.of(deviceAddress));

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final System system1 = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		final System system2 = new System("TemperatureManager", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findByName("TemperatureConsumer")).thenReturn(Optional.of(system1));

		final SystemAddress systemAddress1 = new SystemAddress(system1, AddressType.IPV4, "192.168.4.4");
		final SystemAddress systemAddress2 = new SystemAddress(system2, AddressType.IPV4, "192.168.4.5");
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceSystemConnectorRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(
				Triple.of(system1, List.of(systemAddress1), Map.entry(device, List.of(deviceAddress))),
				Triple.of(system2, List.of(systemAddress2), null));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.createBulk(List.of(systemWithDevice, systemWithoutDevice));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNoDevice() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), null);

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(
				Triple.of(system, List.of(systemAddress), null));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.createBulk(List.of(dto));
		assertEquals(expected, actual);
		verify(deviceSystemConnectorRepo, never()).saveAllAndFlush(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkThrowsInternalServerError() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "DEVICE");
		when(systemRepo.findAllByNameIn(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createBulk(List.of(dto)));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNoAddress() {
		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", null, "1.0.0", List.of(), null);
		final System system = new System("TemperatureConsumer", null, "1.0.0");
		final System savedSystem = new System("TemperatureConsumer", null, "1.0.0");
		savedSystem.setId(1);

		when(systemRepo.findAllByNameIn(List.of("TemperatureConsumer"))).thenReturn(List.of());
		when(deviceRepo.findAllByNameIn(List.of())).thenReturn(List.of());
		when(systemRepo.saveAllAndFlush(List.of(system))).thenReturn(List.of(savedSystem));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createBulk(List.of(dto)));

		assertEquals("At least one system address is needed for every system", ex.getMessage());

		verify(systemRepo).findAllByNameIn(List.of("TemperatureConsumer"));
		verify(deviceRepo).findAllByNameIn(List.of());
		verify(systemRepo).saveAllAndFlush(List.of(system));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateBulk(null));

		assertEquals("The list of systems to update is empty or missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateBulk(List.of()));

		assertEquals("The list of systems to update is empty or missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNotExistingSystemsThrowsInvalidParameterException() {

		final NormalizedSystemRequestDTO dto1 = new NormalizedSystemRequestDTO("TemperatureConsumer1", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE1");
		final NormalizedSystemRequestDTO dto2 = new NormalizedSystemRequestDTO("TemperatureConsumer2", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.8")), "TEST_DEVICE8");
		final NormalizedSystemRequestDTO dto3 = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), "TEST_DEVICE2");

		final System system = new System("TemperatureConsumer2", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateBulk(List.of(dto1, dto2, dto3)));
		assertEquals("System(s) not exists: TemperatureConsumer1, TemperatureManager", ex.getMessage());
		verify(systemRepo).findAllByNameIn(List.of("TemperatureConsumer1", "TemperatureConsumer2", "TemperatureManager"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNotExistingDevicesThrowsInvalidParameterException() {

		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());

		final NormalizedSystemRequestDTO dto1 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE1");
		final NormalizedSystemRequestDTO dto2 = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), "TEST_DEVICE2");

		final System system1 = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		final System system2 = new System("TemperatureManager", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system1, system2));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateBulk(List.of(dto1, dto2)));
		assertEquals("Device names do not exist: TEST_DEVICE1, TEST_DEVICE2", ex.getMessage());
		verify(deviceRepo).findAllByNameIn(List.of("TEST_DEVICE1", "TEST_DEVICE2"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNoDeviceCheckNeededOperations() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), null);

		final System oldSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(oldSystem));
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());

		when(systemAddressRepo.deleteAllBySystemIn(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.empty());

		final System newSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : true\r\n}", "1.0.1");
		final SystemAddress systemAddress = new SystemAddress(newSystem, AddressType.IPV4, "192.168.4.5");
		List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(Triple.of(newSystem, List.of(systemAddress), null));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.updateBulk(List.of(dto));
		assertEquals(expected, actual);
		verify(systemRepo).saveAllAndFlush(List.of(newSystem));
		final InOrder inOrder = Mockito.inOrder(systemAddressRepo);
		inOrder.verify(systemAddressRepo).deleteAllBySystemIn(List.of(newSystem));
		inOrder.verify(systemAddressRepo).saveAllAndFlush(List.of(systemAddress));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNoOldDeviceButNewDevice() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), "DEVICE");

		final System oldSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(oldSystem));
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(List.of("DEVICE"))).thenReturn(List.of(device));
		when(deviceRepo.findByName("DEVICE")).thenReturn(Optional.of(device));

		when(systemAddressRepo.deleteAllBySystemIn(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.empty());
		when(deviceSystemConnectorRepo.saveAll(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(deviceAddressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final System newSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : true\r\n}", "1.0.1");
		final SystemAddress systemAddress = new SystemAddress(newSystem, AddressType.IPV4, "192.168.4.5");
		List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(Triple.of(newSystem, List.of(systemAddress), Map.entry(device, List.of(deviceAddress))));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.updateBulk(List.of(dto));
		assertEquals(expected, actual);
		final InOrder inOrder = Mockito.inOrder(deviceSystemConnectorRepo);
		inOrder.verify(deviceSystemConnectorRepo).saveAll(List.of(new DeviceSystemConnector(device, newSystem)));
		inOrder.verify(deviceSystemConnectorRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkOldDeviceButNoNewDevice() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), null);

		final System oldSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(oldSystem));
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceSystemConnector connection = new DeviceSystemConnector(device, oldSystem);
		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.of(connection));

		when(systemAddressRepo.deleteAllBySystemIn(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final System newSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : true\r\n}", "1.0.1");
		final SystemAddress systemAddress = new SystemAddress(newSystem, AddressType.IPV4, "192.168.4.5");
		List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(Triple.of(newSystem, List.of(systemAddress), null));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.updateBulk(List.of(dto));
		assertEquals(expected, actual);
		final InOrder inOrder = Mockito.inOrder(deviceSystemConnectorRepo);
		inOrder.verify(deviceSystemConnectorRepo).deleteAll(List.of(connection));
		inOrder.verify(deviceSystemConnectorRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkUpdateDevice() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), "DEVICE2");

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final Device device2 = new Device("DEVICE2", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of(device2));
		when(deviceRepo.findByName("DEVICE2")).thenReturn(Optional.of(device2));

		final Device device1 = new Device("DEVICE1", "{\r\n  \"indoor\" : true\r\n}");

		when(systemAddressRepo.deleteAllBySystemIn(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final DeviceAddress deviceAddress = new DeviceAddress(device2, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(deviceAddressRepo.findAllByDevice(device2)).thenReturn(List.of(deviceAddress));

		final DeviceSystemConnector oldConnection = new DeviceSystemConnector(device1, system);
		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.of(oldConnection));

		system.setMetadata("{\r\n  \"indoor\" : true\r\n}");
		system.setVersion("1.0.1");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.5");

		List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(Triple.of(system, List.of(systemAddress), Map.entry(device2, List.of(deviceAddress))));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.updateBulk(List.of(dto));
		assertEquals(expected, actual);
		final InOrder inOrder = Mockito.inOrder(deviceSystemConnectorRepo);
		inOrder.verify(deviceSystemConnectorRepo).saveAll(List.of(new DeviceSystemConnector(device2, system)));
		inOrder.verify(deviceSystemConnectorRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkThrowsInternalServerError() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), "DEVICE2");
		when(systemRepo.findAllByNameIn(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.updateBulk(List.of(dto)));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByNameList(null));

		assertEquals("system name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByNameList(List.of()));

		assertEquals("system name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListOk() {

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(List.of("TemperatureConsumer"))).thenReturn(List.of(system));

		assertDoesNotThrow(() -> service.deleteByNameList(List.of("TemperatureConsumer")));
		final InOrder inOrder = Mockito.inOrder(systemRepo);
		inOrder.verify(systemRepo).deleteAll(List.of(system));
		inOrder.verify(systemRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListThrowsInternalServerError() {

		when(systemRepo.findAllByNameIn(List.of("TemperatureConsumer"))).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.deleteByNameList(List.of("TemperatureConsumer")));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByName(null));

		assertEquals("system name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByName(""));

		assertEquals("system name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameNoMatchingSystem() {

		when(systemRepo.findByName("TemperatureConsumer")).thenReturn(Optional.empty());
		assertEquals(Optional.empty(), service.getByName("TemperatureConsumer"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameDeviceNull() {

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findByName("TemperatureConsumer")).thenReturn(Optional.of(system));

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");
		when(systemAddressRepo.findAllBySystem(any())).thenReturn(List.of(systemAddress));

		final Optional<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = Optional.of(Triple.of(system, List.of(systemAddress), null));
		final Optional<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.getByName("TemperatureConsumer");
		assertEquals(expected, actual);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameDeviceNotNull() {

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findByName("TemperatureConsumer")).thenReturn(Optional.of(system));

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");
		when(systemAddressRepo.findAllBySystem(any())).thenReturn(List.of(systemAddress));

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceSystemConnector connection = new DeviceSystemConnector(device, system);
		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.of(connection));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(deviceAddressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final Optional<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = Optional.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress))));
		final Optional<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.getByName("TemperatureConsumer");
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameThrowsInternalServerError() {

		when(systemRepo.findByName("TemperatureConsumer")).thenThrow(new LockedException("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getByName("TemperatureConsumer"));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameListInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByNameList(null));

		assertEquals("system name list is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameListInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByNameList(List.of()));

		assertEquals("system name list is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameListInputContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByNameList(list));

		assertEquals("system name list contains null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameListInputContainsEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByNameList(List.of("")));

		assertEquals("system name list contains null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameListElementNotPresent() {

		when(systemRepo.findByName("TemperatureConsumer")).thenReturn(Optional.empty());
		assertEquals(List.of(), service.getByNameList(List.of("TemperatureConsumer")));
		verify(service).getByName("TemperatureConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameListElementIsPresent() {

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findByName("TemperatureConsumer")).thenReturn(Optional.of(system));

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");
		when(systemAddressRepo.findAllBySystem(any())).thenReturn(List.of(systemAddress));

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceSystemConnector connection = new DeviceSystemConnector(device, system);
		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.of(connection));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(deviceAddressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> expectedElement = Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)));
		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.getByNameList(List.of("TemperatureConsumer"));
		assertEquals(List.of(expectedElement), actual);
		verify(service).getByName("TemperatureConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithoutFilters() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of()));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());

		service.getPageByFilters(pageRequest, null, null, null, null, null, null);
		verify(systemRepo).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersBySystemNames() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());

		service.getPageByFilters(pageRequest, List.of("TemperatureProvider"), null, null, null, null, null);
		verify(systemRepo).findAllByNameIn(List.of("TemperatureProvider"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByAddressTypeNoMatch() {

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());
		when(systemAddressRepo.findAllBySystemAndAddressType(any(), any())).thenReturn(List.of());

		service.getPageByFilters(pageRequest, null, null, AddressType.IPV4, null, null, null);
		verify(systemAddressRepo).findAllBySystemAndAddressType(any(), eq(AddressType.IPV4));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByAddressTypeAndMatch() {

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(system), pageRequest, 1));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of(systemAddress));
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());
		when(systemAddressRepo.findAllBySystemAndAddressType(any(), any())).thenReturn(List.of(systemAddress));

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), null)),
				pageRequest, 1);

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.getPageByFilters(pageRequest, null, null, AddressType.IPV4, null, null, null);
		verify(systemAddressRepo).findAllBySystemAndAddressType(any(), eq(AddressType.IPV4));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByAddressesNoMatch() {

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(systemAddressRepo.findAllBySystemAndAddressIn(any(), any())).thenReturn(List.of());
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());

		service.getPageByFilters(pageRequest, null, List.of("192.168.8.8"), null, null, null, null);
		verify(systemAddressRepo).findAllBySystemAndAddressIn(any(), eq(List.of("192.168.8.8")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByAddressesAndMatch() {

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(system), pageRequest, 1));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of(systemAddress));
		when(systemAddressRepo.findAllBySystemAndAddressIn(any(), any())).thenReturn(List.of(systemAddress));
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), null)),
				pageRequest, 1);
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.getPageByFilters(pageRequest, null, List.of("192.168.8.8"), null, null, null, null);
		verify(systemAddressRepo).findAllBySystemAndAddressIn(any(), eq(List.of("192.168.8.8")));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByVersionNoMatch() {

		System system = new System("TemperatureConsumer1", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(systemRepo.findAllByVersionIn(any())).thenReturn(List.of());
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> result = service.getPageByFilters(pageRequest, null, null, null, null, List.of("2.0.0"), null);
		verify(systemRepo).findAllByVersionIn(List.of("2.0.0"));
		assertEquals(new PageImpl<>(List.of(), pageRequest, 0), result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByVersionAndMatch() {

		System system = new System("TemperatureConsumer1", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(systemRepo.findAllByVersionIn(any())).thenReturn(List.of(system));
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> result = service.getPageByFilters(pageRequest, null, null, null, null, List.of("1.0.0"), null);
		verify(systemRepo).findAllByVersionIn(List.of("1.0.0"));
		assertEquals(new PageImpl<>(List.of(), pageRequest, 0), result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByDeviceNamesNoMatch() {

		System system = new System("TemperatureConsumer1", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final Device notMatchingDevice = new Device("COMPUTER", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceSystemConnector connection = new DeviceSystemConnector(notMatchingDevice, system);

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.of(connection));
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of(connection));

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> result = service.getPageByFilters(pageRequest, null, null, null, null, null, List.of("DEVICE"));
		verify(deviceSystemConnectorRepo).findBySystem(system);
		assertEquals(new PageImpl<>(List.of(), pageRequest, 0), result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByDeviceNamesNoDevice() {

		System system = new System("TemperatureConsumer1", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(eq(List.of()), any())).thenReturn(new PageImpl<>(List.of()));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.empty());
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> result = service.getPageByFilters(pageRequest, null, null, null, null, null, List.of("DEVICE"));
		verify(deviceSystemConnectorRepo).findBySystem(system);
		assertEquals(new PageImpl<>(List.of(), pageRequest, 0), result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByDeviceNamesAndMatch() {

		System system = new System("TemperatureConsumer1", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final Device matchingDevice = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceSystemConnector connection = new DeviceSystemConnector(matchingDevice, system);

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(system), pageRequest, 1));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.of(connection));
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of(connection));
		when(deviceAddressRepo.findAllByDevice(any())).thenReturn(List.of());

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = new PageImpl<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>>(
				List.of(Triple.of(system, List.of(), Map.entry(matchingDevice, List.of()))), pageRequest, 1);

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> result = service.getPageByFilters(pageRequest, null, null, null, null, null, List.of("DEVICE"));
		verify(deviceSystemConnectorRepo).findBySystem(system);
		assertEquals(expected, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByMetadata() {

		System system = new System("TemperatureConsumer1", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of());
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of());

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", true);

		try (MockedStatic<MetadataRequirementsMatcher> metaMatcherMock = mockStatic(MetadataRequirementsMatcher.class)) {
			metaMatcherMock.when(() -> MetadataRequirementsMatcher.isMetadataMatch(any(), any())).thenReturn(false);

			final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> result = service.getPageByFilters(pageRequest, null, null, null, List.of(requirement), null, null);
			metaMatcherMock.verify(() -> MetadataRequirementsMatcher.isMetadataMatch(Map.of("indoor", false), requirement));
			assertEquals(new PageImpl<>(List.of(), pageRequest, 0), result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersCheckResult() {

		System system = new System("TemperatureConsumer1", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");

		final Device device = new Device("COMPUTER", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");

		final DeviceSystemConnector connection = new DeviceSystemConnector(device, system);

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenReturn(List.of(system));
		when(systemRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(system)));
		when(systemAddressRepo.findAllBySystemIn(any())).thenReturn(List.of(systemAddress));
		when(deviceSystemConnectorRepo.findBySystemIn(any())).thenReturn(List.of(connection));
		when(deviceAddressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", false);

		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = new PageImpl<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>>(
				List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))), pageRequest, 1);
		final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> result = service.getPageByFilters(pageRequest, null, null, null, List.of(requirement), null, null);
		assertEquals(expected, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersThrowsInternalServerError() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(systemRepo.findAll()).thenThrow(new LockedException("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getPageByFilters(pageRequest, null, null, null, null, null, List.of("DEVICE")));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByName(null));

		assertEquals("system name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByName(""));

		assertEquals("system name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameExistingEntity() {

		when(systemRepo.findByName(any())).thenReturn(Optional.empty());

		assertFalse(service.deleteByName("TemperatureConsumer"));
		verify(systemRepo, never()).delete(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameNotExistingEntity() {

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findByName(any())).thenReturn(Optional.of(system));

		assertTrue(service.deleteByName("TemperatureConsumer"));
		final InOrder inOrder = Mockito.inOrder(systemRepo);
		inOrder.verify(systemRepo).delete(system);
		inOrder.verify(systemRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameThrowsInternalServerError() {

		when(systemRepo.findByName(any())).thenThrow(new LockedException("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.deleteByName("TemperatureConsumer"));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}
}
