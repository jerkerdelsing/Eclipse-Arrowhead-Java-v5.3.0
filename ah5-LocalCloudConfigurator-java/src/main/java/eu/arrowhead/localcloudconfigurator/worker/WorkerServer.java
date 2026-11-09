package eu.arrowhead.localcloudconfigurator.worker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.LocalCloudConfiguratorConstants;
import eu.arrowhead.localcloudconfigurator.Planner;
import eu.arrowhead.localcloudconfigurator.Refused;
import eu.arrowhead.localcloudconfigurator.Runner;
import eu.arrowhead.localcloudconfigurator.service.DeploymentCoordinator;
import eu.arrowhead.localcloudconfigurator.service.dto.ApplyRequest;

/**
 * The host worker's loopback endpoint. Every connection needs a client certificate of an accepted system (TLS handshake, see
 * MutualTls); there is no token. Routes: GET /healthz, GET /api/status, POST /api/plan, POST /api/deployments, GET /api/deployments/{id}.
 * Each deployment request is re-planned here against the worker's own catalog and the current runtime state.
 */
public class WorkerServer {

	private final DeploymentCoordinator coordinator;
	private final SSLContext context;
	private final InetSocketAddress address;
	private HttpsServer server;

	public WorkerServer(final DeploymentCoordinator coordinator, final SSLContext context, final InetSocketAddress address) {
		if (!address.getAddress().isLoopbackAddress()) {
			throw new IllegalArgumentException("Refusing to start: the worker listens on host loopback only, not " + address.getAddress());
		}

		this.coordinator = coordinator;
		this.context = context;
		this.address = address;
	}

	public static InetSocketAddress loopback(final String host, final int port) throws IOException {
		return new InetSocketAddress(InetAddress.getByName(host), port);
	}

	public synchronized void start() throws IOException {
		final HttpsServer s = HttpsServer.create(address, 50);
		s.setHttpsConfigurator(new HttpsConfigurator(context) {
			@Override
			public void configure(final HttpsParameters params) {
				final SSLParameters parameters = context.getDefaultSSLParameters();
				parameters.setNeedClientAuth(true);
				parameters.setProtocols(new String[] { "TLSv1.3", "TLSv1.2" });
				params.setSSLParameters(parameters);
			}
		});
		s.createContext("/", this::handle);
		s.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		s.start();
		server = s;
	}

	public synchronized void stop() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
	}

	public synchronized int port() {
		return server.getAddress().getPort();
	}

	private void handle(final HttpExchange x) throws IOException {
		x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
		x.getResponseHeaders().set("Cache-Control", "no-store");
		try {
			dispatch(x);
		} catch (final Refused e) {
			send(x, e.status(), Map.of("error", e.getMessage()));
		} catch (final IllegalArgumentException | JsonProcessingException e) {
			send(x, 400, Map.of("error", String.valueOf(e.getMessage())));
		} catch (final Exception e) {
			send(x, 502, Map.of("error", Runner.redact(String.valueOf(e.getMessage()))));
		} finally {
			x.close();
		}
	}

	private void dispatch(final HttpExchange x) throws Exception {
		final String path = x.getRequestURI().getPath();
		final String method = x.getRequestMethod();
		if ("GET".equals(method) && "/healthz".equals(path)) {
			send(x, 200, Map.of("status", "ok", "executionBackend", coordinator.backendKind()));
		} else if ("GET".equals(method) && "/api/status".equals(path)) {
			send(x, 200, coordinator.status());
		} else if ("POST".equals(method) && "/api/plan".equals(path)) {
			send(x, 200, coordinator.plan(read(x, Planner.Request.class)));
		} else if ("POST".equals(method) && "/api/deployments".equals(path)) {
			final DeploymentCoordinator.Result result = coordinator.apply(read(x, ApplyRequest.class));
			send(x, result.status(), result.body());
		} else if ("GET".equals(method) && path.matches("/api/deployments/" + LocalCloudConfiguratorConstants.DEPLOYMENT_ID_PATTERN)) {
			send(x, 200, coordinator.deployment(path.substring(path.lastIndexOf('/') + 1)));
		} else {
			throw new Refused(404, "Unknown worker operation");
		}
	}

	private static <T> T read(final HttpExchange x, final Class<T> type) throws IOException {
		final byte[] b = x.getRequestBody().readNBytes(LocalCloudConfiguratorConstants.MAX_BODY_BYTES + 1);
		if (b.length > LocalCloudConfiguratorConstants.MAX_BODY_BYTES) {
			throw new Refused(413, "Request exceeds 64 KiB");
		}

		return Json.MAPPER.readValue(b, type);
	}

	private static void send(final HttpExchange x, final int code, final Object v) throws IOException {
		final byte[] body = Json.write(v).getBytes(StandardCharsets.UTF_8);
		x.getResponseHeaders().set("Content-Type", "application/json");
		x.sendResponseHeaders(code, body.length);
		x.getResponseBody().write(body);
	}
}
