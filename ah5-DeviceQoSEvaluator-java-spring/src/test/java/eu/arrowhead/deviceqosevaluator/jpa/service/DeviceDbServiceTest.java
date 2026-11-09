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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.repository.DeviceRepository;

@ExtendWith(MockitoExtension.class)
public class DeviceDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceDbService service;

	@Mock
	private DeviceRepository deviceRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByIdNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.findById(null));

		assertEquals("id is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByIdInternalServerError() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(deviceRepo.findById(deviceId)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.findById(deviceId));

		assertEquals("Database operation error", ex.getMessage());

		verify(deviceRepo).findById(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByIdOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, true, false);

		when(deviceRepo.findById(deviceId)).thenReturn(Optional.of(device));

		final Optional<Device> result = service.findById(deviceId);

		assertEquals(device, result.get());

		verify(deviceRepo).findById(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByAddressesNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.findByAddresses(null));

		assertEquals("address set is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByAddressesEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.findByAddresses(Set.of()));

		assertEquals("address set is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByAddressesInputContainsNull() {
		final Set<String> set = new HashSet<>(1);
		set.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.findByAddresses(set));

		assertEquals("address set contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByAddressesInputContainsEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.findByAddresses(Set.of("")));

		assertEquals("address set contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByAddressesInternalServerError() {
		when(deviceRepo.findAllByAddressIn(Set.of("localhost"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.findByAddresses(Set.of("localhost")));

		assertEquals("Database operation error", ex.getMessage());

		verify(deviceRepo).findAllByAddressIn(Set.of("localhost"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByAddressesOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, true, false);

		when(deviceRepo.findAllByAddressIn(Set.of("localhost"))).thenReturn(List.of(device));

		final List<Device> result = service.findByAddresses(Set.of("localhost"));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(device, result.get(0));

		verify(deviceRepo).findAllByAddressIn(Set.of("localhost"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getPage(null));

		assertEquals("page is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageInternalServerError() {
		final Pageable page = PageRequest.of(0, 10);

		when(deviceRepo.findAll(page)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.getPage(page));

		assertEquals("Database operation error", ex.getMessage());

		verify(deviceRepo).findAll(page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, true, false);

		final Pageable page = PageRequest.of(0, 10);

		when(deviceRepo.findAll(page)).thenReturn(new PageImpl<>(List.of(device)));

		final Page<Device> result = service.getPage(page);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		assertEquals(device, result.getContent().get(0));

		verify(deviceRepo).findAll(page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateAddressNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.create(null, false));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateAddressEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.create("", false));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInternalServerError() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, false, false);

		try (MockedStatic<UUID> mockedStatic = Mockito.mockStatic(UUID.class)) {
			mockedStatic.when(() -> UUID.randomUUID()).thenReturn(deviceId);
			when(deviceRepo.saveAndFlush(device)).thenThrow(RuntimeException.class);

			final Throwable ex = assertThrows(
					InternalServerError.class,
					() -> service.create("localhost", false));

			assertEquals("Database operation error", ex.getMessage());

			mockedStatic.verify(() -> UUID.randomUUID());
			verify(deviceRepo).saveAndFlush(device);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, false, false);

		try (MockedStatic<UUID> mockedStatic = Mockito.mockStatic(UUID.class)) {
			mockedStatic.when(() -> UUID.randomUUID()).thenReturn(deviceId);
			when(deviceRepo.saveAndFlush(device)).thenReturn(device);

			final Device result = service.create("localhost", false);

			assertEquals(device, result);

			mockedStatic.verify(() -> UUID.randomUUID());
			verify(deviceRepo).saveAndFlush(device);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate1DeviceNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.update((Device) null));

		assertEquals("device is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate1DeviceIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.update(new Device()));

		assertEquals("device.id is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate1DeviceAddressNull() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, null, null, false, false);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.update(device));

		assertEquals("device.address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate1DeviceAddressEmpty() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "", null, false, false);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.update(device));

		assertEquals("device.address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate1InternalServerError() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, false, false);

		when(deviceRepo.saveAndFlush(device)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.update(device));

		assertEquals("Database operation error", ex.getMessage());

		verify(deviceRepo).saveAndFlush(device);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate1Ok() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, false, false);

		when(deviceRepo.saveAndFlush(device)).thenReturn(device);

		final Device result = service.update(device);

		assertEquals(device, result);

		verify(deviceRepo).saveAndFlush(device);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate2InputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.update((Iterable<Device>) null));

		assertEquals("devices is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate2InputContainsNull() {
		final List<Device> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.update(list));

		assertEquals("devices contains null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate2InternalServerError() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, false, false);

		when(deviceRepo.saveAllAndFlush(List.of(device))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.update(List.of(device)));

		assertEquals("Database operation error", ex.getMessage());

		verify(deviceRepo).saveAllAndFlush(List.of(device));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdate2Ok() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, false, false);

		when(deviceRepo.saveAllAndFlush(List.of(device))).thenReturn(List.of(device));

		assertDoesNotThrow(() -> service.update(List.of(device)));

		verify(deviceRepo).saveAllAndFlush(List.of(device));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.remove(null));

		assertEquals("devices is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveInternalServerError() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, false, false);

		doThrow(RuntimeException.class).when(deviceRepo).deleteAllInBatch((List.of(device)));

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.remove(List.of(device)));

		assertEquals("Database operation error", ex.getMessage());

		verify(deviceRepo).deleteAllInBatch(List.of(device));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", null, false, false);

		doNothing().when(deviceRepo).deleteAllInBatch((List.of(device)));

		assertDoesNotThrow(() -> service.remove(List.of(device)));

		verify(deviceRepo).deleteAllInBatch(List.of(device));
	}
}