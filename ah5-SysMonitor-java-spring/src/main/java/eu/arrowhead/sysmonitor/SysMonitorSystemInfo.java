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

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class SysMonitorSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	@Value(SysMonitorConstants.$DEFAULT_SUBSCRIPTION_WD)
	private String defaultSubscription;

	@Value(SysMonitorConstants.$RETENTION_DAYS_WD)
	private double retentionDays;

	@Value(SysMonitorConstants.$MQTT_ADDRESS_WD)
	private String mqttAddress;

	@Value(SysMonitorConstants.$MQTT_PORT_WD)
	private int mqttPort;

	@Value(SysMonitorConstants.$CLOUD_WD)
	private String cloud;

	@Value(Constants.$SYS_MONITOR_SSL_ENABLED_WD)
	private boolean brokerSsl;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return SysMonitorConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION)
					.metadata("role", "monitoring-collector")
					.metadata("security", "X.509");

			if (AuthenticationPolicy.CERTIFICATE == getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	// sysMonitor (the system's own echo/power/status) is registered by the common-library SysMonitorPublisher
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(SysMonitorConstants.SERVICE_VERSION)
				.metadata("systemVersion", SysMonitorConstants.SYSTEM_VERSION)
				.serviceInterface(http(SysMonitorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
						.operation(SysMonitorConstants.OP_STATUS, operation(HttpMethod.GET, SysMonitorConstants.HTTP_API_OP_STATUS_PATH))
						.operation(Constants.SERVICE_OP_GET_LOG, operation(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
						.operation(Constants.SERVICE_OP_GET_CONFIG, operation(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
						.build())
				.build();

		final ServiceModel management = new ServiceModel.Builder()
				.serviceDefinition(SysMonitorConstants.SERVICE_DEF_MANAGEMENT)
				.version(SysMonitorConstants.SERVICE_VERSION)
				.metadata("contract", SysMonitorConstants.CONTRACT_MANAGEMENT)
				.metadata("defaultSubscription", defaultSubscription)
				.serviceInterface(http(SysMonitorConstants.HTTP_API_MANAGEMENT_PATH)
						.operation(SysMonitorConstants.OP_STATUS, operation(HttpMethod.GET, SysMonitorConstants.HTTP_API_OP_STATUS_PATH))
						.operation(SysMonitorConstants.OP_PROVIDERS, operation(HttpMethod.GET, SysMonitorConstants.HTTP_API_OP_PROVIDERS_PATH))
						.operation(SysMonitorConstants.OP_SUBSCRIPTIONS, operation(HttpMethod.GET, SysMonitorConstants.HTTP_API_OP_SUBSCRIPTIONS_PATH))
						.operation(SysMonitorConstants.OP_SUBSCRIBE, operation(HttpMethod.PUT, SysMonitorConstants.HTTP_API_OP_SUBSCRIPTION_PATH))
						.operation(SysMonitorConstants.OP_UNSUBSCRIBE, operation(HttpMethod.DELETE, SysMonitorConstants.HTTP_API_OP_SUBSCRIPTION_PATH))
						.operation(SysMonitorConstants.OP_RECONCILE, operation(HttpMethod.POST, SysMonitorConstants.HTTP_API_OP_RECONCILE_PATH))
						.build())
				.build();

		final ServiceModel history = new ServiceModel.Builder()
				.serviceDefinition(SysMonitorConstants.SERVICE_DEF_HISTORY)
				.version(SysMonitorConstants.SERVICE_VERSION)
				.metadata("contract", SysMonitorConstants.CONTRACT_HISTORY)
				.metadata("retentionDays", retentionDays)
				.serviceInterface(http(SysMonitorConstants.HTTP_API_HISTORY_PATH)
						.operation(SysMonitorConstants.OP_LIST_SYSTEMS, operation(HttpMethod.GET, SysMonitorConstants.HTTP_API_OP_SYSTEMS_PATH))
						.operation(SysMonitorConstants.OP_QUERY, operation(HttpMethod.GET, SysMonitorConstants.HTTP_API_OP_OBSERVATIONS_PATH))
						.operation(SysMonitorConstants.OP_LATEST, operation(HttpMethod.GET, SysMonitorConstants.HTTP_API_OP_LATEST_PATH))
						.build())
				.build();

		final ServiceModel data = new ServiceModel.Builder()
				.serviceDefinition(SysMonitorConstants.SERVICE_DEF_DATA)
				.version(SysMonitorConstants.SERVICE_VERSION)
				.metadata("contract", SysMonitorConstants.CONTRACT_DATA)
				.metadata("schemaVersion", "1.0")
				.serviceInterface(new MqttInterfaceModel.Builder(brokerSsl ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME, mqttAddress, mqttPort)
						.baseTopic("arrowhead/" + cloud + "/sysmonitor/data")
						.operations(Set.of("subscribe"))
						.build())
				.build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, management, history, data);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY,
						Constants.NORMALIZATION_MODE,
						SysMonitorConstants.MQTT_ADDRESS,
						SysMonitorConstants.MQTT_PORT,
						SysMonitorConstants.RETENTION_DAYS,
						SysMonitorConstants.PRUNE_INTERVAL_SECONDS,
						SysMonitorConstants.DEFAULT_SUBSCRIPTION,
						SysMonitorConstants.ORCHESTRATION_INTERVAL_SECONDS),
				SysMonitorDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	private HttpInterfaceModel.Builder http(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort()).basePath(basePath);
	}

	//-------------------------------------------------------------------------------------------------
	private HttpOperationModel operation(final HttpMethod method, final String path) {
		return new HttpOperationModel.Builder().method(method.name()).path(path).build();
	}
}
