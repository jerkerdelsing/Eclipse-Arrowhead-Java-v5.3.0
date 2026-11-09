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
package eu.arrowhead.serviceorchestration;

import eu.arrowhead.dto.enums.OrchestrationFlag;

import java.util.List;

public final class SimpleStoreServiceOrchestrationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "SimpleStoreServiceOrchestration";

	// DB
	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.repository";

	public static final String METADATA_KEY_ORCHESTRATION_STRATEGY = "orchestrationStrategy";
	public static final String METADATA_VALUE_ORCHESTRATION_STRATEGY = "simpleStore";

	// HTTP API
	public static final String HTTP_API_BASE_PATH = "/serviceorchestration";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_ORCHESTRATION_PATH = HTTP_API_BASE_PATH + "/orchestration";
	public static final String HTTP_API_ORCHESTRATION_MGMT_PREFIX = HTTP_API_ORCHESTRATION_PATH + "/mgmt";
	public static final String HTTP_API_ORCHESTRATION_STORE_MANAGEMENT_PATH = HTTP_API_ORCHESTRATION_MGMT_PREFIX + "/simple-store";
	public static final String HTTP_API_OP_PUSH_SUBSCRIBE_PATH = "/subscribe";
	public static final String HTTP_PATH_PARAM_ID = "{id}";
	public static final String HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH = "/unsubscribe/" + HTTP_PATH_PARAM_ID;
	public static final String PARAM_NAME_TRIGGER = "trigger";
	public static final String HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH = HTTP_API_ORCHESTRATION_MGMT_PREFIX + "/push";
	public static final String HTTP_API_ORCHESTRATION_HISTORY_MANAGEMENT_PATH = HTTP_API_ORCHESTRATION_MGMT_PREFIX + "/history";
	public static final String HTTP_API_OP_PUSH_TRIGGER_PATH = "/trigger";
	public static final String HTTP_API_OP_PUSH_UNSUBSCRIBE_BULK_PATH = "/unsubscribe";

	// MQTT API
	public static final String MQTT_API_BASE_TOPIC_PREFIX = "arrowhead/serviceorchestration";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/general/management/";
	public static final String MQTT_API_ORCHESTRATION_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/orchestration/";
	public static final String MQTT_API_ORCHESTRATION_MANAGEMENT_PREFIX = MQTT_API_ORCHESTRATION_BASE_TOPIC + "management";
	public static final String MQTT_API_ORCHESTRATION_PUSH_MANAGEMENT_BASE_TOPIC = MQTT_API_ORCHESTRATION_MANAGEMENT_PREFIX + "/push/";
	public static final String MQTT_API_ORCHESTRATION_HISTORY_MANAGEMENT_BASE_TOPIC = MQTT_API_ORCHESTRATION_MANAGEMENT_PREFIX + "/history/";
	public static final String MQTT_API_ORCHESTRATION_STORE_MANAGEMENT_BASE_TOPIC = MQTT_API_ORCHESTRATION_MANAGEMENT_PREFIX + "/store/";


	// service version
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_ORCHESTRATION = "1.0.0";
	public static final String VERSION_ORCHESTRATION_STORE_MANAGEMENT = "1.0.0";
	public static final String VERSION_ORCHESTRATION_PUSH_MANAGEMENT = "1.0.0";
	public static final String VERSION_ORCHESTRATION_HISTORY_MANAGEMENT = "1.0.0";

	// operation
	public static final String HTTP_API_OP_CREATE_PATH = "/create";
	public static final String HTTP_API_OP_QUERY_PATH = "/query";
	public static final String HTTP_API_OP_MODIFY_PRIORITIES_PATH = "/modify-priorities";
	public static final String HTTP_API_OP_REMOVE_PATH = "/remove";
	public static final String HTTP_API_OP_PULL_PATH = "/pull";

	// ignored fields and flags
	public static final String ORCH_WARN_IGNORED_FIELDS_KEY = "ignoredFields";
	public static final String ORCH_WARN_IGNORED_FLAGS_KEY = "ignoredFlags";
	public static final String FIELD_OPERATIONS = "operations";
	public static final String FIELD_VERSIONS = "versions";
	public static final String FIELD_ALIVES_AT = "alivesAt";
	public static final String FIELD_METADATA_REQ = "metadataRequirements";
	public static final String FIELD_INTF_TEMPLATE_NAMES = "interfaceTemplateNames";
	public static final String FIELD_INTF_ADDRESS_TYPES = "interfaceAddressTypes";
	public static final String FIELD_INTF_PROP_REQ = "interfacePropertyRequirements";
	public static final String FIELD_SECURITY_POLICIES = "securityPolicies";
	public static final String FIELD_QUALITY_REQ = "qualityRequirements";
	public static final String FIELD_EXCLUSIVITY_DURATION = "exclusivityDuration";
	public static final List<String> SUPPORTED_FLAGS = List.of(
			OrchestrationFlag.MATCHMAKING.toString(),
			OrchestrationFlag.ONLY_PREFERRED.toString());

	// notify interface
	public static final String NOTIFY_KEY_ADDRESS = "address";
	public static final String NOTIFY_KEY_PORT = "port";
	public static final String NOTIFY_KEY_METHOD = "method";
	public static final String NOTIFY_KEY_PATH = "path";
	public static final String NOTIFY_KEY_TOPIC = "topic";

	// Quartz related
	public static final String CLEANER_TRIGGER = "simpleStoreOrchestrationCleanerTrigger";
	public static final String CLEANER_JOB = "simpleStoreOrchestrationCleanerJob";

	public static final Object SYNC_LOCK_SUBSCRIPTION = new Object();
	public static final String JOB_QUEUE_PUSH_ORCHESTRATION = "jobQueuePushOrchestration";

	public static final String PUSH_ORCHESTRATION_MAX_THREAD = "push.orchestration.max.thread";
	public static final String CLEANER_JOB_INTERVAL = "cleaner.job.interval";
	public static final String $CLEANER_JOB_INTERVAL_WD = "${" + CLEANER_JOB_INTERVAL + ":" + SimpleStoreServiceOrchestrationDefaults.CLEANER_JOB_INTERVAL_DEFAULT + "}";
	public static final String ORCHESTRATION_HISTORY_MAX_AGE = "orchestration.history.max.age";
	public static final String $PUSH_ORCHESTRATION_MAX_THREAD_WD = "${" + PUSH_ORCHESTRATION_MAX_THREAD + ":" + SimpleStoreServiceOrchestrationDefaults.PUSH_ORCHESTRATION_MAX_THREAD_DEFAULT + "}";
	public static final String $ORCHESTRATION_HISTORY_MAX_AGE_WD = "${" + ORCHESTRATION_HISTORY_MAX_AGE + ":" + SimpleStoreServiceOrchestrationDefaults.ORCHESTRATION_HISTORY_MAX_AGE_DEFAULT + "}";


	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SimpleStoreServiceOrchestrationConstants() {
		throw new UnsupportedOperationException();
	}
}
