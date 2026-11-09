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

import java.util.UUID;

import eu.arrowhead.common.jpa.ArrowheadEntity;

public final class Constants {

	//=================================================================================================
	// members

	// Framework version

	public static final String AH_FRAMEWORK_VERSION = "5.2.2";

	// Global

	public static final String SYSOP = "Sysop";
	public static final String UTC = "UTC";
	public static final String LOCALHOST = "localhost";
	public static final String PKCS12 = "PKCS12";
	public static final String APPLICATION_PROPERTIES = "application.properties";
	public static final String BASE_PACKAGE = "eu.arrowhead";
	public static final String HTTPS = "https";
	public static final String HTTP = "http";
	public static final String SSL = "ssl";
	public static final String TCP = "tcp";
	public static final int HTTP_PORT = 80;
	public static final String UNKNOWN = "<unknown>";
	public static final String VERBOSE = "verbose";
	public static final String UNBOUND = "unbound";
	public static final String ARROWHEAD_CONTEXT = "arrowheadContext";
	public static final String SERVER_STANDALONE_MODE = "server.standalone.mode";
	public static final String SERVER_COMMON_NAME = "server.common.name";
	public static final String SERVER_PUBLIC_KEY = "server.public.key";
	public static final String SERVER_PRIVATE_KEY = "server.private.key";
	public static final String SERVER_CERTIFICATE = "server.certificate";

	public static final String KEY_IDENTITY_TOKEN = "identity-token";
	public static final String KEY_IDENTITY_RENEWAL_THRESHOLD = "identity-renewal-threshold";
	public static final String KEY_PREFIX_FOR_SERVICE_MODEL = "service-model$$";

	public static final String METADATA_KEY_UNRESTRICTED_DISCOVERY = "unrestrictedDiscovery";
	public static final String METADATA_KEY_X509_PUBLIC_KEY = "x509PublicKey";
	public static final String METADATA_KEY_ALLOW_EXCLUSIVITY = "allowExclusivity";
	public static final String METADATA_KEY_INTERFACE_BRIDGE = "interfaceBridge";
	public static final String METADATA_KEY_FROM = "from";
	public static final String METADATA_KEY_TO = "to";
	public static final String METADATA_KEY_DATA_MODEL_IDS = "dataModelIds";
	public static final String METADATA_KEY_EVALUATION_TYPE = "evaluationType";

	public static final String PROPERTY_KEY_DATA_MODELS = "dataModels";
	public static final String PROPERTY_KEY_INPUT = "input";
	public static final String PROPERTY_KEY_OUTPUT = "output";
	public static final String PROPERTY_KEY_QOS = "qos";
	public static final String PROPERTY_KEY_DEVICE_AUGMENTED = "deviceAugmented";

	public static final String GENERIC_HTTP_INTERFACE_TEMPLATE_NAME = "generic_http";
	public static final String GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME = "generic_https";
	public static final String GENERIC_MQTT_INTERFACE_TEMPLATE_NAME = "generic_mqtt";
	public static final String GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME = "generic_mqtts";
	public static final String GENERIC_AMQP_INTERFACE_TEMPLATE_NAME = "generic_amqp";
	public static final String GENERIC_AMQPS_INTERFACE_TEMPLATE_NAME = "generic_amqps";

	public static final int REQUEST_FILTER_ORDER_AUTHENTICATION = 15;
	public static final int REQUEST_FILTER_ORDER_AUTHORIZATION_BLACKLIST = 20;
	public static final int REQUEST_FILTER_ORDER_AUTHORIZATION_MGMT_SERVICE = 25;

	public static final int MIN_PORT = 1;
	public static final int MAX_PORT = 65535;
	public static final long CONVERSION_MILLISECOND_TO_SECOND = 1000;
	public static final long CONVERSION_MILLISECOND_TO_MINUTE = 60000;

	public static final String MAX_PAGE_SIZE = "max.page.size";
	public static final String $MAX_PAGE_SIZE_WD = "${" + MAX_PAGE_SIZE + ":" + Defaults.MAX_PAGE_SIZE_DEFAULT + "}";
	public static final String NORMALIZATION_MODE = "normalization.mode";
	public static final String $NORMALIZATION_MODE_WD = "${" + NORMALIZATION_MODE + ":" + Defaults.NORMALIZATION_MODE_DEFAULT + "}";

	public static final String COMMA = ",";
	public static final String DOT = ".";
	public static final String COMPOSITE_ID_DELIMITER = "|";
	public static final String COMPOSITE_ID_DELIMITER_REGEXP = "\\|";

	// System related

	public static final String BEAN_NAME_SYSTEM_INFO = "systemInfo";

