/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration;

public final class ConfigurationConstants {

	public static final String SYSTEM_NAME = "Configuration";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.configuration.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.configuration.jpa.repository";

	// v4.6 services conf/confraw, conf-by-proxy/save-conf-by-proxy and the management endpoints become three
	// v5 service definitions with kebab-case operations; the v4.6 URLs are kept.
	public static final String SERVICE_DEF_CONFIGURATION = "configuration";
	public static final String SERVICE_DEF_CONFIGURATION_PROXY = "configurationProxy";
	public static final String SERVICE_DEF_CONFIGURATION_MANAGEMENT = "configurationManagement";
	public static final String VERSION_CONFIGURATION = "1.0.0";
	public static final String VERSION_CONFIGURATION_PROXY = "1.0.0";
	public static final String VERSION_CONFIGURATION_MANAGEMENT = "1.0.0";
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";

	public static final String SERVICE_OP_GET_CONFIGURATION = "get-configuration";
	public static final String SERVICE_OP_GET_RAW_CONFIGURATION = "get-raw-configuration";
	public static final String SERVICE_OP_STORE_CONFIGURATION = "store-configuration";
	public static final String SERVICE_OP_LIST_CONFIGURATIONS = "list-configurations";
	public static final String SERVICE_OP_REMOVE_CONFIGURATION = "remove-configuration";

	public static final String PATH_VARIABLE_SYSTEM_NAME = "systemName";
	public static final String HTTP_API_BASE_PATH = "/configuration";
	public static final String HTTP_API_PROXY_PATH = HTTP_API_BASE_PATH + "/proxy";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_OP_CONFIG_PATH = "/config/{" + PATH_VARIABLE_SYSTEM_NAME + "}";
	public static final String HTTP_API_OP_RAW_CONFIG_PATH = "/config/raw/{" + PATH_VARIABLE_SYSTEM_NAME + "}";
	public static final String HTTP_API_OP_PROXY_SAVE_CONFIG_PATH = "/save-config";
	public static final String HTTP_API_OP_MGMT_CONFIGS_PATH = "/config";

	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":" + ConfigurationDefaults.SERVICE_AUTHORIZATION_ENABLED_DEFAULT + "}";
	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30000;
	public static final String CONFIGURATION_MAX_SIZE = "configuration.max.size";
	public static final String $CONFIGURATION_MAX_SIZE_WD = "${" + CONFIGURATION_MAX_SIZE + ":" + ConfigurationDefaults.CONFIGURATION_MAX_SIZE_DEFAULT + "}";

	private ConfigurationConstants() {
		throw new UnsupportedOperationException();
	}
}
