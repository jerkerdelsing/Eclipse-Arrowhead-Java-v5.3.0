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
package eu.arrowhead.common.mqtt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Deque;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class MqttResourceManagerTest {

	//=================================================================================================
	// members

	private final MqttResourceManager manager = new MqttResourceManager();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setUp() {
		final Deque<Long> latencies = (Deque<Long>) ReflectionTestUtils.getField(manager, "latencies");
		latencies.clear();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterLatencyNoLimitNoChanges() {
		assertFalse((boolean) ReflectionTestUtils.getField(manager, "threadpoolFixed"));
		assertDoesNotThrow(() -> manager.registerLatency(2010));
		assertFalse((boolean) ReflectionTestUtils.getField(manager, "threadpoolFixed"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRegisterLatencyFixedChangeToNoLimit() {
		ReflectionTestUtils.setField(manager, "threadpoolFixed", true);
		final ThreadPoolExecutor threadPoolMock = Mockito.mock(ThreadPoolExecutor.class);
		ReflectionTestUtils.setField(manager, "threadpool", threadPoolMock);
		final Deque<Long> latencies = (Deque<Long>) ReflectionTestUtils.getField(manager, "latencies");
		latencies.addFirst(400L);

		doNothing().when(threadPoolMock).setMaximumPoolSize(1000);

		assertDoesNotThrow(() -> manager.registerLatency(2010));

		verify(threadPoolMock).setMaximumPoolSize(1000);

		assertFalse((boolean) ReflectionTestUtils.getField(manager, "threadpoolFixed"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRegisterLatencyFixedNoChanges() {
		ReflectionTestUtils.setField(manager, "threadpoolFixed", true);
		final ThreadPoolExecutor threadPoolMock = Mockito.mock(ThreadPoolExecutor.class);
		ReflectionTestUtils.setField(manager, "threadpool", threadPoolMock);
		final Deque<Long> latencies = (Deque<Long>) ReflectionTestUtils.getField(manager, "latencies");
		latencies.addFirst(400L);
		latencies.addFirst(2100L);
		latencies.addFirst(3400L);
		latencies.addFirst(2870L);
		latencies.addFirst(2001L);

		assertDoesNotThrow(() -> manager.registerLatency(2010));

		verify(threadPoolMock, never()).setMaximumPoolSize(anyInt());

		assertTrue((boolean) ReflectionTestUtils.getField(manager, "threadpoolFixed"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRegisterLatencyNoLimitChangeToFixed() {
		ReflectionTestUtils.setField(manager, "threadpoolFixed", false);
		final ThreadPoolExecutor threadPoolMock = Mockito.mock(ThreadPoolExecutor.class);
		ReflectionTestUtils.setField(manager, "threadpool", threadPoolMock);
		final Deque<Long> latencies = (Deque<Long>) ReflectionTestUtils.getField(manager, "latencies");
		latencies.addFirst(400L);
		latencies.addFirst(2100L);
		latencies.addFirst(3400L);
		latencies.addFirst(2870L);
		latencies.addFirst(2001L);

		when(threadPoolMock.getActiveCount()).thenReturn(42);
		doNothing().when(threadPoolMock).setMaximumPoolSize(42);

		assertDoesNotThrow(() -> manager.registerLatency(2010));

		verify(threadPoolMock).getActiveCount();
		verify(threadPoolMock).setMaximumPoolSize(42);

		assertTrue((boolean) ReflectionTestUtils.getField(manager, "threadpoolFixed"));
	}
}