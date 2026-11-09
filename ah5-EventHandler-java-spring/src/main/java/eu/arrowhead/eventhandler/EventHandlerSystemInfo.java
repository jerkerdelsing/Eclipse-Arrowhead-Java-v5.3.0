/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler;

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

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class EventHandlerSystemInfo extends SystemInfo {

	private SystemModel systemModel;

	@Override
	public String getSystemName() {
		return EventHandlerConstants.SYSTEM_NAME;
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
		final ServiceModel eventPublish = new ServiceModel.Builder()
				.serviceDefinition(EventHandlerConstants.SERVICE_DEF_EVENT_PUBLISH)
				.version(EventHandlerConstants.VERSION_EVENT_PUBLISH)
				.serviceInterface(getEventPublishInterface())
				.build();
		final ServiceModel eventSubscription = new ServiceModel.Builder()
				.serviceDefinition(EventHandlerConstants.SERVICE_DEF_EVENT_SUBSCRIPTION)
				.version(EventHandlerConstants.VERSION_EVENT_SUBSCRIPTION)
				.serviceInterface(getEventSubscriptionInterface())
				.build();
		final ServiceModel eventHandlerManagement = new ServiceModel.Builder()
				.serviceDefinition(EventHandlerConstants.SERVICE_DEF_EVENT_HANDLER_MANAGEMENT)
				.version(EventHandlerConstants.VERSION_EVENT_HANDLER_MANAGEMENT)
				.serviceInterface(getEventHandlerManagementInterface())
				.build();
		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(EventHandlerConstants.VERSION_EVENT_HANDLER_MANAGEMENT)
				.serviceInterface(getGeneralManagementInterface())
				.build();

		return List.of(generalManagement, eventHandlerManagement, eventPublish, eventSubscription);
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
						EventHandlerConstants.PUBLISH_TOLERANCE_SECONDS,
						EventHandlerConstants.DELIVERY_ATTEMPTS,
						EventHandlerConstants.EXPIRATION_CLEANUP_INTERVAL),
				EventHandlerDefaults.class);
	}

	private InterfaceModel getEventPublishInterface() {
		return interfaceBuilder(EventHandlerConstants.HTTP_API_EVENT_PUBLISH_PATH)
				.operation(EventHandlerConstants.SERVICE_OP_PUBLISH, operation(HttpMethod.POST, EventHandlerConstants.HTTP_API_OP_PUBLISH_PATH))
				.build();
	}

	private InterfaceModel getEventSubscriptionInterface() {
		return interfaceBuilder(EventHandlerConstants.HTTP_API_EVENT_SUBSCRIPTION_PATH)
				.operation(EventHandlerConstants.SERVICE_OP_SUBSCRIBE, operation(HttpMethod.POST, EventHandlerConstants.HTTP_API_OP_SUBSCRIBE_PATH))
				.operation(EventHandlerConstants.SERVICE_OP_UNSUBSCRIBE, operation(HttpMethod.DELETE, EventHandlerConstants.HTTP_API_OP_UNSUBSCRIBE_PATH))
				.build();
	}

	private InterfaceModel getEventHandlerManagementInterface() {
		return interfaceBuilder(EventHandlerConstants.HTTP_API_MANAGEMENT_PATH)
				.operation(EventHandlerConstants.SERVICE_OP_QUERY_SUBSCRIPTIONS, operation(HttpMethod.GET, EventHandlerConstants.HTTP_API_OP_SUBSCRIPTIONS_PATH))
				.operation(EventHandlerConstants.SERVICE_OP_UPDATE_SUBSCRIPTION, operation(HttpMethod.PUT, EventHandlerConstants.HTTP_API_OP_SUBSCRIPTION_BY_ID_PATH))
				.operation(EventHandlerConstants.SERVICE_OP_REMOVE_SUBSCRIPTION, operation(HttpMethod.DELETE, EventHandlerConstants.HTTP_API_OP_SUBSCRIPTION_BY_ID_PATH))
				.build();
	}

	private InterfaceModel getGeneralManagementInterface() {
		return interfaceBuilder(EventHandlerConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, operation(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
				.operation(Constants.SERVICE_OP_GET_CONFIG, operation(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
				.build();
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
