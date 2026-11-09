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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.data.util.Pair;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.service.normalization.QualityEvaluationNormalization;
import eu.arrowhead.dto.QoSDeviceDataEvaluationConfigDTO;

@ExtendWith(MockitoExtension.class)
public class QualityEvaluationValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private QualityEvaluationValidation validation;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Mock
	private QualityEvaluationNormalization normalizer;

	@Mock
	private SystemNameValidator systemNameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(null, null, false, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(null, null, false, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestListNull() {
		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(null, null, false, "origin"));

		assertEquals("System list is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestListEmpty() {
		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of(), null, false, "origin"));

		assertEquals("System list is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(list, null, false, "origin"));

		assertEquals("System list contains empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestListContainsEmptyElement() {
		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of(""), null, false, "origin"));

		assertEquals("System list contains empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestConfigNull() {
		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), null, false, "origin"));

		assertEquals("Configuration payload is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestMetricNamesListNull() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Metric names configuration is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestMetricNamesListEmpty() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of(),
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Metric names configuration is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestMetricNamesListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				list,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Metric names configuration contains empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestMetricNamesListContainsEmptyElement() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of(""),
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Metric names configuration contains empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestMetricWeightsListContainsNull() {
		final List<Double> list = new ArrayList<>(1);
		list.add(null);

		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN"),
				list,
				null,
				null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Metric weights configuration contains null element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestMetricWeightsListSizeMismatch() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MAXIMUM"),
				List.of(0.5),
				null,
				null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Metric names and weights configuration lists have different sizes", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestThresholdNeededButNotSpecified() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MAXIMUM"),
				List.of(0.4, 0.6),
				null,
				null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, true, "origin"));

		assertEquals("Threshold configuration is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestInvalidThreshold1() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MAXIMUM"),
				null,
				null,
				-1.);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, true, "origin"));

		assertEquals("Invalid threshold configuration, must be between 0 and 100", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestInvalidThreshold2() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MAXIMUM"),
				null,
				null,
				101.);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, true, "origin"));

		assertEquals("Invalid threshold configuration, must be between 0 and 100", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestInvalidTimeWindow1() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MAXIMUM"),
				null,
				-1,
				75.);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, true, "origin"));

		assertEquals("Invalid time window configuration, must be greater than 0", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestInvalidTimeWindow2() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MAXIMUM"),
				null,
				21,
				null);

		when(sysInfo.getEvaluationTimeWindow()).thenReturn(20L);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Invalid time window configuration, must be not greater than 20", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo, times(2)).getEvaluationTimeWindow();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestInvalidParameterException() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN", "CPU_TOTAL_LOAD_MAXIMUM"),
				null,
				10,
				null);

		when(sysInfo.getEvaluationTimeWindow()).thenReturn(20L);
		when(normalizer.normalizeSystemNames(List.of("Te$tSystem"))).thenReturn(List.of("Te$tSystem"));
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("Te$tSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("Te$tSystem"), config, false, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo).getEvaluationTimeWindow();
		verify(normalizer).normalizeSystemNames(List.of("Te$tSystem"));
		verify(systemNameValidator).validateSystemName("Te$tSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestInvalidMetric1() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTTMEAN", "CPUTOTALLOADMAXIMUM"),
				null,
				null,
				null);

		when(normalizer.normalizeSystemNames(List.of("TestSystem"))).thenReturn(List.of("TestSystem"));
		doNothing().when(systemNameValidator).validateSystemName("TestSystem");
		when(normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(config)).thenReturn(config);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Invalid metric name RTTMEAN", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo, never()).getEvaluationTimeWindow();
		verify(normalizer).normalizeSystemNames(List.of("TestSystem"));
		verify(systemNameValidator).validateSystemName("TestSystem");
		verify(normalizer).normalizeQoSDeviceDataEvaluationConfigDTO(config);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestInvalidMetric2() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN_"),
				null,
				null,
				null);

		when(normalizer.normalizeSystemNames(List.of("TestSystem"))).thenReturn(List.of("TestSystem"));
		doNothing().when(systemNameValidator).validateSystemName("TestSystem");
		when(normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(config)).thenReturn(config);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Invalid metric name RTT_MEAN_", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo, never()).getEvaluationTimeWindow();
		verify(normalizer).normalizeSystemNames(List.of("TestSystem"));
		verify(systemNameValidator).validateSystemName("TestSystem");
		verify(normalizer).normalizeQoSDeviceDataEvaluationConfigDTO(config);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestInvalidMetric3() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("UNKNOWN_MEAN"),
				null,
				null,
				null);

		when(normalizer.normalizeSystemNames(List.of("TestSystem"))).thenReturn(List.of("TestSystem"));
		doNothing().when(systemNameValidator).validateSystemName("TestSystem");
		when(normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(config)).thenReturn(config);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Invalid metric name UNKNOWN_MEAN", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo, never()).getEvaluationTimeWindow();
		verify(normalizer).normalizeSystemNames(List.of("TestSystem"));
		verify(systemNameValidator).validateSystemName("TestSystem");
		verify(normalizer).normalizeQoSDeviceDataEvaluationConfigDTO(config);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestInvalidMetric4() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_STAT"),
				null,
				null,
				null);

		when(normalizer.normalizeSystemNames(List.of("TestSystem"))).thenReturn(List.of("TestSystem"));
		doNothing().when(systemNameValidator).validateSystemName("TestSystem");
		when(normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(config)).thenReturn(config);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin"));

		assertEquals("Invalid metric name RTT_STAT", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo, never()).getEvaluationTimeWindow();
		verify(normalizer).normalizeSystemNames(List.of("TestSystem"));
		verify(systemNameValidator).validateSystemName("TestSystem");
		verify(normalizer).normalizeQoSDeviceDataEvaluationConfigDTO(config);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQoSEvaluationRequestOk() {
		final QoSDeviceDataEvaluationConfigDTO config = new QoSDeviceDataEvaluationConfigDTO(
				List.of("RTT_MEAN"),
				null,
				null,
				null);

		when(normalizer.normalizeSystemNames(List.of("TestSystem"))).thenReturn(List.of("TestSystem"));
		doNothing().when(systemNameValidator).validateSystemName("TestSystem");
		when(normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(config)).thenReturn(config);

		final Pair<List<String>, QoSDeviceDataEvaluationConfigDTO> result = validation.validateAndNormalizeQoSEvaluationRequest(List.of("TestSystem"), config, false, "origin");

		assertNotNull(result);
		assertEquals(List.of("TestSystem"), result.getFirst());
		assertEquals(config, result.getSecond());

		verify(sysInfo, never()).getEvaluationTimeWindow();
		verify(normalizer).normalizeSystemNames(List.of("TestSystem"));
		verify(systemNameValidator).validateSystemName("TestSystem");
		verify(normalizer).normalizeQoSDeviceDataEvaluationConfigDTO(config);
	}
}