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
package eu.arrowhead.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.filter.authorization.ManagementPolicy;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

public abstract class SystemInfo {

	//=================================================================================================
	// members

	private static final String DEFAULT_SUFFIX = "DEFAULT";
	private static final String KEY_DELIMITER_REGEX = "\\.";
	private static final String KEY_HYPHEN_REGEX = "-";
	private static final String FIELD_DELIMITER = "_";

	@Value(Constants.$SERVER_ADDRESS)
	private String serverAddress;

	@Value(Constants.$SERVER_PORT)
	private int serverPort;

	@Value(Constants.$DOMAIN_NAME)
	private String domainAddress;

	@Value(Constants.$SERVICE_REGISTRY_ADDRESS_WD)
	private String serviceRegistryAddress;

	@Value(Constants.$SERVICE_REGISTRY_PORT_WD)
	private int serviceRegistryPort;

	@Value(Constants.$AUTHENTICATION_POLICY_WD)
	private AuthenticationPolicy authenticationPolicy;

	@Value(Constants.$AUTHENTICATOR_LOGIN_DELAY_WD)
	private long authenticatorLoginDelay;

	@Value(Constants.$AUTHENTICATOR_CREDENTIALS)
	private Map<String, String> authenticatorCredentials;

	@Value(Constants.$MANAGEMENT_POLICY)
	private ManagementPolicy managementPolicy;

	@Value(Constants.$MANAGEMENT_WHITELIST)
	private List<String> managementWhitelist;
	private final List<String> normalizedManagementWhitelist = new ArrayList<>();

	@Value(Constants.$BLACKLIST_CHECK_EXCLUDE_LIST_WD)
	private List<String> blacklistCheckExcludeList;
	private final List<String> normalizedBlacklistCheckExcludeList = new ArrayList<>();

	@Value(Constants.$MQTT_API_ENABLED_WD)
	private boolean mqttEnabled;

	@Value(Constants.$MQTT_BROKER_ADDRESS_WD)
	private String mqttBrokerAddress;

	@Value(Constants.$MQTT_BROKER_PORT_WD)
	private Integer mqttBrokerPort;

	@Value(Constants.$MQTT_CLIENT_PASSWORD)
	private String mqttClientPassword;

	@Autowired
	private SSLProperties sslProperties;

	@Autowired
	protected SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private AddressNormalizer addressNormalizer;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	private Map<String, String> configDefaultsMap = new HashMap<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public abstract String getSystemName();

	//-------------------------------------------------------------------------------------------------
	public abstract SystemModel getSystemModel();

	//-------------------------------------------------------------------------------------------------
	public abstract List<ServiceModel> getServices();

