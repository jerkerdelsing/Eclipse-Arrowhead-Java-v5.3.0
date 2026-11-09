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
package eu.arrowhead.deviceqosevaluator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.deviceqosevaluator.dto.DTOConverter;
import eu.arrowhead.deviceqosevaluator.engine.MeasurementEngine;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatRoundTripTime;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import eu.arrowhead.deviceqosevaluator.jpa.service.StatDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.model.StatQueryResultModel;
import eu.arrowhead.deviceqosevaluator.service.validation.DeviceQualityDataManagementValidation;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.QoSDeviceStatQueryRequestDTO;
import eu.arrowhead.dto.QoSDeviceStatQueryResponseDTO;
import eu.arrowhead.dto.QoSDeviceStatRecordDTO;

@ExtendWith(MockitoExtension.class)
public class DeviceQualityDataManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceQualityDataManagementService service;

	@Mock
	private StatDbService statDbService;

	@Mock
	private MeasurementEngine measurementEngine;

	@Mock
	private PageService pageService;

	@Mock
	private DeviceQualityDataManagementValidation validator;

	@Mock
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.query(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.query(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryInternalServerError() {
		final PageDTO pageDTO = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDTO,
				"RTT",
				null,
				null,
				List.of("MEAN"),
				null);
		final PageRequest pageRequest = PageRequest.of(0, 10, Direction.ASC, "id");

		when(validator.validateAndNormalizeQueryRequest(dto, "origin")).thenReturn(dto);
		when(pageService.getPageRequest(pageDTO, Direction.DESC, StatEntity.SORTABLE_FIELDS_BY, StatEntity.DEFAULT_SORT_FIELD, "origin")).thenReturn(pageRequest);
		when(statDbService.query(null, null, null, OidGroup.RTT, pageRequest)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.query(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeQueryRequest(dto, "origin");
		verify(pageService).getPageRequest(pageDTO, Direction.DESC, StatEntity.SORTABLE_FIELDS_BY, StatEntity.DEFAULT_SORT_FIELD, "origin");
		verify(statDbService).query(null, null, null, OidGroup.RTT, pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOk() {
		final PageDTO pageDTO = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDTO,
				"RTT",
				null,
				null,
				List.of("MEAN"),
				null);
		final PageRequest pageRequest = PageRequest.of(0, 10, Direction.ASC, "id");
		final StatEntity entity = new StatRoundTripTime();
		final StatQueryResultModel model = new StatQueryResultModel(OidGroup.RTT, entity, List.of(new System("TestSystem", null)));
		final PageImpl<StatQueryResultModel> resultPage = new PageImpl<>(List.of(model));
		final QoSDeviceStatQueryResponseDTO expected = new QoSDeviceStatQueryResponseDTO(
				List.of(new QoSDeviceStatRecordDTO(
						"RTT",
						1L,
						"2026-04-14T10:00:00Z",
						"7d026491-b287-47e5-ba97-2f2eaa39aa05",
						10.,
						10.,
						10.,
						10.,
						10.,
						List.of("TestSystem"))),
				1);

		when(validator.validateAndNormalizeQueryRequest(dto, "origin")).thenReturn(dto);
		when(pageService.getPageRequest(pageDTO, Direction.DESC, StatEntity.SORTABLE_FIELDS_BY, StatEntity.DEFAULT_SORT_FIELD, "origin")).thenReturn(pageRequest);
		when(statDbService.query(null, null, null, OidGroup.RTT, pageRequest)).thenReturn(resultPage);
		when(dtoConverter.convertStatQueryResultModelPageToDTO(resultPage, Set.of(OidMetric.MEAN))).thenReturn(expected);

		final QoSDeviceStatQueryResponseDTO result = service.query(dto, "origin");

		assertEquals(expected, result);

		verify(validator).validateAndNormalizeQueryRequest(dto, "origin");
		verify(pageService).getPageRequest(pageDTO, Direction.DESC, StatEntity.SORTABLE_FIELDS_BY, StatEntity.DEFAULT_SORT_FIELD, "origin");
		verify(statDbService).query(null, null, null, OidGroup.RTT, pageRequest);
		verify(dtoConverter).convertStatQueryResultModelPageToDTO(resultPage, Set.of(OidMetric.MEAN));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReloadOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.reload(null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReloadOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.reload(""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReloadExternalServerError() throws ArrowheadException, SchedulerException {
		when(measurementEngine.organize()).thenThrow(new ExternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> service.reload("origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(measurementEngine).organize();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReloadInternalServerError1() throws ArrowheadException, SchedulerException {
		when(measurementEngine.organize()).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.reload("origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(measurementEngine).organize();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReloadInternalServerError2() throws ArrowheadException, SchedulerException {
		when(measurementEngine.organize()).thenThrow(new SchedulerException("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.reload("origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(measurementEngine).organize();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReloadAgain() throws ArrowheadException, SchedulerException {
		when(measurementEngine.organize()).thenReturn(null);

		final String result = service.reload("origin");

		assertEquals("Reload operation is already in proggress", result);

		verify(measurementEngine).organize();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testReloadOk() throws ArrowheadException, SchedulerException {
		when(measurementEngine.organize()).thenReturn(Pair.of(10, 7));

		final String result = service.reload("origin");

		assertEquals("10 more systems found, 7 systems removed", result);

		verify(measurementEngine).organize();
	}
}