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
package eu.arrowhead.deviceqosevaluator.quartz.job;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.deviceqosevaluator.driver.RttMeasurementDriver;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.service.DeviceDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.StatDbService;

@ExtendWith(MockitoExtension.class)
public class RttMeasurementJobTest {

	//=================================================================================================
	// members

	@InjectMocks
	private RttMeasurementJob job;

	@Mock
	private DeviceDbService deviceDbService;

	@Mock
	private RttMeasurementDriver measurementDriver;

	@Mock
	private StatDbService statDbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetDeviceIdOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		ReflectionTestUtils.setField(job, "deviceId", null);

		assertDoesNotThrow(() -> job.setDeviceId(UUID.fromString(uuid)));
		assertEquals(uuid, ReflectionTestUtils.getField(job, "deviceId").toString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalDeviceIdNull() {
		ReflectionTestUtils.setField(job, "deviceId", null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> job.executeInternal(null));

		assertEquals("device id is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalExceptionHandled() throws IOException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		when(deviceDbService.findById(deviceId)).thenThrow(InternalServerError.class);

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver, never()).measure(anyString(), anyInt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalDeviceNotFound() throws IOException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		when(deviceDbService.findById(deviceId)).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver, never()).measure(anyString(), anyInt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalInactiveDevice() throws IOException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		final Device device = new Device(deviceId, "localhost", 12345, false, true);

		when(deviceDbService.findById(deviceId)).thenReturn(Optional.of(device));

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver, never()).measure(anyString(), anyInt());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteInternalOk1() throws IOException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		final Device device = new Device(deviceId, "localhost", null, false, false);
		final ThreadLocalRandom mockedTLR = Mockito.mock(ThreadLocalRandom.class);

		try (MockedStatic<ThreadLocalRandom> staticMock = Mockito.mockStatic(ThreadLocalRandom.class)) {
			staticMock.when(() -> ThreadLocalRandom.current()).thenReturn(mockedTLR);
			when(deviceDbService.findById(deviceId)).thenReturn(Optional.of(device));
			when(mockedTLR.nextInt(49152, 65536)).thenReturn(59473, 49152);
			when(measurementDriver.measure("localhost", 49152)).thenReturn(10L);
			when(deviceDbService.update(device)).thenReturn(device);
			doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.RTT), eq(deviceId), eq(List.of(10., 10., 10., 10., 10.)));

			assertNull(device.getRttPort());
			assertDoesNotThrow(() -> job.executeInternal(null));
			assertEquals(49152, device.getRttPort());

			verify(deviceDbService).findById(deviceId);
			verify(mockedTLR, times(2)).nextInt(49152, 65536);
			verify(measurementDriver, times(9)).measure("localhost", 49152);
			verify(deviceDbService).update(device);
			verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.RTT), eq(deviceId), eq(List.of(10., 10., 10., 10., 10.)));
			staticMock.verify(() -> ThreadLocalRandom.current(), times(2));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteInternalOk2() throws IOException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		final Device device = new Device(deviceId, "localhost", null, false, false);
		final ThreadLocalRandom mockedTLR = Mockito.mock(ThreadLocalRandom.class);

		try (MockedStatic<ThreadLocalRandom> staticMock = Mockito.mockStatic(ThreadLocalRandom.class)) {
			staticMock.when(() -> ThreadLocalRandom.current()).thenReturn(mockedTLR);
			when(deviceDbService.findById(deviceId)).thenReturn(Optional.of(device));
			when(mockedTLR.nextInt(49152, 65536)).thenReturn(59500, 49152);
			when(measurementDriver.measure("localhost", 59500)).thenReturn(null);
			when(measurementDriver.measure("localhost", 49152)).thenReturn(-1L);
			when(deviceDbService.update(device)).thenReturn(device);
			doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.RTT), eq(deviceId), eq(List.of(-1., -1., -1., -1., -1.)));

			assertNull(device.getRttPort());
			assertDoesNotThrow(() -> job.executeInternal(null));
			assertEquals(49152, device.getRttPort());

			verify(deviceDbService).findById(deviceId);
			verify(mockedTLR, times(2)).nextInt(49152, 65536);
			verify(measurementDriver).measure("localhost", 59500);
			verify(measurementDriver).measure("localhost", 49152);
			verify(deviceDbService).update(device);
			verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.RTT), eq(deviceId), eq(List.of(-1., -1., -1., -1., -1.)));
			staticMock.verify(() -> ThreadLocalRandom.current(), times(2));
		}
	}
}