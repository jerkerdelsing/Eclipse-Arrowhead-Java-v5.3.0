package eu.arrowhead.localcloudmanager.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;
import eu.arrowhead.localcloudmanager.service.ApiException;

/**
 * Providers are selected by the common service collector (orchestration on behalf of LocalCloudManager, results reused for
 * http.collector.orchestration.ttl seconds); requests carry the identity token and the X.509 client certificate. Provider certificates
 * are checked against the cloud CRL; the TLS context is rebuilt when the CRL file changes, so a revocation applies to new connections.
 */
@Component
public class OrchestratedProviderGateway implements ProviderGateway {

	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	private final ObjectMapper mapper = new ObjectMapper();
	private final AtomicReference<Client> client = new AtomicReference<>();

	private record Client(HttpClient http, Object crlStamp) {
	}

	@Autowired
	private ServiceCollector collector;

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private ResourceLoader resources;

	@Value("${server.ssl.key-store:}")
	private String keyStore;
	@Value("${server.ssl.key-store-password:}")
	private String keyStorePassword;
	@Value("${server.ssl.key-password:${server.ssl.key-store-password:}}")
	private String keyPassword;
	@Value("${server.ssl.trust-store:}")
	private String trustStore;
	@Value("${server.ssl.trust-store-password:}")
	private String trustStorePassword;
	@Value(LocalCloudManagerConstants.$CRL_FILE_WD)
	private String crlFile;

	private HttpInterfaceModel target(final String serviceDefinition, final String operation, final String provider) {
		final String template = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		final ServiceModel model;
		try {
			model = collector.getServiceModel(serviceDefinition, template, provider, operation);
		} catch (final RuntimeException ex) {
			throw new ApiException(503, "Orchestration found no authorized provider of " + serviceDefinition + " (" + operation + "): " + ex.getMessage());
		}
		if (model == null || model.interfaces().isEmpty()) {
			throw new ApiException(503, "Orchestration found no authorized provider of " + serviceDefinition + " (" + operation + ")");
		}

		final HttpInterfaceModel intf = (HttpInterfaceModel) model.interfaces().get(0);
		if (!intf.operations().containsKey(operation)) {
			throw new ApiException(502, (provider == null ? "Provider" : provider) + " does not offer " + operation);
		}

		return intf;
	}

	@Override
	public String operationPath(final String serviceDefinition, final String operation, final String provider) {
		return target(serviceDefinition, operation, provider).operations().get(operation).path();
	}

	@Override
	public JsonNode call(final String serviceDefinition, final String operation, final String provider, final String path, final String method, final JsonNode body) {
		final Response response = exchange(serviceDefinition, operation, provider, path, method, body);
		if (response.status() >= 400) {
			final JsonNode payload = response.body();
			final String message = payload != null && payload.hasNonNull("errorMessage") ? payload.get("errorMessage").asText()
					: payload != null && payload.hasNonNull("error") ? payload.get("error").asText()
							: (provider == null ? serviceDefinition : provider) + " API " + response.status();
			throw new ApiException(response.status(), message, payload);
		}

		return response.body();
	}

	@Override
	public Response exchange(final String serviceDefinition, final String operation, final String provider, final String path, final String method, final JsonNode body) {
		final HttpInterfaceModel intf = target(serviceDefinition, operation, provider);
		final HttpOperationModel op = intf.operations().get(operation);
		final String scheme = sysInfo.isSslEnabled() ? "https" : "http";
		final URI uri = URI.create(scheme + "://" + intf.accessAddresses().get(0) + ":" + intf.accessPort() + intf.basePath() + (path == null ? op.path() : path));
		try {
			final Response response = send(uri, method == null ? op.method() : method, body);
			if (response.status() >= 500) {
				collector.forget(serviceDefinition);
			}
			return response;
		} catch (final ApiException ex) {
			throw ex;
		} catch (final Exception ex) {
			collector.forget(serviceDefinition);
			throw new ApiException(502, (provider == null ? serviceDefinition : provider) + " request failed: " + ex.getMessage());
		}
	}

	@Override
	public JsonNode serviceRegistry(final String path, final JsonNode body) {
		final String scheme = sysInfo.isSslEnabled() ? "https" : "http";
		final URI uri = URI.create(scheme + "://" + sysInfo.getServiceRegistryAddress() + ":" + sysInfo.getServiceRegistryPort() + path);
		final Response response;
		try {
			response = send(uri, body == null ? "GET" : "POST", body);
		} catch (final Exception ex) {
			throw new ApiException(502, "ServiceRegistry request failed: " + ex.getMessage());
		}
		if (response.status() >= 400) {
			final JsonNode p = response.body();
			throw new ApiException(response.status(), p != null && p.hasNonNull("errorMessage") ? p.get("errorMessage").asText() : "Arrowhead service returned HTTP " + response.status(), p);
		}
		return response.body();
	}

	private Response send(final URI uri, final String method, final JsonNode body) throws Exception {
		final HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(TIMEOUT).header("Accept", "application/json");
		final String authorization = HttpUtilities.calculateAuthorizationHeader(sysInfo);
		if (authorization != null) {
			builder.header("Authorization", authorization);
		}
		if (body == null) {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		} else {
			builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)));
		}

		final HttpResponse<byte[]> response = http().send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
		final String text = new String(response.body(), StandardCharsets.UTF_8);
		JsonNode value = null;
		if (!text.isBlank()) {
			try {
				value = mapper.readTree(text);
			} catch (final Exception ex) {
				throw new IllegalStateException(uri.getHost() + " returned invalid JSON");
			}
		}

		return new Response(response.statusCode(), value);
	}

	private HttpClient http() throws Exception {
		final Object stamp = crlStamp();
		final Client current = client.get();
		if (current != null && java.util.Objects.equals(current.crlStamp(), stamp)) {
			return current.http();
		}

		final HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER);
		if (sysInfo.isSslEnabled()) {
			final SSLContext context = TlsContextFactory.create(resources, keyStore, keyStorePassword, keyPassword, trustStore, trustStorePassword, crlFile);
			builder.sslContext(context);
		}
		final HttpClient http = builder.build();
		client.set(new Client(http, stamp));
		return http;
	}

	private Object crlStamp() {
		if (crlFile == null || crlFile.isBlank()) {
			return "none";
		}
		try {
			return Files.getLastModifiedTime(Path.of(crlFile)).toMillis();
		} catch (final Exception ex) {
			return "missing";
		}
	}
}
