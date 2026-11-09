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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;

@ExtendWith(MockitoExtension.class)
public class MeasurementOrganizerJobSchedulerTest {

	//=================================================================================================
	// member

	@InjectMocks
	private MeasurementOrganizerJobScheduler testedScheduler;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Mock
	private Scheduler scheduler;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStartAlreadyScheduledJob() throws SchedulerException {
		ReflectionTestUtils.setField(testedScheduler, "jobScheduled", true);

		assertDoesNotThrow(() -> testedScheduler.start());

		verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testStartOk() throws SchedulerException {
		ReflectionTestUtils.setField(testedScheduler, "jobScheduled", false);

		when(sysInfo.getMeasurementOrganizerJobInterval()).thenReturn(10000L);
		when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(new Date());
		doNothing().when(scheduler).start();

		assertDoesNotThrow(() -> testedScheduler.start());

		final ArgumentCaptor<JobDetail> jdCapture = ArgumentCaptor.forClass(JobDetail.class);
		final ArgumentCaptor<Trigger> triggerCapture = ArgumentCaptor.forClass(Trigger.class);

		verify(sysInfo).getMeasurementOrganizerJobInterval();
		verify(scheduler).scheduleJob(jdCapture.capture(), triggerCapture.capture());
		verify(scheduler).start();

		assertEquals("measurement_organizer_job", jdCapture.getValue().getKey().getName());
		assertEquals("measurement_organizer_job_trigger", triggerCapture.getValue().getKey().getName());
		assertTrue((boolean) ReflectionTestUtils.getField(testedScheduler, "jobScheduled"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStopNotScheduledJob() throws SchedulerException {
		ReflectionTestUtils.setField(testedScheduler, "jobScheduled", false);

		assertDoesNotThrow(() -> testedScheduler.stop());

		verify(scheduler, never()).unscheduleJob(any(TriggerKey.class));
		verify(scheduler, never()).deleteJob(any(JobKey.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStopOkNoTrigger() throws SchedulerException {
		ReflectionTestUtils.setField(testedScheduler, "jobScheduled", true);
		ReflectionTestUtils.setField(testedScheduler, "currentTrigger", null);
		final JobDetail jdMock = Mockito.mock(JobDetail.class);
		ReflectionTestUtils.setField(testedScheduler, "jobDetail", jdMock);

		final JobKey jKey = JobKey.jobKey("measurement_organizer_job");

		when(jdMock.getKey()).thenReturn(jKey);
		when(scheduler.deleteJob(jKey)).thenReturn(true);

		assertDoesNotThrow(() -> testedScheduler.stop());

		verify(scheduler, never()).unscheduleJob(any(TriggerKey.class));
		verify(jdMock).getKey();
		verify(scheduler).deleteJob(jKey);

		assertFalse((boolean) ReflectionTestUtils.getField(testedScheduler, "jobScheduled"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStopOkWithTrigger() throws SchedulerException {
		ReflectionTestUtils.setField(testedScheduler, "jobScheduled", true);
		final Trigger tMock = Mockito.mock(Trigger.class);
		ReflectionTestUtils.setField(testedScheduler, "currentTrigger", tMock);
		final JobDetail jdMock = Mockito.mock(JobDetail.class);
		ReflectionTestUtils.setField(testedScheduler, "jobDetail", jdMock);

		final TriggerKey tKey = TriggerKey.triggerKey("measurement_organizer_job_trigger");
		final JobKey jKey = JobKey.jobKey("measurement_organizer_job");

		when(tMock.getKey()).thenReturn(tKey);
		when(scheduler.unscheduleJob(tKey)).thenReturn(true);
		when(jdMock.getKey()).thenReturn(jKey);
		when(scheduler.deleteJob(jKey)).thenReturn(true);

		assertDoesNotThrow(() -> testedScheduler.stop());

		verify(tMock).getKey();
		verify(scheduler).unscheduleJob(tKey);
		verify(jdMock).getKey();
		verify(scheduler).deleteJob(jKey);

		assertNull(ReflectionTestUtils.getField(testedScheduler, "currentTrigger"));
		assertFalse((boolean) ReflectionTestUtils.getField(testedScheduler, "jobScheduled"));
	}
}