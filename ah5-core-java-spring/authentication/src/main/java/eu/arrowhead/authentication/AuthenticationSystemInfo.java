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
package eu.arrowhead.authentication;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.security.SecurityUtilities;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class AuthenticationSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	@Value(AuthenticationConstants.$AUTHENTICATION_SECRET_KEY)
	private String authenticationSecretKey;

	private String specIdentityToken;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return Constants.SYS_NAME_AUTHENTICATION;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			systemModel = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION)
					.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel identity = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_IDENTITY)
				.version(AuthenticationConstants.VERSION_IDENTITY)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForIdentity())
				.serviceInterface(getMqttServiceInterfaceForIdentity())
				.build();

		final ServiceModel identityManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_IDENTITY_MANAGEMENT)
				.version(AuthenticationConstants.VERSION_IDENTITY_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForIdentityManagement())
				.serviceInterface(getMqttServiceInterfaceForIdentityManagement())
				.build();

		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(AuthenticationConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.serviceInterface(getMqttServiceInterfaceForGeneralManagement())
				.build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, identityManagement, identity);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getIdentityToken() {
		return specIdentityToken;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.SERVICE_REGISTRY_ADDRESS,
						Constants.SERVICE_REGISTRY_PORT,
						Constants.MQTT_API_ENABLED,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY,
						Constants.MAX_PAGE_SIZE,
						Constants.NORMALIZATION_MODE,
						AuthenticationConstants.IDENTITY_TOKEN_DURATION,
						AuthenticationConstants.CLEANER_JOB_INTERVAL),
				AuthenticationDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit() {
		if (Utilities.isEmpty(authenticationSecretKey)) {
			throw new InvalidParameterException("'authenticationSecretKey' is missing or empty");
		}

		if (this.getAuthenticationPolicy() != AuthenticationPolicy.INTERNAL) {
			throw new InvalidParameterException("'authenticationPolicy' is invalid: must be internal");
		}

		try {
			specIdentityToken = SecurityUtilities.hashWithSecretKey(getSystemName(), authenticationSecretKey);
		} catch (final InvalidKeyException | NoSuchAlgorithmException ex) {
			throw new InternalServerError(ex.getMessage(), "System initialization", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForIdentity() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel login = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_LOGIN_PATH)
				.build();
		final HttpOperationModel logout = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_LOGOUT_PATH)
				.build();
		final HttpOperationModel change = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_CHANGE_PATH)
				.build();
		final HttpOperationModel verify = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(AuthenticationConstants.HTTP_API_OP_VERIFY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthenticationConstants.HTTP_API_IDENTITY_PATH)
				.operation(Constants.SERVICE_OP_IDENTITY_LOGIN, login)
				.operation(Constants.SERVICE_OP_IDENTITY_LOGOUT, logout)
				.operation(Constants.SERVICE_OP_IDENTITY_CHANGE, change)
				.operation(Constants.SERVICE_OP_IDENTITY_VERIFY, verify)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForIdentity() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(AuthenticationConstants.MQTT_API_IDENTITY_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_IDENTITY_LOGIN,
						Constants.SERVICE_OP_IDENTITY_LOGOUT,
						Constants.SERVICE_OP_IDENTITY_CHANGE,
						Constants.SERVICE_OP_IDENTITY_VERIFY))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForIdentityManagement() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel create = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_IDENTITIES_PATH)
				.build();
		final HttpOperationModel update = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(AuthenticationConstants.HTTP_API_OP_IDENTITIES_PATH)
				.build();
		final HttpOperationModel remove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthenticationConstants.HTTP_API_OP_IDENTITIES_PATH)
				.build();
		final HttpOperationModel query = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_IDENTITIES_QUERY_PATH)
				.build();
		final HttpOperationModel close = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthenticationConstants.HTTP_API_OP_SESSION_PATH)
				.build();
		final HttpOperationModel querySessions = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_SESSION_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthenticationConstants.HTTP_API_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_CREATE, create)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_UPDATE, update)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_REMOVE, remove)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_QUERY, query)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_CLOSE, close)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_QUERY, querySessions)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForIdentityManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(AuthenticationConstants.MQTT_API_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_IDENTITY_MGMT_CREATE,
						Constants.SERVICE_OP_IDENTITY_MGMT_UPDATE,
						Constants.SERVICE_OP_IDENTITY_MGMT_REMOVE,
						Constants.SERVICE_OP_IDENTITY_MGMT_QUERY,
						Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_CLOSE,
						Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_QUERY))
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
				.basePath(AuthenticationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForGeneralManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(AuthenticationConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GET_LOG, Constants.SERVICE_OP_GET_CONFIG))
				.build();
	}
}