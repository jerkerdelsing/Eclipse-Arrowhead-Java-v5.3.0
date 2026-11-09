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
package eu.arrowhead.authorization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class AuthorizationSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	@Value(AuthorizationConstants.$TOKEN_MAX_AGE_WD)
	private int tokenMaxAge;

	@Value(AuthorizationConstants.$TOKEN_TIME_LIMIT_WD)
	private int tokenTimeLimit;

	@Value(AuthorizationConstants.$SIMPLE_TOKEN_BYTE_SIZE_WD)
	private int simpleTokenByteSize;

	@Value(AuthorizationConstants.$SIMPLE_TOKEN_USAGE_LIMIT_WD)
	private int simpleTokenUsageLimit;

	@Value(AuthorizationConstants.$UNBOUNDED_TOKEN_GENERATION_WHITELIST_WD)
	private List<String> unboundedTokenGenerationWhitelist;
	private List<String> normalizedUnboundedTokenGenerationWhitelist = new ArrayList<>();

	@Value(AuthorizationConstants.$SECRET_CRYPTOGRAPHER_KEY)
	private String secretCryptographerKey;

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return Constants.SYS_NAME_CONSUMER_AUTHORIZATION;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION);

			if (this.isSslEnabled()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel authorization = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_AUTHORIZATION)
				.version(AuthorizationConstants.VERSION_AUTHORIZATION)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForAuthorization())
				.serviceInterface(getMqttServiceInterfaceForAuthorization())
				.build();

		final ServiceModel authorizationToken = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_AUTHORIZATION_TOKEN)
				.version(AuthorizationConstants.VERSION_AUTHORIZATION_TOKEN)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForAuthorizationToken())
				.serviceInterface(getMqttServiceInterfaceForAuthorizationToken())
				.build();

		final ServiceModel authorizationManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT)
				.version(AuthorizationConstants.VERSION_AUTHORIZATION_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForAuthorizationManagement())
				.serviceInterface(getMqttServiceInterfaceForAuthorizationManagement())
				.build();

		final ServiceModel authorizationTokenManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT)
				.version(AuthorizationConstants.VERSION_AUTHORIZATION_TOKEN_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForAuthorizationTokenManagement())
				.serviceInterface(getMqttServiceInterfaceForAuthorizationTokenManagement())
				.build();

		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(AuthorizationConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.serviceInterface(getMqttServiceInterfaceForGeneralManagement())
				.build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, authorizationManagement, authorizationTokenManagement, authorization, authorizationToken);
	}

	//-------------------------------------------------------------------------------------------------
	public int getTokenMaxAge() {
		return tokenMaxAge;
	}

	//-------------------------------------------------------------------------------------------------
	public int getTokenTimeLimit() {
		return tokenTimeLimit;
	}

	//-------------------------------------------------------------------------------------------------
	public int getSimpleTokenByteSize() {
		return simpleTokenByteSize;
	}

	//-------------------------------------------------------------------------------------------------
	public int getSimpleTokenUsageLimit() {
		return simpleTokenUsageLimit;
	}

	//-------------------------------------------------------------------------------------------------
	public String getSecretCryptographerKey() {
		return secretCryptographerKey;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasSystemUnboundedTokenGenerationRight(final String systemName) {
		if (Utilities.isEmpty(unboundedTokenGenerationWhitelist)) {
			return false;
		} else if (Utilities.isEmpty(normalizedUnboundedTokenGenerationWhitelist)) {
			normalizedUnboundedTokenGenerationWhitelist = unboundedTokenGenerationWhitelist
					.stream()
					.map(sys -> systemNameNormalizer.normalize(sys))
					.toList();
		}

		return normalizedUnboundedTokenGenerationWhitelist.contains(systemName);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.MQTT_API_ENABLED,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY,
						Constants.ENABLE_BLACKLIST_FILTER,
						Constants.FORCE_BLACKLIST_FILTER,
						Constants.MAX_PAGE_SIZE,
						Constants.NORMALIZATION_MODE,
						AuthorizationConstants.TOKEN_MAX_AGE,
						AuthorizationConstants.TOKEN_TIME_LIMIT,
						AuthorizationConstants.SIMPLE_TOKEN_BYTE_SIZE,
						AuthorizationConstants.SIMPLE_TOKEN_USAGE_LIMIT),
				AuthorizationDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAuthorization() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel grant = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_GRANT_PATH)
				.build();
		final HttpOperationModel revoke = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthorizationConstants.HTTP_API_OP_SERVICE_REVOKE_PATH)
				.build();
		final HttpOperationModel lookup = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_LOOKUP_PATH)
				.build();
		final HttpOperationModel verify = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_VERIFY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthorizationConstants.HTTP_API_AUTHORIZATION_PATH)
				.operation(Constants.SERVICE_OP_GRANT, grant)
				.operation(Constants.SERVICE_OP_REVOKE, revoke)
				.operation(Constants.SERVICE_OP_LOOKUP, lookup)
				.operation(Constants.SERVICE_OP_VERIFY, verify)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForAuthorization() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(AuthorizationConstants.MQTT_API_AUTHORIZATION_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GRANT,
						Constants.SERVICE_OP_REVOKE,
						Constants.SERVICE_OP_LOOKUP,
						Constants.SERVICE_OP_VERIFY))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAuthorizationToken() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel generate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_GENERATE_PATH)
				.build();
		final HttpOperationModel verify = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(AuthorizationConstants.HTTP_API_OP_TOKEN_VERIFY_PATH)
				.build();
		final HttpOperationModel getPublicKey = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(AuthorizationConstants.HTTP_API_OP_PUBLIC_KEY_PATH)
				.build();
		final HttpOperationModel registerEncryptionKey = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH)
				.build();
		final HttpOperationModel unregisterEncryptionKey = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthorizationConstants.HTTP_API_AUTHORIZATION_TOKEN_PATH)
				.operation(Constants.SERVICE_OP_GENERATE, generate)
				.operation(Constants.SERVICE_OP_VERIFY, verify)
				.operation(Constants.SERVICE_OP_GET_PUBLIC_KEY, getPublicKey)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_TOKEN_REGISTER_ENCRYPTION_KEY, registerEncryptionKey)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_TOKEN_UNREGISTER_ENCRYPTION_KEY, unregisterEncryptionKey)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForAuthorizationToken() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(AuthorizationConstants.MQTT_API_AUTHORIZATION_TOKEN_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GENERATE,
						Constants.SERVICE_OP_VERIFY,
						Constants.SERVICE_OP_GET_PUBLIC_KEY,
						Constants.SERVICE_OP_AUTHORIZATION_TOKEN_REGISTER_ENCRYPTION_KEY,
						Constants.SERVICE_OP_AUTHORIZATION_TOKEN_UNREGISTER_ENCRYPTION_KEY))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAuthorizationManagement() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel grant = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_GRANT_PATH)
				.build();
		final HttpOperationModel revoke = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthorizationConstants.HTTP_API_OP_REVOKE_PATH)
				.build();
		final HttpOperationModel query = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_QUERY_PATH)
				.build();
		final HttpOperationModel check = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_CHECK_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthorizationConstants.HTTP_API_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_GRANT_POLICIES, grant)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_REVOKE_POLICIES, revoke)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_QUERY_POLICIES, query)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES, check)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForAuthorizationManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(AuthorizationConstants.MQTT_API_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_AUTHORIZATION_GRANT_POLICIES,
						Constants.SERVICE_OP_AUTHORIZATION_REVOKE_POLICIES,
						Constants.SERVICE_OP_AUTHORIZATION_QUERY_POLICIES,
						Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAuthorizationTokenManagement() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel generateTokens = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_GENERATE_PATH)
				.build();
		final HttpOperationModel queryTokens = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_QUERY_PATH)
				.build();
		final HttpOperationModel revokeTokens = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthorizationConstants.HTTP_API_OP_REVOKE_PATH)
				.build();
		final HttpOperationModel addEncryptionKeys = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH)
				.build();
		final HttpOperationModel removeEncryptionKeys = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthorizationConstants.HTTP_API_MANAGEMENT_PATH + AuthorizationConstants.HTTP_API_TOKEN_SUB_PATH)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS, generateTokens)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_QUERY_TOKENS, queryTokens)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_REVOKE_TOKENS, revokeTokens)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_ADD_ENCRYPTION_KEYS, addEncryptionKeys)
				.operation(Constants.SERVICE_OP_AUTHORIZATION_REMOVE_ENCRYPTION_KEYS, removeEncryptionKeys)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForAuthorizationTokenManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(AuthorizationConstants.MQTT_API_TOKEN_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS,
						Constants.SERVICE_OP_AUTHORIZATION_QUERY_TOKENS,
						Constants.SERVICE_OP_AUTHORIZATION_REVOKE_TOKENS,
						Constants.SERVICE_OP_AUTHORIZATION_ADD_ENCRYPTION_KEYS,
						Constants.SERVICE_OP_AUTHORIZATION_REMOVE_ENCRYPTION_KEYS))
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
				.basePath(AuthorizationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
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
				.baseTopic(AuthorizationConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GET_LOG, Constants.SERVICE_OP_GET_CONFIG))
				.build();
	}
}