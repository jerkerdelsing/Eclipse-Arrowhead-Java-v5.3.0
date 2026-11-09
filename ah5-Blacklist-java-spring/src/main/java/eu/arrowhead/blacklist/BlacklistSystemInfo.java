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
package eu.arrowhead.blacklist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
public class BlacklistSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	private String httpTemplateName;
	private String mqttTemplateName;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return Constants.SYS_NAME_BLACKLIST;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		// starting with management services speeds up management filters
		return List.of(
				getGeneralManagementServiceModel(),
				getManagementServiceModel(),
				getDiscoveryServiceModel());
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

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit() {
		httpTemplateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		mqttTemplateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.MQTT_API_ENABLED,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY,
						Constants.MAX_PAGE_SIZE,
						Constants.NORMALIZATION_MODE,
						BlacklistConstants.WHITELIST),
				BlacklistDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel getDiscoveryServiceModel() {
		final List<InterfaceModel> discoveryInterfaces = new ArrayList<>();

		final HttpOperationModel httpCheckOp = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(BlacklistConstants.HTTP_API_OP_CHECK_PATH)
				.build();

		final HttpOperationModel httpLookupOp = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(BlacklistConstants.HTTP_API_OP_LOOKUP)
				.build();

		final HttpInterfaceModel httpDiscoveryIntf = new HttpInterfaceModel.Builder(httpTemplateName, getDomainAddress(), getServerPort())
				.basePath(BlacklistConstants.HTTP_API_BASE_PATH)
				.operation(Constants.SERVICE_OP_CHECK, httpCheckOp)
				.operation(Constants.SERVICE_OP_LOOKUP, httpLookupOp)
				.build();

		discoveryInterfaces.add(httpDiscoveryIntf);

		if (isMqttApiEnabled()) {
			final MqttInterfaceModel mqttDiscoveryIntf = new MqttInterfaceModel.Builder(mqttTemplateName, getMqttBrokerAddress(), getMqttBrokerPort())
					.baseTopic(BlacklistConstants.MQTT_API_DISCOVERY_BASE_TOPIC)
					.operations(Set.of(Constants.SERVICE_OP_CHECK, Constants.SERVICE_OP_LOOKUP))
					.build();

			discoveryInterfaces.add(mqttDiscoveryIntf);
		}

		return new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_BLACKLIST_DISCOVERY)
				.version(BlacklistConstants.VERSION_DISCOVERY)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterfaces(discoveryInterfaces)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel getManagementServiceModel() {
		final List<InterfaceModel> managementInterfaces = new ArrayList<>();

		final HttpOperationModel httpQueryOp = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(BlacklistConstants.HTTP_API_OP_QUERY)
				.build();

		final HttpOperationModel httpCreateOp = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(BlacklistConstants.HTTP_API_OP_CREATE)
				.build();

		final HttpOperationModel httpRemoveOp = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(BlacklistConstants.HTTP_API_OP_REMOVE)
				.build();

		final HttpInterfaceModel httpManagementIntf = new HttpInterfaceModel.Builder(httpTemplateName, getDomainAddress(), getServerPort())
				.basePath(BlacklistConstants.HTTP_API_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_BLACKLIST_QUERY, httpQueryOp)
				.operation(Constants.SERVICE_OP_BLACKLIST_CREATE, httpCreateOp)
				.operation(Constants.SERVICE_OP_BLACKLIST_REMOVE, httpRemoveOp)
				.build();

		managementInterfaces.add(httpManagementIntf);

		if (isMqttApiEnabled()) {
			final MqttInterfaceModel mqttManagementIntf = new MqttInterfaceModel.Builder(mqttTemplateName, getMqttBrokerAddress(), getMqttBrokerPort())
					.baseTopic(BlacklistConstants.MQTT_API_MANAGEMENT_BASE_TOPIC)
					.operations(Set.of(Constants.SERVICE_OP_BLACKLIST_CREATE, Constants.SERVICE_OP_BLACKLIST_QUERY, Constants.SERVICE_OP_BLACKLIST_REMOVE))
					.build();

			managementInterfaces.add(mqttManagementIntf);
		}

		return new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT)
				.version(BlacklistConstants.VERSION_MANAGEMENT)
				.serviceInterfaces(managementInterfaces)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel getGeneralManagementServiceModel() {
		final List<InterfaceModel> generalManagementInterfaces = new ArrayList<>();

		final HttpOperationModel httpLogOp = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(Constants.HTTP_API_OP_LOGS_PATH)
				.build();

		final HttpOperationModel httpConfigOp = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(Constants.HTTP_API_OP_GET_CONFIG_PATH)
				.build();

		final HttpInterfaceModel httpGeneralManagementIntf = new HttpInterfaceModel.Builder(httpTemplateName, getDomainAddress(), getServerPort())
				.basePath(BlacklistConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, httpLogOp)
				.operation(Constants.SERVICE_OP_GET_CONFIG, httpConfigOp)
				.build();

		generalManagementInterfaces.add(httpGeneralManagementIntf);

		if (isMqttApiEnabled()) {
			final MqttInterfaceModel mqttGeneralManagementIntf = new MqttInterfaceModel.Builder(mqttTemplateName, getMqttBrokerAddress(), getMqttBrokerPort())
					.baseTopic(BlacklistConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC)
					.operations(Set.of(Constants.SERVICE_OP_GET_LOG, Constants.SERVICE_OP_GET_CONFIG))
					.build();
			generalManagementInterfaces.add(mqttGeneralManagementIntf);
		}

		return new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(BlacklistConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterfaces(generalManagementInterfaces)
				.build();
	}

}
