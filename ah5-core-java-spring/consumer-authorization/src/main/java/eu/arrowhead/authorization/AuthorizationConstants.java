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
package eu.arrowhead.authorization;

public final class AuthorizationConstants {

	//=================================================================================================
	// members

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.authorization.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.authorization.jpa.repository";

	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_AUTHORIZATION = "1.0.0";
	public static final String VERSION_AUTHORIZATION_TOKEN = "1.0.0";
	public static final String VERSION_AUTHORIZATION_MANAGEMENT = "1.0.0";
	public static final String VERSION_AUTHORIZATION_TOKEN_MANAGEMENT = "1.0.0";

	public static final String HTTP_API_BASE_PATH = "/consumerauthorization";
	public static final String HTTP_API_AUTHORIZATION_PATH = HTTP_API_BASE_PATH + "/authorization";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_AUTHORIZATION_PATH + "/mgmt";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_AUTHORIZATION_TOKEN_PATH = HTTP_API_BASE_PATH + "/authorization-token";
	public static final String HTTP_API_TOKEN_SUB_PATH = "/token";

	public static final String MQTT_API_BASE_TOPIC_PREFIX = "arrowhead/consumer-authorization";
	public static final String MQTT_API_AUTHORIZATION_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/authorization/";
	public static final String MQTT_API_MANAGEMENT_BASE_TOPIC = MQTT_API_AUTHORIZATION_BASE_TOPIC + "management/";
	public static final String MQTT_API_MONITOR_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/monitor/";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/general/management/";
	public static final String MQTT_API_AUTHORIZATION_TOKEN_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/authorization-token/";
	public static final String MQTT_API_TOKEN_MANAGEMENT_BASE_TOPIC = MQTT_API_AUTHORIZATION_TOKEN_BASE_TOPIC + "management/";


	public static final String HTTP_PATH_PARAM_TOKEN = "{token}";
	public static final String HTTP_PARAM_INSTANCE_ID = "{instanceId}";

	public static final String HTTP_API_OP_GRANT_PATH = "/grant";
	public static final String HTTP_API_OP_REVOKE_PATH = "/revoke";
	public static final String HTTP_API_OP_SERVICE_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/" + HTTP_PARAM_INSTANCE_ID;
	public static final String HTTP_API_OP_LOOKUP_PATH = "/lookup";
	public static final String HTTP_API_OP_VERIFY_PATH = "/verify";
	public static final String HTTP_API_OP_TOKEN_VERIFY_PATH = "/token" + HTTP_API_OP_VERIFY_PATH + "/" + HTTP_PATH_PARAM_TOKEN;
	public static final String HTTP_API_OP_QUERY_PATH = "/query";
	public static final String HTTP_API_OP_CHECK_PATH = "/check";
	public static final String HTTP_API_OP_GENERATE_PATH = "/generate";
	public static final String HTTP_API_OP_PUBLIC_KEY_PATH = "/public-key";
	public static final String HTTP_API_OP_ENCRYPTION_KEY_PATH = "/encryption-key";

	public static final String INSTANCE_ID_PREFIX_MGMT = "MGMT";
	public static final String INSTANCE_ID_PREFIX_PR = "PR";

	// Configuration related

	public static final String TOKEN_MAX_AGE = "token.max.age";
	public static final String $TOKEN_MAX_AGE_WD = "${" + TOKEN_MAX_AGE + ":" + AuthorizationDefaults.DEFAULT_TOKEN_MAX_AGE + "}";
	public static final String TOKEN_TIME_LIMIT = "token.time.limit";
	public static final String $TOKEN_TIME_LIMIT_WD = "${" + TOKEN_TIME_LIMIT + ":" + AuthorizationDefaults.DEFAULT_TOKEN_TIME_LIMIT + "}";
	public static final String SIMPLE_TOKEN_BYTE_SIZE = "simple.token.byte.size";
	public static final String $SIMPLE_TOKEN_BYTE_SIZE_WD = "${" + SIMPLE_TOKEN_BYTE_SIZE + ":" + AuthorizationDefaults.DEFAULT_SIMPLE_TOKEN_BYTE_SIZE + "}";
	public static final String SIMPLE_TOKEN_USAGE_LIMIT = "simple.token.usage.limit";
	public static final String $SIMPLE_TOKEN_USAGE_LIMIT_WD = "${" + SIMPLE_TOKEN_USAGE_LIMIT + ":" + AuthorizationDefaults.DEFAULT_SIMPLE_TOKEN_USAGE_LIMIT + "}";
	public static final String SECRET_CRYPTOGRAPHER_KEY = "secret.cryptographer.key";
	public static final String $SECRET_CRYPTOGRAPHER_KEY = "${" + SECRET_CRYPTOGRAPHER_KEY + "}";
	public static final String UNBOUNDED_TOKEN_GENERATION_WHITELIST = "unbounded.token.generation.whitelist";
	public static final String $UNBOUNDED_TOKEN_GENERATION_WHITELIST_WD = "${" + UNBOUNDED_TOKEN_GENERATION_WHITELIST + ":" + AuthorizationDefaults.DEFAULT_UNBOUNDED_TOKEN_GENERATION_WHITELIST + "}";
	public static final String CLEANER_JOB_INTERVAL = "cleaner.job.interval";
	public static final String $CLEANER_JOB_INTERVAL_WD = "${" + CLEANER_JOB_INTERVAL + ":" + AuthorizationDefaults.CLEANER_JOB_INTERVAL_DEFAULT + "}";

	// Quartz related
	public static final String CLEANER_TRIGGER = "consumerAuthorizationCleanerTrigger";
	public static final String CLEANER_JOB = "consumerAuthorizationCleanerJob";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationConstants() {
		throw new UnsupportedOperationException();
	}
}