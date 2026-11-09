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
package eu.arrowhead.translationmanager;

public final class TranslationManagerConstants {

	//=================================================================================================
	// members

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.translationmanager.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.translationmanager.jpa.repository";

	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_TRANSLATION_REPORT = "1.0.0";
	public static final String VERSION_TRANSLATION_BRIDGE = "1.0.0";
	public static final String VERSION_TRANSLATION_BRIDGE_MANAGEMENT = "1.0.0";

	public static final String HTTP_API_BASE_PATH = "/translation";
	public static final String HTTP_API_REPORT_PATH = HTTP_API_BASE_PATH + "/event";
	public static final String HTTP_API_BRIDGE_PATH = HTTP_API_BASE_PATH + "/bridge";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_BRIDGE_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/bridge/mgmt";

	public static final String MQTT_API_BASE_TOPIC_PREFIX = "arrowhead/translation";
	public static final String MQTT_API_REPORT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/event/";
	public static final String MQTT_API_BRIDGE_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/bridge/";
	public static final String MQTT_API_MONITOR_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/monitor/";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/general/management/";
	public static final String MQTT_API_BRIDGE_MANAGEMENT_BASE_TOPIC = MQTT_API_BRIDGE_BASE_TOPIC + "management/";

	public static final String ALLOW_DISCOVERY_FLAGS = "allow.discovery.flags";
	public static final String $ALLOW_DISCOVERY_FLAGS_WD = "${" + ALLOW_DISCOVERY_FLAGS + ":" + TranslationManagerDefaults.ALLOW_DISCOVERY_FLAGS_DEFAULT + "}";
	public static final String ENABLE_AUTHORIZATION = "enable.authorization";
	public static final String $ENABLE_AUTHORIZATION_WD = "${" + ENABLE_AUTHORIZATION + ":" + TranslationManagerDefaults.ENABLE_AUTHORIZATION_DEFAULT + "}";
	public static final String TRANSLATOR_SERVICE_MIN_AVAILABILITY = "translator.service.min.availability";
	public static final String $TRANSLATOR_SERVICE_MIN_AVAILABILITY_WD = "${" + TRANSLATOR_SERVICE_MIN_AVAILABILITY + ":" + TranslationManagerDefaults.TRANSLATOR_SERVICE_MIN_AVAILABILITY_DEFAULT + "}";
	public static final String ENABLE_CUSTOM_CONFIGURATION = "enable.custom.configuration";
	public static final String $ENABLE_CUSTOM_CONFIGURATION_WD = "${" + ENABLE_CUSTOM_CONFIGURATION + ":" + TranslationManagerDefaults.ENABLE_CUSTOM_CONFIGURATION_DEFAULT + "}";
	public static final String TRANSLATION_DISCOVERY_MAX_AGE = "translation.discovery.max.age";
	public static final String $TRANSLATION_DISCOVERY_MAX_AGE_WD = "${" + TRANSLATION_DISCOVERY_MAX_AGE + ":" + TranslationManagerDefaults.TRANSLATION_DISCOVERY_MAX_AGE_DEFAULT + "}";
	public static final String CLEANER_JOB_INTERVAL = "cleaner.job.interval";
	public static final String $CLEANER_JOB_INTERVAL_WD = "${" + CLEANER_JOB_INTERVAL + ":" + TranslationManagerDefaults.CLEANER_JOB_INTERVAL_DEFAULT + "}";

	public static final String POLICY_TRANSLATION_BRIDGE_TOKEN_AUTH = "TRANSLATION_BRIDGE_TOKEN_AUTH";

	// operation related

	public static final String HTTP_API_OP_REPORT_PATH = "/report";
	public static final String HTTP_API_OP_DISCOVERY_PATH = "/discovery";
	public static final String HTTP_API_OP_NEGOTIATION_PATH = "/negotiation";
	public static final String HTTP_PARAM_BRIDGE_ID = "{bridgeId}";
	public static final String HTTP_API_OP_ABORT_PATH = "/abort";
	public static final String HTTP_API_OP_ABORT_PATH_WITH_PARAM = HTTP_API_OP_ABORT_PATH + "/" + HTTP_PARAM_BRIDGE_ID;
	public static final String HTTP_API_OP_QUERY_PATH = "/query";

	// Quartz related
	public static final String CLEANER_TRIGGER = "translationManagerCleanerTrigger";
	public static final String CLEANER_JOB = "translationManagerCleanerJob";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private TranslationManagerConstants() {
		throw new UnsupportedOperationException();
	}
}