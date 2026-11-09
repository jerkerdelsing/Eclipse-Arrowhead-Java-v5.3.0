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

package eu.arrowhead.serviceorchestration;

import eu.arrowhead.serviceorchestration.service.thread.PushOrchestrationWorker;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

@Configuration
public class BeanConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean(name = SimpleStoreServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	BlockingQueue<UUID> initPushOrchestrationJobQueue() {
		return new LinkedBlockingQueue<>();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	PushOrchestrationWorker createPushOrchestrationWorker(final UUID jobID) {
		return new PushOrchestrationWorker(jobID);
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	Function<UUID, PushOrchestrationWorker> pushOrchestrationWorkerFactory() {
		return jobID -> createPushOrchestrationWorker(jobID);
	}
}
