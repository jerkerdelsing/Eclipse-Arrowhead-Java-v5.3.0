package eu.arrowhead.localcloudconfigurator.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.localcloudconfigurator.Catalog;
import eu.arrowhead.localcloudconfigurator.DirectBackend;
import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.MemoryDeploymentStore;
import eu.arrowhead.localcloudconfigurator.Planner;
import eu.arrowhead.localcloudconfigurator.Refused;
import eu.arrowhead.localcloudconfigurator.Runner;
import eu.arrowhead.localcloudconfigurator.service.DeploymentCoordinator;
import eu.arrowhead.localcloudconfigurator.service.FileDeploymentStore;
import eu.arrowhead.localcloudconfigurator.service.WorkerClient;
import eu.arrowhead.localcloudconfigurator.service.dto.ApplyRequest;
import eu.arrowhead.localcloudconfigurator.tls.TestPki;

// The host worker over mutual TLS with a stub Compose runner: plan -> deployment from the API side, and refusal of every client
// without a valid certificate of an accepted system.
public class WorkerMutualTlsTest {

	private static final String SUFFIX = "TestCloud.Company.arrowhead.eu";

	@TempDir
	Path temp;

	private TestPki pki;
	private TestPki.Issued apiIdentity;
	private Catalog catalog;
	private DirectBackend backend;
	private WorkerServer server;
	private final AtomicInteger mutations = new AtomicInteger();
	private Path crl;

	@BeforeEach
	public void start() throws Exception {
		pki = new TestPki(SUFFIX);
		apiIdentity = pki.issue("LocalCloudConfigurator");
		catalog = Catalog.load(Path.of("catalogs/javacloud.json"));
		Files.writeString(temp.resolve("compose.yaml"), "services: {}\n");
		final List<String> services = catalog.systems().stream().flatMap(s -> s.services().stream()).toList();
		final Runner runner = new Runner(catalog, temp, (dir, args) -> {
			if (!args.contains("ps")) {
				mutations.incrementAndGet();
				throw new AssertionError("verification must not mutate the cloud");
			}
			return String.join("\n", services.stream().map(s -> "\"" + s + "\"").toList());
		}, "docker");
		backend = new DirectBackend(catalog, runner, temp.resolve("state/workflows"));
		final DeploymentCoordinator coordinator = new DeploymentCoordinator(catalog, backend, new FileDeploymentStore(temp.resolve("state")));
		crl = pki.crl(temp.resolve("ca.crl"));
		final SSLContext serverContext = pki.context(pki.issue("LocalCloudConfiguratorWorker"), crl, "LocalCloudConfigurator");
		server = new WorkerServer(coordinator, serverContext, WorkerServer.loopback("127.0.0.1", 0));
		server.start();
	}

	@AfterEach
	public void stop() throws Exception {
		server.stop();
		backend.close();
	}

	private String origin() {
		return "https://127.0.0.1:" + server.port();
	}

	private WorkerClient client(final TestPki.Issued identity, final String... acceptedServers) {
		return new WorkerClient(origin(), () -> {
			try {
				return pki.context(identity, null, acceptedServers);
			} catch (final Exception ex) {
				throw new IllegalStateException(ex);
			}
		});
	}

	@Test
	public void reviewedPlanIsDeployedThroughTheWorkerAndRecheckedThere() throws Exception {
		final WorkerClient worker = client(apiIdentity, "LocalCloudConfiguratorWorker");
		final MemoryDeploymentStore apiStore = new MemoryDeploymentStore();
		final DeploymentCoordinator api = new DeploymentCoordinator(catalog, worker, apiStore);

		final Map<String, Object> plan = api.plan(new Planner.Request(catalog.defaults(), List.of(), false, true));
		final String hash = String.valueOf(plan.get("hash"));
		final DeploymentCoordinator.Result submitted = api.apply(new ApplyRequest(catalog.defaults(), List.of(), false, true, hash));
		assertEquals(202, submitted.status());
		final String id = String.valueOf(submitted.body().get("id"));
		assertTrue(id.startsWith("local-"));
		assertEquals(id, apiStore.active);

		JsonNode state = null;
		for (int i = 0; i < 200; i++) {
			state = Json.LENIENT.valueToTree(api.deployment(id));
			if (Set.of("success", "failed").contains(state.path("state").asText())) {
				break;
			}
			Thread.sleep(20);
		}
		assertEquals("success", state.path("state").asText(), String.valueOf(state));
		assertEquals("success", state.path("steps").get(0).path("state").asText());
		assertTrue(apiStore.observed.containsKey(id));
		// the worker kept its own file records
		assertTrue(Files.exists(temp.resolve("state/deployments/" + id + ".json")));
		assertTrue(Files.exists(temp.resolve("state/workflows/" + id + ".json")));

		// the worker rebuilds the plan itself: a plan hash that does not match its own plan is refused
		final Planner.Plan forged = new Planner.Plan(catalog.cloud(), "changed", List.of(), List.of(new Planner.Step("smoke", "verify-localcloud", null)), List.of());
		assertEquals(409, assertThrows(Refused.class, () -> worker.submit(forged, new Planner.Request(List.of(), List.of(), false, true))).status());
		assertEquals(0, mutations.get());
	}

