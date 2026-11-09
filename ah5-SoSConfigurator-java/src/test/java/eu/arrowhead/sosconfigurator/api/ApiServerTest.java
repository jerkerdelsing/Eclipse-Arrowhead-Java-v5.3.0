package eu.arrowhead.sosconfigurator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.colony.ColonyClient;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;

class ApiServerTest {

	private static final String TOKEN = "0123456789abcdef0123";
	private HttpServer server;
	private String url;
	private final AtomicInteger submitted = new AtomicInteger();
	private final HttpClient http = HttpClient.newHttpClient();

	@BeforeEach
	void start() throws Exception {
		final ApiServer.Backend backend = new ApiServer.Backend() {
			@Override
			public String submit(final Planner.Plan plan, final SosRequest req) {
				submitted.incrementAndGet();
				return "a".repeat(64);
			}

			@Override
			public ColonyClient.Deployment deployment(final String id) {
				return new ColonyClient.Deployment(id, "success", List.of());
			}

			@Override
			public String status(final String role, final Duration timeout) throws IOException {
				if (role.equals("cloud:RustDev")) {
					throw new IOException("no executor answered");
				}
				return "{\"state\":\"ready\"}";
			}
		};
		server = new ApiServer(Catalog.load(Path.of("catalogs/sos.json")), backend, TOKEN, Duration.ofSeconds(1))
				.start(new InetSocketAddress("127.0.0.1", 0));
		url = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterEach
	void stop() {
		server.stop(0);
	}

	private HttpResponse<String> call(final String method, final String path, final String token, final String body) throws Exception {
		final HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url + path))
				.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
		if (token != null) {
			b.header("Authorization", "Bearer " + token);
		}
		return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void requiresTheOperatorToken() throws Exception {
		assertEquals(200, call("GET", "/healthz", null, null).statusCode());
		assertEquals(200, call("GET", "/", null, null).statusCode());
		assertEquals(401, call("GET", "/api/catalog", null, null).statusCode());
		assertEquals(401, call("GET", "/api/catalog", "wrong", null).statusCode());
		assertEquals(200, call("GET", "/api/catalog", TOKEN, null).statusCode());
	}

	@Test
	void refusedSoSIsNotSubmitted() throws Exception {
		final HttpResponse<String> r = call("POST", "/api/deployments", TOKEN, Files.readString(Path.of("requests/refused-rustdev.json")));
		assertEquals(422, r.statusCode());
		assertEquals(0, submitted.get());
		assertTrue(r.body().contains("S4"));
	}

	@Test
	void allowedSoSIsSubmitted() throws Exception {
		final HttpResponse<String> r = call("POST", "/api/deployments", TOKEN, Files.readString(Path.of("requests/testcloud-gocloud.json")));
		assertEquals(202, r.statusCode());
		assertEquals(1, submitted.get());
		assertEquals(11, Json.LENIENT.readTree(r.body()).path("plan").path("steps").size());
	}

	@Test
	void rejectsUnknownFieldsAndBadIds() throws Exception {
		assertEquals(400, call("POST", "/api/plan", TOKEN, "{\"name\":\"x\",\"clouds\":[],\"grantEverything\":true}").statusCode());
		assertEquals(404, call("GET", "/api/deployments/..%2Fetc", TOKEN, null).statusCode());
	}

	@Test
	void statusMarksSilentExecutorsOffline() throws Exception {
		final var st = Json.LENIENT.readTree(call("GET", "/api/status", TOKEN, null).body());
		assertEquals("ready", st.path("relay").path("state").asText());
		assertEquals("offline", st.path("clouds").path("RustDev").path("state").asText());
		assertEquals("ready", st.path("clouds").path("GoCloud").path("state").asText());
	}
}
