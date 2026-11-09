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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryInterfacePolicy;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryPolicy;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class ServiceRegistrySystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	@Value(ServiceRegistryConstants.$DISCOVERY_VERBOSE_WD)
	private boolean discoveryVerbose;

	@Value(ServiceRegistryConstants.$SERVICE_DISCOVERY_DIRECT_ACCESS_WD)
	private List<String> serviceDiscoveryDirectAccess;
	private List<String> normalizedServiceDiscoveryDirectAccess = new ArrayList<>();

	@Value(ServiceRegistryConstants.$SERVICE_DISCOVERY_POLICY_WD)
	private ServiceDiscoveryPolicy serviceDiscoveryPolicy;

	@Value(ServiceRegistryConstants.$SERVICE_DISCOVERY_INTERFACE_POLICY_WD)
	private ServiceDiscoveryInterfacePolicy serviceDiscoveryInterfacePolicy;

	@Value(Constants.$SERVICE_ADDRESS_ALIAS)
	private List<String> serviceAddressAliases;

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return Constants.SYS_NAME_SERVICE_REGISTRY;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel deviceDiscovery = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_DEVICE_DISCOVERY)
				.version(ServiceRegistryConstants.VERSION_DEVICE_DISCOVERY)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForDeviceDiscovery())
				.serviceInterface(getMqttServiceInterfaceForDeviceDiscovery())
				.build();

		final ServiceModel systemDiscovery = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SYSTEM_DISCOVERY)
				.version(ServiceRegistryConstants.VERSION_SYSTEM_DISCOVERY)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForSystemDiscovery())
				.serviceInterface(getMqttServiceInterfaceForSystemDiscovery())
				.build();

		final ServiceModel serviceDiscovery = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_DISCOVERY)
				.version(ServiceRegistryConstants.VERSION_SERVICE_DISCOVERY)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForServiceDiscovery())
				.serviceInterface(getMqttServiceInterfaceForServiceDiscovery())
				.build();

		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(ServiceRegistryConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.serviceInterface(getMqttServiceInterfaceForGeneralManagement())
				.build();

		final ServiceModel serviceRegistryManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_REGISTRY_MANAGEMENT)
				.version(ServiceRegistryConstants.VERSION_SERVICE_REGISTRY_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForServiceRegistryManagement())
				.serviceInterface(getMqttServiceInterfaceForServiceRegistryManagement())
				.build();

		// sysMonitor is registered by the common-library SysMonitorPublisher when sysmonitor.enabled=true.

		// starting with management services speeds up management filters
		return List.of(generalManagement, serviceRegistryManagement, deviceDiscovery, systemDiscovery, serviceDiscovery);
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
	public boolean isDiscoveryVerbose() {
		return this.discoveryVerbose;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasClientDirectAccess(final String systemName) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");

		if (!Utilities.isEmpty(serviceDiscoveryDirectAccess) && Utilities.isEmpty(normalizedServiceDiscoveryDirectAccess)) {
			for (final String name : serviceDiscoveryDirectAccess) {
				if (!Utilities.isEmpty(name)) {
					normalizedServiceDiscoveryDirectAccess.add(systemNameNormalizer.normalize(name));
				}
			}
		}

		return !Utilities.isEmpty(normalizedServiceDiscoveryDirectAccess) && normalizedServiceDiscoveryDirectAccess.contains(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDiscoveryPolicy getServiceDiscoveryPolicy() {
		return this.serviceDiscoveryPolicy;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDiscoveryInterfacePolicy getServiceDiscoveryInterfacePolicy() {
		return this.serviceDiscoveryInterfacePolicy;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getServiceAddressAliases() {
		return serviceAddressAliases;
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
						ServiceRegistryConstants.SERVICE_DISCOVERY_POLICY,
						ServiceRegistryConstants.DISCOVERY_VERBOSE,
						ServiceRegistryConstants.SERVICE_DISCOVERY_INTERFACE_POLICY),
				ServiceRegistryDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	// HTTP Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForDeviceDiscovery() {
		return getHttpServiceInterfaceForADiscoveryService(ServiceRegistryConstants.HTTP_API_DEVICE_DISCOVERY_PATH, ServiceRegistryConstants.HTTP_API_OP_DEVICE_REVOKE_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForSystemDiscovery() {
		return getHttpServiceInterfaceForADiscoveryService(ServiceRegistryConstants.HTTP_API_SYSTEM_DISCOVERY_PATH, ServiceRegistryConstants.HTTP_API_OP_REVOKE_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForServiceDiscovery() {
		return getHttpServiceInterfaceForADiscoveryService(ServiceRegistryConstants.HTTP_API_SERVICE_DISCOVERY_PATH, ServiceRegistryConstants.HTTP_API_OP_SERVICE_REVOKE_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForADiscoveryService(final String basePath, final String revokePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel register = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_REGISTER_PATH)
				.build();
		final HttpOperationModel lookup = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_LOOKUP_PATH)
				.build();
		final HttpOperationModel revoke = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(revokePath)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_REGISTER, register)
				.operation(Constants.SERVICE_OP_LOOKUP, lookup)
				.operation(Constants.SERVICE_OP_REVOKE, revoke)
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
				.basePath(ServiceRegistryConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForServiceRegistryManagement() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel deviceQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_DEVICE_QUERY_PATH)
				.build();
		final HttpOperationModel deviceCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_DEVICE_PATH)
				.build();
		final HttpOperationModel deviceUpdate = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_DEVICE_PATH)
				.build();
		final HttpOperationModel deviceRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_DEVICE_PATH)
				.build();

		final HttpOperationModel systemQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SYSTEM_QUERY_PATH)
				.build();
		final HttpOperationModel systemCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SYSTEM_PATH)
				.build();
		final HttpOperationModel systemUpdate = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SYSTEM_PATH)
				.build();
		final HttpOperationModel systemRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SYSTEM_PATH)
				.build();

		final HttpOperationModel serviceDefQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_DEFINITION_QUERY_PATH)
				.build();
		final HttpOperationModel serviceDefCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_DEFINITION_PATH)
				.build();
		final HttpOperationModel serviceDefRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_DEFINITION_PATH)
				.build();

		final HttpOperationModel serviceQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_INSTANCE_QUERY_PATH)
				.build();
		final HttpOperationModel serviceCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_INSTANCE_PATH)
				.build();
		final HttpOperationModel serviceUpdate = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_INSTANCE_PATH)
				.build();
		final HttpOperationModel serviceRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_INSTANCE_PATH)
				.build();

		final HttpOperationModel intfQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_INTERFACE_TEMPLATE_QUERY_PATH)
				.build();
		final HttpOperationModel intfCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_INTERFACE_TEMPLATE_PATH)
				.build();
		final HttpOperationModel intfRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_INTERFACE_TEMPLATE_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(ServiceRegistryConstants.HTTP_API_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_DEVICE_QUERY, deviceQuery)
				.operation(Constants.SERVICE_OP_DEVICE_CREATE, deviceCreate)
				.operation(Constants.SERVICE_OP_DEVICE_UPDATE, deviceUpdate)
				.operation(Constants.SERVICE_OP_DEVICE_REMOVE, deviceRemove)
				.operation(Constants.SERVICE_OP_SYSTEM_QUERY, systemQuery)
				.operation(Constants.SERVICE_OP_SYSTEM_CREATE, systemCreate)
				.operation(Constants.SERVICE_OP_SYSTEM_UPDATE, systemUpdate)
				.operation(Constants.SERVICE_OP_SYSTEM_REMOVE, systemRemove)
				.operation(Constants.SERVICE_OP_SERVICE_DEF_QUERY, serviceDefQuery)
				.operation(Constants.SERVICE_OP_SERVICE_DEF_CREATE, serviceDefCreate)
				.operation(Constants.SERVICE_OP_SERVICE_DEF_REMOVE, serviceDefRemove)
				.operation(Constants.SERVICE_OP_SERVICE_QUERY, serviceQuery)
				.operation(Constants.SERVICE_OP_SERVICE_CREATE, serviceCreate)
				.operation(Constants.SERVICE_OP_SERVICE_UPDATE, serviceUpdate)
				.operation(Constants.SERVICE_OP_SERVICE_REMOVE, serviceRemove)
				.operation(Constants.SERVICE_OP_INTERFACE_TEMPLATE_QUERY, intfQuery)
				.operation(Constants.SERVICE_OP_INTERFACE_TEMPLATE_CREATE, intfCreate)
				.operation(Constants.SERVICE_OP_INTERFACE_TEMPLATE_REMOVE, intfRemove)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	// MQTT Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForDeviceDiscovery() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		return getMqttServiceInterfaceForADiscoveryService(ServiceRegistryConstants.MQTT_API_DEVICE_DISCOVERY_BASE_TOPIC);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForSystemDiscovery() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		return getMqttServiceInterfaceForADiscoveryService(ServiceRegistryConstants.MQTT_API_SYSTEM_DISCOVERY_BASE_TOPIC);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForServiceDiscovery() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		return getMqttServiceInterfaceForADiscoveryService(ServiceRegistryConstants.MQTT_API_SERVICE_DISCOVERY_BASE_TOPIC);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForADiscoveryService(final String baseTopic) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;

		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(baseTopic)
				.operations(Set.of(Constants.SERVICE_OP_REGISTER, Constants.SERVICE_OP_LOOKUP, Constants.SERVICE_OP_REVOKE))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForGeneralManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(ServiceRegistryConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GET_LOG, Constants.SERVICE_OP_GET_CONFIG))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForServiceRegistryManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(ServiceRegistryConstants.MQTT_API_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(
						Constants.SERVICE_OP_DEVICE_QUERY, Constants.SERVICE_OP_DEVICE_CREATE, Constants.SERVICE_OP_DEVICE_UPDATE, Constants.SERVICE_OP_DEVICE_REMOVE,
						Constants.SERVICE_OP_SYSTEM_QUERY, Constants.SERVICE_OP_SYSTEM_CREATE, Constants.SERVICE_OP_SYSTEM_UPDATE, Constants.SERVICE_OP_SYSTEM_REMOVE,
						Constants.SERVICE_OP_SERVICE_DEF_QUERY, Constants.SERVICE_OP_SERVICE_DEF_CREATE, Constants.SERVICE_OP_SERVICE_DEF_REMOVE,
						Constants.SERVICE_OP_SERVICE_QUERY, Constants.SERVICE_OP_SERVICE_CREATE, Constants.SERVICE_OP_SERVICE_UPDATE, Constants.SERVICE_OP_SERVICE_REMOVE,
						Constants.SERVICE_OP_INTERFACE_TEMPLATE_QUERY, Constants.SERVICE_OP_INTERFACE_TEMPLATE_CREATE, Constants.SERVICE_OP_INTERFACE_TEMPLATE_REMOVE))
				.build();
	}
}