	@Test
	public void workerRefusesClientsWithoutAValidCertificate() throws Exception {
		// no client certificate at all
		final HttpsURLConnection anonymous = (HttpsURLConnection) URI.create(origin() + "/api/status").toURL().openConnection();
		final SSLContext trustOnly = SSLContext.getInstance("TLS");
		trustOnly.init(null, new javax.net.ssl.TrustManager[] { new TrustAll() }, null);
		anonymous.setSSLSocketFactory(trustOnly.getSocketFactory());
		anonymous.setHostnameVerifier((h, s) -> true);
		assertThrows(IOException.class, anonymous::getResponseCode);

		// a certificate of another CA with the right name
		final TestPki foreign = new TestPki(SUFFIX);
		final TestPki.Issued impostor = foreign.issue("LocalCloudConfigurator");
		final WorkerClient foreignClient = new WorkerClient(origin(), () -> {
			try {
				return foreignContextTrustingWorker(foreign, impostor);
			} catch (final Exception ex) {
				throw new IllegalStateException(ex);
			}
		});
		assertThrows(IOException.class, foreignClient::status);

		// a valid certificate of the cloud, but of another system
		assertThrows(IOException.class, client(pki.issue("DataManager"), "LocalCloudConfiguratorWorker")::status);

		// a revoked certificate of the accepted system
		final TestPki.Issued revoked = pki.issue("LocalCloudConfigurator");
		pki.crl(crl, revoked.certificate());
		server.stop();
		final DeploymentCoordinator coordinator = new DeploymentCoordinator(catalog, backend, new FileDeploymentStore(temp.resolve("state")));
		server = new WorkerServer(coordinator, pki.context(pki.issue("LocalCloudConfiguratorWorker"), crl, "LocalCloudConfigurator"), WorkerServer.loopback("127.0.0.1", 0));
		server.start();
		assertThrows(IOException.class, client(revoked, "LocalCloudConfiguratorWorker")::status);
		// the API refuses a worker that is not LocalCloudConfiguratorWorker
		assertThrows(IOException.class, client(apiIdentity, "SomeOtherWorker")::status);
		// while the accepted pair still works
		assertEquals(catalog.systems().stream().mapToLong(s -> s.services().size()).sum(), client(apiIdentity, "LocalCloudConfiguratorWorker").status().size());
	}

	@Test
	public void workerListensOnLoopbackOnly() {
		assertThrows(IllegalArgumentException.class, () -> new WorkerServer(null, null, new java.net.InetSocketAddress(java.net.InetAddress.getByName("0.0.0.0"), 0)));
	}

	@Test
	public void workerClientRefusesRemoteAndPlainOrigins() {
		assertThrows(IllegalArgumentException.class, () -> new WorkerClient("https://example.com:8493", () -> null));
		assertThrows(IllegalArgumentException.class, () -> new WorkerClient("http://127.0.0.1:8493", () -> null));
	}

	// the impostor trusts the real worker (so only the worker's check can refuse it)
	private SSLContext foreignContextTrustingWorker(final TestPki foreign, final TestPki.Issued impostor) throws Exception {
		final java.security.KeyStore keys = java.security.KeyStore.getInstance("PKCS12");
		keys.load(new java.io.ByteArrayInputStream(impostor.keyStore()), TestPki.PASSWORD);
		final javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keys, TestPki.PASSWORD);
		final SSLContext context = SSLContext.getInstance("TLS");
		context.init(kmf.getKeyManagers(), new javax.net.ssl.TrustManager[] { new TrustAll() }, null);
		return context;
	}

	private static final class TrustAll implements javax.net.ssl.X509TrustManager {
		@Override
		public void checkClientTrusted(final java.security.cert.X509Certificate[] chain, final String authType) {
		}

		@Override
		public void checkServerTrusted(final java.security.cert.X509Certificate[] chain, final String authType) {
		}

		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new java.security.cert.X509Certificate[0];
		}
	}
}
