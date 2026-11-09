/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer;

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
public class ChoreographerSystemInfo extends SystemInfo {

	private SystemModel systemModel;

	@Override
	public String getSystemName() {
		return ChoreographerConstants.SYSTEM_NAME;
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
		final ServiceModel session = new ServiceModel.Builder()
				.serviceDefinition(ChoreographerConstants.SERVICE_DEF_SESSION)
				.version(ChoreographerConstants.VERSION)
				.serviceInterface(interfaceBuilder(ChoreographerConstants.HTTP_API_SESSION_PATH)
						.operation(ChoreographerConstants.SERVICE_OP_START_SESSION, operation(HttpMethod.POST, ChoreographerConstants.HTTP_API_OP_START_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_ABORT_SESSION, operation(HttpMethod.DELETE, ChoreographerConstants.HTTP_API_OP_ABORT_PATH))
						.build())
				.build();
		final ServiceModel executor = new ServiceModel.Builder()
				.serviceDefinition(ChoreographerConstants.SERVICE_DEF_EXECUTOR)
				.version(ChoreographerConstants.VERSION)
				.serviceInterface(interfaceBuilder(ChoreographerConstants.HTTP_API_EXECUTOR_PATH)
						.operation(ChoreographerConstants.SERVICE_OP_REGISTER, operation(HttpMethod.POST, ChoreographerConstants.HTTP_API_OP_REGISTER_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_UNREGISTER, operation(HttpMethod.DELETE, ChoreographerConstants.HTTP_API_OP_UNREGISTER_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_NOTIFY_STEP_DONE, operation(HttpMethod.POST, ChoreographerConstants.HTTP_API_OP_NOTIFY_STEP_DONE_PATH))
						.build())
				.build();
		final ServiceModel management = new ServiceModel.Builder()
				.serviceDefinition(ChoreographerConstants.SERVICE_DEF_MANAGEMENT)
				.version(ChoreographerConstants.VERSION)
				.serviceInterface(interfaceBuilder(ChoreographerConstants.HTTP_API_MANAGEMENT_PATH)
						.operation(ChoreographerConstants.SERVICE_OP_QUERY_PLANS, operation(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_PLANS_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_GET_PLAN, operation(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_PLAN_BY_ID_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_CREATE_PLAN, operation(HttpMethod.POST, ChoreographerConstants.HTTP_API_OP_PLANS_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_REMOVE_PLAN, operation(HttpMethod.DELETE, ChoreographerConstants.HTTP_API_OP_PLAN_BY_ID_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_CHECK_PLAN, operation(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_CHECK_PLAN_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_START_SESSIONS, operation(HttpMethod.POST, ChoreographerConstants.HTTP_API_OP_START_SESSIONS_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_MGMT_ABORT_SESSION, operation(HttpMethod.DELETE, ChoreographerConstants.HTTP_API_OP_SESSION_BY_ID_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_QUERY_SESSIONS, operation(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_SESSIONS_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_QUERY_SESSION_STEPS, operation(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_SESSION_STEPS_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_QUERY_WORKLOG, operation(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_WORKLOG_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_QUERY_EXECUTORS, operation(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_EXECUTORS_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_GET_EXECUTOR, operation(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_EXECUTOR_BY_ID_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_CREATE_EXECUTOR, operation(HttpMethod.POST, ChoreographerConstants.HTTP_API_OP_EXECUTORS_PATH))
						.operation(ChoreographerConstants.SERVICE_OP_REMOVE_EXECUTOR, operation(HttpMethod.DELETE, ChoreographerConstants.HTTP_API_OP_EXECUTOR_BY_ID_PATH))
						.build())
				.build();
		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(ChoreographerConstants.VERSION)
				.serviceInterface(interfaceBuilder(ChoreographerConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
						.operation(Constants.SERVICE_OP_GET_LOG, operation(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
						.operation(Constants.SERVICE_OP_GET_CONFIG, operation(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
						.build())
				.build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, management, session, executor);
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
						Constants.MAX_PAGE_SIZE,
						Constants.NORMALIZATION_MODE,
						ChoreographerConstants.MAX_PLAN_ITERATION,
						ChoreographerConstants.ORCHESTRATION_TIMEOUT,
						ChoreographerConstants.SESSION_WORKER_THREADS),
				ChoreographerDefaults.class);
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
