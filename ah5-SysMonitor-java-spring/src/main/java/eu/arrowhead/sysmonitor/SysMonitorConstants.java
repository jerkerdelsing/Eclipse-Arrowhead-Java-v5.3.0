/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor;

import java.util.Set;

public final class SysMonitorConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "SysMonitor";
	public static final String SYSTEM_VERSION = "5.2.2";
	public static final String LOCAL_CLOUD_MANAGER = "LocalCloudManager";
	public static final String SYSOP = "Sysop";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.sysmonitor.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.sysmonitor.jpa.repository";

	// service definitions, operations and contracts (unchanged from the native 5.2.2 implementation and the SD/IDD documents)
	public static final String SERVICE_DEF_MANAGEMENT = "sysMonitorManagement";
	public static final String SERVICE_DEF_HISTORY = "sysMonitorHistory";
	public static final String SERVICE_DEF_DATA = "sysMonitorData";
	public static final String SERVICE_DEF_SYS_MONITOR = "sysMonitor";
	public static final String SERVICE_VERSION = "1.0.0";
	public static final String CONTRACT_MANAGEMENT = "SysMonitor-Management-HTTP-IDD-5.2.2";
	public static final String CONTRACT_HISTORY = "SysMonitor-History-HTTP-IDD-5.2.2";
	public static final String CONTRACT_DATA = "SysMonitor-Data-MQTT-IDD-5.2.2";

	public static final String OP_STATUS = "status";
	public static final String OP_PROVIDERS = "providers";
	public static final String OP_SUBSCRIPTIONS = "subscriptions";
	public static final String OP_SUBSCRIBE = "subscribe";
	public static final String OP_UNSUBSCRIBE = "unsubscribe";
	public static final String OP_RECONCILE = "reconcile";
	public static final String OP_LIST_SYSTEMS = "list-systems";
	public static final String OP_QUERY = "query";
	public static final String OP_LATEST = "latest";

	// HTTP paths
	public static final String HTTP_API_BASE_PATH = "/sysmonitor";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/management";
	public static final String HTTP_API_HISTORY_PATH = HTTP_API_BASE_PATH + "/history";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt/general";
	public static final String HTTP_API_OP_STATUS_PATH = "/status";
	public static final String HTTP_API_OP_PROVIDERS_PATH = "/providers";
	public static final String HTTP_API_OP_SUBSCRIPTIONS_PATH = "/subscriptions";
	public static final String HTTP_API_OP_SUBSCRIPTION_PATH = "/subscriptions/{providerSystem}";
	public static final String HTTP_API_OP_RECONCILE_PATH = "/reconcile";
	public static final String HTTP_API_OP_SYSTEMS_PATH = "/systems";
	public static final String HTTP_API_OP_LATEST_PATH = "/systems/{systemName}/latest";
	public static final String HTTP_API_OP_OBSERVATIONS_PATH = "/systems/{systemName}/observations";
	public static final String HTTP_API_HEALTH_PATH = "/health";

	// AMQP intake and MQTT output (SysMonitor-AMQP-IDD-5.2.2 and SysMonitor-Data-MQTT-IDD-5.2.2)
	public static final String EXCHANGE = "arrowhead";
	public static final String INTERFACE_AMQPS = "generic_amqps";
	public static final String POLICY_CERT_AUTH = "CERT_AUTH";
	public static final int MAX_OBSERVATION_BYTES = 1048576;
	public static final int MAX_ID_LENGTH = 255;
	public static final int OUTBOX_BATCH = 50;
	public static final double OUTBOX_RETRY_SECONDS = 5;
	public static final int MQTT_QOS = 1;

	// management callers and certificate profiles that may call any service
	public static final Set<String> MANAGERS = Set.of(LOCAL_CLOUD_MANAGER, SYSOP);
	public static final Set<String> CALLER_PROFILES = Set.of("sy", "op");
	public static final long AUTHORIZATION_CACHE_MILLIS = 30000;

	// filter order: after authentication (15), before the management filter
	public static final int REQUEST_FILTER_ORDER_CALLER_PROFILE = 16;
	public static final int REQUEST_FILTER_ORDER_ACCESS = 17;

	// configuration (environment names stay as ${ENV:default} placeholders in config/application.properties)
	public static final String MQTT_ADDRESS = "sysmonitor.mqtt.address";
	public static final String $MQTT_ADDRESS_WD = "${" + MQTT_ADDRESS + ":" + SysMonitorDefaults.MQTT_ADDRESS_DEFAULT + "}";
	public static final String MQTT_PORT = "sysmonitor.mqtt.port";
	public static final String $MQTT_PORT_WD = "${" + MQTT_PORT + ":" + SysMonitorDefaults.MQTT_PORT_DEFAULT + "}";
	public static final String RETENTION_DAYS = "sysmonitor.retention.days";
	public static final String $RETENTION_DAYS_WD = "${" + RETENTION_DAYS + ":" + SysMonitorDefaults.RETENTION_DAYS_DEFAULT + "}";
	public static final String PRUNE_INTERVAL_SECONDS = "sysmonitor.prune.interval.seconds";
	public static final String $PRUNE_INTERVAL_MILLIS_WD = "#{${" + PRUNE_INTERVAL_SECONDS + ":" + SysMonitorDefaults.PRUNE_INTERVAL_SECONDS_DEFAULT + "} * 1000}";
	public static final String DEFAULT_SUBSCRIPTION = "sysmonitor.default.subscription";
	public static final String $DEFAULT_SUBSCRIPTION_WD = "${" + DEFAULT_SUBSCRIPTION + ":" + SysMonitorDefaults.DEFAULT_SUBSCRIPTION_DEFAULT + "}";
	public static final String ORCHESTRATION_INTERVAL_SECONDS = "sysmonitor.orchestration.interval.seconds";
	public static final String $ORCHESTRATION_INTERVAL_SECONDS_WD = "${" + ORCHESTRATION_INTERVAL_SECONDS + ":" + SysMonitorDefaults.ORCHESTRATION_INTERVAL_SECONDS_DEFAULT + "}";
	public static final String $ORCHESTRATION_INTERVAL_MILLIS_WD = "#{${" + ORCHESTRATION_INTERVAL_SECONDS + ":" + SysMonitorDefaults.ORCHESTRATION_INTERVAL_SECONDS_DEFAULT + "} * 1000}";
	public static final String OUTBOX_INTERVAL_MILLIS = "sysmonitor.outbox.interval.millis";
	public static final String $OUTBOX_INTERVAL_MILLIS_WD = "${" + OUTBOX_INTERVAL_MILLIS + ":" + SysMonitorDefaults.OUTBOX_INTERVAL_MILLIS_DEFAULT + "}";
	public static final String CONNECT_INTERVAL_SECONDS = "sysmonitor.connect.interval.seconds";
	public static final String $CONNECT_INTERVAL_MILLIS_WD = "#{${" + CONNECT_INTERVAL_SECONDS + ":" + SysMonitorDefaults.CONNECT_INTERVAL_SECONDS_DEFAULT + "} * 1000}";
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":true}";
	public static final String $CLOUD_WD = "${sysmonitor.local.cloud:${arrowhead.cloud.name:TestCloud}}";
	public static final String CRL_FILE = "arrowhead.ssl.crl-file";
	public static final String $CRL_FILE_WD = "${" + CRL_FILE + ":}";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SysMonitorConstants() {
		throw new UnsupportedOperationException();
	}
}
