/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import eu.arrowhead.choreographer.ChoreographerConstants;

// Worker pool for session execution (replaces the v4.6 JMS queues "start-session" and "session-step-done").
@Configuration
public class EngineConfig {

	public static final String WORKER = "choreographerWorker";

	@Bean(name = WORKER)
	ThreadPoolTaskExecutor choreographerWorker(@Value(ChoreographerConstants.$SESSION_WORKER_THREADS_WD) final int threads) {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(threads);
		executor.setMaxPoolSize(threads);
		executor.setThreadNamePrefix("choreographer-");
		executor.setWaitForTasksToCompleteOnShutdown(false);
		executor.initialize();
		return executor;
	}
}