	public static final String SYS_NAME_SERVICE_REGISTRY = "ServiceRegistry";
	public static final String SYS_NAME_AUTHENTICATION = "Authentication";
	public static final String SYS_NAME_CONSUMER_AUTHORIZATION = "ConsumerAuthorization";
	public static final String SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION = "DynamicServiceOrchestration";
	public static final String SYS_NAME_FLEXIBLE_SERVICE_ORCHESTRATION = "FlexibleServiceOrchestration";
	public static final String SYS_NAME_BLACKLIST = "Blacklist";
	public static final String SYS_NAME_TRANSLATION_MANAGER = "TranslationManager";
	public static final String SYS_NAME_DEVICE_QOS_EVALUATOR = "DeviceQoSEvaluator";

	public static final String SECURITY_REQ_AUTHORIZATION = "Authorization";

	public static final String DATABASE_URL = "spring.datasource.url";
	public static final String DATABASE_USER = "spring.datasource.username";
	public static final String DATABASE_PASSWORD = "spring.datasource.password";
	public static final String DATABASE_DRIVER_CLASS = "spring.datasource.driver-class-name";
	public static final String SERVER_ADDRESS = "server.address";
	public static final String $SERVER_ADDRESS = "${" + SERVER_ADDRESS + ":" + Defaults.SERVER_ADDRESS_DEFAULT + "}";
	public static final String SERVER_PORT = "server.port";
	public static final String $SERVER_PORT = "${" + SERVER_PORT + ":" + Defaults.SERVER_PORT_DEFAULT + "}";
	public static final String DOMAIN_NAME = "domain.name";
	public static final String $DOMAIN_NAME = "${" + DOMAIN_NAME + ":" + Defaults.DOMAIN_NAME_DEFAULT + "}";
	public static final String SERVICE_REGISTRY_ADDRESS = "service.registry.address";
	public static final String $SERVICE_REGISTRY_ADDRESS_WD = "${" + SERVICE_REGISTRY_ADDRESS + ":" + Defaults.SERVICE_REGISTRY_ADDRESS_DEFAULT + "}";
	public static final String SERVICE_REGISTRY_PORT = "service.registry.port";
	public static final String $SERVICE_REGISTRY_PORT_WD = "${" + SERVICE_REGISTRY_PORT + ":" + Defaults.SERVICE_REGISTRY_PORT_DEFAULT + "}";
	public static final String AUTHENTICATION_POLICY = "authentication.policy";
	public static final String $AUTHENTICATION_POLICY_WD = "${" + AUTHENTICATION_POLICY + ":" + Defaults.AUTHENTICATION_POLICY_DEFAULT + "}";
	public static final String AUTHENTICATOR_SECRET_KEYS = "authenticator.secret.keys";
	public static final String $AUTHENTICATOR_SECRET_KEYS = "#{${" + AUTHENTICATOR_SECRET_KEYS + ":null}}";
	public static final String AUTHENTICATOR_CREDENTIALS = "authenticator.credentials";
	public static final String $AUTHENTICATOR_CREDENTIALS = "#{${" + AUTHENTICATOR_CREDENTIALS + ":null}}";
	public static final String AUTHENTICATIOR_LOGIN_INTERVAL = "authenticator.login.interval";
	public static final String $AUTHENTICATOR_LOGIN_INTERVAL_WD = "${" + AUTHENTICATIOR_LOGIN_INTERVAL + ":10000}";
	public static final String AUTHENTICATOR_LOGIN_DELAY = "authenticator.login.delay";
	public static final String $AUTHENTICATOR_LOGIN_DELAY_WD = "${" + AUTHENTICATOR_LOGIN_DELAY + ":3000}";
	public static final String ENABLE_MANAGEMENT_FILTER = "enable.management.filter";
	public static final String MANAGEMENT_POLICY = "management.policy";
	public static final String $MANAGEMENT_POLICY = "${" + MANAGEMENT_POLICY + ":" + Defaults.MANAGEMENT_POLICY_DEFAULT + "}";
	public static final String MANAGEMENT_WHITELIST = "management.whitelist";
	public static final String $MANAGEMENT_WHITELIST = "${" + MANAGEMENT_WHITELIST + ":" + Defaults.MANAGEMENT_WHITELIST_DEFAULT + "}";
	public static final String ALLOW_SELF_ADDRESSING = "allow.self.addressing";
	public static final String $ALLOW_SELF_ADDRESSING_WD = "${" + ALLOW_SELF_ADDRESSING + ":" + Defaults.ALLOW_SELF_ADDRESSING_DEFAULT + "}";
	public static final String ALLOW_NON_ROUTABLE_ADDRESSING = "allow.non.routable.addressing";
	public static final String $ALLOW_NON_ROUTABLE_ADDRESSING_WD = "${" + ALLOW_NON_ROUTABLE_ADDRESSING + ":" + Defaults.ALLOW_NON_ROUTABLE_ADDRESSING_DEFAULT + "}";
	public static final String HTTP_COLLECTOR_MODE = "http.collector.mode";
	public static final String $HTTP_COLLECTOR_MODE_WD = "${" + HTTP_COLLECTOR_MODE + ":" + Defaults.HTTP_COLLECTOR_MODE_DEFAULT + "}";
	public static final String HTTP_COLLECTOR_BOOTSTRAP_SERVICES = "http.collector.bootstrap.services";
	public static final String $HTTP_COLLECTOR_BOOTSTRAP_SERVICES_WD = "${" + HTTP_COLLECTOR_BOOTSTRAP_SERVICES + ":" + Defaults.HTTP_COLLECTOR_BOOTSTRAP_SERVICES_DEFAULT + "}";
	public static final String HTTP_COLLECTOR_ORCHESTRATION_TTL = "http.collector.orchestration.ttl"; // seconds an orchestration result is reused
	public static final String $HTTP_COLLECTOR_ORCHESTRATION_TTL_WD = "${" + HTTP_COLLECTOR_ORCHESTRATION_TTL + ":" + Defaults.HTTP_COLLECTOR_ORCHESTRATION_TTL_DEFAULT + "}";
	public static final String ENABLE_BLACKLIST_FILTER = "enable.blacklist.filter";
	public static final String $ENABLE_BLACKLIST_FILTER_WD = "${" + ENABLE_BLACKLIST_FILTER + ":" + Defaults.ENABLE_BLACKLIST_FILTER_DEFAULT + "}";
	public static final String FORCE_BLACKLIST_FILTER = "force.blacklist.filter";
	public static final String $FORCE_BLACKLIST_FILTER_WD = "${" + FORCE_BLACKLIST_FILTER + ":" + Defaults.FORCE_BLACKLIST_FILTER_DEFAULT + "}";
	public static final String BLACKLIST_CHECK_EXCLUDE_LIST = "blacklist.check.exclude.list";
	public static final String $BLACKLIST_CHECK_EXCLUDE_LIST_WD = "${" + BLACKLIST_CHECK_EXCLUDE_LIST + ":" + Defaults.BLACKLIST_CHECK_EXCLUDE_LIST_DEFAULT + "}";
	public static final String SERVICE_ADDRESS_ALIAS = "service.address.alias";
	public static final String $SERVICE_ADDRESS_ALIAS = "${" + SERVICE_ADDRESS_ALIAS + "}";

