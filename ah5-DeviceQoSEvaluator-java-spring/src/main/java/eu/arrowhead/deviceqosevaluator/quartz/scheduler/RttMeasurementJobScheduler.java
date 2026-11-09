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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.quartz.job.RttMeasurementJob;

@Service
public class RttMeasurementJobScheduler {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Autowired
	private Scheduler scheduler;

	private static final String jobSuffix = "_job_rtt";
	private static final String triggerSuffix = "_trigger_rtt";

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void start(final Device device) throws SchedulerException {
		logger.debug("RttMeasurementJobScheduler.start started");
		Assert.notNull(device, "device is null");
		Assert.notNull(device.getId(), "device id is null");

		if (scheduler.checkExists(JobKey.jobKey(device.getId().toString() + jobSuffix))) {
			logger.warn("RttMeasurementJobScheduler for " + device.getId().toString() + " already exists");
			return;
		}

		final JobDataMap data = new JobDataMap();
		data.put("deviceId", device.getId());

		final JobDetail jobDetail = JobBuilder.newJob(RttMeasurementJob.class)
				.withIdentity(device.getId().toString() + jobSuffix)
				.usingJobData(data)
				.storeDurably()
				.build();

		final Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(device.getId().toString() + triggerSuffix)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule()
						.withIntervalInMilliseconds(sysInfo.getRttMeasurementJobInterval() * DeviceQoSEvaluatorConstants.SEC_TO_MS) // from sec to milisec
						.repeatForever())
				.build();

		scheduler.scheduleJob(jobDetail, trigger);
	}

	//-------------------------------------------------------------------------------------------------
	public void stop(final List<Device> devices) throws SchedulerException {
		logger.debug("RttMeasurementJobScheduler.stop started");
		Assert.notNull(devices, "device list is null");
		Assert.isTrue(!Utilities.containsNull(devices), "device list contains null");

		for (final Device device : devices) {
			if (device.getId() != null) {
				scheduler.unscheduleJob(TriggerKey.triggerKey(device.getId().toString() + triggerSuffix));
				scheduler.deleteJob(JobKey.jobKey(device.getId().toString() + jobSuffix));
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isScheduled(final Device device) throws SchedulerException {
		logger.debug("RttMeasurementJobScheduler.isScheduled started");
		Assert.notNull(device, "device is null");
		Assert.notNull(device.getId(), "device id is null");

		return scheduler.checkExists(JobKey.jobKey(device.getId().toString() + jobSuffix));
	}
}