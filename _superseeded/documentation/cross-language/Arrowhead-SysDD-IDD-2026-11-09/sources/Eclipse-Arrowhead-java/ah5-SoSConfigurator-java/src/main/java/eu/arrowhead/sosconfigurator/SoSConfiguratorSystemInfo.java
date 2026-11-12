/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator;

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
public class SoSConfiguratorSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return SoSConfiguratorConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION)
					.metadata("role", "sos-configurator");

			if (AuthenticationPolicy.CERTIFICATE == getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	// sysMonitor is registered by the common-library SysMonitorPublisher when sysmonitor.enabled=true.
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(SoSConfiguratorConstants.SERVICE_VERSION)
				.serviceInterface(interfaceBuilder(SoSConfiguratorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
						.operation(Constants.SERVICE_OP_GET_LOG, operation(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
						.operation(Constants.SERVICE_OP_GET_CONFIG, operation(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
						.build())
				.build();

		final ServiceModel configuration = new ServiceModel.Builder()
				.serviceDefinition(SoSConfiguratorConstants.SERVICE_DEF_SOS_CONFIGURATION)
				.version(SoSConfiguratorConstants.SERVICE_VERSION)
				.metadata("systemVersion", SoSConfiguratorConstants.SYSTEM_VERSION)
				.serviceInterface(interfaceBuilder(SoSConfiguratorConstants.HTTP_API_BASE_PATH)
						.operation(SoSConfiguratorConstants.SERVICE_OP_CATALOG, operation(HttpMethod.GET, SoSConfiguratorConstants.HTTP_API_OP_CATALOG_PATH))
						.operation(SoSConfiguratorConstants.SERVICE_OP_STATUS, operation(HttpMethod.GET, SoSConfiguratorConstants.HTTP_API_OP_STATUS_PATH))
						.operation(SoSConfiguratorConstants.SERVICE_OP_PLAN, operation(HttpMethod.POST, SoSConfiguratorConstants.HTTP_API_OP_PLAN_PATH))
						.operation(SoSConfiguratorConstants.SERVICE_OP_SUBMIT_DEPLOYMENT, operation(HttpMethod.POST, SoSConfiguratorConstants.HTTP_API_OP_DEPLOYMENTS_PATH))
						.operation(SoSConfiguratorConstants.SERVICE_OP_LIST_DEPLOYMENTS, operation(HttpMethod.GET, SoSConfiguratorConstants.HTTP_API_OP_DEPLOYMENTS_PATH))
						.operation(SoSConfiguratorConstants.SERVICE_OP_GET_DEPLOYMENT, operation(HttpMethod.GET, SoSConfiguratorConstants.HTTP_API_OP_DEPLOYMENT_PATH))
						.build())
				.build();

		return List.of(generalManagement, configuration);
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
						SoSConfiguratorConstants.CATALOG_FILE,
						SoSConfiguratorConstants.STATUS_TIMEOUT_SECONDS,
						SoSConfiguratorConstants.API_WHITELIST,
						SoSConfiguratorConstants.CONSOLE_PORT),
				SoSConfiguratorDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	private HttpInterfaceModel.Builder interfaceBuilder(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled()
				? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME
				: Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort()).basePath(basePath);
	}

	//-------------------------------------------------------------------------------------------------
	private HttpOperationModel operation(final HttpMethod method, final String path) {
		return new HttpOperationModel.Builder().method(method.name()).path(path).build();
	}
}
