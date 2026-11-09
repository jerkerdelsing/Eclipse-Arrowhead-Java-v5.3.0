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
package eu.arrowhead.deviceqosevaluator.quartz.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;

@ExtendWith(MockitoExtension.class)
public class AugmentedMeasurementJobSchedulerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AugmentedMeasurementJobScheduler testedScheduler;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Mock
	private Scheduler scheduler;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStartDeviceNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> testedScheduler.start(null));

		assertEquals("device is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStartDeviceIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> testedScheduler.start(new Device()));

		assertEquals("device id is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStartJobExists() throws SchedulerException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final Device device = new Device(
				UUID.fromString(uuid),
				"localhost",
				12345,
				true,
				false);
		final JobKey jobKey = JobKey.jobKey(uuid + "_job_aug");

		when(scheduler.checkExists(jobKey)).thenReturn(true);

		assertDoesNotThrow(() -> testedScheduler.start(device));

		verify(scheduler).checkExists(jobKey);
		verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStartOk() throws SchedulerException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final Device device = new Device(
				UUID.fromString(uuid),
				"localhost",
				12345,
				true,
				false);
		final JobKey jobKey = JobKey.jobKey(uuid + "_job_aug");

		when(scheduler.checkExists(jobKey)).thenReturn(false);
		when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(new Date());

		assertDoesNotThrow(() -> testedScheduler.start(device));

		final ArgumentCaptor<JobDetail> jdCapture = ArgumentCaptor.forClass(JobDetail.class);
		final ArgumentCaptor<Trigger> triggerCapture = ArgumentCaptor.forClass(Trigger.class);

		verify(scheduler).checkExists(jobKey);
		verify(scheduler).scheduleJob(jdCapture.capture(), triggerCapture.capture());

		assertEquals(uuid + "_job_aug", jdCapture.getValue().getKey().getName());
		assertEquals(uuid + "_trigger_aug", triggerCapture.getValue().getKey().getName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStopInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> testedScheduler.stop(null));

		assertEquals("device list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStopInputListContainsNull() {
		final List<Device> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> testedScheduler.stop(list));

		assertEquals("device list contains null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStopIgnoreIdlessDevices() throws SchedulerException {
		assertDoesNotThrow(() -> testedScheduler.stop(List.of(new Device())));

		verify(scheduler, never()).unscheduleJob(any(TriggerKey.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStopOk() throws SchedulerException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final Device device = new Device(
				UUID.fromString(uuid),
				"localhost",
				12345,
				true,
				false);
		final TriggerKey triggerKey = TriggerKey.triggerKey(uuid + "_trigger_aug");
		final JobKey jobKey = JobKey.jobKey(uuid + "_job_aug");

		when(scheduler.unscheduleJob(triggerKey)).thenReturn(true);
		when(scheduler.deleteJob(jobKey)).thenReturn(true);

		assertDoesNotThrow(() -> testedScheduler.stop(List.of(device)));

		verify(scheduler).unscheduleJob(triggerKey);
		verify(scheduler).deleteJob(jobKey);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsScheduledDeviceNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> testedScheduler.isScheduled(null));

		assertEquals("device is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsScheduledDeviceIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> testedScheduler.isScheduled(new Device()));

		assertEquals("device id is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsScheduledTrue() throws SchedulerException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final Device device = new Device(
				UUID.fromString(uuid),
				"localhost",
				12345,
				true,
				false);
		final JobKey jobKey = JobKey.jobKey(uuid + "_job_aug");

		when(scheduler.checkExists(jobKey)).thenReturn(true);

		final boolean result = testedScheduler.isScheduled(device);

		assertTrue(result);

		verify(scheduler).checkExists(jobKey);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsScheduledFalse() throws SchedulerException {
		final String uuid = "2f0a6b4d-3207-4eec-8694-b44780f18182";
		final Device device = new Device(
				UUID.fromString(uuid),
				"localhost",
				12345,
				true,
				false);
		final JobKey jobKey = JobKey.jobKey(uuid + "_job_aug");

		when(scheduler.checkExists(jobKey)).thenReturn(false);

		final boolean result = testedScheduler.isScheduled(device);

		assertFalse(result);

		verify(scheduler).checkExists(jobKey);
	}
}