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
package eu.arrowhead.common.quartz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.IdentityLoginResponseDTO;
import eu.arrowhead.dto.IdentityRequestDTO;

@ExtendWith(MockitoExtension.class)
public class OutsourcedLoginJobTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OutsourcedLoginJob job;

	@Mock
	private SystemInfo systemInfo;

	@Mock
	private ArrowheadHttpService httpService;

	@Mock
	private Scheduler scheduler;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteNoNeedForLogin() throws JobExecutionException {
		when(systemInfo.getArrowheadContext()).thenReturn(Map.of("identity-renewal-threshold", Utilities.utcNow().plusHours(5)));

		assertDoesNotThrow(() -> job.execute(new DummyJobExecutionContext()));

		verify(systemInfo).getArrowheadContext();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteExceptionHandled() throws JobExecutionException {
		when(systemInfo.getArrowheadContext()).thenReturn(Map.of());
		when(systemInfo.getSystemName()).thenReturn("TestSystem");
		when(systemInfo.getAuthenticatorCredentials()).thenReturn(Map.of("password", "123456"));
		when(httpService.consumeService(eq("identity"), eq("identity-login"), eq(IdentityLoginResponseDTO.class), any(IdentityRequestDTO.class))).thenThrow(new ArrowheadException("test"));

		// exception is handled inside
		assertDoesNotThrow(() -> job.execute(new DummyJobExecutionContext()));

		verify(systemInfo).getArrowheadContext();
		verify(systemInfo).getSystemName();
		verify(systemInfo).getAuthenticatorCredentials();
		verify(httpService).consumeService(eq("identity"), eq("identity-login"), eq(IdentityLoginResponseDTO.class), any(IdentityRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteAuthExceptionHandled() throws SchedulerException {
		when(systemInfo.getArrowheadContext()).thenReturn(Map.of("identity-renewal-threshold", Utilities.utcNow().minusMinutes(1)));
		when(systemInfo.getSystemName()).thenReturn("TestSystem");
		when(systemInfo.getAuthenticatorCredentials()).thenReturn(Map.of("password", "12345"));
		when(httpService.consumeService(eq("identity"), eq("identity-login"), eq(IdentityLoginResponseDTO.class), any(IdentityRequestDTO.class))).thenThrow(new AuthException("test"));
		// exception is handled inside; the job stays scheduled and retries at the next interval
		assertDoesNotThrow(() -> job.execute(new DummyJobExecutionContext()));

		verify(systemInfo).getArrowheadContext();
		verify(systemInfo).getSystemName();
		verify(systemInfo).getAuthenticatorCredentials();
		verify(httpService).consumeService(eq("identity"), eq("identity-login"), eq(IdentityLoginResponseDTO.class), any(IdentityRequestDTO.class));
		verify(scheduler, never()).unscheduleJob(any(TriggerKey.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteRenewalOk() throws SchedulerException {
		ReflectionTestUtils.setField(job, "interval", 10000);
		final Map<String, Object> context = new HashMap<>();
		context.put("identity-renewal-threshold", Utilities.utcNow().minusMinutes(1));
		final IdentityLoginResponseDTO response = new IdentityLoginResponseDTO(
				"testToken",
				"2025-07-16T12:00:00Z");

		when(systemInfo.getArrowheadContext()).thenReturn(context);
		when(systemInfo.getSystemName()).thenReturn("TestSystem");
		when(systemInfo.getAuthenticatorCredentials()).thenReturn(Map.of("password", "123456"));
		when(httpService.consumeService(eq("identity"), eq("identity-login"), eq(IdentityLoginResponseDTO.class), any(IdentityRequestDTO.class))).thenReturn(response);

		assertDoesNotThrow(() -> job.execute(new DummyJobExecutionContext()));
		assertEquals("testToken", context.get("identity-token"));
		assertEquals("2025-07-16T11:59:09.999Z", Utilities.convertZonedDateTimeToUTCString((ZonedDateTime) context.get("identity-renewal-threshold")));

		verify(systemInfo, times(3)).getArrowheadContext();
		verify(systemInfo).getSystemName();
		verify(systemInfo).getAuthenticatorCredentials();
		verify(httpService).consumeService(eq("identity"), eq("identity-login"), eq(IdentityLoginResponseDTO.class), any(IdentityRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	// Release 5.3.0 live run: at start-up the system logs in from the main thread (the Spring bean, through
	// ApplicationInitListener) while Quartz runs the job on a job instance of its own. Both saw no token and both logged
	// in; the second token superseded the first while the registration call was under way ("Invalid authorization
	// header"). Two job instances that log in concurrently must log in once.
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConcurrentLoginFromTwoJobInstancesLogsInOnce() throws Exception {
		final Map<String, Object> context = new ConcurrentHashMap<>();
		when(systemInfo.getArrowheadContext()).thenReturn(context);
		when(systemInfo.getSystemName()).thenReturn("TestSystem");
		when(systemInfo.getAuthenticatorCredentials()).thenReturn(Map.of("password", "123456"));
		when(httpService.consumeService(eq("identity"), eq("identity-login"), eq(IdentityLoginResponseDTO.class), any(IdentityRequestDTO.class))).thenAnswer(invocation -> {
			Thread.sleep(300); // a login round trip to Authentication
			return new IdentityLoginResponseDTO("token-" + System.nanoTime(), Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)));
		});

		final OutsourcedLoginJob startup = job;
		final OutsourcedLoginJob scheduled = new OutsourcedLoginJob(); // what Quartz creates for each execution
		for (final OutsourcedLoginJob instance : new OutsourcedLoginJob[] { startup, scheduled }) {
			ReflectionTestUtils.setField(instance, "interval", 10000);
			ReflectionTestUtils.setField(instance, "systemInfo", systemInfo);
			ReflectionTestUtils.setField(instance, "httpService", httpService);
		}

		final CountDownLatch go = new CountDownLatch(1);
		final ExecutorService threads = Executors.newFixedThreadPool(2);
		try {
			threads.submit(() -> {
				go.await();
				startup.loginIfNeeded();
				return null;
			});
			threads.submit(() -> {
				go.await();
				scheduled.execute(new DummyJobExecutionContext());
				return null;
			});
			go.countDown();
			threads.shutdown();
			assertEquals(true, threads.awaitTermination(10, TimeUnit.SECONDS));
		} finally {
			threads.shutdownNow();
		}

		verify(httpService, times(1)).consumeService(eq("identity"), eq("identity-login"), eq(IdentityLoginResponseDTO.class), any(IdentityRequestDTO.class));
		assertEquals(true, ((String) context.get("identity-token")).startsWith("token-"));
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