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

package eu.arrowhead.deviceqosevaluator.quartz.job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import eu.arrowhead.deviceqosevaluator.engine.MeasurementEngine;

@DisallowConcurrentExecution
public class MeasurementOrganizerJob extends QuartzJobBean {

	//=================================================================================================
	// members

	@Autowired
	private MeasurementEngine measurementEngine;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void executeInternal(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("MeasurementOrganizerJob.executeInternal started");

		try {
			measurementEngine.organize();
		} catch (final Exception ex) {
			logger.error("Measurement organizer job failure");
			logger.error(ex.getMessage());
			logger.debug(ex);
		}
	}
}
