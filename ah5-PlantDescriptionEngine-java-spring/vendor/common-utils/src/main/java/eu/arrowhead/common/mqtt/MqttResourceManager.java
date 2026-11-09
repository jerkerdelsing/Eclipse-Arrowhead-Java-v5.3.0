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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MqttResourceManager {

	//=================================================================================================
	// members

	private static final int MEASURE_SIZE = 5;
	private final Deque<Long> latencies = new ArrayDeque<>(MEASURE_SIZE);

	private static final int MIN_THREAD = 1;
	private static final int THREAD_TIMEOUT = 15; // sec
	private static final int LATENCY_THRESHOLD = 2000; // ms
	private static final int NO_LIMIT = 1000;

	private ThreadPoolExecutor threadpool;
	private boolean threadpoolFixed;

	private static final Object LOCK = new Object();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public MqttResourceManager() {
		threadpool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		threadpool.setCorePoolSize(MIN_THREAD);
		threadpool.setMaximumPoolSize(NO_LIMIT);
		threadpool.setKeepAliveTime(THREAD_TIMEOUT, TimeUnit.SECONDS);
	}

	//-------------------------------------------------------------------------------------------------
	public ThreadPoolExecutor getThreadpool() {
		return this.threadpool;
	}

	//-------------------------------------------------------------------------------------------------
	public void registerLatency(final long latency) {
		synchronized (LOCK) {
			if (latencies.size() == MEASURE_SIZE) {
				latencies.pollLast();
			}
			latencies.addFirst(latency);
			reconsider();
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	/*
	 * If latency starts to increase, we would like to prevent creating new threads and reject the incoming messages
	 */
	private void reconsider() {
		int count = 0;
		for (final Long latency : latencies) {
			if (latency >= LATENCY_THRESHOLD) {
				count++;
			}
		}

		final boolean shouldFix = count >= MEASURE_SIZE;
		if (shouldFix && threadpoolFixed) {
			return;
		} else if (shouldFix && !threadpoolFixed) {
			threadpool.setMaximumPoolSize(threadpool.getActiveCount());
			threadpoolFixed = true;
		} else if (!shouldFix && threadpoolFixed) {
			threadpool.setMaximumPoolSize(NO_LIMIT);
			threadpoolFixed = false;
		}
	}
}