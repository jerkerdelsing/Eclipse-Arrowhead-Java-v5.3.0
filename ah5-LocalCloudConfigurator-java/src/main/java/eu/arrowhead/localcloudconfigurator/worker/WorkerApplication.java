package eu.arrowhead.localcloudconfigurator.worker;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

import eu.arrowhead.localcloudconfigurator.Catalog;
import eu.arrowhead.localcloudconfigurator.DirectBackend;
import eu.arrowhead.localcloudconfigurator.LocalCloudConfiguratorConstants;
import eu.arrowhead.localcloudconfigurator.LocalCloudConfiguratorDefaults;
import eu.arrowhead.localcloudconfigurator.Runner;
import eu.arrowhead.localcloudconfigurator.service.DeploymentCoordinator;
import eu.arrowhead.localcloudconfigurator.service.FileDeploymentStore;
import eu.arrowhead.localcloudconfigurator.tls.MutualTls;

/**
 * The host worker: the same JAR as a non-web Spring application, started by the per-user login service (scripts/install-direct.py).
 * It runs catalogued Compose actions for one cloud, keeps its own catalog checks and file state, and listens on host loopback with
 * mutual TLS. It has no database and does not register in the ServiceRegistry: it is the configurator's local agent, not a cloud system.
 */
@SpringBootConfiguration
public class WorkerApplication {

	@Bean
	Catalog catalog(@Value(LocalCloudConfiguratorConstants.$CATALOG_FILE_WD) final String file) throws Exception {
		return Catalog.load(Path.of(file));
	}

	@Bean(destroyMethod = "close")
	DirectBackend directBackend(final Catalog catalog,
			@Value("${lcc.worker.cloud-dir}") final String cloudDir,
			@Value("${lcc.worker.state}") final String state,
			@Value("${lcc.worker.docker:docker}") final String docker) throws Exception {
		return new DirectBackend(catalog, new Runner(catalog, Path.of(cloudDir), new Runner.Exec(), docker), Path.of(state).resolve("workflows"));
	}

	@Bean
	DeploymentCoordinator coordinator(final Catalog catalog, final DirectBackend backend, @Value("${lcc.worker.state}") final String state) throws Exception {
		return new DeploymentCoordinator(catalog, backend, new FileDeploymentStore(Path.of(state)));
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	WorkerServer workerServer(final DeploymentCoordinator coordinator,
			@Value("${lcc.worker.address:" + LocalCloudConfiguratorDefaults.WORKER_ADDRESS_DEFAULT + "}") final String address,
			@Value("${lcc.worker.port:" + LocalCloudConfiguratorDefaults.WORKER_PORT_DEFAULT + "}") final int port,
			@Value("${lcc.worker.ssl.key-store}") final String keyStore,
			@Value("${lcc.worker.ssl.key-store-password}") final String keyStorePassword,
			@Value("${lcc.worker.ssl.key-password:${lcc.worker.ssl.key-store-password}}") final String keyPassword,
			@Value("${lcc.worker.ssl.trust-store}") final String trustStore,
			@Value("${lcc.worker.ssl.trust-store-password}") final String trustStorePassword,
			@Value("${lcc.worker.ssl.crl-file:}") final String crlFile,
			@Value("${lcc.worker.allowed-clients:" + LocalCloudConfiguratorDefaults.WORKER_ALLOWED_CLIENTS_DEFAULT + "}") final String allowedClients) throws Exception {
		final Set<String> accepted = Arrays.stream(allowedClients.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
		final SSLContext context;
		try (InputStream keys = Files.newInputStream(Path.of(keyStore)); InputStream trust = Files.newInputStream(Path.of(trustStore))) {
			context = MutualTls.context(keys, keyStorePassword.trim().toCharArray(), keyPassword.trim().toCharArray(), trust, trustStorePassword.trim().toCharArray(),
					crlFile.isBlank() ? null : Path.of(crlFile), accepted);
		}

		return new WorkerServer(coordinator, context, WorkerServer.loopback(address, port));
	}
}
