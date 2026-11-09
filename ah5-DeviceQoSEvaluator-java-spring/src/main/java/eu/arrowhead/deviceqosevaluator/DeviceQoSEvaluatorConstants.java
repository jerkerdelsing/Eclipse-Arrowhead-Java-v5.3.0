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
package eu.arrowhead.deviceqosevaluator;

import java.util.List;

public final class DeviceQoSEvaluatorConstants {

	//=================================================================================================
	// members

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.deviceqosevaluator.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.deviceqosevaluator.jpa.repository";

	public static final String OID_NAME_DELIMITER = "_";
	public static final int SEC_TO_MS = 1000;
	public static final int MIN_TO_SEC = 60;

	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_QUALITY_EVALUATION = "1.0.0";
	public static final String VERSION_DEVICE_QUALITY_DATA_MANAGEMENT = "1.0.0";

	public static final String METADATA_VALUE_QUALITY_EVALUATION_SERVICE_VARIANT = "basic-device-kpi";

	// Config related

	public static final String MEASUREMENT_ORGANIZER_JOB_INTERVAL = "measurement.organizer.job.interval";
	public static final String $MEASUREMENT_ORGANIZER_JOB_INTERVAL_WD = "${" + MEASUREMENT_ORGANIZER_JOB_INTERVAL + ":" + DeviceQoSEvaluatorDefaults.MEASUREMENT_ORGANIZER_JOB_INTERVAL_DEFAULT + "}";
	public static final int MEASUREMENT_ORGANIZER_JOB_INTERVAL_MIN_VALUE = 10; // sec
	public static final String RTT_MEASUREMENT_JOB_INTERVAL = "rtt.measurement.job.interval";
	public static final String $RTT_MEASUREMENT_JOB_INTERVAL_WD = "${" + RTT_MEASUREMENT_JOB_INTERVAL + ":" + DeviceQoSEvaluatorDefaults.RTT_MEASUEREMENT_JOB_INTERVAL_DEFAULT + "}";
	public static final int RTT_MEASUREMENT_JOB_INTERVAL_MIN_VALUE = 5; // sec
	public static final String RTT_MEASUREMENT_TIMEOUT = "rtt.measurement.timeout";
	public static final String $RTT_MEASUREMENT_TIMEOUT_WD = "${" + RTT_MEASUREMENT_TIMEOUT + ":" + DeviceQoSEvaluatorDefaults.RTT_MEASUEREMENT_TIMEOUT_DEFAULT + "}";
	public static final int RTT_MEASUREMENT_TIMEOUT_MIN_VALUE = 3000; // ms
	public static final String AUGMENTED_MEASUREMENT_JOB_INTERVAL = "augmented.measurement.job.interval";
	public static final String $AUGMENTED_MEASUREMENT_JOB_INTERVAL_WD = "${" + AUGMENTED_MEASUREMENT_JOB_INTERVAL + ":" + DeviceQoSEvaluatorDefaults.AUGMENTED_MEASUEREMENT_JOB_INTERVAL_DEFAULT + "}";
	public static final int AUGMENTED_MEASUREMENT_JOB_INTERVAL_MIN_VALUE = 5; // sec
	public static final String EVALUATION_TIME_WINDOW = "evaluation.time.window";
	public static final String $EVALUATION_TIME_WINDOW_WD = "${" + EVALUATION_TIME_WINDOW + ":" + DeviceQoSEvaluatorDefaults.EVALUATION_TIME_WINDOW_DEFAULT + "}";
	public static final String CLEANING_JOB_INTERVAL = "cleaning.job.interval";
	public static final String $CLEANING_JOB_INTERVAL_WD = "${" + CLEANING_JOB_INTERVAL + ":" + DeviceQoSEvaluatorDefaults.CLEANING_JOB_INTERVAL_DEFAULT + "}";
	public static final int CLEANING_JOB_INTERVAL_MIN_VALUE = 60; // sec
	public static final String RAW_MEASUREMENT_DATA_MAX_AGE = "raw.measurement.data.max.age";
	public static final String $RAW_MEASUREMENT_DATA_MAX_AGE_WD = "${" + RAW_MEASUREMENT_DATA_MAX_AGE + ":" + DeviceQoSEvaluatorDefaults.RAW_MEASUREMENT_DATA_MAX_AGE_DEFAULT + "}";
	public static final String INACTIVE_DEVICE_MAX_AGE = "inactive.device.max.age";
	public static final String $INACTIVE_DEVICE_MAX_AGE_WD = "${" + INACTIVE_DEVICE_MAX_AGE + ":" + DeviceQoSEvaluatorDefaults.INACTIVE_DEVICE_MAX_AGE_DEFAULT + "}";

	// API related

	public static final String HTTP_API_BASE_PATH = "/deviceqosevaluator";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_QUALITY_EVALUATION_PATH = HTTP_API_BASE_PATH + "/qualityevaluation";
	public static final String HTTP_API_DEVICE_QUALITY_DATA_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";

	public static final String HTTP_API_OP_FILTER_PATH = "/filter";
	public static final String HTTP_API_OP_SORT_PATH = "/sort";
	public static final String HTTP_API_OP_QUERY_PATH = "/query";
	public static final String HTTP_API_OP_RELOAD_PATH = "/reload";

	public static final String MQTT_API_BASE_TOPIC_PREFIX = "arrowhead/deviceqosevaluator";
	public static final String MQTT_API_MONITOR_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/monitor/";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/general/management/";
	public static final String MQTT_API_QUALITY_EVALUATION_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/qualityevaluation/";
	public static final String MQTT_API_DEVICE_QUALITY_DATA_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/management/";

	// Quartz related

	public static final String MEASUREMENT_ORGANIZER_JOB = "measurement_organizer_job";
	public static final String MEASUREMENT_ORGANIZER_JOB_TRIGGER = "measurement_organizer_job_trigger";
	public static final String CLEANING_JOB = "cleaning_job";
	public static final String CLEANING_JOB_TRIGGER = "cleaning_job_trigger";

	// Measurement related

	public static final int AUGMENTED_MEASUREMENT_PORT = 59473;
	public static final List<Double> NO_MEASUREMENT_VALUES = List.of(-1d, -1d, -1d, -1d, -1d);

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DeviceQoSEvaluatorConstants() {
		throw new UnsupportedOperationException();
	}
}
