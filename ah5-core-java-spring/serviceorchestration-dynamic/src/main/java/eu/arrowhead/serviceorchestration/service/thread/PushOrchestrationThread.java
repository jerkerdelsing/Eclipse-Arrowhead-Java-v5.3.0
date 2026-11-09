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

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Component
public class PushOrchestrationThread extends Thread {

	//=================================================================================================
	// members

	@Resource(name = DynamicServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	private BlockingQueue<UUID> pushOrchJobQueue;

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Autowired
	private Function<UUID, PushOrchestrationWorker> pushOrchestrationWorkerFactory;

	private boolean doWork = false;

	private ThreadPoolExecutor threadpool;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run() {
		logger.debug("run started...");

		doWork = true;
		while (doWork) {
			try {
				final UUID jobId = pushOrchJobQueue.take();
				threadpool.execute(pushOrchestrationWorkerFactory.apply(jobId));

			} catch (final InterruptedException ex) {
				logger.debug(ex);
				logger.error(ex.getMessage());
				interrupt();
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void interrupt() {
		logger.debug("interrupt started...");
		doWork = false;
		super.interrupt();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		logger.debug("init started...");

		threadpool = (ThreadPoolExecutor) Executors.newFixedThreadPool(sysInfo.getPushOrchestrationMaxThread());
	}
}