	public static final String AUTHENTICATION_SCHEMA = "Bearer";
	public static final String AUTHENTICATION_KEY_DELIMITER = "//";
	public static final String AUTHENTICATION_PREFIX_AUTHENTICATOR_KEY = "AUTHENTICATOR-KEY";
	public static final String AUTHENTICATION_PREFIX_SYSTEM = "SYSTEM";
	public static final String AUTHENTICATION_PREFIX_IDENTITY_TOKEN = "IDENTITY-TOKEN";

	public static final String AUTHORIZATION_TOKEN_VARIANT_SUFFIX = "TOKEN_AUTH";
	public static final String AUTHORIZATION_SCHEMA = "Bearer";

	// SSL related

	public static final int SYSTEM_CERT_CN_LENGTH = 5;
	public static final int CLOUD_CERT_CN_LENGTH = 4;

	public static final String X_509 = "X.509";
	public static final String SERVER_SSL_ENABLED = "server.ssl.enabled";
	public static final String $SERVER_SSL_ENABLED_WD = "${" + SERVER_SSL_ENABLED + ":" + Defaults.SERVER_SSL_ENABLED_DEFAULT + "}";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String SERVER_SSL_KEY__STORE__TYPE = "server.ssl.key-store-type";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String $SERVER_SSL_KEY__STORE_TYPE_WD = "${" + SERVER_SSL_KEY__STORE__TYPE + ":" + Defaults.SERVER_SSL_KEY__STORE__TYPE_DEFAULT + "}";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String SERVER_SSL_KEY__STORE = "server.ssl.key-store";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String $SERVER_SSL_KEY__STORE_WD = "${" + SERVER_SSL_KEY__STORE + ":" + Defaults.SERVER_SSL_KEY__STORE_DEFAULT + "}";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String SERVER_SSL_KEY__STORE__PASSWORD = "server.ssl.key-store-password";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String $SERVER_SSL_KEY__STORE__PASSWORD_WD = "${" + SERVER_SSL_KEY__STORE__PASSWORD + ":" + Defaults.SERVER_SSL_KEY__STORE__PASSWORD_DEFAULT + "}";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String SERVER_SSL_KEY__PASSWORD = "server.ssl.key-password";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String $SERVER_SSL_KEY__PASSWORD_WD = "${" + SERVER_SSL_KEY__PASSWORD + ":" + Defaults.SERVER_SSL_KEY__PASSWORD_DEFAULT + "}";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String SERVER_SSL_KEY__ALIAS = "server.ssl.key-alias";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String $SERVER_SSL_KEY__ALIAS_WD = "${" + SERVER_SSL_KEY__ALIAS + ":" + Defaults.SERVER_SSL_KEY__ALIAS_DEFAULT + "}";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String SERVER_SSL_TRUST__STORE = "server.ssl.trust-store";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String $SERVER_SSL_TRUST__STORE_WD = "${" + SERVER_SSL_TRUST__STORE + ":" + Defaults.SERVER_SSL_TRUST__STORE_DEFAULT + "}";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String SERVER_SSL_TRUST__STORE__PASSWORD = "server.ssl.trust-store-password";
	@SuppressWarnings("checkstyle:ConstantName")
	public static final String $SERVER_SSL_TRUST__STORE__PASSWORD_WD = "${" + SERVER_SSL_TRUST__STORE__PASSWORD + ":" + Defaults.SERVER_SSL_TRUST__STORE__PASSWORD_DEFAULT + "}";
	public static final String DISABLE_HOSTNAME_VERIFIER = "disable.hostname.verifier";
	public static final String $DISABLE_HOSTNAME_VERIFIER_WD = "${" + DISABLE_HOSTNAME_VERIFIER + ":" + Defaults.DISABLE_HOSTNAME_VERIFIER_DEFAULT + "}";

