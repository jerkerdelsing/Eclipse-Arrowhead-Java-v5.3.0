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
package eu.arrowhead.deviceqosevaluator.init;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.service.DeviceDbService;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.AugmentedMeasurementJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.CleaningJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.MeasurementOrganizerJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.RttMeasurementJobScheduler;

@ExtendWith(MockitoExtension.class)
public class DeviceQoSEvaluatorApplicationInitListenerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceQoSEvaluatorApplicationInitListener listener;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Mock
	private MeasurementOrganizerJobScheduler measurementOrganizerJobScheduler;

	@Mock
	private RttMeasurementJobScheduler rttMeasurementJobScheduler;

	@Mock
	private AugmentedMeasurementJobScheduler augmentedMeasurementJobScheduler;

	@Mock
	private CleaningJobScheduler cleaningJobScheduler;

	@Mock
	private DeviceDbService deviceDbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitInvalidConfig1() {
		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(5L);

		final Throwable ex = assertThrows(
				ConfigurationException.class,
				() -> listener.customInit(null));

		assertEquals("Invalid configuration: 'measurement.organizer.job.interval' cannot be less than 10 sec", ex.getMessage());

		verify(sysInfo).getMeasurementOrganizerJobInterval();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitInvalidConfig2() {
		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(10L);
		when(sysInfo.getRttMeasurementJobInterval()).thenReturn(3L);

		final Throwable ex = assertThrows(
				ConfigurationException.class,
				() -> listener.customInit(null));

		assertEquals("Invalid configuration: 'rtt.measurement.job.interval' cannot be less than 5 sec", ex.getMessage());

		verify(sysInfo).getMeasurementOrganizerJobInterval();
		verify(sysInfo).getRttMeasurementJobInterval();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitInvalidConfig3() {
		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(10L);
		when(sysInfo.getRttMeasurementJobInterval()).thenReturn(5L);
		when(sysInfo.getRttMeasurementTimeout()).thenReturn(200);

		final Throwable ex = assertThrows(
				ConfigurationException.class,
				() -> listener.customInit(null));

		assertEquals("Invalid configuration: 'rtt.measurement.timeout' cannot be less than 3000 ms", ex.getMessage());

		verify(sysInfo).getMeasurementOrganizerJobInterval();
		verify(sysInfo).getRttMeasurementJobInterval();
		verify(sysInfo).getRttMeasurementTimeout();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitInvalidConfig4() {
		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(10L);
		when(sysInfo.getRttMeasurementJobInterval()).thenReturn(5L);
		when(sysInfo.getRttMeasurementTimeout()).thenReturn(6000);

		final Throwable ex = assertThrows(
				ConfigurationException.class,
				() -> listener.customInit(null));

		assertEquals("Invalid configuration: 'rtt.measurement.timeout' must be less than 'rtt.measurement.job.interval' (5000 ms)", ex.getMessage());

		verify(sysInfo).getMeasurementOrganizerJobInterval();
		verify(sysInfo, times(3)).getRttMeasurementJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementTimeout();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitInvalidConfig5() {
		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(10L);
		when(sysInfo.getRttMeasurementJobInterval()).thenReturn(5L);
		when(sysInfo.getRttMeasurementTimeout()).thenReturn(4000);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(4L);

		final Throwable ex = assertThrows(
				ConfigurationException.class,
				() -> listener.customInit(null));

		assertEquals("Invalid configuration: 'augmented.measurement.job.interval' cannot be less than 5 sec", ex.getMessage());

		verify(sysInfo).getMeasurementOrganizerJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementTimeout();
		verify(sysInfo).getAugmentedMeasurementJobInterval();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitInvalidConfig6() {
		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(10L);
		when(sysInfo.getRttMeasurementJobInterval()).thenReturn(5L);
		when(sysInfo.getRttMeasurementTimeout()).thenReturn(4000);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(6L);
		when(sysInfo.getCleaningJobInterval()).thenReturn(10L);

		final Throwable ex = assertThrows(
				ConfigurationException.class,
				() -> listener.customInit(null));

		assertEquals("Invalid configuration: 'cleaning.job.interval' cannot be less than 60 sec", ex.getMessage());

		verify(sysInfo).getMeasurementOrganizerJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementTimeout();
		verify(sysInfo).getAugmentedMeasurementJobInterval();
		verify(sysInfo).getCleaningJobInterval();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitInvalidConfig7() {
		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(10L);
		when(sysInfo.getRttMeasurementJobInterval()).thenReturn(5L);
		when(sysInfo.getRttMeasurementTimeout()).thenReturn(4000);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(6L);
		when(sysInfo.getCleaningJobInterval()).thenReturn(70L);
		when(sysInfo.getEvaluationTimeWindow()).thenReturn(125L);
		when(sysInfo.getRawMeasurementDataMaxAge()).thenReturn(2);

		final Throwable ex = assertThrows(
				ConfigurationException.class,
				() -> listener.customInit(null));

		assertEquals("Invalid configuration: 'evaluation.time.window' must be less than 'raw.measurement.data.max.age' (120 sec)", ex.getMessage());

		verify(sysInfo).getMeasurementOrganizerJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementTimeout();
		verify(sysInfo).getAugmentedMeasurementJobInterval();
		verify(sysInfo).getCleaningJobInterval();
		verify(sysInfo).getEvaluationTimeWindow();
		verify(sysInfo, times(2)).getRawMeasurementDataMaxAge();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitSchedulerException() throws SchedulerException {
		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(10L);
		when(sysInfo.getRttMeasurementJobInterval()).thenReturn(5L);
		when(sysInfo.getRttMeasurementTimeout()).thenReturn(4000);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(6L);
		when(sysInfo.getCleaningJobInterval()).thenReturn(70L);
		when(sysInfo.getEvaluationTimeWindow()).thenReturn(100L);
		when(sysInfo.getRawMeasurementDataMaxAge()).thenReturn(2);
		doThrow(new SchedulerException("test")).when(measurementOrganizerJobScheduler).start();

		final Throwable ex = assertThrows(
				ConfigurationException.class,
				() -> listener.customInit(null));

		assertEquals("test", ex.getMessage());

		verify(sysInfo).getMeasurementOrganizerJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementTimeout();
		verify(sysInfo).getAugmentedMeasurementJobInterval();
		verify(sysInfo).getCleaningJobInterval();
		verify(sysInfo).getEvaluationTimeWindow();
		verify(sysInfo).getRawMeasurementDataMaxAge();
		verify(measurementOrganizerJobScheduler).start();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitOk() throws SchedulerException {
		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(10L);
		when(sysInfo.getRttMeasurementJobInterval()).thenReturn(5L);
		when(sysInfo.getRttMeasurementTimeout()).thenReturn(4000);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(6L);
		when(sysInfo.getCleaningJobInterval()).thenReturn(70L);
		when(sysInfo.getEvaluationTimeWindow()).thenReturn(100L);
		when(sysInfo.getRawMeasurementDataMaxAge()).thenReturn(2);
		doNothing().when(measurementOrganizerJobScheduler).start();
		doNothing().when(cleaningJobScheduler).start();

		assertDoesNotThrow(() -> listener.customInit(null));

		verify(sysInfo).getMeasurementOrganizerJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementJobInterval();
		verify(sysInfo, times(2)).getRttMeasurementTimeout();
		verify(sysInfo).getAugmentedMeasurementJobInterval();
		verify(sysInfo).getCleaningJobInterval();
		verify(sysInfo).getEvaluationTimeWindow();
		verify(sysInfo).getRawMeasurementDataMaxAge();
		verify(measurementOrganizerJobScheduler).start();
		verify(cleaningJobScheduler).start();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomDestroySchedulerException() throws SchedulerException {
		doThrow(new SchedulerException("test")).when(measurementOrganizerJobScheduler).stop();

		assertDoesNotThrow(() -> listener.customDestroy());

		verify(measurementOrganizerJobScheduler).stop();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomDestroyOk() throws SchedulerException {
		final PageRequest pageRequest1 = PageRequest.of(0, 2, Direction.ASC, Device.DEFAULT_SORT_FIELD);
		final PageRequest pageRequest2 = PageRequest.of(1, 2, Direction.ASC, Device.DEFAULT_SORT_FIELD);
		final Device d1 = new Device();
		d1.setId(UUID.fromString("581fd924-d8b0-4548-8cf8-4334e9f3cba2"));
		final Device d2 = new Device();
		d2.setId(UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe"));
		final Device d3 = new Device();
		d3.setId(UUID.fromString("37afcc60-e8c6-45ea-9fce-17f281e67a56"));

		doNothing().when(measurementOrganizerJobScheduler).stop();
		doNothing().when(cleaningJobScheduler).stop();
		when(sysInfo.getMaxPageSize()).thenReturn(2);
		when(deviceDbService.getPage(pageRequest1)).thenReturn(new PageImpl<>(List.of(d1, d2), pageRequest1, 3));
		when(deviceDbService.getPage(pageRequest2)).thenReturn(new PageImpl<>(List.of(d3), pageRequest2, 3));
		doNothing().when(augmentedMeasurementJobScheduler).stop(List.of(d1, d2));
		doNothing().when(rttMeasurementJobScheduler).stop(List.of(d1, d2));
		doNothing().when(augmentedMeasurementJobScheduler).stop(List.of(d3));
		doNothing().when(rttMeasurementJobScheduler).stop(List.of(d3));

		assertDoesNotThrow(() -> listener.customDestroy());

		verify(measurementOrganizerJobScheduler).stop();
		verify(cleaningJobScheduler).stop();
		verify(sysInfo, times(2)).getMaxPageSize();
		verify(deviceDbService).getPage(pageRequest1);
		verify(deviceDbService).getPage(pageRequest2);
		verify(augmentedMeasurementJobScheduler).stop(List.of(d1, d2));
		verify(rttMeasurementJobScheduler).stop(List.of(d1, d2));
		verify(augmentedMeasurementJobScheduler).stop(List.of(d3));
		verify(rttMeasurementJobScheduler).stop(List.of(d3));
	}
}