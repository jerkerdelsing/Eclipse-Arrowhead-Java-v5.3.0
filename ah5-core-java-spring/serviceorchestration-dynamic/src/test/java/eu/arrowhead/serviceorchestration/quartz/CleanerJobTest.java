/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
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
package eu.arrowhead.serviceorchestration.quartz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationLockDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;

@ExtendWith(MockitoExtension.class)
public class CleanerJobTest {

	//=================================================================================================
	// members

	@InjectMocks
	private CleanerJob job;

	@Mock
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Mock
	private SubscriptionDbService subscriptionDbService;

	@Mock
	private OrchestrationLockDbService orchestrationLockDbService;

	@Mock
	private OrchestrationJobDbService orchestrationJobDbService;

	@Captor
	private ArgumentCaptor<ZonedDateTime> dateTimeCaptor;

	@Captor
	private ArgumentCaptor<List<OrchestrationJobStatus>> orchJobStatusListCaptor;

	@Captor
	private ArgumentCaptor<List<UUID>> uuidCollectionCaptor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecute() {
		final List<OrchestrationJob> orchJobs = List.of(new OrchestrationJob());
		orchJobs.get(0).setId(UUID.randomUUID());
		orchJobs.get(0).setFinishedAt(Utilities.utcNow().minusDays(2));

		when(orchestrationJobDbService.getAllByStatusIn(anyList())).thenReturn(orchJobs);
		when(sysInfo.getOrchestrationHistoryMaxAge()).thenReturn(1);

		assertDoesNotThrow(() -> job.execute(new DummyJobExecutionContext()));

		verify(subscriptionDbService).deleteInBatchByExpiredBefore(dateTimeCaptor.capture());
		verify(orchestrationLockDbService).deleteInBatchByExpiredBefore(dateTimeCaptor.capture());
		verify(sysInfo).getOrchestrationHistoryMaxAge();
		verify(orchestrationJobDbService).getAllByStatusIn(orchJobStatusListCaptor.capture());
		verify(orchestrationJobDbService).deleteInBatch(uuidCollectionCaptor.capture());

		final ZonedDateTime almostNow = Utilities.utcNow().plusSeconds(1);
		assertTrue(dateTimeCaptor.getAllValues().get(0).isBefore(almostNow));
		assertTrue(dateTimeCaptor.getAllValues().get(1).isBefore(almostNow));
		assertTrue(orchJobStatusListCaptor.getValue().size() == 2);
		assertTrue(orchJobStatusListCaptor.getValue().contains(OrchestrationJobStatus.DONE));
		assertTrue(orchJobStatusListCaptor.getValue().contains(OrchestrationJobStatus.ERROR));
		assertTrue(uuidCollectionCaptor.getValue().size() == 1);
		assertTrue(uuidCollectionCaptor.getValue().contains(orchJobs.get(0).getId()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteJobNotExpired() {
		final List<OrchestrationJob> orchJobs = List.of(new OrchestrationJob());
		orchJobs.get(0).setId(UUID.randomUUID());
		orchJobs.get(0).setFinishedAt(Utilities.utcNow().minusHours(2));

		when(orchestrationJobDbService.getAllByStatusIn(anyList())).thenReturn(orchJobs);
		when(sysInfo.getOrchestrationHistoryMaxAge()).thenReturn(1);

		assertDoesNotThrow(() -> job.execute(new DummyJobExecutionContext()));

		verify(subscriptionDbService).deleteInBatchByExpiredBefore(dateTimeCaptor.capture());
		verify(orchestrationLockDbService).deleteInBatchByExpiredBefore(dateTimeCaptor.capture());
		verify(sysInfo).getOrchestrationHistoryMaxAge();
		verify(orchestrationJobDbService).getAllByStatusIn(orchJobStatusListCaptor.capture());
		verify(orchestrationJobDbService, never()).deleteInBatch(anyCollection());

		final ZonedDateTime almostNow = Utilities.utcNow().plusSeconds(1);
		assertTrue(dateTimeCaptor.getAllValues().get(0).isBefore(almostNow));
		assertTrue(dateTimeCaptor.getAllValues().get(1).isBefore(almostNow));
		assertTrue(orchJobStatusListCaptor.getValue().size() == 2);
		assertTrue(orchJobStatusListCaptor.getValue().contains(OrchestrationJobStatus.DONE));
		assertTrue(orchJobStatusListCaptor.getValue().contains(OrchestrationJobStatus.ERROR));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteDBErreor() {
		doThrow(new InternalServerError("test message")).when(subscriptionDbService).deleteInBatchByExpiredBefore(any());
		assertDoesNotThrow(() -> job.execute(new DummyJobExecutionContext()));

		verify(subscriptionDbService).deleteInBatchByExpiredBefore(any());
		verify(orchestrationLockDbService, never()).deleteInBatchByExpiredBefore(any());
		verify(sysInfo, never()).getOrchestrationHistoryMaxAge();
		verify(orchestrationJobDbService, never()).getAllByStatusIn(any());
		verify(orchestrationJobDbService, never()).deleteInBatch(anyCollection());
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	private static final class DummyTrigger implements Trigger {

		//-------------------------------------------------------------------------------------------------
		@Override
		public TriggerKey getKey() {
			return new TriggerKey("testKey");
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public JobKey getJobKey() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getDescription() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getCalendarName() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public JobDataMap getJobDataMap() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public int getPriority() {
			return 0;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean mayFireAgain() {
			return false;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getStartTime() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getEndTime() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getNextFireTime() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getPreviousFireTime() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getFireTimeAfter(final Date afterTime) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getFinalFireTime() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public int getMisfireInstruction() {
			return 0;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public TriggerBuilder<? extends Trigger> getTriggerBuilder() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public ScheduleBuilder<? extends Trigger> getScheduleBuilder() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public int compareTo(final Trigger other) {
			return 0;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static final class DummyJobExecutionContext implements JobExecutionContext {

		//-------------------------------------------------------------------------------------------------
		@Override
		public Scheduler getScheduler() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Trigger getTrigger() {
			return new DummyTrigger();
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Calendar getCalendar() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean isRecovering() {
			return false;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public TriggerKey getRecoveringTriggerKey() throws IllegalStateException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public int getRefireCount() {
			return 0;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public JobDataMap getMergedJobDataMap() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public JobDetail getJobDetail() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Job getJobInstance() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getFireTime() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getScheduledFireTime() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getPreviousFireTime() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Date getNextFireTime() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getFireInstanceId() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Object getResult() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void setResult(final Object result) {
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public long getJobRunTime() {
			return 0;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void put(final Object key, final Object value) {
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Object get(final Object key) {
			return null;
		}
	}
}