	// HTTP related

	public static final String HTTP_STATUS_OK = "200";
	public static final String HTTP_STATUS_CREATED = "201";
	public static final String HTTP_STATUS_NO_CONTENT = "204";
	public static final String HTTP_STATUS_BAD_REQUEST = "400";
	public static final String HTTP_STATUS_UNAUTHORIZED = "401";
	public static final String HTTP_STATUS_FORBIDDEN = "403";
	public static final String HTTP_STATUS_NOT_FOUND = "404";
	public static final String HTTP_STATUS_LOCKED = "423";
	public static final String HTTP_STATUS_INTERNAL_SERVER_ERROR = "500";
	public static final String HTTP_STATUS_SERVICE_UNAVAILABLE = "503";

	public static final String HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM = "arrowhead.authenticated.system";
	public static final String HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST = "arrowhead.sysop.request";
	public static final String HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE = "jakarta.servlet.request.X509Certificate";

	public static final String HTTP_CLIENT_CONNECTION_TIMEOUT = "http.client.connection.timeout";
	public static final String $HTTP_CLIENT_CONNECTION_TIMEOUT_WD = "${" + HTTP_CLIENT_CONNECTION_TIMEOUT + ":" + Defaults.HTTP_CLIENT_CONNECTION_TIMEOUT_DEFAULT + "}";
	public static final String HTTP_CLIENT_SOCKET_TIMEOUT = "http.client.socket.timeout";
	public static final String $HTTP_CLIENT_SOCKET_TIMEOUT_WD = "${" + HTTP_CLIENT_SOCKET_TIMEOUT + ":" + Defaults.HTTP_CLIENT_SOCKET_TIMEOUT_DEFAULT + "}";
	public static final String LOG_ALL_REQUEST_AND_RESPONSE = "log.all.request.and.response";
	public static final String $LOG_ALL_REQUEST_AND_RESPONSE_WD = "${" + LOG_ALL_REQUEST_AND_RESPONSE + ":" + Defaults.LOG_ALL_REQUEST_AND_RESPONSE_DEFAULT + "}";

	// Swagger

	public static final String SWAGGER_API_DOCS_URI = "/v3/api-docs";
	public static final String SWAGGER_UI_URI = "/swagger-ui";
	public static final String SWAGGER_UI_INDEX_HTML = SWAGGER_UI_URI + "/index.html";
	public static final String SWAGGER_HTTP_200_MESSAGE = "Ok";
	public static final String SWAGGER_HTTP_201_MESSAGE = "Created";
	public static final String SWAGGER_HTTP_204_MESSAGE = "No changes was necessary";
	public static final String SWAGGER_HTTP_400_MESSAGE = "Bad request";
	public static final String SWAGGER_HTTP_401_MESSAGE = "You are not authenticated";
	public static final String SWAGGER_HTTP_403_MESSAGE = "You have no permission";
	public static final String SWAGGER_HTTP_404_MESSAGE = "Not found";
	public static final String SWAGGER_HTTP_423_MESSAGE = "Locked";
	public static final String SWAGGER_HTTP_500_MESSAGE = "Internal server error";
	public static final String SWAGGER_HTTP_503_MESSAGE = "Service unavailable";

