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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationLockDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;

@Component
@DisallowConcurrentExecution
public class CleanerJob implements Job {

	//=================================================================================================
	// members

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private OrchestrationLockDbService orchestrationLockDbService;

	@Autowired
	private OrchestrationJobDbService orchestrationJobDbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("execute started...");

		final ZonedDateTime now = Utilities.utcNow();

		try {
			removeExpiredSubscriptions(now);
			removeExpiredOrchestrationLocks(now);
			removeOldOrchestrationJobs(now);
		} catch (final Exception ex) {
			logger.debug(ex);
			logger.error("Cleaner job error: " + ex.getMessage());
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void removeExpiredSubscriptions(final ZonedDateTime now) {
		logger.debug("removeExpiredSubscriptions started..");

		synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
			subscriptionDbService.deleteInBatchByExpiredBefore(now);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void removeExpiredOrchestrationLocks(final ZonedDateTime now) {
		logger.debug("removeExpiredOrchestrationLocks started...");

		synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_ORCH_LOCK) {
			orchestrationLockDbService.deleteInBatchByExpiredBefore(now);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void removeOldOrchestrationJobs(final ZonedDateTime now) {
		logger.debug("removeOldOrchestrationJobs started...");

		final List<UUID> toRemove = new ArrayList<>();
		orchestrationJobDbService.getAllByStatusIn(List.of(OrchestrationJobStatus.DONE, OrchestrationJobStatus.ERROR)).forEach(job -> {
			final ZonedDateTime expirationTime = job.getFinishedAt().plusDays(sysInfo.getOrchestrationHistoryMaxAge());
			if (expirationTime.isBefore(now)) {
				toRemove.add(job.getId());
			}
		});
		if (!Utilities.isEmpty(toRemove)) {
			orchestrationJobDbService.deleteInBatch(toRemove);
		}
	}
}