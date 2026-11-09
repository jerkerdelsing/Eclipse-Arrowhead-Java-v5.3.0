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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatCpuTotalLoad;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatMemoryUsed;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatNetEgressLoad;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatNetIngressLoad;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatRoundTripTime;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatCpuTotalLoadRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatMemoryUsedRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatNetEgressLoadRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatNetIngressLoadRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatRoundTripTimeRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.SystemRepository;
import eu.arrowhead.deviceqosevaluator.jpa.service.model.StatQueryResultModel;

@ExtendWith(MockitoExtension.class)
public class StatDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private StatDbService service;

	@Mock
	private StatRoundTripTimeRepository rttStatRepo;

	@Mock
	private StatCpuTotalLoadRepository cpuStatRepo;

	@Mock
	private StatMemoryUsedRepository memoryStatRepo;

	@Mock
	private StatNetEgressLoadRepository netEgressStatRepo;

	@Mock
	private StatNetIngressLoadRepository netIngressStatRepo;

	@Mock
	private SystemRepository systemRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTimestampNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.save(null, null, null, null));

		assertEquals("timestamp is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveOidGroupNull() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.save(timestamp, null, null, null));

		assertEquals("oidGroup is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveDeviceIdNull() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.save(timestamp, OidGroup.CPU_TOTAL_LOAD, null, null));

		assertEquals("deviceId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveDataListNull() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.save(timestamp, OidGroup.CPU_TOTAL_LOAD, deviceId, null));

		assertEquals("data is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveDataListEmpty() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.save(timestamp, OidGroup.CPU_TOTAL_LOAD, deviceId, List.of()));

		assertEquals("data is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveDataListContainsNull() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final List<Double> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.save(timestamp, OidGroup.CPU_TOTAL_LOAD, deviceId, list));

		assertEquals("data contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveDataListInvalidSize() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.save(timestamp, OidGroup.CPU_TOTAL_LOAD, deviceId, List.of(1., 2.)));

		assertEquals("data list has invalid size", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSaveInternalServerError() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final StatCpuTotalLoad stat = new StatCpuTotalLoad(deviceId, timestamp, 1., 2., 3., 4., 5.);

		when(cpuStatRepo.saveAndFlush(stat)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.save(timestamp, OidGroup.CPU_TOTAL_LOAD, deviceId, List.of(1., 2., 3., 4., 5.)));

		assertEquals("Database operation error", ex.getMessage());

		verify(cpuStatRepo).saveAndFlush(stat);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSaveRTTOk() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final StatRoundTripTime stat = new StatRoundTripTime(deviceId, timestamp, 1., 2., 3., 4., 5.);

		when(rttStatRepo.saveAndFlush(stat)).thenReturn(stat);

		assertDoesNotThrow(() -> service.save(timestamp, OidGroup.RTT, deviceId, List.of(1., 2., 3., 4., 5.)));

		verify(rttStatRepo).saveAndFlush(stat);
		verify(cpuStatRepo, never()).saveAndFlush(any(StatCpuTotalLoad.class));
		verify(memoryStatRepo, never()).saveAndFlush(any(StatMemoryUsed.class));
		verify(netEgressStatRepo, never()).saveAndFlush(any(StatNetEgressLoad.class));
		verify(netIngressStatRepo, never()).saveAndFlush(any(StatNetIngressLoad.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSaveCPUOk() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final StatCpuTotalLoad stat = new StatCpuTotalLoad(deviceId, timestamp, 1., 2., 3., 4., 5.);

		when(cpuStatRepo.saveAndFlush(stat)).thenReturn(stat);

		assertDoesNotThrow(() -> service.save(timestamp, OidGroup.CPU_TOTAL_LOAD, deviceId, List.of(1., 2., 3., 4., 5.)));

		verify(rttStatRepo, never()).saveAndFlush(any(StatRoundTripTime.class));
		verify(cpuStatRepo).saveAndFlush(stat);
		verify(memoryStatRepo, never()).saveAndFlush(any(StatMemoryUsed.class));
		verify(netEgressStatRepo, never()).saveAndFlush(any(StatNetEgressLoad.class));
		verify(netIngressStatRepo, never()).saveAndFlush(any(StatNetIngressLoad.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSaveMemoryOk() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final StatMemoryUsed stat = new StatMemoryUsed(deviceId, timestamp, 1., 2., 3., 4., 5.);

		when(memoryStatRepo.saveAndFlush(stat)).thenReturn(stat);

		assertDoesNotThrow(() -> service.save(timestamp, OidGroup.MEMORY_USED, deviceId, List.of(1., 2., 3., 4., 5.)));

		verify(rttStatRepo, never()).saveAndFlush(any(StatRoundTripTime.class));
		verify(cpuStatRepo, never()).saveAndFlush(any(StatCpuTotalLoad.class));
		verify(memoryStatRepo).saveAndFlush(stat);
		verify(netEgressStatRepo, never()).saveAndFlush(any(StatNetEgressLoad.class));
		verify(netIngressStatRepo, never()).saveAndFlush(any(StatNetIngressLoad.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSaveNetworkEgressOk() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final StatNetEgressLoad stat = new StatNetEgressLoad(deviceId, timestamp, 1., 2., 3., 4., 5.);

		when(netEgressStatRepo.saveAndFlush(stat)).thenReturn(stat);

		assertDoesNotThrow(() -> service.save(timestamp, OidGroup.NETWORK_EGRESS_LOAD, deviceId, List.of(1., 2., 3., 4., 5.)));

		verify(rttStatRepo, never()).saveAndFlush(any(StatRoundTripTime.class));
		verify(cpuStatRepo, never()).saveAndFlush(any(StatCpuTotalLoad.class));
		verify(memoryStatRepo, never()).saveAndFlush(any(StatMemoryUsed.class));
		verify(netEgressStatRepo).saveAndFlush(stat);
		verify(netIngressStatRepo, never()).saveAndFlush(any(StatNetIngressLoad.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSaveNetworkIngressOk() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final StatNetIngressLoad stat = new StatNetIngressLoad(deviceId, timestamp, 1., 2., 3., 4., 5.);

		when(netIngressStatRepo.saveAndFlush(stat)).thenReturn(stat);

		assertDoesNotThrow(() -> service.save(timestamp, OidGroup.NETWORK_INGRESS_LOAD, deviceId, List.of(1., 2., 3., 4., 5.)));

		verify(rttStatRepo, never()).saveAndFlush(any(StatRoundTripTime.class));
		verify(cpuStatRepo, never()).saveAndFlush(any(StatCpuTotalLoad.class));
		verify(memoryStatRepo, never()).saveAndFlush(any(StatMemoryUsed.class));
		verify(netEgressStatRepo, never()).saveAndFlush(any(StatNetEgressLoad.class));
		verify(netIngressStatRepo).saveAndFlush(stat);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByDeviceIdAfterTimestampOidGroupNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByDeviceIdAfterTimestamp(null, null, null));

		assertEquals("oidGroup is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByDeviceIdAfterTimestampDeviceIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByDeviceIdAfterTimestamp(OidGroup.RTT, null, null));

		assertEquals("deviceId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByDeviceIdAfterTimestampTimestampNull() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByDeviceIdAfterTimestamp(OidGroup.RTT, deviceId, null));

		assertEquals("timestamp is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByDeviceIdAfterTimestampInternalServerError() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));

		when(rttStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.getByDeviceIdAfterTimestamp(OidGroup.RTT, deviceId, timestamp));

		assertEquals("Database operation error", ex.getMessage());

		verify(rttStatRepo).findAllByUuidAndTimestampAfter(deviceId, timestamp);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByDeviceIdAfterTimestampRTTOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime plusOneMinutes = timestamp.plusMinutes(1L);
		final StatRoundTripTime stat = new StatRoundTripTime(deviceId, plusOneMinutes, 1., 2., 3., 4., 5.);

		when(rttStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp)).thenReturn(List.of(stat));

		final List<StatEntity> result = service.getByDeviceIdAfterTimestamp(OidGroup.RTT, deviceId, timestamp);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(stat, result.get(0));

		verify(rttStatRepo).findAllByUuidAndTimestampAfter(deviceId, timestamp);
		verify(cpuStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(memoryStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(netEgressStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(netIngressStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByDeviceIdAfterTimestampCPUOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime plusOneMinutes = timestamp.plusMinutes(1L);
		final StatCpuTotalLoad stat = new StatCpuTotalLoad(deviceId, plusOneMinutes, 1., 2., 3., 4., 5.);

		when(cpuStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp)).thenReturn(List.of(stat));

		final List<StatEntity> result = service.getByDeviceIdAfterTimestamp(OidGroup.CPU_TOTAL_LOAD, deviceId, timestamp);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(stat, result.get(0));

		verify(rttStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(cpuStatRepo).findAllByUuidAndTimestampAfter(deviceId, timestamp);
		verify(memoryStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(netEgressStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(netIngressStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByDeviceIdAfterTimestampMemoryOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime plusOneMinutes = timestamp.plusMinutes(1L);
		final StatMemoryUsed stat = new StatMemoryUsed(deviceId, plusOneMinutes, 1., 2., 3., 4., 5.);

		when(memoryStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp)).thenReturn(List.of(stat));

		final List<StatEntity> result = service.getByDeviceIdAfterTimestamp(OidGroup.MEMORY_USED, deviceId, timestamp);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(stat, result.get(0));

		verify(rttStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(cpuStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(memoryStatRepo).findAllByUuidAndTimestampAfter(deviceId, timestamp);
		verify(netEgressStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(netIngressStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByDeviceIdAfterTimestampNetworkEgressOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime plusOneMinutes = timestamp.plusMinutes(1L);
		final StatNetEgressLoad stat = new StatNetEgressLoad(deviceId, plusOneMinutes, 1., 2., 3., 4., 5.);

		when(netEgressStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp)).thenReturn(List.of(stat));

		final List<StatEntity> result = service.getByDeviceIdAfterTimestamp(OidGroup.NETWORK_EGRESS_LOAD, deviceId, timestamp);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(stat, result.get(0));

		verify(rttStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(cpuStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(memoryStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(netEgressStatRepo).findAllByUuidAndTimestampAfter(deviceId, timestamp);
		verify(netIngressStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByDeviceIdAfterTimestampNetworkIngressOk() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime plusOneMinutes = timestamp.plusMinutes(1L);
		final StatNetIngressLoad stat = new StatNetIngressLoad(deviceId, plusOneMinutes, 1., 2., 3., 4., 5.);

		when(netIngressStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp)).thenReturn(List.of(stat));

		final List<StatEntity> result = service.getByDeviceIdAfterTimestamp(OidGroup.NETWORK_INGRESS_LOAD, deviceId, timestamp);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(stat, result.get(0));

		verify(rttStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(cpuStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(memoryStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(netEgressStatRepo, never()).findAllByUuidAndTimestampAfter(any(UUID.class), any(ZonedDateTime.class));
		verify(netIngressStatRepo).findAllByUuidAndTimestampAfter(deviceId, timestamp);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyDeviceIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.hasAny(null));

		assertEquals("deviceId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyInternalServerError() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(rttStatRepo.existsByUuid(deviceId)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.hasAny(deviceId));

		assertEquals("Database operation error", ex.getMessage());

		verify(rttStatRepo).existsByUuid(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyTrue1() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(rttStatRepo.existsByUuid(deviceId)).thenReturn(true);

		final boolean result = service.hasAny(deviceId);

		assertTrue(result);

		verify(rttStatRepo).existsByUuid(deviceId);
		verify(cpuStatRepo, never()).existsByUuid(any(UUID.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyTrue2() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(rttStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(cpuStatRepo.existsByUuid(deviceId)).thenReturn(true);

		final boolean result = service.hasAny(deviceId);

		assertTrue(result);

		verify(rttStatRepo).existsByUuid(deviceId);
		verify(cpuStatRepo).existsByUuid(deviceId);
		verify(memoryStatRepo, never()).existsByUuid(any(UUID.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyTrue3() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(rttStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(cpuStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(memoryStatRepo.existsByUuid(deviceId)).thenReturn(true);

		final boolean result = service.hasAny(deviceId);

		assertTrue(result);

		verify(rttStatRepo).existsByUuid(deviceId);
		verify(cpuStatRepo).existsByUuid(deviceId);
		verify(memoryStatRepo).existsByUuid(deviceId);
		verify(netEgressStatRepo, never()).existsByUuid(any(UUID.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyTrue4() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(rttStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(cpuStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(memoryStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(netEgressStatRepo.existsByUuid(deviceId)).thenReturn(true);

		final boolean result = service.hasAny(deviceId);

		assertTrue(result);

		verify(rttStatRepo).existsByUuid(deviceId);
		verify(cpuStatRepo).existsByUuid(deviceId);
		verify(memoryStatRepo).existsByUuid(deviceId);
		verify(netEgressStatRepo).existsByUuid(deviceId);
		verify(netIngressStatRepo, never()).existsByUuid(any(UUID.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyTrue5() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(rttStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(cpuStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(memoryStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(netEgressStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(netIngressStatRepo.existsByUuid(deviceId)).thenReturn(true);

		final boolean result = service.hasAny(deviceId);

		assertTrue(result);

		verify(rttStatRepo).existsByUuid(deviceId);
		verify(cpuStatRepo).existsByUuid(deviceId);
		verify(memoryStatRepo).existsByUuid(deviceId);
		verify(netEgressStatRepo).existsByUuid(deviceId);
		verify(netIngressStatRepo).existsByUuid(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFalse() {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);

		when(rttStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(cpuStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(memoryStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(netEgressStatRepo.existsByUuid(deviceId)).thenReturn(false);
		when(netIngressStatRepo.existsByUuid(deviceId)).thenReturn(false);

		final boolean result = service.hasAny(deviceId);

		assertFalse(result);

		verify(rttStatRepo).existsByUuid(deviceId);
		verify(cpuStatRepo).existsByUuid(deviceId);
		verify(memoryStatRepo).existsByUuid(deviceId);
		verify(netEgressStatRepo).existsByUuid(deviceId);
		verify(netIngressStatRepo).existsByUuid(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveBeforeTimestampInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeBeforeTimestamp(null));

		assertEquals("timestamp is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveBeforeTimestampInternalServerError() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));

		doThrow(RuntimeException.class).when(rttStatRepo).deleteAllByTimestampBefore(timestamp);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.removeBeforeTimestamp(timestamp));

		assertEquals("Database operation error", ex.getMessage());

		verify(rttStatRepo).deleteAllByTimestampBefore(timestamp);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveBeforeTimestampOk() {
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));

		doNothing().when(rttStatRepo).deleteAllByTimestampBefore(timestamp);
		doNothing().when(cpuStatRepo).deleteAllByTimestampBefore(timestamp);
		doNothing().when(memoryStatRepo).deleteAllByTimestampBefore(timestamp);
		doNothing().when(netEgressStatRepo).deleteAllByTimestampBefore(timestamp);
		doNothing().when(netIngressStatRepo).deleteAllByTimestampBefore(timestamp);

		assertDoesNotThrow(() -> service.removeBeforeTimestamp(timestamp));

		verify(rttStatRepo).deleteAllByTimestampBefore(timestamp);
		verify(cpuStatRepo).deleteAllByTimestampBefore(timestamp);
		verify(cpuStatRepo).deleteAllByTimestampBefore(timestamp);
		verify(netEgressStatRepo).deleteAllByTimestampBefore(timestamp);
		verify(netIngressStatRepo).deleteAllByTimestampBefore(timestamp);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOidGroupNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.query(null, null, null, null, null));

		assertEquals("oidGroup is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPaginationNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.query(null, null, null, OidGroup.CPU_TOTAL_LOAD, null));

		assertEquals("pagination is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryInternalServerError() {
		final PageRequest pagination = PageRequest.of(0, 10);

		when(systemRepo.findAllByNameIn(List.of("TestSystem"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.query(List.of("TestSystem"), null, null, OidGroup.CPU_TOTAL_LOAD, pagination));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepo).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryRTTOKWithSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatRoundTripTime stat = new StatRoundTripTime(deviceId, statTime, 1., 2., 3., 4., 5.);
		final StatRoundTripTime stat2 = new StatRoundTripTime(deviceId, statTime.plusSeconds(30), 1., 2., 3., 4., 5.);

		when(systemRepo.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(rttStatRepo.findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat, stat2)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(List.of("TestSystem"), start, end, OidGroup.RTT, pagination);

		assertNotNull(result);
		assertEquals(2, result.getTotalElements());
		final StatQueryResultModel model1 = result.getContent().get(0);
		assertEquals(OidGroup.RTT, model1.group());
		assertEquals(stat, model1.stat());
		assertEquals(List.of(sys), model1.system());
		final StatQueryResultModel model2 = result.getContent().get(1);
		assertEquals(OidGroup.RTT, model2.group());
		assertEquals(stat, model2.stat());
		assertEquals(List.of(sys), model2.system());

		verify(systemRepo).findAllByNameIn(List.of("TestSystem"));
		verify(rttStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(cpuStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(memoryStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryRTTOKWithoutSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatRoundTripTime stat = new StatRoundTripTime(deviceId, statTime, 1., 2., 3., 4., 5.);

		when(rttStatRepo.findAllByTimestampBetween(start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(null, start, end, OidGroup.RTT, pagination);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		final StatQueryResultModel model = result.getContent().get(0);
		assertEquals(OidGroup.RTT, model.group());
		assertEquals(stat, model.stat());
		assertEquals(List.of(sys), model.system());

		verify(systemRepo, never()).findAllByNameIn(anyList());
		verify(rttStatRepo).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(cpuStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(memoryStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netEgressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netIngressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryCPUOKWithoutSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatCpuTotalLoad stat = new StatCpuTotalLoad(deviceId, statTime, 1., 2., 3., 4., 5.);

		when(cpuStatRepo.findAllByTimestampBetween(start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(null, start, end, OidGroup.CPU_TOTAL_LOAD, pagination);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		final StatQueryResultModel model = result.getContent().get(0);
		assertEquals(OidGroup.CPU_TOTAL_LOAD, model.group());
		assertEquals(stat, model.stat());
		assertEquals(List.of(sys), model.system());

		verify(systemRepo, never()).findAllByNameIn(anyList());
		verify(rttStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(cpuStatRepo).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(memoryStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netEgressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netIngressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryCPUOKWithSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatCpuTotalLoad stat = new StatCpuTotalLoad(deviceId, statTime, 1., 2., 3., 4., 5.);

		when(systemRepo.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(cpuStatRepo.findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(List.of("TestSystem"), start, end, OidGroup.CPU_TOTAL_LOAD, pagination);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		final StatQueryResultModel model = result.getContent().get(0);
		assertEquals(OidGroup.CPU_TOTAL_LOAD, model.group());
		assertEquals(stat, model.stat());
		assertEquals(List.of(sys), model.system());

		verify(systemRepo).findAllByNameIn(List.of("TestSystem"));
		verify(rttStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(cpuStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(memoryStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryMemoryOKWithSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatMemoryUsed stat = new StatMemoryUsed(deviceId, statTime, 1., 2., 3., 4., 5.);

		when(systemRepo.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(memoryStatRepo.findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(List.of("TestSystem"), start, end, OidGroup.MEMORY_USED, pagination);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		final StatQueryResultModel model = result.getContent().get(0);
		assertEquals(OidGroup.MEMORY_USED, model.group());
		assertEquals(stat, model.stat());
		assertEquals(List.of(sys), model.system());

		verify(systemRepo).findAllByNameIn(List.of("TestSystem"));
		verify(rttStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(cpuStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(memoryStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryMemoryOKWithoutSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatMemoryUsed stat = new StatMemoryUsed(deviceId, statTime, 1., 2., 3., 4., 5.);

		when(memoryStatRepo.findAllByTimestampBetween(start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(null, start, end, OidGroup.MEMORY_USED, pagination);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		final StatQueryResultModel model = result.getContent().get(0);
		assertEquals(OidGroup.MEMORY_USED, model.group());
		assertEquals(stat, model.stat());
		assertEquals(List.of(sys), model.system());

		verify(systemRepo, never()).findAllByNameIn(anyList());
		verify(rttStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(cpuStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(memoryStatRepo).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netEgressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netIngressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryNetworkEgressOKWithoutSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatNetEgressLoad stat = new StatNetEgressLoad(deviceId, statTime, 1., 2., 3., 4., 5.);

		when(netEgressStatRepo.findAllByTimestampBetween(start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(null, start, end, OidGroup.NETWORK_EGRESS_LOAD, pagination);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		final StatQueryResultModel model = result.getContent().get(0);
		assertEquals(OidGroup.NETWORK_EGRESS_LOAD, model.group());
		assertEquals(stat, model.stat());
		assertEquals(List.of(sys), model.system());

		verify(systemRepo, never()).findAllByNameIn(anyList());
		verify(rttStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(cpuStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(memoryStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netEgressStatRepo).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netIngressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryNetworkEgressOKWithSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatNetEgressLoad stat = new StatNetEgressLoad(deviceId, statTime, 1., 2., 3., 4., 5.);

		when(systemRepo.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(netEgressStatRepo.findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(List.of("TestSystem"), start, end, OidGroup.NETWORK_EGRESS_LOAD, pagination);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		final StatQueryResultModel model = result.getContent().get(0);
		assertEquals(OidGroup.NETWORK_EGRESS_LOAD, model.group());
		assertEquals(stat, model.stat());
		assertEquals(List.of(sys), model.system());

		verify(systemRepo).findAllByNameIn(List.of("TestSystem"));
		verify(rttStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(cpuStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(memoryStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryNetworkIngressOKWithSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatNetIngressLoad stat = new StatNetIngressLoad(deviceId, statTime, 1., 2., 3., 4., 5.);

		when(systemRepo.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(netIngressStatRepo.findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(List.of("TestSystem"), start, end, OidGroup.NETWORK_INGRESS_LOAD, pagination);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		final StatQueryResultModel model = result.getContent().get(0);
		assertEquals(OidGroup.NETWORK_INGRESS_LOAD, model.group());
		assertEquals(stat, model.stat());
		assertEquals(List.of(sys), model.system());

		verify(systemRepo).findAllByNameIn(List.of("TestSystem"));
		verify(rttStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(cpuStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(memoryStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo).findAllByUuidInAndTimestampBetween(Set.of(deviceId), start, end, pagination);
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryNetworkIngressOKWithoutSystemNames() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final ZonedDateTime start = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime end = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime statTime = ZonedDateTime.of(2026, 4, 20, 12, 11, 10, 0, ZoneId.of(Constants.UTC));
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final UUID deviceId = UUID.fromString(uuid);
		final Device device = new Device(deviceId, "localhost", 12345, true, false);
		final System sys = new System("TestSystem", device);

		final StatNetIngressLoad stat = new StatNetIngressLoad(deviceId, statTime, 1., 2., 3., 4., 5.);

		when(netIngressStatRepo.findAllByTimestampBetween(start, end, pagination)).thenReturn(new PageImpl<>(List.of(stat)));
		when(systemRepo.findAllByDevice_Id(deviceId)).thenReturn(List.of(sys));

		final Page<StatQueryResultModel> result = service.query(null, start, end, OidGroup.NETWORK_INGRESS_LOAD, pagination);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		final StatQueryResultModel model = result.getContent().get(0);
		assertEquals(OidGroup.NETWORK_INGRESS_LOAD, model.group());
		assertEquals(stat, model.stat());
		assertEquals(List.of(sys), model.system());

		verify(systemRepo, never()).findAllByNameIn(anyList());
		verify(rttStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(rttStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(cpuStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(cpuStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(memoryStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(memoryStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netEgressStatRepo, never()).findAllByTimestampBetween(start, end, pagination);
		verify(netEgressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(netIngressStatRepo).findAllByTimestampBetween(start, end, pagination);
		verify(netIngressStatRepo, never()).findAllByUuidInAndTimestampBetween(anySet(), eq(start), eq(end), eq(pagination));
		verify(systemRepo).findAllByDevice_Id(deviceId);
	}
}