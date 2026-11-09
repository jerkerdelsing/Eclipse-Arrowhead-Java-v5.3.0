/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel;

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

// The services registered in the Service Registry: those of 5.2.2 (base path /gatetunnel, metadata generation 5 and
// v4Compatibility) and generalManagement at /gatetunnel/mgmt/general. sysMonitor is added by the common publisher.
@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class GateTunnelSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;
	private String httpTemplateName;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return GateTunnelConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		// management services first: speeds up the management filter
		return List.of(
				getGeneralManagementServiceModel(),
				getManagementServiceModel(),
				getConnectionServiceModel(),
				getKeyServiceModel());
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(GateTunnelConstants.SYSTEM_VERSION)
					.metadata("role", "secure-inter-cloud-tunnel")
					.metadata("replaces", "Gateway-4.6.2");
			if (AuthenticationPolicy.CERTIFICATE == getAuthenticationPolicy()) {
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
	}

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
						GateTunnelConstants.FEDERATION_PROFILE,
						GateTunnelConstants.NEIGHBOR_CLOUDS,
						GateTunnelConstants.ADVERTISED_ADDRESS,
						GateTunnelConstants.SESSION_MAX_AGE_SECONDS,
						GateTunnelConstants.SESSION_IDLE_SECONDS,
						GateTunnelConstants.INTERCLOUD_MIN_PORT,
						GateTunnelConstants.INTERCLOUD_MAX_PORT,
						GateTunnelConstants.CONTROL_MIN_PORT,
						GateTunnelConstants.CONTROL_MAX_PORT,
						GateTunnelConstants.RELAY_HOST,
						GateTunnelConstants.RELAY_PORT),
				GateTunnelDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	private static HttpOperationModel op(final HttpMethod method, final String path) {
		return new HttpOperationModel.Builder().method(method.name()).path(path).build();
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel service(final String definition, final String version, final HttpInterfaceModel intf, final boolean v4Compatible) {
		final ServiceModel.Builder builder = new ServiceModel.Builder()
				.serviceDefinition(definition)
				.version(version)
				.serviceInterface(intf);
		if (v4Compatible) {
			builder.metadata("generation", "5").metadata("v4Compatibility", true);
		} else {
			builder.metadata("systemVersion", GateTunnelConstants.SYSTEM_VERSION);
		}

		return builder.build();
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel getConnectionServiceModel() {
		final HttpInterfaceModel intf = new HttpInterfaceModel.Builder(httpTemplateName, getDomainAddress(), getServerPort())
				.basePath(GateTunnelConstants.HTTP_API_BASE_PATH)
				.operation(GateTunnelConstants.SERVICE_OP_CONNECT_PROVIDER, op(HttpMethod.POST, GateTunnelConstants.HTTP_API_OP_CONNECT_PROVIDER))
				.operation(GateTunnelConstants.SERVICE_OP_CONNECT_CONSUMER, op(HttpMethod.POST, GateTunnelConstants.HTTP_API_OP_CONNECT_CONSUMER))
				.operation(GateTunnelConstants.SERVICE_OP_CLOSE_SESSIONS, op(HttpMethod.POST, GateTunnelConstants.HTTP_API_OP_CLOSE_SESSIONS))
				.operation(GateTunnelConstants.SERVICE_OP_OPEN_PROVIDER_SESSION, op(HttpMethod.POST, GateTunnelConstants.HTTP_API_OP_INTERCLOUD_PROVIDER))
				.operation(GateTunnelConstants.SERVICE_OP_OPEN_CONSUMER_SESSION, op(HttpMethod.POST, GateTunnelConstants.HTTP_API_OP_INTERCLOUD_CONSUMER))
				.build();

		return service(GateTunnelConstants.SERVICE_DEF_CONNECTION, GateTunnelConstants.VERSION_CONNECTION, intf, true);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel getKeyServiceModel() {
		final HttpInterfaceModel intf = new HttpInterfaceModel.Builder(httpTemplateName, getDomainAddress(), getServerPort())
				.basePath(GateTunnelConstants.HTTP_API_BASE_PATH)
				.operation(GateTunnelConstants.SERVICE_OP_PUBLIC_KEY, op(HttpMethod.GET, GateTunnelConstants.HTTP_API_OP_PUBLIC_KEY))
				.build();

		return service(GateTunnelConstants.SERVICE_DEF_KEY, GateTunnelConstants.VERSION_KEY, intf, true);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel getManagementServiceModel() {
		final HttpInterfaceModel intf = new HttpInterfaceModel.Builder(httpTemplateName, getDomainAddress(), getServerPort())
				.basePath(GateTunnelConstants.HTTP_API_BASE_PATH)
				.operation(GateTunnelConstants.SERVICE_OP_SESSIONS, op(HttpMethod.GET, GateTunnelConstants.HTTP_API_OP_SESSIONS))
				.operation(GateTunnelConstants.SERVICE_OP_CLOSE, op(HttpMethod.POST, GateTunnelConstants.HTTP_API_OP_SESSIONS_CLOSE))
				.operation(GateTunnelConstants.SERVICE_OP_STATUS, op(HttpMethod.GET, GateTunnelConstants.HTTP_API_OP_STATUS))
				.build();

		return service(GateTunnelConstants.SERVICE_DEF_MANAGEMENT, GateTunnelConstants.VERSION_MANAGEMENT, intf, true);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel getGeneralManagementServiceModel() {
		final HttpInterfaceModel intf = new HttpInterfaceModel.Builder(httpTemplateName, getDomainAddress(), getServerPort())
				.basePath(GateTunnelConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(GateTunnelConstants.SERVICE_OP_STATUS, op(HttpMethod.GET, GateTunnelConstants.HTTP_API_OP_GENERAL_STATUS))
				.operation(Constants.SERVICE_OP_GET_LOG, op(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
				.operation(Constants.SERVICE_OP_GET_CONFIG, op(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
				.build();

		return service(Constants.SERVICE_DEF_GENERAL_MANAGEMENT, GateTunnelConstants.VERSION_GENERAL_MANAGEMENT, intf, false);
	}
}