	// CORS defaults

	public static final long CORS_MAX_AGE = 600;
	public static final boolean CORS_ALLOW_CREDENTIALS = true;
	public static final String CORS_ORIGIN_PATTERNS = "cors.origin.patterns";
	public static final String $CORS_ORIGIN_PATTERNS_WD = "${" + CORS_ORIGIN_PATTERNS + ":" + Defaults.CORS_ORIGIN_PATTERN_DEFAULT + "}";

	// MQTT related

	public static final String MQTT_AUTH_INFO_DELIMITER = "//";
	public static final String MQTT_AUTH_INFO_PREFIX_SYSTEM = "SYSTEM";
	public static final String MQTT_AUTH_INFO_PREFIX_IDENTITY_TOKEN = "IDENTITY-TOKEN";
	public static final String MQTT_AUTH_INFO_PREFIX_AUTH_TOKEN = "AUTH-TOKEN";
	public static final String MQTT_AUTH_INFO_PREFIX_AUTHENTICATOR_KEY = "AUTHENTICATOR-KEY";
	public static final String MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID = "SERVICE-PROVIDING-" + UUID.randomUUID().toString();
	public static final String MQTT_TOPIC_UNSUPPORTED = UUID.randomUUID().toString();
	public static final int MQTT_DEFAULT_QOS = 0;

	// Native sysMonitor publisher (SysMonitor-AMQP-IDD-5.2.2)
	public static final String SYS_MONITOR_ENABLED = "sysmonitor.enabled";
	public static final String $SYS_MONITOR_ENABLED_WD = "${" + SYS_MONITOR_ENABLED + ":" + Defaults.SYS_MONITOR_ENABLED_DEFAULT + "}";
	public static final String SYS_MONITOR_BROKER_ADDRESS = "sysmonitor.broker.address";
	public static final String $SYS_MONITOR_BROKER_ADDRESS_WD = "${" + SYS_MONITOR_BROKER_ADDRESS + ":" + Defaults.SYS_MONITOR_BROKER_ADDRESS_DEFAULT + "}";
	public static final String SYS_MONITOR_BROKER_PORT = "sysmonitor.broker.port";
	public static final String $SYS_MONITOR_BROKER_PORT_WD = "${" + SYS_MONITOR_BROKER_PORT + ":" + Defaults.SYS_MONITOR_BROKER_PORT_DEFAULT + "}";
	public static final String SYS_MONITOR_EXCHANGE = "sysmonitor.exchange";
	public static final String $SYS_MONITOR_EXCHANGE_WD = "${" + SYS_MONITOR_EXCHANGE + ":" + Defaults.SYS_MONITOR_EXCHANGE_DEFAULT + "}";
	public static final String SYS_MONITOR_LOCAL_CLOUD = "sysmonitor.local.cloud";
	public static final String $SYS_MONITOR_LOCAL_CLOUD_WD = "${" + SYS_MONITOR_LOCAL_CLOUD + ":" + Defaults.SYS_MONITOR_LOCAL_CLOUD_DEFAULT + "}";
	public static final String SYS_MONITOR_SERVICE_INSTANCE = "sysmonitor.service.instance";
	public static final String $SYS_MONITOR_SERVICE_INSTANCE_WD = "${" + SYS_MONITOR_SERVICE_INSTANCE + ":}";
	public static final String SYS_MONITOR_PUBLISH_INTERVAL = "sysmonitor.publish.interval";
	public static final String $SYS_MONITOR_PUBLISH_INTERVAL_WD = "${" + SYS_MONITOR_PUBLISH_INTERVAL + ":" + Defaults.SYS_MONITOR_PUBLISH_INTERVAL_DEFAULT + "}";
	public static final String SYS_MONITOR_SSL_ENABLED = "sysmonitor.ssl.enabled";
	public static final String $SYS_MONITOR_SSL_ENABLED_WD = "${" + SYS_MONITOR_SSL_ENABLED + ":" + Defaults.SYS_MONITOR_SSL_ENABLED_DEFAULT + "}";
	public static final String SYS_MONITOR_SSL_KEY_STORE = "sysmonitor.ssl.key-store";
	public static final String $SYS_MONITOR_SSL_KEY_STORE_WD = "${" + SYS_MONITOR_SSL_KEY_STORE + ":}";
	public static final String SYS_MONITOR_SSL_KEY_STORE_PASSWORD = "sysmonitor.ssl.key-store-password";
	public static final String $SYS_MONITOR_SSL_KEY_STORE_PASSWORD_WD = "${" + SYS_MONITOR_SSL_KEY_STORE_PASSWORD + ":}";
	public static final String SYS_MONITOR_SSL_TRUST_STORE = "sysmonitor.ssl.trust-store";
	public static final String $SYS_MONITOR_SSL_TRUST_STORE_WD = "${" + SYS_MONITOR_SSL_TRUST_STORE + ":}";
	public static final String SYS_MONITOR_SSL_TRUST_STORE_PASSWORD = "sysmonitor.ssl.trust-store-password";
	public static final String $SYS_MONITOR_SSL_TRUST_STORE_PASSWORD_WD = "${" + SYS_MONITOR_SSL_TRUST_STORE_PASSWORD + ":}";
	public static final String SYS_MONITOR_CONTRACT = "SysMonitor-AMQP-IDD-5.2.2";
	public static final String SYS_MONITOR_VERSION = "1.0.0";

