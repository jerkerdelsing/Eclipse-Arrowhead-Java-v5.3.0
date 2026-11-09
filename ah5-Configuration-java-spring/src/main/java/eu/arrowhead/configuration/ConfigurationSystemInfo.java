/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class ConfigurationSystemInfo extends SystemInfo {

	private SystemModel systemModel;

	@Override
	public String getSystemName() {
		return ConfigurationConstants.SYSTEM_NAME;
	}

	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION);

			if (AuthenticationPolicy.CERTIFICATE == getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	// sysMonitor is registered by the common-library SysMonitorPublisher when sysmonitor.enabled=true.
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel configuration = new ServiceModel.Builder()
				.serviceDefinition(ConfigurationConstants.SERVICE_DEF_CONFIGURATION)
				.version(ConfigurationConstants.VERSION_CONFIGURATION)
				.serviceInterface(interfaceBuilder(ConfigurationConstants.HTTP_API_BASE_PATH)
						.operation(ConfigurationConstants.SERVICE_OP_GET_CONFIGURATION, operation(HttpMethod.GET, ConfigurationConstants.HTTP_API_OP_CONFIG_PATH))
						.operation(ConfigurationConstants.SERVICE_OP_GET_RAW_CONFIGURATION, operation(HttpMethod.GET, ConfigurationConstants.HTTP_API_OP_RAW_CONFIG_PATH))
						.build())
				.build();
		final ServiceModel configurationProxy = new ServiceModel.Builder()
				.serviceDefinition(ConfigurationConstants.SERVICE_DEF_CONFIGURATION_PROXY)
				.version(ConfigurationConstants.VERSION_CONFIGURATION_PROXY)
				.serviceInterface(interfaceBuilder(ConfigurationConstants.HTTP_API_PROXY_PATH)
						.operation(ConfigurationConstants.SERVICE_OP_GET_CONFIGURATION, operation(HttpMethod.GET, ConfigurationConstants.HTTP_API_OP_CONFIG_PATH))
						.operation(ConfigurationConstants.SERVICE_OP_STORE_CONFIGURATION, operation(HttpMethod.PUT, ConfigurationConstants.HTTP_API_OP_PROXY_SAVE_CONFIG_PATH))
						.build())
				.build();
		final ServiceModel configurationManagement = new ServiceModel.Builder()
				.serviceDefinition(ConfigurationConstants.SERVICE_DEF_CONFIGURATION_MANAGEMENT)
				.version(ConfigurationConstants.VERSION_CONFIGURATION_MANAGEMENT)
				.serviceInterface(interfaceBuilder(ConfigurationConstants.HTTP_API_MANAGEMENT_PATH)
						.operation(ConfigurationConstants.SERVICE_OP_LIST_CONFIGURATIONS, operation(HttpMethod.GET, ConfigurationConstants.HTTP_API_OP_MGMT_CONFIGS_PATH))
						.operation(ConfigurationConstants.SERVICE_OP_STORE_CONFIGURATION, operation(HttpMethod.PUT, ConfigurationConstants.HTTP_API_OP_CONFIG_PATH))
						.operation(ConfigurationConstants.SERVICE_OP_REMOVE_CONFIGURATION, operation(HttpMethod.DELETE, ConfigurationConstants.HTTP_API_OP_CONFIG_PATH))
						.build())
				.build();
		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(ConfigurationConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(interfaceBuilder(ConfigurationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
						.operation(Constants.SERVICE_OP_GET_LOG, operation(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
						.operation(Constants.SERVICE_OP_GET_CONFIG, operation(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
						.build())
				.build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, configurationManagement, configuration, configurationProxy);
	}

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
						ConfigurationConstants.SERVICE_AUTHORIZATION_ENABLED,
						ConfigurationConstants.CONFIGURATION_MAX_SIZE),
				ConfigurationDefaults.class);
	}

	private HttpInterfaceModel.Builder interfaceBuilder(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled()
				? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME
				: Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort()).basePath(basePath);
	}

	private HttpOperationModel operation(final HttpMethod method, final String path) {
		return new HttpOperationModel.Builder().method(method.name()).path(path).build();
	}
}
