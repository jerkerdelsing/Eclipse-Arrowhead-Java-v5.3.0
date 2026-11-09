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
package eu.arrowhead.deviceqosevaluator.service.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.dto.QoSDeviceDataEvaluationConfigDTO;

@ExtendWith(MockitoExtension.class)
public class QualityEvaluationNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private QualityEvaluationNormalization normalizer;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNamesNullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeSystemNames(null));

		assertEquals("names is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNamesContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeSystemNames(list));

		assertEquals("names contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNamesContainsEmptyElement() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeSystemNames(List.of("")));

		assertEquals("names contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNamesOk() {
		when(systemNameNormalizer.normalize("TestSystem ")).thenReturn("TestSystem");

		final List<String> result = normalizer.normalizeSystemNames(List.of("TestSystem "));

		assertEquals(List.of("TestSystem"), result);

		verify(systemNameNormalizer).normalize("TestSystem ");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTONullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(null));

		assertEquals("dto is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTOMetricNamesNull() {
		final QoSDeviceDataEvaluationConfigDTO dto = new QoSDeviceDataEvaluationConfigDTO(
				null,
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(dto));

		assertEquals("metric names list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTOMetricNamesEmpty() {
		final QoSDeviceDataEvaluationConfigDTO dto = new QoSDeviceDataEvaluationConfigDTO(
				List.of(),
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(dto));

		assertEquals("metric names list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTOMetricNamesContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final QoSDeviceDataEvaluationConfigDTO dto = new QoSDeviceDataEvaluationConfigDTO(
				list,
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(dto));

		assertEquals("metric names list contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTOMetricNamesContainsEmptyElement() {
		final QoSDeviceDataEvaluationConfigDTO dto = new QoSDeviceDataEvaluationConfigDTO(
				List.of(""),
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(dto));

		assertEquals("metric names list contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTOMetricWeightsContainsNull() {
		final List<Double> list = new ArrayList<>(1);
		list.add(null);

		final QoSDeviceDataEvaluationConfigDTO dto = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN"),
				list,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(dto));

		assertEquals("metric weights list contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTOOk1() {
		final List<String> names = List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MEAN");
		final QoSDeviceDataEvaluationConfigDTO dto = new QoSDeviceDataEvaluationConfigDTO(
				names,
				null,
				null,
				null);

		when(sysInfo.getEvaluationTimeWindow()).thenReturn(20L);

		final QoSDeviceDataEvaluationConfigDTO result = normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(dto);

		assertEquals(names, result.metricNames());
		assertNotNull(result.metricWeights());
		assertEquals(2, result.metricWeights().size());
		assertEquals(0.5, result.metricWeights().get(0));
		assertEquals(0.5, result.metricWeights().get(1));
		assertEquals(20, result.timeWindow());
		assertEquals(dto.threshold(), result.threshold());

		verify(sysInfo).getEvaluationTimeWindow();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTOOk2() {
		final List<String> names = List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MEAN");
		final QoSDeviceDataEvaluationConfigDTO dto = new QoSDeviceDataEvaluationConfigDTO(
				names,
				List.of(0.4, 0.4),
				9,
				null);

		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(10L);

		final QoSDeviceDataEvaluationConfigDTO result = normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(dto);

		assertEquals(names, result.metricNames());
		assertNotNull(result.metricWeights());
		assertEquals(2, result.metricWeights().size());
		assertEquals(0.5, result.metricWeights().get(0));
		assertEquals(0.5, result.metricWeights().get(1));
		assertEquals(10, result.timeWindow());
		assertEquals(dto.threshold(), result.threshold());

		verify(sysInfo, never()).getEvaluationTimeWindow();
		verify(sysInfo, times(2)).getAugmentedMeasurementJobInterval();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTOOk3() {
		final List<String> names = List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MEAN");
		final QoSDeviceDataEvaluationConfigDTO dto = new QoSDeviceDataEvaluationConfigDTO(
				names,
				List.of(0.6, 0.6),
				12,
				null);

		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(10L);

		final QoSDeviceDataEvaluationConfigDTO result = normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(dto);

		assertEquals(names, result.metricNames());
		assertNotNull(result.metricWeights());
		assertEquals(2, result.metricWeights().size());
		assertEquals(0.5, result.metricWeights().get(0));
		assertEquals(0.5, result.metricWeights().get(1));
		assertEquals(12, result.timeWindow());
		assertEquals(dto.threshold(), result.threshold());

		verify(sysInfo, never()).getEvaluationTimeWindow();
		verify(sysInfo).getAugmentedMeasurementJobInterval();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeQoSDeviceDataEvaluationConfigDTOOk4() {
		final List<String> names = List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MEAN");
		final QoSDeviceDataEvaluationConfigDTO dto = new QoSDeviceDataEvaluationConfigDTO(
				names,
				List.of(0.5, 0.5),
				12,
				null);

		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(10L);

		final QoSDeviceDataEvaluationConfigDTO result = normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(dto);

		assertEquals(names, result.metricNames());
		assertNotNull(result.metricWeights());
		assertEquals(2, result.metricWeights().size());
		assertEquals(0.5, result.metricWeights().get(0));
		assertEquals(0.5, result.metricWeights().get(1));
		assertEquals(12, result.timeWindow());
		assertEquals(dto.threshold(), result.threshold());

		verify(sysInfo, never()).getEvaluationTimeWindow();
		verify(sysInfo).getAugmentedMeasurementJobInterval();
	}
}