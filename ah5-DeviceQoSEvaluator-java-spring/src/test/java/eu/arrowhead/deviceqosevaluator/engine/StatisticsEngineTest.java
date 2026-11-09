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
package eu.arrowhead.deviceqosevaluator.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatCpuTotalLoad;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatRoundTripTime;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import eu.arrowhead.deviceqosevaluator.jpa.service.StatDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.SystemDbService;
import eu.arrowhead.deviceqosevaluator.service.model.OidMetricModel;
import eu.arrowhead.deviceqosevaluator.service.model.SystemEvalModel;

@ExtendWith(MockitoExtension.class)
public class StatisticsEngineTest {

	//=================================================================================================
	// members

	@InjectMocks
	private StatisticsEngine engine;

	@Mock
	private SystemDbService sysDbService;

	@Mock
	private StatDbService statDbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEvaluateSystemNamesNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.evaluate(null, null, 0));

		assertEquals("systemNames is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEvaluateSystemNamesContainsNull() {
		final Set<String> set = new HashSet<>(1);
		set.add(null);
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.evaluate(set, null, 0));

		assertEquals("systemNames contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEvaluateSystemNamesContainsEmptyElement() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.evaluate(Set.of(""), null, 0));

		assertEquals("systemNames contains empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEvaluateMetricsNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.evaluate(Set.of("TestSystem"), null, 0));

		assertEquals("metrics is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEvaluateMetricsContainsNull() {
		final List<OidMetricModel> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.evaluate(Set.of("TestSystem"), list, 0));

		assertEquals("metrics contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testEvaluateOk() {
		final UUID deviceUUID = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final Device device = new Device(deviceUUID, "example2.com", null, false, false);
		final System system = new System("TestSystem", device);
		final OidMetricModel metricModel = new OidMetricModel(OidGroup.CPU_TOTAL_LOAD, null);
		metricModel.getMetricWeight().put(OidMetric.MAXIMUM, 1.);
		final ZonedDateTime timestamp = Utilities.utcNow().minusSeconds(10);
		final StatCpuTotalLoad stat1 = new StatCpuTotalLoad(deviceUUID, timestamp.plusSeconds(2), 1, 10, 5.5, 5, 6);
		final StatCpuTotalLoad stat2 = new StatCpuTotalLoad(deviceUUID, timestamp.plusSeconds(5), 2, 12, 7, 6, 9);

		when(sysDbService.findByNames(Set.of("TestSystem", "TestSystem2"))).thenReturn(List.of(system));
		when(statDbService.getByDeviceIdAfterTimestamp(eq(OidGroup.CPU_TOTAL_LOAD), eq(deviceUUID), any(ZonedDateTime.class))).thenReturn(List.of(stat1, stat2));

		final List<SystemEvalModel> result = engine.evaluate(
				Set.of("TestSystem", "TestSystem2"),
				List.of(metricModel),
				5);
		assertNotNull(result);
		assertEquals(2, result.size());
		final SystemEvalModel model1 = result.get(0);
		assertEquals("TestSystem", model1.getName());
		assertEquals(12., model1.getScore());
		assertTrue(model1.getNoStat().isEmpty());
		final SystemEvalModel model2 = result.get(1);
		assertEquals("TestSystem2", model2.getName());
		assertEquals(100., model2.getScore());
		assertFalse(model2.getNoStat().isEmpty());
		assertEquals(OidGroup.CPU_TOTAL_LOAD, model2.getNoStat().iterator().next());

		verify(sysDbService).findByNames(Set.of("TestSystem", "TestSystem2"));
		verify(statDbService).getByDeviceIdAfterTimestamp(eq(OidGroup.CPU_TOTAL_LOAD), eq(deviceUUID), any(ZonedDateTime.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateDeviceMetricsNoData() {
		final UUID deviceUUID = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final OidMetricModel model = new OidMetricModel(OidGroup.CPU_TOTAL_LOAD, null);
		model.getMetricWeight().put(OidMetric.MAXIMUM, 1.);
		final ZonedDateTime timestamp = Utilities.utcNow().minusSeconds(5);
		final Set<OidGroup> noStat = new HashSet<>();

		when(statDbService.getByDeviceIdAfterTimestamp(OidGroup.CPU_TOTAL_LOAD, deviceUUID, timestamp)).thenReturn(List.of());

		assertTrue(noStat.isEmpty());

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "calculateDeviceMetrics", deviceUUID, List.of(model), timestamp, noStat);

		assertEquals(1, noStat.size());
		assertEquals(OidGroup.CPU_TOTAL_LOAD, noStat.iterator().next());
		assertEquals(100., resultObj);

		verify(statDbService).getByDeviceIdAfterTimestamp(OidGroup.CPU_TOTAL_LOAD, deviceUUID, timestamp);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateDeviceMetricsWithData() {
		final UUID deviceUUID = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final OidMetricModel model = new OidMetricModel(OidGroup.CPU_TOTAL_LOAD, null);
		model.getMetricWeight().put(OidMetric.MAXIMUM, 1.);
		final ZonedDateTime timestamp = Utilities.utcNow().minusSeconds(5);
		final Set<OidGroup> noStat = new HashSet<>();
		final StatCpuTotalLoad stat1 = new StatCpuTotalLoad(deviceUUID, timestamp.plusSeconds(2), 1, 10, 5.5, 5, 6);
		final StatCpuTotalLoad stat2 = new StatCpuTotalLoad(deviceUUID, timestamp.plusSeconds(5), 2, 12, 7, 6, 9);

		when(statDbService.getByDeviceIdAfterTimestamp(OidGroup.CPU_TOTAL_LOAD, deviceUUID, timestamp)).thenReturn(List.of(stat1, stat2));

		assertTrue(noStat.isEmpty());

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "calculateDeviceMetrics", deviceUUID, List.of(model), timestamp, noStat);

		assertTrue(noStat.isEmpty());
		assertEquals(12., resultObj);

		verify(statDbService).getByDeviceIdAfterTimestamp(OidGroup.CPU_TOTAL_LOAD, deviceUUID, timestamp);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasDataNullInput() {
		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "hasData", (List<StatEntity>) null);

		assertFalse((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasDataEmptyInput() {
		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "hasData", List.of());

		assertFalse((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasDataFalse() {
		final StatCpuTotalLoad stat = new StatCpuTotalLoad(UUID.randomUUID(), Utilities.utcNow(), -1, -1, -1, -1, -1);
		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "hasData", List.of(stat));

		assertFalse((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasDataTrue() {
		final StatCpuTotalLoad stat = new StatCpuTotalLoad(UUID.randomUUID(), Utilities.utcNow(), 1, 10, 5.5, 5, 6);
		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "hasData", List.of(stat));

		assertTrue((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateMetricScoreNullDataList() {
		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "calculateMetricScore", OidMetric.MEAN, (Double) null, 0.5, (List<StatEntity>) null, 100.);

		assertEquals(50., resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateMetricScoreEmptyDataList() {
		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "calculateMetricScore", OidMetric.MEAN, (Double) null, 0.5, List.of(), 100.);

		assertEquals(50., resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateMetricScoreCurrent() {
		final ZonedDateTime now = Utilities.utcNow();
		final StatCpuTotalLoad stat1 = new StatCpuTotalLoad(UUID.randomUUID(), now, 1, 10, 5.5, 5, 6);
		final StatCpuTotalLoad stat2 = new StatCpuTotalLoad(UUID.randomUUID(), now.minusSeconds(5), 2, 12, 7, 6, 9);
		final List<StatEntity> data = new ArrayList<>(2);
		data.add(stat1);
		data.add(stat2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "calculateMetricScore", OidMetric.CURRENT, (Double) null, 0.5, data, 100.);

		assertEquals(3., resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateMetricScoreMin() {
		final ZonedDateTime now = Utilities.utcNow();
		final StatCpuTotalLoad stat1 = new StatCpuTotalLoad(UUID.randomUUID(), now, 1, 10, 5.5, 5, 6);
		final StatCpuTotalLoad stat2 = new StatCpuTotalLoad(UUID.randomUUID(), now.minusSeconds(5), 2, 12, 7, 6, 9);
		final List<StatEntity> data = new ArrayList<>(2);
		data.add(stat1);
		data.add(stat2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "calculateMetricScore", OidMetric.MINIMUM, (Double) null, 0.5, data, 100.);

		assertEquals(0.5, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateMetricScoreMax() {
		final ZonedDateTime now = Utilities.utcNow();
		final StatCpuTotalLoad stat1 = new StatCpuTotalLoad(UUID.randomUUID(), now, 1, 10, 5.5, 5, 6);
		final StatCpuTotalLoad stat2 = new StatCpuTotalLoad(UUID.randomUUID(), now.minusSeconds(5), 2, 12, 7, 6, 9);
		final List<StatEntity> data = new ArrayList<>(2);
		data.add(stat1);
		data.add(stat2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "calculateMetricScore", OidMetric.MAXIMUM, (Double) null, 0.5, data, 100.);

		assertEquals(6., resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateMetricScoreMean() {
		final ZonedDateTime now = Utilities.utcNow();
		final StatCpuTotalLoad stat1 = new StatCpuTotalLoad(UUID.randomUUID(), now, 1, 10, 5.5, 5, 6);
		final StatCpuTotalLoad stat2 = new StatCpuTotalLoad(UUID.randomUUID(), now.minusSeconds(5), 2, 12, 7, 6, 9);
		final List<StatEntity> data = new ArrayList<>(2);
		data.add(stat1);
		data.add(stat2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "calculateMetricScore", OidMetric.MEAN, (Double) null, 0.5, data, 100.);

		assertEquals(3.125, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateMetricScoreMedian() {
		final ZonedDateTime now = Utilities.utcNow();
		final StatCpuTotalLoad stat1 = new StatCpuTotalLoad(UUID.randomUUID(), now, 1, 10, 5.5, 5, 6);
		final StatCpuTotalLoad stat2 = new StatCpuTotalLoad(UUID.randomUUID(), now.minusSeconds(5), 2, 12, 7, 6, 9);
		final List<StatEntity> data = new ArrayList<>(2);
		data.add(stat1);
		data.add(stat2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "calculateMetricScore", OidMetric.MEDIAN, (Double) null, 0.5, data, 100.);

		assertEquals(2.75, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetValueWorstStat() {
		final UUID uuid = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final StatEntity stat = new StatCpuTotalLoad(
				uuid,
				Utilities.utcNow(),
				-1.,
				-1.,
				-1.,
				-1.,
				-1.);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "getValue", OidMetric.MINIMUM, stat, null, 100.);

		assertEquals(100., resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetValueMin() {
		final UUID uuid = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final StatEntity stat = new StatCpuTotalLoad(
				uuid,
				Utilities.utcNow(),
				11.2,
				76.0,
				32.3,
				29.0,
				13.2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "getValue", OidMetric.MINIMUM, stat, null, 100.);

		assertEquals(11.2, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetValueMax() {
		final UUID uuid = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final StatEntity stat = new StatCpuTotalLoad(
				uuid,
				Utilities.utcNow(),
				11.2,
				76.0,
				32.3,
				29.0,
				13.2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "getValue", OidMetric.MAXIMUM, stat, null, 100.);

		assertEquals(76.0, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetValueMean() {
		final UUID uuid = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final StatEntity stat = new StatCpuTotalLoad(
				uuid,
				Utilities.utcNow(),
				11.2,
				76.0,
				32.3,
				29.0,
				13.2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "getValue", OidMetric.MEAN, stat, null, 100.);

		assertEquals(32.3, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetValueMedian() {
		final UUID uuid = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final StatEntity stat = new StatCpuTotalLoad(
				uuid,
				Utilities.utcNow(),
				11.2,
				76.0,
				32.3,
				29.0,
				13.2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "getValue", OidMetric.MEDIAN, stat, null, 100.);

		assertEquals(29.0, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetValueCurrent() {
		final UUID uuid = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final StatEntity stat = new StatCpuTotalLoad(
				uuid,
				Utilities.utcNow(),
				11.2,
				76.0,
				32.3,
				29.0,
				13.2);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "getValue", OidMetric.CURRENT, stat, null, 100.);

		assertEquals(13.2, resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetValueWithScaling() {
		final UUID uuid = UUID.fromString("4b4c4d76-c3e0-4dcc-82bb-ca3d06cc15fe");
		final StatEntity stat = new StatRoundTripTime(
				uuid,
				Utilities.utcNow(),
				112,
				10000,
				3234,
				3111,
				1000);

		final Object resultObj = ReflectionTestUtils.invokeMethod(engine, "getValue", OidMetric.CURRENT, stat, 10000., 100.);

		assertEquals(10., resultObj);
	}
}