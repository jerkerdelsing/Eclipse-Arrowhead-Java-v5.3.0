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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class DynamicServiceOrchestrationSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	@Value(Constants.$ENABLE_BLACKLIST_FILTER_WD)
	private boolean enableBlackistFilter;

	@Value(Constants.$FORCE_BLACKLIST_FILTER_WD)
	private boolean forceBlackistFilter;

	@Value(DynamicServiceOrchestrationConstants.$ENABLE_AUTHORIZATION_WD)
	private boolean enableAuthorization;

	@Value(DynamicServiceOrchestrationConstants.$ENABLE_TRANSLATION_WD)
	private boolean enableTranslation;

	@Value(DynamicServiceOrchestrationConstants.$ENABLE_QOS_WD)
	private boolean enableQoS;

	@Value(DynamicServiceOrchestrationConstants.$ENABLE_INTERCLOUD_WD)
	private boolean enableIntercloud;

	@Value(DynamicServiceOrchestrationConstants.$ORCHESTRATION_HISTORY_MAX_AGE_WD)
	private int orchestrationHistoryMaxAge;

	@Value(DynamicServiceOrchestrationConstants.$PUSH_ORCHESTRATION_MAX_THREAD_WD)
	private int pushOrchestrationMaxThread;

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION)
					.metadata(DynamicServiceOrchestrationConstants.METADATA_KEY_ORCHESTRATION_STRATEGY, DynamicServiceOrchestrationConstants.METADATA_VALUE_ORCHESTRATION_STRATEGY);

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
				.version(DynamicServiceOrchestrationConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.serviceInterface(getMqttServiceInterfaceForGeneralManagement())
				.build();

		final ServiceModel orchestration = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION)
				.version(DynamicServiceOrchestrationConstants.VERSION_ORCHESTRATION)
				.metadata(DynamicServiceOrchestrationConstants.METADATA_KEY_ORCHESTRATION_STRATEGY, DynamicServiceOrchestrationConstants.METADATA_VALUE_ORCHESTRATION_STRATEGY)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForOrchestration())
				.serviceInterface(getMqttServiceInterfaceForOrchestration())
				.build();

		final ServiceModel orchestrationPushManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION_PUSH_MANAGEMENT)
				.version(DynamicServiceOrchestrationConstants.VERSION_ORCHESTRATION_PUSH_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForOrchestrationPushManagement())
				.serviceInterface(getMqttServiceInterfaceForOrchestrationPushManagement())
				.build();

		final ServiceModel orchestrationLockManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION_LOCK_MANAGEMENT)
				.version(DynamicServiceOrchestrationConstants.VERSION_ORCHESTRATION_LOCK_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForOrchestrationLockManagement())
				.serviceInterface(getMqttServiceInterfaceForOrchestrationLockManagement())
				.build();

		final ServiceModel orchestrationHistoryManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION_HISTORY_MANAGEMENT)
				.version(DynamicServiceOrchestrationConstants.VERSION_ORCHESTRATION_HISTORY_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForOrchestrationHistoryManagement())
				.serviceInterface(getMqttServiceInterfaceForOrchestrationHistoryManagement())
				.build();

        final ServiceModel plantManagement = new ServiceModel.Builder()
                .serviceDefinition("serviceOrchestrationPlantManagement")
                .version("1.0.0")
                .serviceInterface(getHttpServiceInterfaceForPlantManagement())
                .build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, orchestrationPushManagement, orchestrationLockManagement, orchestrationHistoryManagement, plantManagement, orchestration);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isBlacklistEnabled() {
		return enableBlackistFilter;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isBlacklistForced() {
		return forceBlackistFilter;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isAuthorizationEnabled() {
		return enableAuthorization;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isTranslationEnabled() {
		return enableTranslation;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isQoSEnabled() {
		return enableQoS;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isIntercloudEnabled() {
		return enableIntercloud;
	}

	//-------------------------------------------------------------------------------------------------
	public int getOrchestrationHistoryMaxAge() {
		return orchestrationHistoryMaxAge;
	}

	//-------------------------------------------------------------------------------------------------
	public int getPushOrchestrationMaxThread() {
		return pushOrchestrationMaxThread;
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
						Constants.SERVICE_ADDRESS_ALIAS,
						DynamicServiceOrchestrationConstants.ENABLE_AUTHORIZATION,
						DynamicServiceOrchestrationConstants.ENABLE_INTERCLOUD,
						DynamicServiceOrchestrationConstants.ENABLE_QOS,
						DynamicServiceOrchestrationConstants.ENABLE_TRANSLATION),
				DynamicServiceOrchestrationDefaults.class);
	}

	// HTTP Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForGeneralManagement() {
		return getHttpServiceInterfaceForAGeneralManagementService(DynamicServiceOrchestrationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestration() {
		return getHttpServiceInterfaceForAnOrchestrationService(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestrationPushManagement() {
		return getHttpServiceInterfaceForAnOrchestrationPushManagementService(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestrationLockManagement() {
		return getHttpServiceInterfaceForAnOrchestrationLockManagementService(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_LOCK_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestrationHistoryManagement() {
		return getHttpServiceInterfaceForAnOrchestrationHistoryManagementService(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_HISTORY_MANAGEMENT_PATH);
	}

    private InterfaceModel getHttpServiceInterfaceForPlantManagement() {
        final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
        final HttpOperationModel query = new HttpOperationModel.Builder().method(HttpMethod.POST.name()).path("/query").build();
        final HttpOperationModel replace = new HttpOperationModel.Builder().method(HttpMethod.PUT.name()).path("/replace").build();
        return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
                .basePath("/serviceorchestration/mgmt/plant-rules")
                .operation("query", query)
                .operation("replace", replace)
                .build();
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
				.basePath(DynamicServiceOrchestrationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel pull = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PULL_PATH)
				.build();
		final HttpOperationModel pushSubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH)
				.build();
		final HttpOperationModel pushUnsubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH)
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
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH)
				.build();
		final HttpOperationModel pushUnsubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_BULK_PATH)
				.build();
		final HttpOperationModel pushTrigger = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_TRIGGER_PATH)
				.build();
		final HttpOperationModel pushQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH)
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
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationLockManagementService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel lockQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH)
				.build();
		final HttpOperationModel lockRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_REMOVE_LOCK_PATH)
				.build();
		final HttpOperationModel lockCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_CREATE_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_QUERY, lockQuery)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_REMOVE, lockRemove)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_CREATE, lockCreate)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationHistoryManagementService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel historyQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH)
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
				.baseTopic(DynamicServiceOrchestrationConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC)
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
				.baseTopic(DynamicServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_BASE_TOPIC)
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
				.baseTopic(DynamicServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_PUSH_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE, Constants.SERVICE_OP_ORCHESTRATION_TRIGGER, Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE, Constants.SERVICE_OP_ORCHESTRATION_QUERY))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForOrchestrationLockManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(DynamicServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_LOCK_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_ORCHESTRATION_CREATE, Constants.SERVICE_OP_ORCHESTRATION_QUERY, Constants.SERVICE_OP_ORCHESTRATION_REMOVE))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForOrchestrationHistoryManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(DynamicServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_HISTORY_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_ORCHESTRATION_QUERY))
				.build();
	}
}
