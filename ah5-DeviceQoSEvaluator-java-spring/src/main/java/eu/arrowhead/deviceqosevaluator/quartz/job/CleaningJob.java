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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.quartz.QuartzJobBean;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.service.DeviceDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.StatDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.SystemDbService;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.AugmentedMeasurementJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.RttMeasurementJobScheduler;

@DisallowConcurrentExecution
public class CleaningJob extends QuartzJobBean {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Autowired
	private DeviceDbService deviceDbService;

	@Autowired
	private SystemDbService systemDbService;

	@Autowired
	private StatDbService statDbService;

	@Autowired
	private RttMeasurementJobScheduler rttMeasurementJobScheduler;

	@Autowired
	private AugmentedMeasurementJobScheduler augmentedMeasurementJobScheduler;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void executeInternal(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("executeInternal started");

		try {
			final ZonedDateTime now = Utilities.utcNow();

			statDbService.removeBeforeTimestamp(now.minusMinutes(sysInfo.getRawMeasurementDataMaxAge()));

			final List<Device> toRemove = new ArrayList<>();
			int page = 0;
			boolean hasNext = true;
			do {
				final Page<Device> devicePage = deviceDbService.getPage(PageRequest.of(page, sysInfo.getMaxPageSize(), Direction.ASC, Device.DEFAULT_SORT_FIELD));
				hasNext = devicePage.hasNext();
				page++;

				final List<Device> toUpdate = new ArrayList<>();
				for (final Device device : devicePage.getContent()) {
					if (device.isInactive()
							&& device.getUpdatedAt().plusMinutes(sysInfo.getInactiveDeviceMaxAge()).isBefore(now)
							&& !statDbService.hasAny(device.getId())) {
						toRemove.add(device);
						stopMeasuring(device); // just for sure

					} else if (Utilities.isEmpty(systemDbService.findByDeviceId(device.getId()))) {
						device.setInactive(true);
						stopMeasuring(device);
						toUpdate.add(device);
					}
				}

				deviceDbService.update(toUpdate);

			} while (hasNext);

			deviceDbService.remove(toRemove);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void stopMeasuring(final Device device) throws SchedulerException {
		if (rttMeasurementJobScheduler.isScheduled(device)) {
			rttMeasurementJobScheduler.stop(List.of(device));
		}
		if (augmentedMeasurementJobScheduler.isScheduled(device)) {
			augmentedMeasurementJobScheduler.stop(List.of(device));
		}
	}
}
