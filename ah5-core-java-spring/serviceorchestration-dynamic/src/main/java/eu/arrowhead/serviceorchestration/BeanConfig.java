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

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import eu.arrowhead.serviceorchestration.service.thread.PushOrchestrationWorker;
import eu.arrowhead.serviceorchestration.service.utils.matchmaker.DefaultServiceInstanceMatchmaker;
import eu.arrowhead.serviceorchestration.service.utils.matchmaker.ServiceInstanceMatchmaker;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

@Configuration
public class BeanConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean(name = DynamicServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	BlockingQueue<UUID> initPushOrchestrationJobQueue() {
		return new LinkedBlockingQueue<>();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean(name = DynamicServiceOrchestrationConstants.SERVICE_INSTANCE_MATCHMAKER)
	ServiceInstanceMatchmaker initServiceInstanceMatchmaker() {
		return new DefaultServiceInstanceMatchmaker();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	PushOrchestrationWorker createPushOrchestrationWorker(final UUID jobId) {
		return new PushOrchestrationWorker(jobId);
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	Function<UUID, PushOrchestrationWorker> pushOrchestrationWorkerFactory() {
		return jobId -> createPushOrchestrationWorker(jobId);
	}
}