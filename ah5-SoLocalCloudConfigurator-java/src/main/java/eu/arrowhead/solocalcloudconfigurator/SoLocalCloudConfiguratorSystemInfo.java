/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator;

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
public class SoLocalCloudConfiguratorSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return SoLocalCloudConfiguratorConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION)
					.metadata("role", "so-local-cloud-configurator");

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
				.version(SoLocalCloudConfiguratorConstants.SERVICE_VERSION)
				.serviceInterface(interfaceBuilder(SoLocalCloudConfiguratorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
						.operation(Constants.SERVICE_OP_GET_LOG, operation(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
						.operation(Constants.SERVICE_OP_GET_CONFIG, operation(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
						.build())
				.build();

		final ServiceModel configuration = new ServiceModel.Builder()
				.serviceDefinition(SoLocalCloudConfiguratorConstants.SERVICE_DEF_SO_LOCAL_CLOUD_CONFIGURATION)
				.version(SoLocalCloudConfiguratorConstants.SERVICE_VERSION)
				.metadata("systemVersion", SoLocalCloudConfiguratorConstants.SYSTEM_VERSION)
				.serviceInterface(interfaceBuilder(SoLocalCloudConfiguratorConstants.HTTP_API_BASE_PATH)
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_CATALOG, operation(HttpMethod.GET, SoLocalCloudConfiguratorConstants.HTTP_API_OP_CATALOG_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_LIST_LC_DESIGNS, operation(HttpMethod.GET, SoLocalCloudConfiguratorConstants.HTTP_API_OP_LC_DESIGNS_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_LIST_DESIGNS, operation(HttpMethod.GET, SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGNS_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_GET_DESIGN, operation(HttpMethod.GET, SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGN_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_CREATE_DESIGN, operation(HttpMethod.POST, SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGNS_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_UPDATE_DESIGN, operation(HttpMethod.PUT, SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGN_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_REMOVE_DESIGN, operation(HttpMethod.DELETE, SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGN_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_PLAN, operation(HttpMethod.POST, SoLocalCloudConfiguratorConstants.HTTP_API_OP_PLAN_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_BUILD_PACKAGE, operation(HttpMethod.POST, SoLocalCloudConfiguratorConstants.HTTP_API_OP_PACKAGES_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_LIST_PACKAGES, operation(HttpMethod.GET, SoLocalCloudConfiguratorConstants.HTTP_API_OP_PACKAGES_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_GET_PACKAGE, operation(HttpMethod.GET, SoLocalCloudConfiguratorConstants.HTTP_API_OP_PACKAGE_PATH))
						.operation(SoLocalCloudConfiguratorConstants.SERVICE_OP_REMOVE_PACKAGE, operation(HttpMethod.DELETE, SoLocalCloudConfiguratorConstants.HTTP_API_OP_PACKAGE_PATH))
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
						SoLocalCloudConfiguratorConstants.CATALOG_FILE,
						SoLocalCloudConfiguratorConstants.SOURCE_REPOSITORY,
						SoLocalCloudConfiguratorConstants.SOURCE_COMMIT,
						SoLocalCloudConfiguratorConstants.API_WHITELIST),
				SoLocalCloudConfiguratorDefaults.class);
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
