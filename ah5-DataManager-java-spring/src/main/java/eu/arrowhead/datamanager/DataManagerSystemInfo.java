/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager;

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

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class DataManagerSystemInfo extends SystemInfo {

	private SystemModel systemModel;


	@Override
	public String getSystemName() {
		return DataManagerConstants.SYSTEM_NAME;
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

	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel dataProxy = new ServiceModel.Builder()
				.serviceDefinition(DataManagerConstants.SERVICE_DEF_DATA_PROXY)
				.version(DataManagerConstants.VERSION_DATA_PROXY)
				.serviceInterface(createHttpInterface(DataManagerConstants.HTTP_API_DATA_PROXY_PATH))
				.build();
		final ServiceModel dataHistorian = new ServiceModel.Builder()
				.serviceDefinition(DataManagerConstants.SERVICE_DEF_DATA_HISTORIAN)
				.version(DataManagerConstants.VERSION_DATA_HISTORIAN)
				.serviceInterface(createHttpInterface(DataManagerConstants.HTTP_API_DATA_HISTORIAN_PATH))
				.build();
        final ServiceModel generalManagement = new ServiceModel.Builder()
                .serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
                .version("1.0.0")
                .serviceInterface(new HttpInterfaceModel.Builder(
                        getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME,
                        getDomainAddress(), getServerPort())
                        .basePath("/datamanager/mgmt/general")
                        .operation("status", new HttpOperationModel.Builder().method("GET").path("/status").build())
                        .build())
                .build();
        return List.of(generalManagement, dataProxy, dataHistorian);
	}

	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.NORMALIZATION_MODE,
						Constants.ALLOW_SELF_ADDRESSING,
						Constants.ALLOW_NON_ROUTABLE_ADDRESSING),
				DataManagerDefaults.class);
	}

	private InterfaceModel createHttpInterface(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled()
				? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME
				: Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel listSystems = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(DataManagerConstants.HTTP_API_OP_SYSTEMS_PATH)
				.build();
		final HttpOperationModel listServices = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(DataManagerConstants.HTTP_API_OP_SERVICES_PATH)
				.build();
		final HttpOperationModel getData = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(DataManagerConstants.HTTP_API_OP_DATA_PATH)
				.build();
		final HttpOperationModel putData = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(DataManagerConstants.HTTP_API_OP_DATA_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(DataManagerConstants.SERVICE_OP_LIST_SYSTEMS, listSystems)
				.operation(DataManagerConstants.SERVICE_OP_LIST_SERVICES, listServices)
				.operation(DataManagerConstants.SERVICE_OP_GET_DATA, getData)
				.operation(DataManagerConstants.SERVICE_OP_PUT_DATA, putData)
				.build();
	}
}
