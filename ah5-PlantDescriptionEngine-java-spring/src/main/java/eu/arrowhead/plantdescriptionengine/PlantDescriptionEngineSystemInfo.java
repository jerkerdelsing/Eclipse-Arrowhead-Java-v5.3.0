/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine;

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
public class PlantDescriptionEngineSystemInfo extends SystemInfo {

	private SystemModel systemModel;

	@Override
	public String getSystemName() {
		return PlantDescriptionEngineConstants.SYSTEM_NAME;
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
		final String pds = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTIONS_PATH;
		final String pd = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTION_PATH;
		final ServiceModel management = new ServiceModel.Builder()
				.serviceDefinition(PlantDescriptionEngineConstants.SERVICE_DEF_PLANT_DESCRIPTION_MANAGEMENT)
				.version(PlantDescriptionEngineConstants.VERSION_PLANT_DESCRIPTION_MANAGEMENT)
				.serviceInterface(interfaceBuilder(PlantDescriptionEngineConstants.HTTP_API_MANAGEMENT_PATH)
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_QUERY, operation(HttpMethod.GET, pds))
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_GET, operation(HttpMethod.GET, pd))
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_CREATE, operation(HttpMethod.POST, pds))
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_REPLACE, operation(HttpMethod.PUT, pd))
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_UPDATE, operation(HttpMethod.PATCH, pd))
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_REMOVE, operation(HttpMethod.DELETE, pd))
						.build())
				.build();
		final ServiceModel monitor = new ServiceModel.Builder()
				.serviceDefinition(PlantDescriptionEngineConstants.SERVICE_DEF_PLANT_DESCRIPTION_MONITOR)
				.version(PlantDescriptionEngineConstants.VERSION_PLANT_DESCRIPTION_MONITOR)
				.serviceInterface(interfaceBuilder(PlantDescriptionEngineConstants.HTTP_API_MONITOR_PATH)
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_QUERY_PLANT_DESCRIPTIONS, operation(HttpMethod.GET, pds))
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_GET_PLANT_DESCRIPTION, operation(HttpMethod.GET, pd))
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_QUERY_ALARMS, operation(HttpMethod.GET, PlantDescriptionEngineConstants.HTTP_API_OP_ALARMS_PATH))
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_GET_ALARM, operation(HttpMethod.GET, PlantDescriptionEngineConstants.HTTP_API_OP_ALARM_PATH))
						.operation(PlantDescriptionEngineConstants.SERVICE_OP_ACKNOWLEDGE_ALARM, operation(HttpMethod.PATCH, PlantDescriptionEngineConstants.HTTP_API_OP_ALARM_PATH))
						.build())
				.build();
		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(PlantDescriptionEngineConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(interfaceBuilder(PlantDescriptionEngineConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
						.operation(Constants.SERVICE_OP_GET_LOG, operation(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
						.operation(Constants.SERVICE_OP_GET_CONFIG, operation(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
						.build())
				.build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, management, monitor);
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
						PlantDescriptionEngineConstants.SERVICE_AUTHORIZATION_ENABLED,
						PlantDescriptionEngineConstants.PLANT_DESCRIPTION_MAX_SIZE,
						PlantDescriptionEngineConstants.SYNC_INTERVAL,
						PlantDescriptionEngineConstants.RULE_PRIORITY_DEFAULT,
						PlantDescriptionEngineConstants.MONITORING_ENABLED,
						PlantDescriptionEngineConstants.MONITORING_STALE_AFTER,
						PlantDescriptionEngineConstants.ALARM_IGNORED_SYSTEMS),
				PlantDescriptionEngineDefaults.class);
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
