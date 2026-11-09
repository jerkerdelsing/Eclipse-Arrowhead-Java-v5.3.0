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
package eu.arrowhead.deviceqosevaluator.quartz.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.quartz.job.MeasurementOrganizerJob;

@Service
public class MeasurementOrganizerJobScheduler {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Autowired
	private Scheduler scheduler;

	private JobDetail jobDetail;
	private Trigger currentTrigger;
	private boolean jobScheduled = false;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public synchronized void start() throws SchedulerException {
		logger.debug("MeasurementOrganizerJobScheduler.start started");

		if (jobScheduled) {
			return;
		}

		jobDetail = JobBuilder.newJob(MeasurementOrganizerJob.class)
				.withIdentity(DeviceQoSEvaluatorConstants.MEASUREMENT_ORGANIZER_JOB)
				.storeDurably()
				.build();

		currentTrigger = TriggerBuilder.newTrigger()
				.withIdentity(DeviceQoSEvaluatorConstants.MEASUREMENT_ORGANIZER_JOB_TRIGGER)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule()
						.withIntervalInMilliseconds(sysInfo.getMeasurementOrganizerJobInterval() * DeviceQoSEvaluatorConstants.SEC_TO_MS) // from sec to milisec
						.repeatForever())
				.build();

		scheduler.scheduleJob(jobDetail, currentTrigger);

		scheduler.start();
		jobScheduled = true;
	}

	//-------------------------------------------------------------------------------------------------
	public synchronized void stop() throws SchedulerException {
		logger.debug("MeasurementOrganizerJobScheduler.stop started");

		if (!jobScheduled) {
			return;
		}

		if (currentTrigger != null) {
			scheduler.unscheduleJob(currentTrigger.getKey());
			currentTrigger = null;
		}

		scheduler.deleteJob(jobDetail.getKey());
		jobScheduled = false;
	}
}
