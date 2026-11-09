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
package eu.arrowhead.serviceregistry;

public final class ServiceRegistryConstants {

	//=================================================================================================
	// members

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceregistry.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceregistry.jpa.repository";

	public static final String REQUEST_ATTR_RESTRICTED_SERVICE_LOOKUP = "restricted.service.lookup";
	public static final int REQUEST_FILTER_ORDER_SERVICE_LOOKUP = 35;

	public static final String HTTP_API_BASE_PATH = "/serviceregistry";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_DEVICE_DISCOVERY_PATH = HTTP_API_BASE_PATH + "/device-discovery";
	public static final String HTTP_API_SERVICE_DISCOVERY_PATH = HTTP_API_BASE_PATH + "/service-discovery";
	public static final String HTTP_API_SYSTEM_DISCOVERY_PATH = HTTP_API_BASE_PATH + "/system-discovery";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";

	public static final String MQTT_API_BASE_TOPIC_PREFIX = "arrowhead/serviceregistry";
	public static final String MQTT_API_MONITOR_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/monitor/";
	public static final String MQTT_API_DEVICE_DISCOVERY_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/device-discovery/";
	public static final String MQTT_API_SERVICE_DISCOVERY_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/service-discovery/";
	public static final String MQTT_API_SYSTEM_DISCOVERY_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/system-discovery/";
	public static final String MQTT_API_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/management/";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/general/management/";

	public static final String VERSION_DEVICE_DISCOVERY = "1.0.0";
	public static final String VERSION_SYSTEM_DISCOVERY = "1.0.0";
	public static final String VERSION_SERVICE_DISCOVERY = "1.0.0";
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_SERVICE_REGISTRY_MANAGEMENT = "1.0.0";

	// Operation related

	public static final String HTTP_API_OP_DEVICE_PATH = "/devices";
	public static final String HTTP_API_OP_DEVICE_QUERY_PATH = HTTP_API_OP_DEVICE_PATH + "/query";
	public static final String HTTP_API_OP_REGISTER_PATH = "/register";
	public static final String HTTP_API_OP_LOOKUP_PATH = "/lookup";
	public static final String HTTP_API_OP_REVOKE_PATH = "/revoke";
	public static final String HTTP_PARAM_NAME = "{name}";
	public static final String HTTP_API_OP_DEVICE_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/" + HTTP_PARAM_NAME;
	public static final String HTTP_API_OP_SERVICE_DEFINITION_PATH = "/service-definitions";
	public static final String HTTP_API_OP_SERVICE_DEFINITION_QUERY_PATH = HTTP_API_OP_SERVICE_DEFINITION_PATH + "/query";
	public static final String HTTP_API_OP_SYSTEM_PATH = "/systems";
	public static final String HTTP_API_OP_SYSTEM_QUERY_PATH = HTTP_API_OP_SYSTEM_PATH + "/query";
	public static final String HTTP_PARAM_INSTANCE_ID = "{instanceId}";
	public static final String HTTP_API_OP_SERVICE_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/" + HTTP_PARAM_INSTANCE_ID;
	public static final String HTTP_API_OP_SERVICE_INSTANCE_PATH = "/service-instances";
	public static final String HTTP_API_OP_SERVICE_INSTANCE_QUERY_PATH = HTTP_API_OP_SERVICE_INSTANCE_PATH + "/query";
	public static final String HTTP_API_OP_INTERFACE_TEMPLATE_PATH = "/interface-templates";
	public static final String HTTP_API_OP_INTERFACE_TEMPLATE_QUERY_PATH = HTTP_API_OP_INTERFACE_TEMPLATE_PATH + "/query";

	// Configuration related

	public static final String DISCOVERY_VERBOSE = "discovery.verbose";
	public static final String $DISCOVERY_VERBOSE_WD = "${" + DISCOVERY_VERBOSE + ":" + ServiceRegistryDefaults.DISCOVERY_VERBOSE_DEFAULT + "}";
	public static final String SERVICE_DISCOVERY_POLICY = "service.discovery.policy";
	// JSON list of additional interfaces for the ServiceRegistry's own services, registered at every start-up:
	// [{"serviceDefinition":"systemDiscovery","templateName":"generic_https","protocol":"https","policy":"CERT_AUTH",
	//   "address":"authentication-registry-mtls","port":8443}]; base path and operations are taken from the native interface.
	public static final String EXTRA_INTERFACES = "service.registry.extra.interfaces";
	public static final String $EXTRA_INTERFACES_WD = "${" + EXTRA_INTERFACES + ":" + ServiceRegistryDefaults.EXTRA_INTERFACES_DEFAULT + "}";
	public static final String $SERVICE_DISCOVERY_POLICY_WD = "${" + SERVICE_DISCOVERY_POLICY + ":" + ServiceRegistryDefaults.SERVICE_DISCOVERY_POLICY_DEFAULT + "}";
	public static final String SERVICE_DISCOVERY_DIRECT_ACCESS = "service.discovery.direct.access";
	public static final String $SERVICE_DISCOVERY_DIRECT_ACCESS_WD = "${" + SERVICE_DISCOVERY_DIRECT_ACCESS + ":" + ServiceRegistryDefaults.SERVICE_DISCOVERY_DIRECT_ACCESS_DEFAULT + "}";
	public static final String SERVICE_DISCOVERY_INTERFACE_POLICY = "service.discovery.interface.policy";
	public static final String $SERVICE_DISCOVERY_INTERFACE_POLICY_WD = "${" + SERVICE_DISCOVERY_INTERFACE_POLICY + ":" + ServiceRegistryDefaults.SERVICE_DISCOVERY_INTERFACE_POLICY_DEFAULT + "}";

	// Others

	public static final String INTERFACE_PROPERTY_VALIDATOR_DELIMITER = "|";
	public static final String INTERFACE_PROPERTY_VALIDATOR_DELIMITER_REGEXP = "\\|";
	public static final String VERBOSE_PARAM_DEFAULT = "false"; // this is the query parameter default, not the same as the configuration default

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryConstants() {
		throw new UnsupportedOperationException();
	}
}