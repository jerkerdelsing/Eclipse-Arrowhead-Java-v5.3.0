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
package eu.arrowhead.translationmanager.quartz;

import java.time.ZonedDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.translationmanager.TranslationManagerConstants;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDiscovery;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService;

@Component
@DisallowConcurrentExecution
public class CleanerJob implements Job {

	//=================================================================================================
	// members

	@Value(TranslationManagerConstants.$TRANSLATION_DISCOVERY_MAX_AGE_WD)
	private int maxAge;

	@Autowired
	private BridgeDbService dbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("execute started...");

		final ZonedDateTime now = Utilities.utcNow();
		try {
			final List<BridgeDiscovery> obsoleted = dbService.getBridgeDiscoveriesCreatedBefore(now.minusHours(maxAge));
			obsoleted.forEach(bd -> dbService.handleObsoletedBridgeDiscovery(bd));
		} catch (final Exception ex) {
			logger.debug(ex);
			logger.error("Cleaner job error: " + ex.getMessage());
		}
	}
}