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
package eu.arrowhead.deviceqosevaluator.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatCpuTotalLoad;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import eu.arrowhead.deviceqosevaluator.jpa.service.model.StatQueryResultModel;
import eu.arrowhead.dto.QoSDeviceStatQueryResponseDTO;
import eu.arrowhead.dto.QoSDeviceStatRecordDTO;

public class DTOConverterTest {

	//=================================================================================================
	// members

	private DTOConverter converter = new DTOConverter();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertStatQueryResultModelPageToDTOPageNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertStatQueryResultModelPageToDTO(null, null));

		assertEquals("page is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertStatQueryResultModelPageToDTOMetricsNeededNull() {
		final UUID deviceUUID = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device = new Device(deviceUUID, "example2.com", null, true, false);
		final System system = new System("TestSystem", device);
		final StatEntity entity = new StatCpuTotalLoad(deviceUUID, Utilities.utcNow(), 1, 10, 5.5, 5, 6);
		final StatQueryResultModel model = new StatQueryResultModel(OidGroup.CPU_TOTAL_LOAD, entity, List.of(system));
		final Page<StatQueryResultModel> page = new PageImpl<>(List.of(model));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertStatQueryResultModelPageToDTO(page, null));

		assertEquals("metricsNeeded is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertStatQueryResultModelPageToDTOOk1() {
		final UUID deviceUUID = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device = new Device(deviceUUID, "example2.com", null, true, false);
		final System system = new System("TestSystem", device);
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 5, 8, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final StatEntity entity = new StatCpuTotalLoad(deviceUUID, timestamp, 1, 10, 5.5, 5, 6);
		entity.setId(1L);
		final StatQueryResultModel model = new StatQueryResultModel(OidGroup.CPU_TOTAL_LOAD, entity, List.of(system));
		final Page<StatQueryResultModel> page = new PageImpl<>(List.of(model));

		final QoSDeviceStatQueryResponseDTO result = converter.convertStatQueryResultModelPageToDTO(page, Set.of(OidMetric.MINIMUM, OidMetric.MAXIMUM));

		assertNotNull(result);
		assertEquals(1, result.count());
		final QoSDeviceStatRecordDTO record = result.records().get(0);
		assertEquals("CPU_TOTAL_LOAD", record.metricGroup());
		assertEquals(1L, record.id());
		assertEquals("2026-05-08T10:00:00Z", record.timestamp());
		assertEquals("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe", record.uuid());
		assertEquals(1., record.minimum());
		assertEquals(10., record.maximum());
		assertNull(record.mean());
		assertNull(record.median());
		assertNull(record.current());
		assertEquals(List.of("TestSystem"), record.systems());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertStatQueryResultModelPageToDTOOk2() {
		final UUID deviceUUID = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device = new Device(deviceUUID, "example2.com", null, true, false);
		final System system = new System("TestSystem", device);
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 5, 8, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final StatEntity entity = new StatCpuTotalLoad(deviceUUID, timestamp, 1, 10, 5.5, 5, 6);
		entity.setId(1L);
		final StatQueryResultModel model = new StatQueryResultModel(OidGroup.CPU_TOTAL_LOAD, entity, List.of(system));
		final Page<StatQueryResultModel> page = new PageImpl<>(List.of(model));

		final QoSDeviceStatQueryResponseDTO result = converter.convertStatQueryResultModelPageToDTO(page, Set.of(OidMetric.MEAN, OidMetric.MEDIAN, OidMetric.CURRENT));

		assertNotNull(result);
		assertEquals(1, result.count());
		final QoSDeviceStatRecordDTO record = result.records().get(0);
		assertEquals("CPU_TOTAL_LOAD", record.metricGroup());
		assertEquals(1L, record.id());
		assertEquals("2026-05-08T10:00:00Z", record.timestamp());
		assertEquals("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe", record.uuid());
		assertNull(record.minimum());
		assertNull(record.maximum());
		assertEquals(5.5, record.mean());
		assertEquals(5., record.median());
		assertEquals(6., record.current());
		assertEquals(List.of("TestSystem"), record.systems());
	}
}