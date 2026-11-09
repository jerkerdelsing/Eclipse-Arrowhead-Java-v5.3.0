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
public class TranslationManagerSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	@Value(Constants.$ENABLE_BLACKLIST_FILTER_WD)
	private boolean blacklistEnabled;

	@Value(Constants.$FORCE_BLACKLIST_FILTER_WD)
	private boolean forceBlackist;

	@Value(TranslationManagerConstants.$ALLOW_DISCOVERY_FLAGS_WD)
	private boolean allowDiscoveryFlags;

	@Value(TranslationManagerConstants.$ENABLE_AUTHORIZATION_WD)
	private boolean authorizationEnabled;

	@Value(TranslationManagerConstants.$ENABLE_CUSTOM_CONFIGURATION_WD)
	private boolean customConfigurationEnabled;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return Constants.SYS_NAME_TRANSLATION_MANAGER;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
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
		final ServiceModel translationReport = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_TRANSLATION_REPORT)
				.version(TranslationManagerConstants.VERSION_TRANSLATION_REPORT)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForTranslationReport())
				.serviceInterface(getMqttServiceInterfaceForTranslationReport())
				.build();

		final ServiceModel translationBridge = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_TRANSLATION_BRIDGE)
				.version(TranslationManagerConstants.VERSION_TRANSLATION_BRIDGE)
				.serviceInterface(getHttpServiceInterfaceForTranslationBridge())
				.serviceInterface(getMqttServiceInterfaceForTranslationBridge())
				.build();

		final ServiceModel translationBridgeManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT)
				.version(TranslationManagerConstants.VERSION_TRANSLATION_BRIDGE_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForTranslationBridgeManagement())
				.serviceInterface(getMqttServiceInterfaceForTranslationBridgeManagement())
				.build();

		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(TranslationManagerConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.serviceInterface(getMqttServiceInterfaceForGeneralManagement())
				.build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, translationBridgeManagement, translationBridge, translationReport);
	}
	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public boolean isBlacklistEnabled() {
		return blacklistEnabled;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isBlacklistForced() {
		return forceBlackist;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isDiscoveryFlagsAllowed() {
		return allowDiscoveryFlags;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isAuthorizationEnabled() {
		return authorizationEnabled;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isCustomConfigurationEnabled() {
		return customConfigurationEnabled;
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
						Constants.MAX_PAGE_SIZE,
						Constants.NORMALIZATION_MODE,
						TranslationManagerConstants.ALLOW_DISCOVERY_FLAGS,
						TranslationManagerConstants.ENABLE_AUTHORIZATION,
						TranslationManagerConstants.TRANSLATOR_SERVICE_MIN_AVAILABILITY,
						TranslationManagerConstants.ENABLE_CUSTOM_CONFIGURATION,
						TranslationManagerConstants.TRANSLATION_DISCOVERY_MAX_AGE,
						TranslationManagerConstants.CLEANER_JOB_INTERVAL),
				TranslationManagerDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForTranslationReport() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel report = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(TranslationManagerConstants.HTTP_API_OP_REPORT_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(TranslationManagerConstants.HTTP_API_REPORT_PATH)
				.operation(Constants.SERVICE_OP_REPORT, report)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForTranslationReport() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(TranslationManagerConstants.MQTT_API_REPORT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_REPORT))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForTranslationBridge() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel discovery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(TranslationManagerConstants.HTTP_API_OP_DISCOVERY_PATH)
				.build();

		final HttpOperationModel negotiation = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(TranslationManagerConstants.HTTP_API_OP_NEGOTIATION_PATH)
				.build();

		final HttpOperationModel abort = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(TranslationManagerConstants.HTTP_API_OP_ABORT_PATH_WITH_PARAM)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(TranslationManagerConstants.HTTP_API_BRIDGE_PATH)
				.operation(Constants.SERVICE_OP_DISCOVERY, discovery)
				.operation(Constants.SERVICE_OP_NEGOTIATION, negotiation)
				.operation(Constants.SERVICE_OP_ABORT, abort)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForTranslationBridge() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(TranslationManagerConstants.MQTT_API_BRIDGE_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_DISCOVERY, Constants.SERVICE_OP_NEGOTIATION, Constants.SERVICE_OP_ABORT))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForTranslationBridgeManagement() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel discovery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(TranslationManagerConstants.HTTP_API_OP_DISCOVERY_PATH)
				.build();

		final HttpOperationModel negotiation = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(TranslationManagerConstants.HTTP_API_OP_NEGOTIATION_PATH)
				.build();

		final HttpOperationModel abort = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(TranslationManagerConstants.HTTP_API_OP_ABORT_PATH)
				.build();

		final HttpOperationModel query = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(TranslationManagerConstants.HTTP_API_OP_QUERY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(TranslationManagerConstants.HTTP_API_BRIDGE_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_DISCOVERY, discovery)
				.operation(Constants.SERVICE_OP_NEGOTIATION, negotiation)
				.operation(Constants.SERVICE_OP_ABORT, abort)
				.operation(Constants.SERVICE_OP_QUERY, query)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForTranslationBridgeManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(TranslationManagerConstants.MQTT_API_BRIDGE_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_DISCOVERY, Constants.SERVICE_OP_NEGOTIATION, Constants.SERVICE_OP_ABORT, Constants.SERVICE_OP_QUERY))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForGeneralManagement() {
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
				.basePath(TranslationManagerConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForGeneralManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(TranslationManagerConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GET_LOG, Constants.SERVICE_OP_GET_CONFIG))
				.build();
	}
}