	public static final String MQTT_API_ENABLED = "mqtt.api.enabled";
	public static final String $MQTT_API_ENABLED_WD = "${" + MQTT_API_ENABLED + ":" + Defaults.MQTT_API_ENABLED_DEFAULT + "}";
	public static final String MQTT_BROKER_ADDRESS = "mqtt.broker.address";
	public static final String $MQTT_BROKER_ADDRESS_WD = "${" + MQTT_BROKER_ADDRESS + ":" + Defaults.MQTT_BROKER_ADDRESS_DEFAULT + "}";
	public static final String MQTT_BROKER_PORT = "mqtt.broker.port";
	public static final String $MQTT_BROKER_PORT_WD = "${" + MQTT_BROKER_PORT + ":" + Defaults.MQTT_BROKER_PORT_DEFAULT + "}";
	public static final String MQTT_CLIENT_PASSWORD = "mqtt.client.password";
	public static final String $MQTT_CLIENT_PASSWORD = "${" + MQTT_CLIENT_PASSWORD + ":" + Defaults.MQTT_CLIENT_PASSWORD_DEFAULT + "}";

	// Service related

	public static final String SERVICE_DEF_GENERAL_MANAGEMENT = "generalManagement";

	public static final String SERVICE_DEF_DEVICE_DISCOVERY = "deviceDiscovery";
	public static final String SERVICE_DEF_SYSTEM_DISCOVERY = "systemDiscovery";
	public static final String SERVICE_DEF_SERVICE_DISCOVERY = "serviceDiscovery";
	public static final String SERVICE_DEF_SERVICE_REGISTRY_MANAGEMENT = "serviceRegistryManagement";
	public static final String SERVICE_DEF_BLACKLIST_DISCOVERY = "blacklistDiscovery";
	public static final String SERVICE_DEF_BLACKLIST_MANAGEMENT = "blacklistManagement";
	public static final String SERVICE_DEF_SYS_MONITOR = "sysMonitor";

	public static final String SERVICE_DEF_SERVICE_ORCHESTRATION = "serviceOrchestration";
	public static final String SERVICE_DEF_SERVICE_ORCHESTRATION_PUSH_MANAGEMENT = "serviceOrchestrationPushManagement";
	public static final String SERVICE_DEF_SERVICE_ORCHESTRATION_LOCK_MANAGEMENT = "serviceOrchestrationLockManagement";
	public static final String SERVICE_DEF_SERVICE_ORCHESTRATION_HISTORY_MANAGEMENT = "serviceOrchestrationHistoryManagement";
    public static final String SERVICE_DEF_SERVICE_ORCHESTRATION_SIMPLE_STORE_MANAGEMENT = "serviceOrchestrationSimpleStoreManagement";

	public static final String SERVICE_DEF_IDENTITY = "identity";
	public static final String SERVICE_DEF_IDENTITY_MANAGEMENT = "identityManagement";

	public static final String SERVICE_DEF_AUTHORIZATION = "authorization";
	public static final String SERVICE_DEF_AUTHORIZATION_MANAGEMENT = "authorizationManagement";
	public static final String SERVICE_DEF_AUTHORIZATION_TOKEN = "authorizationToken";
	public static final String SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT = "authorizationTokenManagement";

	public static final String SERVICE_DEF_TRANSLATION_REPORT = "translationReport";
	public static final String SERVICE_DEF_TRANSLATION_BRIDGE = "translationBridge";
	public static final String SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT = "translationBridgeManagement";

	public static final String SERVICE_DEF_INTERFACE_BRIDGE_MANAGEMENT = "interfaceBridgeManagement";
	public static final String SERVICE_DEF_DATA_MODEL_TRANSLATION = "dataModelTranslation";
	public static final String SERVICE_DEF_DATA_MODEL_TRANSLATOR_FACTORY_CONTROL = "dataModelTranslatorFactoryControl";

