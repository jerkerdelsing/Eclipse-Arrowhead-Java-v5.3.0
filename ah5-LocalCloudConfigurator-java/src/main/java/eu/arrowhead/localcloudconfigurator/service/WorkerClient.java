package eu.arrowhead.localcloudconfigurator.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.Planner;
import eu.arrowhead.localcloudconfigurator.Refused;
import eu.arrowhead.localcloudconfigurator.service.dto.ApplyRequest;

/**
 * The connection from the API container to the host worker over mutual TLS (replaces the shared-token HTTP DirectClient). The worker is
 * identified by its certificate (accepted system name, checked by the TLS context), not by the host name it is reached under.
 */
public class WorkerClient implements ConfiguratorBackend {

	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int READ_TIMEOUT_MS = 60000;
	private static final int MAX_RESPONSE_BYTES = 4 << 20;

	private final URI origin;
	private final Supplier<SSLContext> contextSupplier;
	private volatile SSLContext context;

	public WorkerClient(final String origin, final Supplier<SSLContext> contextSupplier) {
		this.origin = URI.create(origin);
		this.contextSupplier = contextSupplier;
		if (!"https".equals(this.origin.getScheme()) || !Set.of("127.0.0.1", "localhost", "host.docker.internal").contains(this.origin.getHost())
				|| this.origin.getUserInfo() != null || this.origin.getQuery() != null || this.origin.getFragment() != null
				|| !(this.origin.getPath() == null || Set.of("", "/").contains(this.origin.getPath()))) {
			throw new IllegalArgumentException("Direct worker must use an https local host origin");
		}
	}

	@Override
	public List<String> status() throws Exception {
		final JsonNode result = call("/api/status", null).path("running");
		if (!result.isArray()) {
			throw new IllegalStateException("Invalid worker status");
		}

		return Json.LENIENT.convertValue(result, Json.MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
	}

	@Override
	public String submit(final Planner.Plan plan) {
		throw new IllegalStateException("A reviewed request is required");
	}

	// the host independently rebuilds and checks the exact plan against its own catalog and current runtime state
	@Override
	public String submit(final Planner.Plan plan, final Planner.Request request) throws Exception {
		final ApplyRequest payload = new ApplyRequest(request.systems(), request.remove(), request.reapply(), request.smoke(), plan.hash());
		return call("/api/deployments", payload).path("id").asText();
	}

	@Override
	public Object deployment(final String id) throws Exception {
		if (!id.matches("local-[a-f0-9-]{36}")) {
			throw new Refused(404, "Historical ColonyOS workflow; no local action was replayed");
		}

		return call("/api/deployments/" + id, null);
	}

	private JsonNode call(final String path, final Object body) throws IOException {
		final HttpsURLConnection connection = (HttpsURLConnection) origin.resolve(path).toURL().openConnection();
		connection.setSSLSocketFactory(context().getSocketFactory());
		// identity is the worker certificate's system name, enforced by the trust manager
		connection.setHostnameVerifier((host, session) -> true);
		connection.setInstanceFollowRedirects(false);
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setRequestProperty("Accept", "application/json");
		try {
			if (body == null) {
				connection.setRequestMethod("GET");
			} else {
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/json");
				try (OutputStream out = connection.getOutputStream()) {
					out.write(Json.write(body).getBytes(StandardCharsets.UTF_8));
				}
			}

			final int status = connection.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_ACCEPTED) {
				throw new Refused(status, "Local worker refused request (HTTP " + status + ")");
			}

			try (InputStream in = connection.getInputStream()) {
				final byte[] data = in.readNBytes(MAX_RESPONSE_BYTES + 1);
				if (data.length > MAX_RESPONSE_BYTES) {
					throw new IOException("Worker response too large");
				}

				return Json.LENIENT.readTree(data);
			}
		} finally {
			connection.disconnect();
		}
	}

	private SSLContext context() {
		SSLContext c = context;
		if (c == null) {
			synchronized (this) {
				if (context == null) {
					context = contextSupplier.get();
				}
				c = context;
			}
		}

		return c;
	}
}
