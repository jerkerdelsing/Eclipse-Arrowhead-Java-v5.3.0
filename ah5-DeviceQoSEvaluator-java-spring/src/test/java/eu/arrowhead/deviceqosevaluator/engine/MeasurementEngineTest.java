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
package eu.arrowhead.deviceqosevaluator.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.springframework.data.util.Pair;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.deviceqosevaluator.engine.SystemDeviceMap.Address;
import eu.arrowhead.deviceqosevaluator.engine.SystemDeviceMap.Bool;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.service.DeviceDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.SystemDbService;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.AugmentedMeasurementJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.RttMeasurementJobScheduler;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;

@ExtendWith(MockitoExtension.class)
public class MeasurementEngineTest {

	//=================================================================================================
	// members

	@InjectMocks
	private MeasurementEngine engine;

	@Mock
	private ArrowheadHttpService ahHttpService;

	@Mock
	private DeviceDbService deviceDbService;

	@Mock
	private SystemDbService systemDbService;

	@Mock
	private RttMeasurementJobScheduler rttMeasurementJobScheduler;

	@Mock
	private AugmentedMeasurementJobScheduler augmentedMeasurementJobScheduler;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOrganizeAlreadyWorking() throws ArrowheadException, SchedulerException {
		ReflectionTestUtils.setField(engine, "working", true);

		final Pair<Integer, Integer> result = engine.organize();

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testOrganizeOk() throws ArrowheadException, SchedulerException {
		ReflectionTestUtils.setField(engine, "working", false);
		final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(1);
		queryParams.put(Constants.VERBOSE, List.of(String.valueOf(true)));
		final SystemQueryRequestDTO sysRequest1 = new SystemQueryRequestDTO(new PageDTO(null, null, null, null), null, null, null, null, null, null);
		final AddressDTO address1 = new AddressDTO("IPV4", "10.0.0.1");
		final SystemResponseDTO sysResponse1 = new SystemResponseDTO("TestSystem", null, "1.0.0", List.of(address1), null, null, null);
		final AddressDTO address2 = new AddressDTO("IPV4", "10.0.0.2");
		final SystemResponseDTO sysResponse2 = new SystemResponseDTO("TestSystem2", null, "1.0.0", List.of(address2), null, null, null);
		final SystemQueryRequestDTO sysRequest2 = new SystemQueryRequestDTO(new PageDTO(1, 2, null, null), null, null, null, null, null, null);
		final AddressDTO address3 = new AddressDTO("IPV4", "10.0.0.1");
		final SystemResponseDTO sysResponse3 = new SystemResponseDTO("TestSystem3", null, "1.0.0", List.of(address3), null, null, null);
		final UUID deviceUuid1 = UUID.fromString("82b6d4db-71e4-4db0-a546-8d3250525570");
		final Device device1 = new Device(deviceUuid1, "10.0.0.1", null, false, false);
		final UUID deviceUuid2 = UUID.fromString("1fa30526-a3c5-45fb-aced-98bda87c70de");
		final Device device2 = new Device(deviceUuid2, "10.0.0.2", null, false, false);
		final System system1 = new System("TestSystem", device1);
		final System system2 = new System("TestSystem2", device2);
		final System system3 = new System("TestSystem3", device1);

		when(ahHttpService.consumeService(
				"serviceRegistryManagement",
				"system-query",
				SystemListResponseDTO.class,
				sysRequest1,
				queryParams)).thenReturn(new SystemListResponseDTO(List.of(sysResponse1, sysResponse2), 3));
		when(ahHttpService.consumeService(
				"serviceRegistryManagement",
				"system-query",
				SystemListResponseDTO.class,
				sysRequest2,
				queryParams)).thenReturn(new SystemListResponseDTO(List.of(sysResponse3), 3));
		when(deviceDbService.findByAddresses(Set.of("10.0.0.1"))).thenReturn(List.of());
		when(deviceDbService.create("10.0.0.1", false)).thenReturn(device1);
		doNothing().when(rttMeasurementJobScheduler).start(device1);
		when(systemDbService.findByDeviceId(deviceUuid1)).thenReturn(List.of());
		when(systemDbService.findByNames(Set.of("TestSystem", "TestSystem3"))).thenReturn(List.of());
		doNothing().when(systemDbService).save(List.of(system1, system3));
		when(deviceDbService.findByAddresses(Set.of("10.0.0.2"))).thenReturn(List.of());
		when(deviceDbService.create("10.0.0.2", false)).thenReturn(device2);
		doNothing().when(rttMeasurementJobScheduler).start(device2);
		when(systemDbService.findByDeviceId(deviceUuid2)).thenReturn(List.of());
		when(systemDbService.findByNames(Set.of("TestSystem2"))).thenReturn(List.of());
		doNothing().when(systemDbService).save(List.of(system2));
		when(systemDbService.deleteSystemsWithoutDevice()).thenReturn(1);

		final Pair<Integer, Integer> result = engine.organize();

		assertNotNull(result);
		assertEquals(3, result.getFirst());
		assertEquals(1, result.getSecond());

		verify(ahHttpService).consumeService(
				"serviceRegistryManagement",
				"system-query",
				SystemListResponseDTO.class,
				sysRequest1,
				queryParams);
		verify(ahHttpService).consumeService(
				"serviceRegistryManagement",
				"system-query",
				SystemListResponseDTO.class,
				sysRequest2,
				queryParams);
		verify(deviceDbService).findByAddresses(Set.of("10.0.0.1"));
		verify(deviceDbService).create("10.0.0.1", false);
		verify(rttMeasurementJobScheduler).start(device1);
		verify(systemDbService).findByDeviceId(deviceUuid1);
		verify(systemDbService).findByNames(Set.of("TestSystem", "TestSystem3"));
		verify(systemDbService).save(List.of(system1, system3));
		verify(deviceDbService).findByAddresses(Set.of("10.0.0.2"));
		verify(deviceDbService).create("10.0.0.2", false);
		verify(rttMeasurementJobScheduler).start(device2);
		verify(systemDbService).findByDeviceId(deviceUuid2);
		verify(systemDbService).findByNames(Set.of("TestSystem2"));
		verify(systemDbService).save(List.of(system2));
		verify(systemDbService).deleteSystemsWithoutDevice();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testArrangeDatabaseAndMeasurements1() throws SchedulerException {
		final SystemDeviceMap map = new SystemDeviceMap();
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		devices.put(0, Triple.of(
				Set.of(new Address("", AddressType.HOSTNAME, false)),
				Set.of("TestSystem"),
				new Bool()));
		ReflectionTestUtils.setField(map, "devices", devices);

		when(deviceDbService.findByAddresses(Set.of(""))).thenReturn(List.of());

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "arrangeDatabaseAndMeasurements", map);

		assertEquals(0, resultObj);

		verify(deviceDbService).findByAddresses(Set.of(""));
		verify(deviceDbService, never()).create("", false);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testArrangeDatabaseAndMeasurements2() throws SchedulerException {
		final SystemDeviceMap map = new SystemDeviceMap();
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		devices.put(0, Triple.of(
				Set.of(new Address("10.0.0.1", AddressType.IPV4, false)),
				Set.of("TestSystem"),
				new Bool()));
		ReflectionTestUtils.setField(map, "devices", devices);
		final UUID deviceId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device = new Device(deviceId, "10.0.0.1", null, false, false);
		final System system = new System("TestSystem", device);

		when(deviceDbService.findByAddresses(Set.of("10.0.0.1"))).thenReturn(List.of());
		when(deviceDbService.create("10.0.0.1", false)).thenReturn(device);
		doNothing().when(rttMeasurementJobScheduler).start(device);
		when(systemDbService.findByDeviceId(deviceId)).thenReturn(List.of());
		when(systemDbService.findByNames(Set.of("TestSystem"))).thenReturn(List.of());
		doNothing().when(systemDbService).save(List.of(system));

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "arrangeDatabaseAndMeasurements", map);

		assertEquals(1, resultObj);

		verify(deviceDbService).findByAddresses(Set.of("10.0.0.1"));
		verify(deviceDbService).create("10.0.0.1", false);
		verify(rttMeasurementJobScheduler).start(device);
		verify(augmentedMeasurementJobScheduler, never()).start(device);
		verify(systemDbService).findByDeviceId(deviceId);
		verify(systemDbService).save(List.of(system));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testArrangeDatabaseAndMeasurements3() throws SchedulerException {
		final SystemDeviceMap map = new SystemDeviceMap();
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		final Bool boolTrue = new Bool();
		boolTrue.setValue(true);
		devices.put(0, Triple.of(
				Set.of(new Address("10.0.0.1", AddressType.IPV4, false)),
				Set.of("TestSystem"),
				boolTrue));
		ReflectionTestUtils.setField(map, "devices", devices);
		final UUID deviceId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device = new Device(deviceId, "10.0.0.1", null, true, false);
		final System system = new System("TestSystem", device);

		when(deviceDbService.findByAddresses(Set.of("10.0.0.1"))).thenReturn(List.of());
		when(deviceDbService.create("10.0.0.1", true)).thenReturn(device);
		doNothing().when(rttMeasurementJobScheduler).start(device);
		doNothing().when(augmentedMeasurementJobScheduler).start(device);
		when(systemDbService.findByDeviceId(deviceId)).thenReturn(List.of());
		when(systemDbService.findByNames(Set.of("TestSystem"))).thenReturn(List.of());
		doNothing().when(systemDbService).save(List.of(system));

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "arrangeDatabaseAndMeasurements", map);

		assertEquals(1, resultObj);

		verify(deviceDbService).findByAddresses(Set.of("10.0.0.1"));
		verify(deviceDbService).create("10.0.0.1", true);
		verify(rttMeasurementJobScheduler).start(device);
		verify(augmentedMeasurementJobScheduler).start(device);
		verify(systemDbService).findByDeviceId(deviceId);
		verify(systemDbService).save(List.of(system));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testArrangeDatabaseAndMeasurements4() throws SchedulerException {
		final SystemDeviceMap map = new SystemDeviceMap();
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		final Bool boolTrue = new Bool();
		boolTrue.setValue(true);
		devices.put(0, Triple.of(
				Set.of(new Address("10.0.0.1", AddressType.IPV4, false)),
				Set.of("TestSystem"),
				boolTrue));
		ReflectionTestUtils.setField(map, "devices", devices);
		final UUID deviceId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device = new Device(deviceId, "10.0.0.1", null, false, true);
		final System system = new System("TestSystem", null);
		system.setId(1L);
		final Device updatedDevice = new Device(deviceId, "10.0.0.1", null, true, false);
		final System system2 = new System("ObsoleteSystem", device);
		system2.setId(2L);

		when(deviceDbService.findByAddresses(Set.of("10.0.0.1"))).thenReturn(List.of(device));
		when(deviceDbService.update(updatedDevice)).thenReturn(updatedDevice);
		when(rttMeasurementJobScheduler.isScheduled(updatedDevice)).thenReturn(false);
		doNothing().when(rttMeasurementJobScheduler).start(updatedDevice);
		when(augmentedMeasurementJobScheduler.isScheduled(updatedDevice)).thenReturn(false);
		doNothing().when(augmentedMeasurementJobScheduler).start(updatedDevice);
		when(systemDbService.findByDeviceId(deviceId)).thenReturn(List.of(system2));
		doAnswer(invocation -> {
			final Object arg = invocation.getArgument(0);
			assertEquals(List.of(system2), arg);

			return null;
		}).when(systemDbService).save(List.of(system2));
		when(systemDbService.findByNames(Set.of("TestSystem"))).thenReturn(List.of(system));
		doNothing().when(systemDbService).save(List.of(system));

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "arrangeDatabaseAndMeasurements", map);

		assertEquals(0, resultObj);
		assertEquals(updatedDevice, system.getDevice());
		assertNull(system2.getDevice());

		verify(deviceDbService).findByAddresses(Set.of("10.0.0.1"));
		verify(deviceDbService, never()).create("10.0.0.1", true);
		verify(deviceDbService).update(updatedDevice);
		verify(rttMeasurementJobScheduler).isScheduled(updatedDevice);
		verify(rttMeasurementJobScheduler).start(updatedDevice);
		verify(augmentedMeasurementJobScheduler).isScheduled(updatedDevice);
		verify(augmentedMeasurementJobScheduler).start(updatedDevice);
		verify(systemDbService).findByDeviceId(deviceId);
		verify(systemDbService).findByNames(Set.of("TestSystem"));
		verify(systemDbService, atLeastOnce()).save(List.of(system)); // Unexpected JUnit behavior: it expects 2 saves with the same list, not with 2 different lists
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testArrangeDatabaseAndMeasurements5() throws SchedulerException {
		final SystemDeviceMap map = new SystemDeviceMap();
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		final Bool boolTrue = new Bool();
		boolTrue.setValue(true);
		devices.put(0, Triple.of(
				Set.of(new Address("10.0.0.1", AddressType.IPV4, false)),
				Set.of("TestSystem"),
				boolTrue));
		ReflectionTestUtils.setField(map, "devices", devices);
		final UUID deviceId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device = new Device(deviceId, "10.0.0.1", null, true, false);
		final System system = new System("TestSystem", device);
		system.setId(1L);

		when(deviceDbService.findByAddresses(Set.of("10.0.0.1"))).thenReturn(List.of(device));
		when(rttMeasurementJobScheduler.isScheduled(device)).thenReturn(true);
		when(augmentedMeasurementJobScheduler.isScheduled(device)).thenReturn(true);
		when(systemDbService.findByDeviceId(deviceId)).thenReturn(List.of(system));
		when(systemDbService.findByNames(Set.of("TestSystem"))).thenReturn(List.of(system));

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "arrangeDatabaseAndMeasurements", map);

		assertEquals(0, resultObj);

		verify(deviceDbService).findByAddresses(Set.of("10.0.0.1"));
		verify(deviceDbService, never()).create("10.0.0.1", true);
		verify(deviceDbService, never()).update(any(Device.class));
		verify(rttMeasurementJobScheduler).isScheduled(device);
		verify(rttMeasurementJobScheduler, never()).start(any(Device.class));
		verify(augmentedMeasurementJobScheduler).isScheduled(device);
		verify(augmentedMeasurementJobScheduler, never()).start(any(Device.class));
		verify(systemDbService).findByDeviceId(deviceId);
		verify(systemDbService).findByNames(Set.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testArrangeDatabaseAndMeasurements6() throws SchedulerException {
		final SystemDeviceMap map = new SystemDeviceMap();
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		devices.put(0, Triple.of(
				Set.of(new Address("10.0.0.1", AddressType.IPV4, false)),
				Set.of("TestSystem"),
				new Bool()));
		ReflectionTestUtils.setField(map, "devices", devices);
		final UUID deviceId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device = new Device(deviceId, "10.0.0.1", null, true, false);
		final UUID otherDeviceId = UUID.fromString("1ddffcf8-a60c-489b-860e-1c4cb13048c1");
		final Device otherDevice = new Device(otherDeviceId, "10.0.0.23", null, true, false);
		final System system = new System("TestSystem", otherDevice);
		system.setId(1L);
		final Device updatedDevice = new Device(deviceId, "10.0.0.1", null, false, false);
		final System system2 = new System("TestSystem2", otherDevice);
		system2.setId(2L);

		when(deviceDbService.findByAddresses(Set.of("10.0.0.1"))).thenReturn(List.of(device));
		when(deviceDbService.update(updatedDevice)).thenReturn(updatedDevice);

		when(rttMeasurementJobScheduler.isScheduled(device)).thenReturn(true);
		when(augmentedMeasurementJobScheduler.isScheduled(device)).thenReturn(true);
		doNothing().when(augmentedMeasurementJobScheduler).stop(List.of(updatedDevice));
		when(systemDbService.findByDeviceId(deviceId)).thenReturn(List.of());
		when(systemDbService.findByNames(Set.of("TestSystem"))).thenReturn(List.of(system2, system));
		doNothing().when(systemDbService).save(List.of(system));

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "arrangeDatabaseAndMeasurements", map);

		assertEquals(0, resultObj);
		assertEquals(updatedDevice, system.getDevice());

		verify(deviceDbService).findByAddresses(Set.of("10.0.0.1"));
		verify(deviceDbService, never()).create("10.0.0.1", true);
		verify(deviceDbService).update(updatedDevice);
		verify(rttMeasurementJobScheduler).isScheduled(updatedDevice);
		verify(rttMeasurementJobScheduler, never()).start(any(Device.class));
		verify(augmentedMeasurementJobScheduler).isScheduled(updatedDevice);
		verify(augmentedMeasurementJobScheduler).stop(List.of(updatedDevice));
		verify(systemDbService).findByDeviceId(deviceId);
		verify(systemDbService).findByNames(Set.of("TestSystem"));
		verify(systemDbService).save(List.of(system));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testArrangeDatabaseAndMeasurements7() throws SchedulerException {
		final SystemDeviceMap map = new SystemDeviceMap();
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		devices.put(0, Triple.of(
				Set.of(new Address("10.0.0.1", AddressType.IPV4, false)),
				Set.of("TestSystem"),
				new Bool()));
		ReflectionTestUtils.setField(map, "devices", devices);
		final UUID deviceId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device = new Device(deviceId, "10.0.0.1", null, true, false);
		final UUID otherDeviceId = UUID.fromString("1ddffcf8-a60c-489b-860e-1c4cb13048c1");
		final Device otherDevice = new Device(otherDeviceId, "10.0.0.23", null, true, false);
		final System system = new System("TestSystem", otherDevice);
		system.setId(1L);
		final Device updatedDevice = new Device(deviceId, "10.0.0.1", null, false, false);
		final System system2 = new System("TestSystem2", otherDevice);
		system2.setId(2L);

		when(deviceDbService.findByAddresses(Set.of("10.0.0.1"))).thenReturn(List.of(device));
		when(deviceDbService.update(updatedDevice)).thenReturn(updatedDevice);

		when(rttMeasurementJobScheduler.isScheduled(device)).thenReturn(true);
		when(augmentedMeasurementJobScheduler.isScheduled(device)).thenReturn(false);
		when(systemDbService.findByDeviceId(deviceId)).thenReturn(List.of());
		when(systemDbService.findByNames(Set.of("TestSystem"))).thenReturn(List.of(system2, system));
		doNothing().when(systemDbService).save(List.of(system));

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "arrangeDatabaseAndMeasurements", map);

		assertEquals(0, resultObj);
		assertEquals(updatedDevice, system.getDevice());

		verify(deviceDbService).findByAddresses(Set.of("10.0.0.1"));
		verify(deviceDbService, never()).create("10.0.0.1", true);
		verify(deviceDbService).update(updatedDevice);
		verify(rttMeasurementJobScheduler).isScheduled(updatedDevice);
		verify(rttMeasurementJobScheduler, never()).start(any(Device.class));
		verify(augmentedMeasurementJobScheduler).isScheduled(updatedDevice);
		verify(augmentedMeasurementJobScheduler, never()).start(any(Device.class));
		verify(augmentedMeasurementJobScheduler, never()).stop(anyList());
		verify(systemDbService).findByDeviceId(deviceId);
		verify(systemDbService).findByNames(Set.of("TestSystem"));
		verify(systemDbService).save(List.of(system));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectAddress1() {
		final Address address1 = new Address("mac", AddressType.MAC, false);
		final Address address2 = new Address("example.com", AddressType.HOSTNAME, false);
		final Address address3 = new Address("10.0.0.14", AddressType.IPV4, false);
		final Set<Address> set = new LinkedHashSet<>(3);
		set.add(address1);
		set.add(address2);
		set.add(address3);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "selectAddress", set);

		assertEquals("10.0.0.14", resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectAddress2() {
		final Address address1 = new Address("10.0.0.14", AddressType.IPV4, false);
		final Address address2 = new Address("example.com", AddressType.HOSTNAME, false);
		final Set<Address> set = new LinkedHashSet<>(3);
		set.add(address1);
		set.add(address2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "selectAddress", set);

		assertEquals("10.0.0.14", resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectAddress3() {
		final Address address1 = new Address("example.com", AddressType.HOSTNAME, true);
		final Address address2 = new Address("10.0.0.14", AddressType.IPV4, true);
		final Address address3 = new Address("10.0.0.15", AddressType.IPV4, false);

		final Set<Address> set = new LinkedHashSet<>(3);
		set.add(address1);
		set.add(address2);
		set.add(address3);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "selectAddress", set);

		assertEquals("10.0.0.14", resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSpecifyDevice1() {
		final UUID deviceUUID = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device = new Device(deviceUUID, "example.com", null, false, false);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "specifyDevice", List.of(device));

		assertEquals(device, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSpecifyDevice2() {
		final UUID deviceUUID1 = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device1 = new Device(deviceUUID1, "example.com", null, false, true);

		final UUID deviceUUID2 = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device2 = new Device(deviceUUID2, "example2.com", null, false, false);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "specifyDevice", List.of(device1, device2));

		assertEquals(device2, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSpecifyDevice3() {
		final UUID deviceUUID1 = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device1 = new Device(deviceUUID1, "example.com", null, false, false);

		final UUID deviceUUID2 = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device2 = new Device(deviceUUID2, "example2.com", null, false, true);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "specifyDevice", List.of(device1, device2));

		assertEquals(device1, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSpecifyDevice4() {
		final UUID deviceUUID1 = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device1 = new Device(deviceUUID1, "example.com", null, false, false);

		final UUID deviceUUID2 = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device2 = new Device(deviceUUID2, "example2.com", null, true, false);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "specifyDevice", List.of(device1, device2));

		assertEquals(device2, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSpecifyDevice5() {
		final UUID deviceUUID1 = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device1 = new Device(deviceUUID1, "example.com", null, false, true);

		final UUID deviceUUID2 = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device2 = new Device(deviceUUID2, "example2.com", null, true, true);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "specifyDevice", List.of(device1, device2));

		assertEquals(device2, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSpecifyDevice6() {
		final ZonedDateTime timestamp = Utilities.utcNow();
		final UUID deviceUUID1 = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device1 = new Device(deviceUUID1, "example.com", null, true, true);
		device1.setCreatedAt(timestamp);

		final UUID deviceUUID2 = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device2 = new Device(deviceUUID2, "example2.com", null, true, true);
		device2.setCreatedAt(timestamp.plusMinutes(1));

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "specifyDevice", List.of(device1, device2));

		assertEquals(device1, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSpecifyDevice7() {
		final ZonedDateTime timestamp = Utilities.utcNow();
		final UUID deviceUUID1 = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Device device1 = new Device(deviceUUID1, "example.com", null, false, false);
		device1.setCreatedAt(timestamp.plusMinutes(1));

		final UUID deviceUUID2 = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device2 = new Device(deviceUUID2, "example2.com", null, false, false);
		device2.setCreatedAt(timestamp);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "specifyDevice", List.of(device1, device2));

		assertEquals(device2, resultObj);
	}
}