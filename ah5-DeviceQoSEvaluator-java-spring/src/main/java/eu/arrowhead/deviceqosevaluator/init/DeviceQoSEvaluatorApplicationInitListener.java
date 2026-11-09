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
package eu.arrowhead.deviceqosevaluator.init;

import javax.naming.ConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.service.DeviceDbService;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.AugmentedMeasurementJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.CleaningJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.MeasurementOrganizerJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.RttMeasurementJobScheduler;

@Component
public class DeviceQoSEvaluatorApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Autowired
	private MeasurementOrganizerJobScheduler measurementOrganizerJobScheduler;

	@Autowired
	private RttMeasurementJobScheduler rttMeasurementJobScheduler;

	@Autowired
	private AugmentedMeasurementJobScheduler augmentedMeasurementJobScheduler;

	@Autowired
	private CleaningJobScheduler cleaningJobScheduler;

	@Autowired
	private DeviceDbService deviceDbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException, ConfigurationException {
		validateConfiguration();

		try {
			measurementOrganizerJobScheduler.start();
			logger.info("Mesaurement organizer job has been started");

			cleaningJobScheduler.start();
			logger.info("Cleaning job has been started");
		} catch (final SchedulerException ex) {
			logger.error("Error occured while scheduling jobs at start-up");
			logger.debug(ex);
			throw new ConfigurationException(ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customDestroy() {
		try {
			measurementOrganizerJobScheduler.stop();
			logger.info("Mesaurement organizer job has been terminated");

			cleaningJobScheduler.stop();
			logger.info("Cleaning job has been terminated");

			int page = 0;
			boolean hasNext = true;
			do {
				final Page<Device> devicePage = deviceDbService.getPage(PageRequest.of(page, sysInfo.getMaxPageSize(), Direction.ASC, Device.DEFAULT_SORT_FIELD));
				hasNext = devicePage.hasNext();
				page++;

				augmentedMeasurementJobScheduler.stop(devicePage.getContent());
				rttMeasurementJobScheduler.stop(devicePage.getContent());
			} while (hasNext);

		} catch (final SchedulerException ex) {
			logger.error("Error occured while terminating jobs scheduling");
			logger.debug(ex);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateConfiguration() throws ConfigurationException {
		if (sysInfo.getMeasurementOrganizerJobInterval() < DeviceQoSEvaluatorConstants.MEASUREMENT_ORGANIZER_JOB_INTERVAL_MIN_VALUE) {
			throw new ConfigurationException("Invalid configuration: '" + DeviceQoSEvaluatorConstants.MEASUREMENT_ORGANIZER_JOB_INTERVAL + "' cannot be less than "
					+ DeviceQoSEvaluatorConstants.MEASUREMENT_ORGANIZER_JOB_INTERVAL_MIN_VALUE + " sec");
		}

		if (sysInfo.getRttMeasurementJobInterval() < DeviceQoSEvaluatorConstants.RTT_MEASUREMENT_JOB_INTERVAL_MIN_VALUE) {
			throw new ConfigurationException("Invalid configuration: '" + DeviceQoSEvaluatorConstants.RTT_MEASUREMENT_JOB_INTERVAL + "' cannot be less than " + DeviceQoSEvaluatorConstants.RTT_MEASUREMENT_JOB_INTERVAL_MIN_VALUE + " sec");
		}

		if (sysInfo.getRttMeasurementTimeout() < DeviceQoSEvaluatorConstants.RTT_MEASUREMENT_TIMEOUT_MIN_VALUE) {
			throw new ConfigurationException("Invalid configuration: '" + DeviceQoSEvaluatorConstants.RTT_MEASUREMENT_TIMEOUT + "' cannot be less than " + DeviceQoSEvaluatorConstants.RTT_MEASUREMENT_TIMEOUT_MIN_VALUE + " ms");
		}

		if (sysInfo.getRttMeasurementTimeout() >= sysInfo.getRttMeasurementJobInterval() * DeviceQoSEvaluatorConstants.SEC_TO_MS) {
			throw new ConfigurationException("Invalid configuration: '" + DeviceQoSEvaluatorConstants.RTT_MEASUREMENT_TIMEOUT + "' must be less than '"
					+ DeviceQoSEvaluatorConstants.RTT_MEASUREMENT_JOB_INTERVAL + "' (" + (sysInfo.getRttMeasurementJobInterval() * DeviceQoSEvaluatorConstants.SEC_TO_MS) + " ms)");
		}

		if (sysInfo.getAugmentedMeasurementJobInterval() < DeviceQoSEvaluatorConstants.AUGMENTED_MEASUREMENT_JOB_INTERVAL_MIN_VALUE) {
			throw new ConfigurationException("Invalid configuration: '" + DeviceQoSEvaluatorConstants.AUGMENTED_MEASUREMENT_JOB_INTERVAL + "' cannot be less than "
					+ DeviceQoSEvaluatorConstants.AUGMENTED_MEASUREMENT_JOB_INTERVAL_MIN_VALUE + " sec");
		}

		if (sysInfo.getCleaningJobInterval() < DeviceQoSEvaluatorConstants.CLEANING_JOB_INTERVAL_MIN_VALUE) {
			throw new ConfigurationException("Invalid configuration: '" + DeviceQoSEvaluatorConstants.CLEANING_JOB_INTERVAL + "' cannot be less than " + DeviceQoSEvaluatorConstants.CLEANING_JOB_INTERVAL_MIN_VALUE + " sec");
		}

		if (sysInfo.getEvaluationTimeWindow() >= sysInfo.getRawMeasurementDataMaxAge() * DeviceQoSEvaluatorConstants.MIN_TO_SEC) {
			throw new ConfigurationException("Invalid configuration: '" + DeviceQoSEvaluatorConstants.EVALUATION_TIME_WINDOW + "' must be less than '"
					+ DeviceQoSEvaluatorConstants.RAW_MEASUREMENT_DATA_MAX_AGE + "' (" + (sysInfo.getRawMeasurementDataMaxAge() * DeviceQoSEvaluatorConstants.MIN_TO_SEC) + " sec)");
		}
	}
}