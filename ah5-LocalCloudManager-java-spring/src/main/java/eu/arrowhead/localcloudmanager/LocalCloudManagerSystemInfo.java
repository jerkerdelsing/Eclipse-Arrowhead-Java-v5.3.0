package eu.arrowhead.localcloudmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class LocalCloudManagerSystemInfo extends SystemInfo {

	// localCloudManagement operations (v5 convention: path parameters are appended to the registered path). The console-only
	// session operations of the native registration are not offered on the Arrowhead connector.
	public static final List<String[]> OPERATIONS = List.of(
			new String[] { "overview", "GET", "/overview" },
			new String[] { "serviceRegistryManagement", "GET", "/service-registry-management" },
			new String[] { "dataManager", "GET", "/datamanager" },
			new String[] { "consumers", "GET", "/consumers" },
			new String[] { "interCloud", "GET", "/intercloud" },
			new String[] { "sysMonitorHistory", "GET", "/sysmonitor" },
			new String[] { "sysMonitorSubscriptions", "GET", "/sysmonitor/subscriptions" },
			new String[] { "subscribeSysMonitor", "PUT", "/sysmonitor/subscriptions" },
			new String[] { "unsubscribeSysMonitor", "DELETE", "/sysmonitor/subscriptions" },
			new String[] { "plantDescriptions", "GET", "/plantdescriptions" },
			new String[] { "createPlantDescription", "POST", "/plantdescriptions" },
			new String[] { "replacePlantDescription", "PUT", "/plantdescriptions" },
			new String[] { "updatePlantDescription", "PATCH", "/plantdescriptions" },
			new String[] { "removePlantDescription", "DELETE", "/plantdescriptions" },
			new String[] { "orchestrationRules", "GET", "/orchestration/rules" },
			new String[] { "createOrchestrationRule", "POST", "/orchestration/rules" },
			new String[] { "removeOrchestrationRule", "DELETE", "/orchestration/rules" },
			new String[] { "logs", "GET", "/logs" },
			new String[] { "lifecycle", "POST", "/containers" });

	@Value(LocalCloudManagerConstants.$PROJECT_NAME_WD)
	private String projectName;

	private SystemModel systemModel;

	@Override
	public String getSystemName() {
		return LocalCloudManagerConstants.SYSTEM_NAME;
	}

	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(LocalCloudManagerConstants.SERVICE_VERSION)
					.metadata("role", "local-cloud-monitor")
					.metadata("managedProject", projectName)
					.metadata("consumerInterfaceTemplates", Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME);
			final String key = getPublicKey();
			if (!key.isEmpty()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, key);
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	// sysMonitor is registered by the common-library SysMonitorPublisher when sysmonitor.enabled=true.
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(LocalCloudManagerConstants.SERVICE_VERSION)
				.serviceInterface(interfaceBuilder(LocalCloudManagerConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
						.operation(Constants.SERVICE_OP_GET_CONFIG, operation("GET", Constants.HTTP_API_OP_GET_CONFIG_PATH))
						.build())
				.build();

		final HttpInterfaceModel.Builder management = interfaceBuilder(LocalCloudManagerConstants.HTTP_API_BASE_PATH);
		for (final String[] op : OPERATIONS) {
			management.operation(op[0], operation(op[1], op[2]));
		}

		final ServiceModel localCloudManagement = new ServiceModel.Builder()
				.serviceDefinition(LocalCloudManagerConstants.SERVICE_DEF_LOCAL_CLOUD_MANAGEMENT)
				.version(LocalCloudManagerConstants.SERVICE_VERSION)
				.metadata("description", "Local cloud health, inventory, logs and lifecycle management")
				.serviceInterface(management.build())
				.build();

		return new ArrayList<>(List.of(generalManagement, localCloudManagement));
	}

	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS, Constants.SERVER_PORT, Constants.DOMAIN_NAME, Constants.AUTHENTICATION_POLICY, Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY, LocalCloudManagerConstants.CONSOLE_PORT, LocalCloudManagerConstants.API_WHITELIST),
				Defaults.class);
	}

	private HttpInterfaceModel.Builder interfaceBuilder(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort()).basePath(basePath);
	}

	private static HttpOperationModel operation(final String method, final String path) {
		return new HttpOperationModel.Builder().method(HttpMethod.valueOf(method).name()).path(path).build();
	}
}
