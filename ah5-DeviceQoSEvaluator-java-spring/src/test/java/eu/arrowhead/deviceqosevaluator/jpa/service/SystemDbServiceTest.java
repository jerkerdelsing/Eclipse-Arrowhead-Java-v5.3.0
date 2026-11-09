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
package eu.arrowhead.deviceqosevaluator.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.repository.DeviceRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.SystemRepository;

@ExtendWith(MockitoExtension.class)
public class SystemDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SystemDbService service;

	@Mock
	private SystemRepository systemRepo;

	@Mock
	private DeviceRepository deviceRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.save(null));

		assertEquals("systems is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveInputContainsNull() {
		final List<System> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.save(list));

		assertEquals("systems contains null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveInternalServerError() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		when(systemRepo.saveAllAndFlush(List.of(sys))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.save(List.of(sys)));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepo).saveAllAndFlush(List.of(sys));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		when(systemRepo.saveAllAndFlush(List.of(sys))).thenReturn(List.of(sys));

		assertDoesNotThrow(() -> service.save(List.of(sys)));

		verify(systemRepo).saveAllAndFlush(List.of(sys));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByNamesInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.findByNames(null));

		assertEquals("names is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByNamesInputContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.findByNames(list));

		assertEquals("names contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByNamesInputContainsEmptyElement() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.findByNames(List.of("")));

		assertEquals("names contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByNamesInternalServerError() {
		when(systemRepo.findAllByNameIn(List.of("TestSystem"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.findByNames(List.of("TestSystem")));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepo).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByNamesOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);
		sys.setId(1L);

		when(systemRepo.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));

		final List<System> result = service.findByNames(List.of("TestSystem"));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(sys, result.get(0));

		verify(systemRepo).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByDeviceIdInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.findByDeviceId(null));

		assertEquals("deviceId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByDeviceIdInternalServerError() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(deviceRepository.findById(deviceId)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.findByDeviceId(deviceId));

		assertEquals("Database operation error", ex.getMessage());

		verify(deviceRepository).findById(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByDeviceIdUnknownDevice() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

		final List<System> result = service.findByDeviceId(deviceId);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(deviceRepository).findById(deviceId);
		verify(systemRepo, never()).findAllByDevice(any(Device.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByDeviceIdOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);
		sys.setId(1L);

		when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
		when(systemRepo.findAllByDevice(device)).thenReturn(List.of(sys));

		final List<System> result = service.findByDeviceId(deviceId);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(sys, result.get(0));

		verify(deviceRepository).findById(deviceId);
		verify(systemRepo).findAllByDevice(device);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteSystemsWithoutDeviceInternalServerError() {
		when(systemRepo.findAllByDeviceIsNull()).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.deleteSystemsWithoutDevice());

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepo).findAllByDeviceIsNull();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteSystemsWithoutDeviceOk() {
		final System sys = new System("TestSystem", null);
		sys.setId(1L);

		when(systemRepo.findAllByDeviceIsNull()).thenReturn(List.of(sys));
		doNothing().when(systemRepo).deleteAll(List.of(sys));
		doNothing().when(systemRepo).flush();

		final int result = service.deleteSystemsWithoutDevice();

		assertEquals(1, result);

		verify(systemRepo).findAllByDeviceIsNull();
		verify(systemRepo).deleteAll(List.of(sys));
		verify(systemRepo).flush();
	}
}