	public static final String SERVICE_DEF_QUALITY_EVALUATION = "qualityEvaluation";
	public static final String SERVICE_DEF_DEVICE_QUALITY_DATA_MANAGEMENT = "deviceQualityDataManagement";

	// Operation related

	public static final String SERVICE_OP_GET_LOG = "get-log";
	public static final String SERVICE_OP_GET_CONFIG = "get-config";
	public static final String SERVICE_OP_GET_CONFIG_REQ_PARAM = "keys";

	public static final String SERVICE_OP_ECHO = "echo";
	public static final String SERVICE_OP_REGISTER = "register";
	public static final String SERVICE_OP_LOOKUP = "lookup";
	public static final String SERVICE_OP_GRANT = "grant";
	public static final String SERVICE_OP_REVOKE = "revoke";
	public static final String SERVICE_OP_CHECK = "check";
	public static final String SERVICE_OP_VERIFY = "verify";
	public static final String SERVICE_OP_GENERATE = "generate";
	public static final String SERVICE_OP_GET_PUBLIC_KEY = "get-public-key";
	public static final String SERVICE_OP_REPORT = "report";
	public static final String SERVICE_OP_DISCOVERY = "discovery";
	public static final String SERVICE_OP_NEGOTIATION = "negotiation";
	public static final String SERVICE_OP_ABORT = "abort";
	public static final String SERVICE_OP_QUERY = "query";
	public static final String SERVICE_OP_FILTER = "filter";
	public static final String SERVICE_OP_SORT = "sort";
	public static final String SERVICE_OP_RELOAD = "reload";

	public static final String SERVICE_OP_DEVICE_QUERY = "device-query";
	public static final String SERVICE_OP_DEVICE_CREATE = "device-create";
	public static final String SERVICE_OP_DEVICE_UPDATE = "device-update";
	public static final String SERVICE_OP_DEVICE_REMOVE = "device-remove";
	public static final String SERVICE_OP_SYSTEM_QUERY = "system-query";
	public static final String SERVICE_OP_SYSTEM_CREATE = "system-create";
	public static final String SERVICE_OP_SYSTEM_UPDATE = "system-update";
	public static final String SERVICE_OP_SYSTEM_REMOVE = "system-remove";
	public static final String SERVICE_OP_SERVICE_DEF_QUERY = "service-definition-query";
	public static final String SERVICE_OP_SERVICE_DEF_CREATE = "service-definition-create";
	public static final String SERVICE_OP_SERVICE_DEF_REMOVE = "service-definition-remove";
	public static final String SERVICE_OP_SERVICE_QUERY = "service-query";
	public static final String SERVICE_OP_SERVICE_CREATE = "service-create";
	public static final String SERVICE_OP_SERVICE_UPDATE = "service-update";
	public static final String SERVICE_OP_SERVICE_REMOVE = "service-remove";
	public static final String SERVICE_OP_INTERFACE_TEMPLATE_QUERY = "interface-template-query";
	public static final String SERVICE_OP_INTERFACE_TEMPLATE_CREATE = "interface-template-create";
	public static final String SERVICE_OP_INTERFACE_TEMPLATE_REMOVE = "interface-template-remove";

	public static final String SERVICE_OP_BLACKLIST_QUERY = "query";
	public static final String SERVICE_OP_BLACKLIST_CREATE = "create";
	public static final String SERVICE_OP_BLACKLIST_REMOVE = "remove";

	public static final String SERVICE_OP_ORCHESTRATION_PULL = "pull";
	public static final String SERVICE_OP_ORCHESTRATION_SUBSCRIBE = "subscribe";
	public static final String SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE = "unsubscribe";
	public static final String SERVICE_OP_ORCHESTRATION_TRIGGER = "trigger";
	public static final String SERVICE_OP_ORCHESTRATION_QUERY = "query";
	public static final String SERVICE_OP_ORCHESTRATION_REMOVE = "remove";
	public static final String SERVICE_OP_ORCHESTRATION_CREATE = "create";
    public static final String SERVICE_OP_ORCHESTRATION_MODIFY_PRIORITIES = "modify-priorities";

	public static final String SERVICE_OP_IDENTITY_LOGIN = "identity-login";
	public static final String SERVICE_OP_IDENTITY_LOGOUT = "identity-logout";
	public static final String SERVICE_OP_IDENTITY_CHANGE = "identity-change-credentials";
	public static final String SERVICE_OP_IDENTITY_VERIFY = "identity-verify";

