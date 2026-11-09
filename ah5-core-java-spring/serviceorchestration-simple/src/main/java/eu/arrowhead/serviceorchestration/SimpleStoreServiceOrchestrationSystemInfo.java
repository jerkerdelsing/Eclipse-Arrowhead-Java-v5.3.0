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

import java.util.List;
import java.util.Set;

import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class SimpleStoreServiceOrchestrationSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	@Value(SimpleStoreServiceOrchestrationConstants.$PUSH_ORCHESTRATION_MAX_THREAD_WD)
	private int pushOrchestrationMaxThread;

	@Value(SimpleStoreServiceOrchestrationConstants.$ORCHESTRATION_HISTORY_MAX_AGE_WD)
	private int orchestrationHistoryMaxAge;

	//=================================================================================================
	// methods

	@Override
	//-------------------------------------------------------------------------------------------------
	public String getSystemName() {
		return SimpleStoreServiceOrchestrationConstants.SYSTEM_NAME;
	}

	@Override
	//-------------------------------------------------------------------------------------------------
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION);

			if (AuthenticationPolicy.CERTIFICATE == this.getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(SimpleStoreServiceOrchestrationConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.serviceInterface(getMqttServiceInterfaceForGeneralManagement())
				.build();

		final ServiceModel orchestration = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION)
				.version(SimpleStoreServiceOrchestrationConstants.VERSION_ORCHESTRATION)
				.metadata(SimpleStoreServiceOrchestrationConstants.METADATA_KEY_ORCHESTRATION_STRATEGY, SimpleStoreServiceOrchestrationConstants.METADATA_VALUE_ORCHESTRATION_STRATEGY)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForOrchestration())
				.serviceInterface(getMqttServiceInterfaceForOrchestration())
				.build();

		final ServiceModel orchestrationStoreManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION_SIMPLE_STORE_MANAGEMENT)
				.version(SimpleStoreServiceOrchestrationConstants.VERSION_ORCHESTRATION_STORE_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForOrchestrationStoreManagement())
				.serviceInterface(getMqttServiceInterfaceForOrchestrationStoreManagement())
				.build();

		final ServiceModel orchestrationPushManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION_PUSH_MANAGEMENT)
				.version(SimpleStoreServiceOrchestrationConstants.VERSION_ORCHESTRATION_PUSH_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForOrchestrationPushManagement())
				.serviceInterface(getMqttServiceInterfaceForOrchestrationPushManagement())
				.build();

		final ServiceModel orchestrationHistoryManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION_HISTORY_MANAGEMENT)
				.version(SimpleStoreServiceOrchestrationConstants.VERSION_ORCHESTRATION_HISTORY_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForOrchestrationHistoryManagement())
				.serviceInterface(getMqttServiceInterfaceForOrchestrationHistoryManagement())
				.build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, orchestrationPushManagement, orchestrationStoreManagement, orchestrationHistoryManagement, orchestration);
	}

	//-------------------------------------------------------------------------------------------------
	public int getPushOrchestrationMaxThread() {
		return pushOrchestrationMaxThread;
	}

	//-------------------------------------------------------------------------------------------------
	public int getOrchestrationHistoryMaxAge() {
		return orchestrationHistoryMaxAge;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.MQTT_API_ENABLED,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY,
						Constants.ENABLE_BLACKLIST_FILTER,
						Constants.FORCE_BLACKLIST_FILTER,
						Constants.ALLOW_SELF_ADDRESSING,
						Constants.ALLOW_NON_ROUTABLE_ADDRESSING,
						Constants.MAX_PAGE_SIZE,
						Constants.NORMALIZATION_MODE,
						Constants.SERVICE_ADDRESS_ALIAS
				),
				SimpleStoreServiceOrchestrationDefaults.class);
	}

	// HTTP Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForGeneralManagement() {
		return getHttpServiceInterfaceForAGeneralManagementService(SimpleStoreServiceOrchestrationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestration() {
		return getHttpServiceInterfaceForAnOrchestrationService(SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestrationPushManagement() {
		return getHttpServiceInterfaceForAnOrchestrationPushManagementService(SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestrationStoreManagement() {
		return getHttpServiceInterfaceForAnOrchestrationStoreManagementService(SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_STORE_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestrationHistoryManagement() {
		return getHttpServiceInterfaceForAnOrchestrationHistoryManagementService(SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_HISTORY_MANAGEMENT_PATH);
	}

	// HTTP Interface Operations

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAGeneralManagementService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel log = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(Constants.HTTP_API_OP_LOGS_PATH)
				.build();

		final HttpOperationModel config = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(Constants.HTTP_API_OP_GET_CONFIG_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(SimpleStoreServiceOrchestrationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel pull = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PULL_PATH)
				.build();
		final HttpOperationModel pushSubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH)
				.build();
		final HttpOperationModel pushUnsubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_PULL, pull)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE, pushSubscribe)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE, pushUnsubscribe)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationPushManagementService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel pushSubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH)
				.build();
		final HttpOperationModel pushUnsubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_BULK_PATH)
				.build();
		final HttpOperationModel pushTrigger = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_TRIGGER_PATH)
				.build();
		final HttpOperationModel pushQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE, pushSubscribe)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE, pushUnsubscribe)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_TRIGGER, pushTrigger)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_QUERY, pushQuery)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationStoreManagementService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel query = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH)
				.build();
		final HttpOperationModel create = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_CREATE_PATH)
				.build();
		final HttpOperationModel modifyPriorities = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_MODIFY_PRIORITIES_PATH)
				.build();
		final HttpOperationModel remove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_REMOVE_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_QUERY, query)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_REMOVE, remove)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_CREATE, create)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_MODIFY_PRIORITIES, modifyPriorities)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationHistoryManagementService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel historyQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_QUERY, historyQuery)
				.build();
	}

	// MQTT Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForGeneralManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(SimpleStoreServiceOrchestrationConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GET_LOG, Constants.SERVICE_OP_GET_CONFIG))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForOrchestration() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(SimpleStoreServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_ORCHESTRATION_PULL, Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE, Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForOrchestrationPushManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(SimpleStoreServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_PUSH_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE, Constants.SERVICE_OP_ORCHESTRATION_TRIGGER, Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE, Constants.SERVICE_OP_ORCHESTRATION_QUERY))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForOrchestrationStoreManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(SimpleStoreServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_STORE_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_ORCHESTRATION_CREATE, Constants.SERVICE_OP_ORCHESTRATION_QUERY, Constants.SERVICE_OP_ORCHESTRATION_REMOVE, Constants.SERVICE_OP_ORCHESTRATION_MODIFY_PRIORITIES))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForOrchestrationHistoryManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(SimpleStoreServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_HISTORY_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_ORCHESTRATION_QUERY))
				.build();
	}
}
