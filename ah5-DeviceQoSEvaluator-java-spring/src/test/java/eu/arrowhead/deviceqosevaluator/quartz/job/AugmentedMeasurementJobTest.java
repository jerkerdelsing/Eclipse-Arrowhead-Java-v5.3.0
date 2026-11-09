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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.deviceqosevaluator.driver.AugmentedMeasurementDriver;
import eu.arrowhead.deviceqosevaluator.dto.AugmentedMeasurementsDTO;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.service.DeviceDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.StatDbService;

@ExtendWith(MockitoExtension.class)
public class AugmentedMeasurementJobTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AugmentedMeasurementJob job;

	@Mock
	private DeviceDbService deviceDbService;

	@Mock
	private AugmentedMeasurementDriver measurementDriver;

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
	public void testExecuteInternalExceptionHandled() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		when(deviceDbService.findById(deviceId)).thenThrow(InternalServerError.class);

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver, never()).fetch(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalDeviceNotFound() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		when(deviceDbService.findById(deviceId)).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver, never()).fetch(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalInactiveDevice() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		final Device device = new Device(deviceId, "localhost", 12345, false, true);

		when(deviceDbService.findById(deviceId)).thenReturn(Optional.of(device));

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver, never()).fetch(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalAugmentedMeasuresNotSupported() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		final Device device = new Device(deviceId, "localhost", 12345, false, false);

		when(deviceDbService.findById(deviceId)).thenReturn(Optional.of(device));

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver, never()).fetch(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalExceptionDuringDataFetching() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		final Device device = new Device(deviceId, "localhost", 12345, true, false);

		when(deviceDbService.findById(deviceId)).thenReturn(Optional.of(device));
		when(measurementDriver.fetch("localhost")).thenThrow(ExternalServerError.class);
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.CPU_TOTAL_LOAD), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.MEMORY_USED), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_EGRESS_LOAD), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_INGRESS_LOAD), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver).fetch("localhost");
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.CPU_TOTAL_LOAD), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.MEMORY_USED), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_EGRESS_LOAD), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_INGRESS_LOAD), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteInternalOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final AugmentedMeasurementsDTO response = new AugmentedMeasurementsDTO();
		response.put("1.4", List.of(22.5, 22.5, 22.5));
		response.put("2.1", List.of(42.5, 42.5, 42.5));
		response.put("3.1", List.of(2.5, 2.5, 2.5));
		response.put("3.2", List.of(12.5, 12.5, 12.5));

		when(deviceDbService.findById(deviceId)).thenReturn(Optional.of(device));
		when(measurementDriver.fetch("localhost")).thenReturn(response);
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.CPU_TOTAL_LOAD), eq(deviceId), eq(List.of(22.5, 22.5, 22.5, 22.5, 22.5)));
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.MEMORY_USED), eq(deviceId), eq(List.of(42.5, 42.5, 42.5, 42.5, 42.5)));
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_EGRESS_LOAD), eq(deviceId), eq(List.of(2.5, 2.5, 2.5, 2.5, 2.5)));
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_INGRESS_LOAD), eq(deviceId), eq(List.of(12.5, 12.5, 12.5, 12.5, 12.5)));

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver).fetch("localhost");
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.CPU_TOTAL_LOAD), eq(deviceId), eq(List.of(22.5, 22.5, 22.5, 22.5, 22.5)));
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.MEMORY_USED), eq(deviceId), eq(List.of(42.5, 42.5, 42.5, 42.5, 42.5)));
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_EGRESS_LOAD), eq(deviceId), eq(List.of(2.5, 2.5, 2.5, 2.5, 2.5)));
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_INGRESS_LOAD), eq(deviceId), eq(List.of(12.5, 12.5, 12.5, 12.5, 12.5)));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteInternalOk2() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		ReflectionTestUtils.setField(job, "deviceId", deviceId);

		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final AugmentedMeasurementsDTO response = new AugmentedMeasurementsDTO();
		response.put("1.4", List.of());
		response.put("2.1", List.of(42.5, 42.5, 42.5));
		response.put("3.1", List.of(2.5, 2.5, 2.5));
		response.put("3.2", List.of(12.5, 12.5, 12.5));

		when(deviceDbService.findById(deviceId)).thenReturn(Optional.of(device));
		when(measurementDriver.fetch("localhost")).thenReturn(response);
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.CPU_TOTAL_LOAD), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.MEMORY_USED), eq(deviceId), eq(List.of(42.5, 42.5, 42.5, 42.5, 42.5)));
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_EGRESS_LOAD), eq(deviceId), eq(List.of(2.5, 2.5, 2.5, 2.5, 2.5)));
		doNothing().when(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_INGRESS_LOAD), eq(deviceId), eq(List.of(12.5, 12.5, 12.5, 12.5, 12.5)));

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(deviceDbService).findById(deviceId);
		verify(measurementDriver).fetch("localhost");
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.CPU_TOTAL_LOAD), eq(deviceId), eq(List.of(-1d, -1d, -1d, -1d, -1d)));
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.MEMORY_USED), eq(deviceId), eq(List.of(42.5, 42.5, 42.5, 42.5, 42.5)));
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_EGRESS_LOAD), eq(deviceId), eq(List.of(2.5, 2.5, 2.5, 2.5, 2.5)));
		verify(statDbService).save(any(ZonedDateTime.class), eq(OidGroup.NETWORK_INGRESS_LOAD), eq(deviceId), eq(List.of(12.5, 12.5, 12.5, 12.5, 12.5)));
	}
}