	//-------------------------------------------------------------------------------------------------
	public String getIdentityToken() {
		return authenticationPolicy == AuthenticationPolicy.OUTSOURCED ? (String) arrowheadContext.get(Constants.KEY_IDENTITY_TOKEN) : null;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getManagementWhitelist() {
		if (!Utilities.isEmpty(managementWhitelist) && Utilities.isEmpty(normalizedManagementWhitelist)) {
			for (final String name : managementWhitelist) {
				if (!Utilities.isEmpty(name)) {
					normalizedManagementWhitelist.add(systemNameNormalizer.normalize(name));
				}
			}
		}

		return normalizedManagementWhitelist;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getBlacklistCheckExcludeList() {
		if (!Utilities.isEmpty(blacklistCheckExcludeList) && Utilities.isEmpty(normalizedBlacklistCheckExcludeList)) {
			for (final String name : blacklistCheckExcludeList) {
				if (!Utilities.isEmpty(name)) {
					normalizedBlacklistCheckExcludeList.add(systemNameNormalizer.normalize(name));
				}
			}
		}

		return normalizedBlacklistCheckExcludeList;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isSslEnabled() {
		return sslProperties != null && sslProperties.isSslEnabled();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	protected abstract PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults();

	//-------------------------------------------------------------------------------------------------
	protected void customInit() {
	};

	//-------------------------------------------------------------------------------------------------
	protected String getAddress() {
		return addressNormalizer.normalize(domainAddress);
	}

	//-------------------------------------------------------------------------------------------------
	protected String getPublicKey() {
		if (arrowheadContext.containsKey(Constants.SERVER_PUBLIC_KEY)) {
			final PublicKey publicKey = (PublicKey) arrowheadContext.get(Constants.SERVER_PUBLIC_KEY);

			return Base64.getEncoder().encodeToString(publicKey.getEncoded());
		}

		return "";
	}

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		if (Utilities.isEmpty(getSystemName())) {
			throw new InvalidParameterException("System name is missing or empty");
		}

		if (Utilities.isEmpty(domainAddress)) {
			throw new InvalidParameterException("'domainAddress' is missing or empty");
		}

		if (mqttEnabled && Utilities.isEmpty(mqttBrokerAddress)) {
			throw new InvalidParameterException("MQTT Broker address is not defined");
		}

		if (mqttEnabled && mqttBrokerPort == null) {
			throw new InvalidParameterException("MQTT Broker port is not defined");
		}

		if (AuthenticationPolicy.OUTSOURCED == authenticationPolicy && Utilities.isEmpty(authenticatorCredentials)) {
			throw new InvalidParameterException("No credentials are specified to login to the authentication system");
		}

		collectConfigDefaults();

		customInit();
	}

	//-------------------------------------------------------------------------------------------------
	private void collectConfigDefaults() {
		final PublicConfigurationKeysAndDefaults configInfo = getPublicConfigurationKeysAndDefaults();
		if (configInfo != null && configInfo.defaultsClass() != null && !Utilities.isEmpty(configInfo.configKeys())) {
			final Class<?> defaults = configInfo.defaultsClass();
			final Set<String> configKeys = configInfo.configKeys();

			final Map<String, String> defaultsMap = new HashMap<>(configKeys.size());
			for (final String key : configKeys) {
				final String fieldName = transformConfigKeyToDefaultFieldName(key);
				try {
					final Field field = defaults.getField(fieldName);
					if (!Modifier.isStatic(field.getModifiers())) {
						// field is not static => ignore
						defaultsMap.put(key, null);
					} else {
						defaultsMap.put(key, field.get(null).toString());
					}
				} catch (final NoSuchFieldException __) {
					// no default
					defaultsMap.put(key, null);
				} catch (final IllegalArgumentException __) {
					// never happens
					throw new IllegalStateException("Something that should never happen, happened");
				} catch (final IllegalAccessException ex) {
					throw new ServiceConfigurationError("Java security does not allow to read the default values from class " + defaults.getName());
				}
			}

			this.configDefaultsMap = Collections.unmodifiableMap(defaultsMap);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private String transformConfigKeyToDefaultFieldName(final String key) {
		return key.trim()
				.toUpperCase()
				.replaceAll(KEY_DELIMITER_REGEX, FIELD_DELIMITER)
				.replaceAll(KEY_HYPHEN_REGEX, FIELD_DELIMITER + FIELD_DELIMITER)
				+ FIELD_DELIMITER
				+ DEFAULT_SUFFIX;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getServerAddress() {
		return serverAddress;
	}

	//-------------------------------------------------------------------------------------------------
	public int getServerPort() {
		return serverPort;
	}

	//-------------------------------------------------------------------------------------------------
	public String getDomainAddress() {
		return domainAddress;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceRegistryAddress() {
		return serviceRegistryAddress;
	}

	//-------------------------------------------------------------------------------------------------
	public int getServiceRegistryPort() {
		return serviceRegistryPort;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthenticationPolicy getAuthenticationPolicy() {
		return authenticationPolicy;
	}

	//-------------------------------------------------------------------------------------------------
	public ManagementPolicy getManagementPolicy() {
		return managementPolicy;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, String> getConfigDefaultsMap() {
		return configDefaultsMap;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isMqttApiEnabled() {
		return this.mqttEnabled;
	}

	//-------------------------------------------------------------------------------------------------
	public String getMqttBrokerAddress() {
		return this.mqttBrokerAddress;
	}

	//-------------------------------------------------------------------------------------------------
	public String getMqttClientPassword() {
		return this.mqttClientPassword;
	}

	//-------------------------------------------------------------------------------------------------
	public Integer getMqttBrokerPort() {
		return this.mqttBrokerPort;
	}

	//-------------------------------------------------------------------------------------------------
	public SSLProperties getSslProperties() {
		return sslProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> getArrowheadContext() {
		return arrowheadContext;
	}

	//-------------------------------------------------------------------------------------------------
	public long getAuthenticatorLoginDelay() {
		return authenticatorLoginDelay;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, String> getAuthenticatorCredentials() {
		return Collections.unmodifiableMap(authenticatorCredentials);
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	public record PublicConfigurationKeysAndDefaults(Set<String> configKeys, Class<?> defaultsClass) {
	}
}