	public static final String SERVICE_OP_IDENTITY_MGMT_CREATE = "identity-mgmt-create";
	public static final String SERVICE_OP_IDENTITY_MGMT_UPDATE = "identity-mgmt-update";
	public static final String SERVICE_OP_IDENTITY_MGMT_REMOVE = "identity-mgmt-remove";
	public static final String SERVICE_OP_IDENTITY_MGMT_QUERY = "identity-mgmt-query";
	public static final String SERVICE_OP_IDENTITY_MGMT_SESSION_CLOSE = "identity-mgmt-session-close";
	public static final String SERVICE_OP_IDENTITY_MGMT_SESSION_QUERY = "identity-mgmt-session-query";

	public static final String SERVICE_OP_AUTHORIZATION_GRANT_POLICIES = "grant-policies";
	public static final String SERVICE_OP_AUTHORIZATION_REVOKE_POLICIES = "revoke-policies";
	public static final String SERVICE_OP_AUTHORIZATION_QUERY_POLICIES = "query-policies";
	public static final String SERVICE_OP_AUTHORIZATION_CHECK_POLICIES = "check-policies";
	public static final String SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS = "generate-tokens";
	public static final String SERVICE_OP_AUTHORIZATION_QUERY_TOKENS = "query-tokens";
	public static final String SERVICE_OP_AUTHORIZATION_REVOKE_TOKENS = "revoke-tokens";
	public static final String SERVICE_OP_AUTHORIZATION_ADD_ENCRYPTION_KEYS = "add-encryption-keys";
	public static final String SERVICE_OP_AUTHORIZATION_REMOVE_ENCRYPTION_KEYS = "remove-encryption-keys";
	public static final String SERVICE_OP_AUTHORIZATION_TOKEN_REGISTER_ENCRYPTION_KEY = "register-encryption-key";
	public static final String SERVICE_OP_AUTHORIZATION_TOKEN_UNREGISTER_ENCRYPTION_KEY = "unregister-encryption-key";

	public static final String SERVICE_OP_INTERFACE_TRANSLATOR_CHECK_TARGETS = "check-targets";
	public static final String SERVICE_OP_INTERFACE_TRANSLATOR_INIT_BRIDGE = "initialize-bridge";
	public static final String SERVICE_OP_INTERFACE_TRANSLATOR_ABORT_BRIDGE = "abort-bridge";
	public static final String SERVICE_OP_DATA_MODEL_TRANSLATOR_FACTORY_CHECK = "check";
	public static final String SERVICE_OP_DATA_MODEL_TRANSLATOR_FACTORY_INITIALIZE = "initialize";
	public static final String SERVICE_OP_DATA_MODEL_TRANSLATOR_INIT_TRANSLATION = "init-translation";
	public static final String SERVICE_OP_DATA_MODEL_TRANSLATOR_GET_TRANSLATION_RESULT = "get-translation-result";
	public static final String SERVICE_OP_DATA_MODEL_TRANSLATOR_ABORT_TRANSLATION = "abort-translation";

	public static final String HTTP_API_OP_ECHO_PATH = "/echo";
	public static final String HTTP_API_OP_LOGS_PATH = "/logs";
	public static final String HTTP_API_OP_GET_CONFIG_PATH = "/get-config";

	// Common property related

	public static final int DEVICE_NAME_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int SYSTEM_NAME_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int SYSTEM_VERSION_MAX_LENGTH = ArrowheadEntity.VARCHAR_TINY;
	public static final int SERVICE_DEFINITION_NAME_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int SERVICE_OPERATION_NAME_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int ADDRESS_MAX_LENGTH = ArrowheadEntity.VARCHAR_LARGE;
	public static final int INTERFACE_TEMPLATE_NAME_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int INTERFACE_TEMPLATE_PROTOCOL_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int INTERFACE_PROPERTY_NAME_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int SERVICE_INSTANCE_ID_MAX_LENGTH = ArrowheadEntity.VARCHAR_MEDIUM;
	public static final int CLOUD_IDENTIFIER_MAX_LENGTH = 2 * SYSTEM_NAME_MAX_LENGTH + COMPOSITE_ID_DELIMITER.length();
	public static final int EVENT_TYPE_NAME_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int SCOPE_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int AUTHORIZATION_POLICY_ID_MAX_LENGTH = ArrowheadEntity.VARCHAR_LARGE;
	public static final int DATA_MODEL_ID_MAX_LENGTH = ArrowheadEntity.VARCHAR_SMALL;

	// Quartz related

	public static final String OUTSOURCED_LOGIN_TRIGGER = "outsourcedLoginTrigger";
	public static final String OUTSOURCED_LOGIN_JOB_FACTORY = "outsourcedLoginJobFactory";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Constants() {
		throw new UnsupportedOperationException();
	}
}
