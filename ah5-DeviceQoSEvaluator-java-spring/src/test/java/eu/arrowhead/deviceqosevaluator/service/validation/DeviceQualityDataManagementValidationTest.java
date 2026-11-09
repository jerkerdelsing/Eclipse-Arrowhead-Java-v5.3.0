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
package eu.arrowhead.deviceqosevaluator.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import eu.arrowhead.deviceqosevaluator.service.normalization.DeviceQualityDataManagementNormalization;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.QoSDeviceStatQueryRequestDTO;

@ExtendWith(MockitoExtension.class)
public class DeviceQualityDataManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceQualityDataManagementValidation validator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private SystemNameValidator sysNameValidator;

	@Mock
	private DeviceQualityDataManagementNormalization normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeQueryRequest(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeQueryRequest(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestPayloadNull() {
		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(null, "origin"));

		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestMetricGroupNull() {
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				null,
				null,
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(null, StatEntity.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(dto, "origin"));

		assertEquals("metricGroup is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, StatEntity.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestMetricGroupEmpty() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDto,
				"",
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(dto, "origin"));

		assertEquals("metricGroup is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestInvalidMetricGroup() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDto,
				"INVALID",
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto)).thenReturn(dto);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(dto, "origin"));

		assertEquals("Invalid metricGroup: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeQoSDeviceStatQueryRequestDTO(dto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestInvalidFrom() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDto,
				"RTT",
				"INVALID",
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto)).thenReturn(dto);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(dto, "origin"));

		assertEquals("Invalid 'from' time: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeQoSDeviceStatQueryRequestDTO(dto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestInvalidTo() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDto,
				"RTT",
				null,
				"INVALID",
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto)).thenReturn(dto);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(dto, "origin"));

		assertEquals("Invalid 'to' time: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeQoSDeviceStatQueryRequestDTO(dto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestInvalidPeriod() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDto,
				"RTT",
				"2026-04-09T12:00:00Z",
				"2026-04-09T10:00:00Z",
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto)).thenReturn(dto);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(dto, "origin"));

		assertEquals("Invalid period", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeQoSDeviceStatQueryRequestDTO(dto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestInvalidAggregation() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDto,
				"RTT",
				"2026-04-09T10:00:00Z",
				"2026-04-09T12:00:00Z",
				List.of("INVALID"),
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto)).thenReturn(dto);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(dto, "origin"));

		assertEquals("Invalid aggregation: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeQoSDeviceStatQueryRequestDTO(dto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestInvalidSystemName() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDto,
				"RTT",
				"2026-04-09T10:00:00Z",
				null,
				List.of("MAXIMUM"),
				List.of("Te$tSystem"));

		doNothing().when(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto)).thenReturn(dto);
		doThrow(new InvalidParameterException("test")).when(sysNameValidator).validateSystemName("Te$tSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryRequest(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeQoSDeviceStatQueryRequestDTO(dto);
		verify(sysNameValidator).validateSystemName("Te$tSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestOk1() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDto,
				"RTT",
				null,
				null,
				null,
				List.of("TestSystem"));

		doNothing().when(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto)).thenReturn(dto);
		doNothing().when(sysNameValidator).validateSystemName("TestSystem");

		final QoSDeviceStatQueryRequestDTO result = validator.validateAndNormalizeQueryRequest(dto, "origin");

		assertEquals(dto, result);

		verify(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeQoSDeviceStatQueryRequestDTO(dto);
		verify(sysNameValidator).validateSystemName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryRequestOk2() {
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				pageDto,
				"RTT",
				null,
				null,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto)).thenReturn(dto);

		final QoSDeviceStatQueryRequestDTO result = validator.validateAndNormalizeQueryRequest(dto, "origin");

		assertEquals(dto, result);

		verify(pageValidator).validatePageParameter(pageDto, StatEntity.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeQoSDeviceStatQueryRequestDTO(dto);
		verify(sysNameValidator, never()).validateSystemName(anyString());
	}
}