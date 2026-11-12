package eu.arrowhead.localcloudconfigurator;

public final class LocalCloudConfiguratorConstants {

	public static final String SYSTEM_NAME = "LocalCloudConfigurator";
	public static final String SYSTEM_VERSION = "5.3.0";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.localcloudconfigurator.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.localcloudconfigurator.jpa.repository";

	// localCloudConfiguration service: the /api paths of the native 5.2.2 operator API
	public static final String SERVICE_DEF_LOCAL_CLOUD_CONFIGURATION = "localCloudConfiguration";
	public static final String SERVICE_VERSION = "1.0.0";
	public static final String SERVICE_OP_CATALOG = "catalog";
	public static final String SERVICE_OP_STATUS = "status";
	public static final String SERVICE_OP_PLAN = "plan";
	public static final String SERVICE_OP_SUBMIT_DEPLOYMENT = "submit-deployment";
	public static final String SERVICE_OP_GET_DEPLOYMENT = "get-deployment";
	public static final String SERVICE_OP_LIST_CONFIGURATIONS = "list-configurations";
	public static final String SERVICE_OP_CREATE_CONFIGURATION = "create-configuration";
	public static final String SERVICE_OP_GET_CONFIGURATION = "get-configuration";
	public static final String SERVICE_OP_UPDATE_CONFIGURATION = "update-configuration";
	public static final String SERVICE_OP_REMOVE_CONFIGURATION = "remove-configuration";

	public static final String HTTP_API_BASE_PATH = "/api";
	public static final String HTTP_API_OP_CATALOG_PATH = "/catalog";
	public static final String HTTP_API_OP_STATUS_PATH = "/status";
	public static final String HTTP_API_OP_PLAN_PATH = "/plan";
	public static final String HTTP_API_OP_DEPLOYMENTS_PATH = "/deployments";
	public static final String HTTP_API_OP_DEPLOYMENT_PATH = "/deployments/{id}";
	public static final String HTTP_API_OP_CONFIGURATIONS_PATH = "/configurations";
	public static final String HTTP_API_OP_CONFIGURATION_PATH = "/configurations/{id}";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = "/localcloudconfigurator/general/mgmt";

	public static final String DEPLOYMENT_ID_PATTERN = "[A-Za-z0-9-]{1,128}";
	public static final String CONFIGURATION_ID_PATTERN = "[a-f0-9-]{36}";

	// configuration keys
	public static final String CATALOG_FILE = "lcc.catalog.file";
	public static final String $CATALOG_FILE_WD = "${" + CATALOG_FILE + ":" + LocalCloudConfiguratorDefaults.CATALOG_FILE_DEFAULT + "}";
	public static final String BACKEND = "lcc.backend";
	public static final String $BACKEND_WD = "${" + BACKEND + ":" + LocalCloudConfiguratorDefaults.BACKEND_DEFAULT + "}";
	public static final String WORKER_URL = "lcc.worker.url";
	public static final String $WORKER_URL_WD = "${" + WORKER_URL + ":" + LocalCloudConfiguratorDefaults.WORKER_URL_DEFAULT + "}";
	public static final String WORKER_SERVER_NAME = "lcc.worker.server-name";
	public static final String $WORKER_SERVER_NAME_WD = "${" + WORKER_SERVER_NAME + ":" + LocalCloudConfiguratorDefaults.WORKER_SERVER_NAME_DEFAULT + "}";
	public static final String API_WHITELIST = "lcc.api.whitelist";
	public static final String $API_WHITELIST_WD = "${" + API_WHITELIST + ":" + LocalCloudConfiguratorDefaults.API_WHITELIST_DEFAULT + "}";
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":" + LocalCloudConfiguratorDefaults.SERVICE_AUTHORIZATION_ENABLED_DEFAULT + "}";

	// host worker (non-web profile)
	public static final String WORKER_PORT = "lcc.worker.port";
	public static final String WORKER_ADDRESS = "lcc.worker.address";
	public static final String WORKER_ALLOWED_CLIENTS = "lcc.worker.allowed-clients";

	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30000;
	public static final int REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION = 30;
	public static final int MAX_BODY_BYTES = 64 << 10;

	private LocalCloudConfiguratorConstants() {
		throw new UnsupportedOperationException();
	}
}
