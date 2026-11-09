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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.engine.StatisticsEngine;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.service.model.OidMetricModel;
import eu.arrowhead.deviceqosevaluator.service.model.SystemEvalModel;
import eu.arrowhead.deviceqosevaluator.service.validation.QualityEvaluationValidation;
import eu.arrowhead.dto.QoSDeviceDataEvaluationConfigDTO;
import eu.arrowhead.dto.QoSEvaluationFilterResponseDTO;
import eu.arrowhead.dto.QoSEvaluationRequestDTO;
import eu.arrowhead.dto.QoSEvaluationSortResponseDTO;

@ExtendWith(MockitoExtension.class)
public class QualityEvaluationServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private QualityEvaluationService service;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Mock
	private QualityEvaluationValidation validator;

	@Mock
	private StatisticsEngine statEngine;

	@Mock
	private ObjectMapper mapper;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.filter(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.filter(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterInvalidConiguration() throws JsonMappingException, JsonProcessingException {
		final QoSEvaluationRequestDTO dto = new QoSEvaluationRequestDTO(
				List.of("TestProvider", "TestProvider2"),
				Map.of("not", "aConfig"));

		when(mapper.readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class))).thenThrow(JsonProcessingException.class);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.filter(dto, "origin"));

		assertEquals("Invalid configuration payload", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(mapper).readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterDTONull() {
		when(validator.validateAndNormalizeQoSEvaluationRequest(null, null, true, "origin")).thenThrow(new InvalidParameterException("System list is empty", "origin"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.filter(null, "origin"));

		assertEquals("System list is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeQoSEvaluationRequest(null, null, true, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterConfigurationNull() {
		final QoSEvaluationRequestDTO dto = new QoSEvaluationRequestDTO(
				List.of("TestProvider", "TestProvider2"),
				null);

		when(validator.validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), null, true, "origin")).thenThrow(
				new InvalidParameterException("Configuration payload is missing", "origin"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.filter(dto, "origin"));

		assertEquals("Configuration payload is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), null, true, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testFilterOk1() throws Exception {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN"),
				List.of(1.),
				9,
				75.);

		final QoSEvaluationRequestDTO dto = new QoSEvaluationRequestDTO(
				List.of("TestProvider", "TestProvider2"),
				Map.of("metricNames", List.of("RTT_MEAN",
						"metricWeights", List.of(1),
						"timeWindow", 9,
						"threshold", 75)));

		final OidMetricModel metricModel = new OidMetricModel(OidGroup.RTT, 10000.);
		metricModel.getMetricWeight().put(OidMetric.MEAN, 1.);

		final SystemEvalModel eval1 = new SystemEvalModel("TestProvider");
		eval1.addNoStat(OidGroup.RTT);
		eval1.setScore(100.);
		final SystemEvalModel eval2 = new SystemEvalModel("TestProvider2");
		eval2.setScore(69.);
		final List<SystemEvalModel> evalList = List.of(eval1, eval2);

		final QoSEvaluationFilterResponseDTO expected = new QoSEvaluationFilterResponseDTO(
				List.of("TestProvider2"),
				List.of("TestProvider"),
				Map.of("TestProvider", List.of("RTT")));

		when(mapper.readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class))).thenReturn(config);
		when(validator.validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), config, true, "origin")).thenReturn(Pair.of(List.of("TestProvider", "TestProvider2"), config));
		when(sysInfo.getRttMeasurementTimeout()).thenReturn(10000);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(15L);
		when(statEngine.evaluate(Set.of("TestProvider", "TestProvider2"), List.of(metricModel), 15)).thenReturn(evalList);

		final QoSEvaluationFilterResponseDTO result = service.filter(dto, "origin");

		assertEquals(expected, result);

		verify(mapper).readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class));
		verify(validator).validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), config, true, "origin");
		verify(sysInfo).getRttMeasurementTimeout();
		verify(sysInfo, times(2)).getAugmentedMeasurementJobInterval();
		verify(statEngine).evaluate(Set.of("TestProvider", "TestProvider2"), List.of(metricModel), 15);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testFilterOk2() throws Exception {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("CPU_TOTAL_LOAD_MEAN"),
				List.of(1.),
				null,
				75.);

		final QoSEvaluationRequestDTO dto = new QoSEvaluationRequestDTO(
				List.of("TestProvider", "TestProvider2"),
				Map.of("metricNames", List.of("CPU_TOTAL_LOAD_MEAN",
						"metricWeights", List.of(1),
						"timeWindow", 9,
						"threshold", 75)));

		final OidMetricModel metricModel = new OidMetricModel(OidGroup.CPU_TOTAL_LOAD, null);
		metricModel.getMetricWeight().put(OidMetric.MEAN, 1.);

		final SystemEvalModel eval1 = new SystemEvalModel("TestProvider");
		eval1.addNoStat(OidGroup.CPU_TOTAL_LOAD);
		eval1.setScore(100.);
		final SystemEvalModel eval2 = new SystemEvalModel("TestProvider2");
		eval2.setScore(69.);
		final List<SystemEvalModel> evalList = List.of(eval1, eval2);

		final QoSEvaluationFilterResponseDTO expected = new QoSEvaluationFilterResponseDTO(
				List.of("TestProvider2"),
				List.of("TestProvider"),
				Map.of("TestProvider", List.of("CPU_TOTAL_LOAD")));

		when(mapper.readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class))).thenReturn(config);
		when(validator.validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), config, true, "origin")).thenReturn(Pair.of(List.of("TestProvider", "TestProvider2"), config));
		when(sysInfo.getEvaluationTimeWindow()).thenReturn(20L);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(15L);
		when(statEngine.evaluate(Set.of("TestProvider", "TestProvider2"), List.of(metricModel), 20)).thenReturn(evalList);

		final QoSEvaluationFilterResponseDTO result = service.filter(dto, "origin");

		assertEquals(expected, result);

		verify(mapper).readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class));
		verify(validator).validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), config, true, "origin");
		verify(sysInfo).getEvaluationTimeWindow();
		verify(sysInfo, never()).getRttMeasurementTimeout();
		verify(sysInfo).getAugmentedMeasurementJobInterval();
		verify(statEngine).evaluate(Set.of("TestProvider", "TestProvider2"), List.of(metricModel), 20);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSortOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.sort(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSortOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.sort(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSortInvalidConiguration() throws JsonMappingException, JsonProcessingException {
		final QoSEvaluationRequestDTO dto = new QoSEvaluationRequestDTO(
				List.of("TestProvider", "TestProvider2"),
				Map.of("not", "aConfig"));

		when(mapper.readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class))).thenThrow(JsonProcessingException.class);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.sort(dto, "origin"));

		assertEquals("Invalid configuration payload", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(mapper).readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSortDTONull() {
		when(validator.validateAndNormalizeQoSEvaluationRequest(null, null, false, "origin")).thenThrow(new InvalidParameterException("System list is empty", "origin"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.sort(null, "origin"));

		assertEquals("System list is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeQoSEvaluationRequest(null, null, false, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSortConfigurationNull() {
		final QoSEvaluationRequestDTO dto = new QoSEvaluationRequestDTO(
				List.of("TestProvider", "TestProvider2"),
				null);

		when(validator.validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), null, false, "origin")).thenThrow(
				new InvalidParameterException("Configuration payload is missing", "origin"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.sort(dto, "origin"));

		assertEquals("Configuration payload is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), null, false, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSortOk() throws Exception {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN"),
				List.of(1.),
				9,
				null);

		final QoSEvaluationRequestDTO dto = new QoSEvaluationRequestDTO(
				List.of("TestProvider", "TestProvider2"),
				Map.of("metricNames", List.of("RTT_MEAN",
						"metricWeights", List.of(1),
						"timeWindow", 9)));

		final OidMetricModel metricModel = new OidMetricModel(OidGroup.RTT, 10000.);
		metricModel.getMetricWeight().put(OidMetric.MEAN, 1.);

		final SystemEvalModel eval1 = new SystemEvalModel("TestProvider");
		eval1.addNoStat(OidGroup.RTT);
		eval1.setScore(100.);
		final SystemEvalModel eval2 = new SystemEvalModel("TestProvider2");
		eval2.setScore(69.);
		final List<SystemEvalModel> evalList = new ArrayList<>(2);
		evalList.add(eval1);
		evalList.add(eval2);

		final QoSEvaluationSortResponseDTO expected = new QoSEvaluationSortResponseDTO(
				List.of("TestProvider2", "TestProvider"),
				Map.of("TestProvider", List.of("RTT")));

		when(mapper.readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class))).thenReturn(config);
		when(validator.validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), config, false, "origin")).thenReturn(Pair.of(List.of("TestProvider", "TestProvider2"), config));
		when(sysInfo.getRttMeasurementTimeout()).thenReturn(10000);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(15L);
		when(statEngine.evaluate(Set.of("TestProvider", "TestProvider2"), List.of(metricModel), 15)).thenReturn(evalList);

		final QoSEvaluationSortResponseDTO result = service.sort(dto, "origin");

		assertEquals(expected, result);

		verify(mapper).readValue(anyString(), eq(QoSDeviceDataEvaluationConfigDTO.class));
		verify(validator).validateAndNormalizeQoSEvaluationRequest(List.of("TestProvider", "TestProvider2"), config, false, "origin");
		verify(sysInfo).getRttMeasurementTimeout();
		verify(sysInfo, times(2)).getAugmentedMeasurementJobInterval();
		verify(statEngine).evaluate(Set.of("TestProvider", "TestProvider2"), List.of(metricModel), 15);
	}
}