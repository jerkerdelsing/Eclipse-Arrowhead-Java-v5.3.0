/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper;

import java.util.List;
import java.util.Map;
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

// Same service definitions, versions, operations, paths and metadata as the native 5.2.2 Gatekeeper registered.
@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class GatekeeperSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private static final Map<String, Object> SERVICE_METADATA = Map.of("generation", "5", "v4Compatibility", true);

	private SystemModel systemModel;
	private String httpTemplateName;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return GatekeeperConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		// management services first: speeds up the management filter
		return List.of(
				generalManagement(),
				service(GatekeeperConstants.SERVICE_DEF_GATEKEEPER_MANAGEMENT, Map.of(
						GatekeeperConstants.SERVICE_OP_CLOUDS, op(HttpMethod.GET, GatekeeperConstants.HTTP_API_OP_MGMT_CLOUDS),
						GatekeeperConstants.SERVICE_OP_RELAYS, op(HttpMethod.GET, GatekeeperConstants.HTTP_API_OP_MGMT_RELAYS),
						GatekeeperConstants.SERVICE_OP_STATUS, op(HttpMethod.GET, GatekeeperConstants.HTTP_API_OP_MGMT_STATUS))),
				service(GatekeeperConstants.SERVICE_DEF_GLOBAL_SERVICE_DISCOVERY, Map.of(
						GatekeeperConstants.SERVICE_OP_QUERY, op(HttpMethod.POST, GatekeeperConstants.HTTP_API_OP_INIT_GSD),
						GatekeeperConstants.SERVICE_OP_MULTI_QUERY, op(HttpMethod.POST, GatekeeperConstants.HTTP_API_OP_INIT_MULTI_GSD))),
				service(GatekeeperConstants.SERVICE_DEF_INTER_CLOUD_NEGOTIATION, Map.of(
						GatekeeperConstants.SERVICE_OP_NEGOTIATE, op(HttpMethod.POST, GatekeeperConstants.HTTP_API_OP_INIT_ICN))));
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION)
					.metadata("role", "inter-cloud-gatekeeper")
					.metadata("sourceCompatibility", "4.6.2");

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
						GatekeeperConstants.LOCAL_CLOUD,
						GatekeeperConstants.ORGANIZATION,
						GatekeeperConstants.FEDERATION_PROFILE,
						GatekeeperConstants.FEDERATION_PEER_ORGANIZATIONS,
						GatekeeperConstants.RELAY_HOST,
						GatekeeperConstants.RELAY_PORT),
				GatekeeperDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	private static HttpOperationModel op(final HttpMethod method, final String path) {
		return new HttpOperationModel.Builder().method(method.name()).path(path).build();
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel service(final String definition, final Map<String, HttpOperationModel> operations) {
		final HttpInterfaceModel.Builder builder = new HttpInterfaceModel.Builder(httpTemplateName, getDomainAddress(), getServerPort())
				.basePath(GatekeeperConstants.HTTP_API_BASE_PATH);
		operations.forEach(builder::operation);

		return new ServiceModel.Builder()
				.serviceDefinition(definition)
				.version(GatekeeperConstants.VERSION_SERVICES)
				.metadata(SERVICE_METADATA)
				.serviceInterface(builder.build())
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel generalManagement() {
		final HttpInterfaceModel intf = new HttpInterfaceModel.Builder(httpTemplateName, getDomainAddress(), getServerPort())
				.basePath(GatekeeperConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(GatekeeperConstants.SERVICE_OP_STATUS, op(HttpMethod.GET, GatekeeperConstants.HTTP_API_OP_GENERAL_STATUS))
				.operation(Constants.SERVICE_OP_GET_LOG, op(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
				.operation(Constants.SERVICE_OP_GET_CONFIG, op(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
				.build();

		return new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(GatekeeperConstants.VERSION_GENERAL_MANAGEMENT)
				.metadata("systemVersion", GatekeeperConstants.SYSTEM_VERSION)
				.serviceInterface(intf)
				.build();
	}
}
