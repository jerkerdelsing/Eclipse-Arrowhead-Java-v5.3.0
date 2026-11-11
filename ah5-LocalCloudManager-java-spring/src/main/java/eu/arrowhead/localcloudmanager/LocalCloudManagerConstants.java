package eu.arrowhead.localcloudmanager;

public final class LocalCloudManagerConstants {

	public static final String SYSTEM_NAME = "LocalCloudManager";
	public static final String SERVICE_DEF_LOCAL_CLOUD_MANAGEMENT = "localCloudManagement";
	public static final String SERVICE_VERSION = "1.0.0";

	public static final String HTTP_API_BASE_PATH = "/api";
	public static final String HTTP_API_SESSION_PATH = "/api/session";
	public static final String HTTP_HEALTH_PATH = "/health";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = "/localcloudmanager/general/mgmt";

	public static final String ACTION_HEADER = "X-Arrowhead-Action";
	public static final String ACTION_CONFIRMATION = "confirm";
	public static final int MAX_BODY_BYTES = 64 * 1024;
	public static final int HISTORY_LIMIT = 60;

	// configuration keys
	public static final String CONSOLE_PORT = "console.port";
	public static final String $CONSOLE_PORT_WD = "${" + CONSOLE_PORT + ":8490}";
	public static final String CONSOLE_ADDRESS = "console.address";
	public static final String $CONSOLE_ADDRESS_WD = "${" + CONSOLE_ADDRESS + ":127.0.0.1}";
	public static final String $CONSOLE_PASSWORD_HASH_WD = "${console.password.hash:}";
	public static final String $CONSOLE_ALLOWED_HOSTS_WD = "${console.allowed.hosts:127.0.0.1,localhost,[::1]}";
	public static final String $CONSOLE_PROXY_SECRET_WD = "${console.proxy-secret:}";
	public static final String $CONSOLE_SESSION_SECONDS_WD = "${console.session.seconds:28800}";
	public static final String $CHIPLET_MANAGER_WD = "${lcm.chiplet-manager:false}";
	public static final String $CHIPLET_FLOORPLANS_WD = "${lcm.chiplet-floorplans.enabled:false}";
	public static final String $LOCAL_CLOUD_CONFIGURATION_WD = "${lcm.local-cloud-configuration.enabled:true}";
	public static final String $SO_LOCAL_CLOUD_CONFIGURATION_WD = "${lcm.so-local-cloud-configuration.enabled:false}";
	// a System of LocalClouds deployment package (SoLocalCloudConfigurator, tar.gz without source or image archives)
	public static final int MAX_PACKAGE_BYTES = 32 << 20;
	public static final String $PROJECT_NAME_WD = "${lcm.compose-project:arrowhead-v52-test-cloud}";
	public static final String $DOCKER_URL_WD = "${lcm.docker.url:http://docker-proxy:2375}";
	public static final String $MANAGED_SERVICES_WD = "${lcm.managed-services:}";
	public static final String $LOCAL_CLOUD_WD = "${lcm.local-cloud:TestCloud}";
	public static final String $RELEASE_WD = "${lcm.release:5.2.2}";
	public static final String $ORCHESTRATOR_SYSTEM_WD = "${lcm.orchestrator-system:DynamicServiceOrchestration}";
	public static final String $PRIORITY_MODE_WD = "${lcm.orchestration-priority-mode:native}";
	public static final String $CRL_FILE_WD = "${arrowhead.ssl.crl-file:}";
	public static final String API_WHITELIST = "lcm.api.whitelist";
	public static final String $API_WHITELIST_WD = "${" + API_WHITELIST + ":}";
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":true}";

	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30000;
	public static final int REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION = 30;

	private LocalCloudManagerConstants() {
		throw new UnsupportedOperationException();
	}
}
