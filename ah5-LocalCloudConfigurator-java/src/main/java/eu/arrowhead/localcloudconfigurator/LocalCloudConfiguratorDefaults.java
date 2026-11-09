package eu.arrowhead.localcloudconfigurator;

import eu.arrowhead.common.Defaults;

public final class LocalCloudConfiguratorDefaults extends Defaults {

	public static final String SERVER_PORT_DEFAULT = "8492";
	public static final String CATALOG_FILE_DEFAULT = "/app/catalogs/javacloud.json";
	// direct: the host worker over mutual TLS; colony: the retained ColonyOS backend
	public static final String BACKEND_DEFAULT = "direct";
	public static final String WORKER_URL_DEFAULT = "https://host.docker.internal:8493";
	public static final String WORKER_SERVER_NAME_DEFAULT = "LocalCloudConfiguratorWorker";
	public static final String API_WHITELIST_DEFAULT = "";
	public static final String SERVICE_AUTHORIZATION_ENABLED_DEFAULT = "true";
	// names used by the generalManagement get-config defaults (key with dots as underscores)
	public static final String LCC_CATALOG_FILE_DEFAULT = CATALOG_FILE_DEFAULT;
	public static final String LCC_BACKEND_DEFAULT = BACKEND_DEFAULT;
	public static final String LCC_WORKER_URL_DEFAULT = WORKER_URL_DEFAULT;
	public static final String LCC_API_WHITELIST_DEFAULT = API_WHITELIST_DEFAULT;
	public static final String WORKER_PORT_DEFAULT = "8493";
	public static final String WORKER_ADDRESS_DEFAULT = "127.0.0.1";
	public static final String WORKER_ALLOWED_CLIENTS_DEFAULT = "LocalCloudConfigurator";

	private LocalCloudConfiguratorDefaults() {
		throw new UnsupportedOperationException();
	}
}
