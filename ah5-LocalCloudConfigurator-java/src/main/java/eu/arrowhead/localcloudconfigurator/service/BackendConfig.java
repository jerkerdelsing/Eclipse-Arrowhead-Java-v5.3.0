package eu.arrowhead.localcloudconfigurator.service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import eu.arrowhead.localcloudconfigurator.Catalog;
import eu.arrowhead.localcloudconfigurator.CloudColony;
import eu.arrowhead.localcloudconfigurator.LocalCloudConfiguratorConstants;
import eu.arrowhead.localcloudconfigurator.colony.ColonyClient;
import eu.arrowhead.localcloudconfigurator.jpa.service.JpaDeploymentStore;
import eu.arrowhead.localcloudconfigurator.tls.MutualTls;

// The catalog, the execution backend and the deployment coordinator of the Arrowhead API.
@Configuration
public class BackendConfig {

	@Bean
	Catalog catalog(@Value(LocalCloudConfiguratorConstants.$CATALOG_FILE_WD) final String file) throws Exception {
		return Catalog.load(Path.of(file));
	}

	// direct: the host worker over mutual TLS with this system's own certificate; colony: the retained ColonyOS backend
	@Bean
	ConfiguratorBackend configuratorBackend(final Catalog catalog, final ResourceLoader resources,
			@Value(LocalCloudConfiguratorConstants.$BACKEND_WD) final String backend,
			@Value(LocalCloudConfiguratorConstants.$WORKER_URL_WD) final String workerUrl,
			@Value(LocalCloudConfiguratorConstants.$WORKER_SERVER_NAME_WD) final String workerNames,
			@Value("${server.ssl.key-store:}") final String keyStore,
			@Value("${server.ssl.key-store-password:}") final String keyStorePassword,
			@Value("${server.ssl.key-password:${server.ssl.key-store-password:}}") final String keyPassword,
			@Value("${server.ssl.trust-store:}") final String trustStore,
			@Value("${server.ssl.trust-store-password:}") final String trustStorePassword,
			@Value("${arrowhead.ssl.crl-file:}") final String crlFile) throws Exception {
		if ("colony".equals(backend)) {
			final CloudColony colony = new CloudColony(new ColonyClient(ColonyClient.Config.fromEnv()), catalog);
			colony.keepApiAlive();
			return new ColonyBackend(colony);
		}

		if (!"direct".equals(backend)) {
			throw new IllegalArgumentException(LocalCloudConfiguratorConstants.BACKEND + " must be 'direct' or 'colony'");
		}

		final Set<String> accepted = Arrays.stream(workerNames.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
		return new WorkerClient(workerUrl, () -> {
			try (InputStream keys = resources.getResource(keyStore).getInputStream(); InputStream trust = resources.getResource(trustStore).getInputStream()) {
				return MutualTls.context(keys, keyStorePassword.toCharArray(), keyPassword.toCharArray(), trust, trustStorePassword.toCharArray(),
						crlFile.isBlank() ? null : Path.of(crlFile), accepted);
			} catch (final Exception ex) {
				throw new IllegalStateException("Worker TLS context unavailable: " + ex.getMessage(), ex);
			}
		});
	}

	@Bean
	DeploymentCoordinator deploymentCoordinator(final Catalog catalog, final ConfiguratorBackend backend, final JpaDeploymentStore store) {
		return new DeploymentCoordinator(catalog, backend, store);
	}
}
