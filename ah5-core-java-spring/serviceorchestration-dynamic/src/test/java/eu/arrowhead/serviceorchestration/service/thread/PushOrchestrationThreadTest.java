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
package eu.arrowhead.serviceorchestration.service.thread;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;

@ExtendWith(MockitoExtension.class)
public class PushOrchestrationThreadTest {

	//=================================================================================================
	// members

	@InjectMocks
	private PushOrchestrationThread thread;

	@Mock
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Mock
	private Function<UUID, PushOrchestrationWorker> pushOrchestrationWorkerFactory;

	//=================================================================================================
	// methods

	@BeforeEach
	public void callPostConstructInit() throws Exception {
		// Manually invoke PostConstruct
		when(sysInfo.getPushOrchestrationMaxThread()).thenReturn(1);
		Method postConstruct = PushOrchestrationThread.class.getDeclaredMethod("init");
		postConstruct.setAccessible(true);
		postConstruct.invoke(thread);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRun() {
		final UUID jobId = UUID.randomUUID();
		final BlockingQueue<UUID> testQueue = new LinkedBlockingQueue<>();
		testQueue.add(jobId);
		ReflectionTestUtils.setField(thread, "pushOrchJobQueue", testQueue);

		final DummyPushOrchestrationWorker testWorker = new DummyPushOrchestrationWorker(jobId);

		when(pushOrchestrationWorkerFactory.apply(eq(jobId))).thenAnswer(invocation -> {
			ReflectionTestUtils.setField(thread, "doWork", false);
			return testWorker;
		});

		assertDoesNotThrow(() -> thread.run());

		verify(sysInfo).getPushOrchestrationMaxThread();
		verify(pushOrchestrationWorkerFactory).apply(eq(jobId));

		final boolean doWork = (boolean) ReflectionTestUtils.getField(thread, "doWork");
		assertFalse(doWork);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunInterrupt() {
		final UUID jobId = UUID.randomUUID();
		final BlockingQueue<UUID> testQueue = new LinkedBlockingQueue<>();
		testQueue.add(jobId);
		ReflectionTestUtils.setField(thread, "pushOrchJobQueue", testQueue);

		when(pushOrchestrationWorkerFactory.apply(eq(jobId))).thenAnswer(invocation -> {
			ReflectionTestUtils.setField(thread, "doWork", false);
			throw new InterruptedException();
		});

		assertDoesNotThrow(() -> thread.run());

		verify(sysInfo).getPushOrchestrationMaxThread();
		verify(pushOrchestrationWorkerFactory).apply(eq(jobId));

		final boolean doWork = (boolean) ReflectionTestUtils.getField(thread, "doWork");
		assertFalse(doWork);
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static final class DummyPushOrchestrationWorker extends PushOrchestrationWorker {

		//-------------------------------------------------------------------------------------------------
		public DummyPushOrchestrationWorker(final UUID jobid) {
			super(jobid);
		}

		//-------------------------------------------------------------------------------------------------
		@SuppressWarnings("checkstyle:MagicNumber")
		public void run() {
			try {
				Thread.sleep(100);
			} catch (final InterruptedException ex) {
				// intentionally blank
			}
		}
	}
}
