package eu.arrowhead.sosconfigurator.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.colony.ColonyClient;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;

/** HTTP API and operator UI of the SoSConfigurator; every /api call needs the operator token. */
public class ApiServer {

	/** The ColonyOS side of the API. */
	public interface Backend {
		String submit(Planner.Plan plan, SosRequest req) throws IOException;

		ColonyClient.Deployment deployment(String id) throws IOException;

		String status(String role, Duration timeout) throws IOException;
	}

	private static final java.util.concurrent.ExecutorService WORKERS = Executors.newVirtualThreadPerTaskExecutor();
	private static final int MAX_BODY = 64 << 10;
	private static final Pattern ID = Pattern.compile("[0-9a-f]{64}");
	private static final Map<String, String> STATIC = Map.of("/", "index.html", "/index.html", "index.html", "/app.js", "app.js");

	private final Catalog catalog;
	private final Planner planner;
	private final Backend backend;
	private final byte[] token;
	private final Duration statusTimeout;

	public ApiServer(final Catalog catalog, final Backend backend, final String token, final Duration statusTimeout) {
		this.catalog = catalog;
		this.planner = new Planner(catalog);
		this.backend = backend;
		this.token = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
		this.statusTimeout = statusTimeout;
	}

	public HttpServer start(final InetSocketAddress addr) throws IOException {
		final HttpServer s = HttpServer.create(addr, 0);
		s.createContext("/", this::handle);
		s.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		s.start();
		return s;
	}

	void handle(final HttpExchange ex) throws IOException {
		try (ex) {
			final var h = ex.getResponseHeaders();
			h.set("X-Content-Type-Options", "nosniff");
			h.set("X-Frame-Options", "DENY");
			h.set("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'");
			final String method = ex.getRequestMethod();
			final String path = ex.getRequestURI().getPath();
			if ("GET".equals(method) && STATIC.containsKey(path)) {
				serveStatic(ex, STATIC.get(path));
				return;
			}
			if ("GET".equals(method) && "/healthz".equals(path)) {
				send(ex, 200, "text/plain", "ok\n".getBytes(StandardCharsets.UTF_8));
				return;
			}
			if (!path.startsWith("/api/")) {
				json(ex, 404, Map.of("error", "not found"));
				return;
			}
			if (!authorized(ex)) {
				json(ex, 401, Map.of("error", "operator token required"));
				return;
			}
			route(ex, method, path);
		} catch (final RuntimeException e) {
			json(ex, 500, Map.of("error", "internal error"));
		}
	}

	private void route(final HttpExchange ex, final String method, final String path) throws IOException {
		if ("GET".equals(method) && "/api/catalog".equals(path)) {
			json(ex, 200, catalog);
		} else if ("GET".equals(method) && "/api/status".equals(path)) {
			json(ex, 200, status());
		} else if ("POST".equals(method) && "/api/plan".equals(path)) {
			final SosRequest req = readRequest(ex);
			if (req != null) {
				json(ex, 200, planner.build(req));
			}
		} else if ("POST".equals(method) && "/api/deployments".equals(path)) {
			deploy(ex);
		} else if ("GET".equals(method) && path.startsWith("/api/deployments/")) {
			final String id = path.substring("/api/deployments/".length());
			if (!ID.matcher(id).matches()) {
				json(ex, 404, Map.of("error", "deployment not found"));
				return;
			}
			try {
				json(ex, 200, backend.deployment(id));
			} catch (final IOException e) {
				json(ex, 502, Map.of("error", String.valueOf(e.getMessage())));
			}
		} else {
			json(ex, 404, Map.of("error", "not found"));
		}
	}

	private void deploy(final HttpExchange ex) throws IOException {
		final SosRequest req = readRequest(ex);
		if (req == null) {
			return;
		}
		final Planner.Plan plan = planner.build(req);
		if (!plan.allowed()) {
			json(ex, 422, Map.of("error", "the SoS violates the security or authorization policies", "plan", plan));
			return;
		}
		try {
			json(ex, 202, Map.of("plan", plan, "id", backend.submit(plan, req)));
		} catch (final IOException e) {
			json(ex, 502, Map.of("error", String.valueOf(e.getMessage())));
		}
	}

	/** Status of the relay and every catalogued cloud, asked in parallel; an executor that does not answer is offline. */
	Map<String, Object> status() {
		final Map<String, CompletableFuture<Object>> futures = new LinkedHashMap<>();
		for (final String role : catalog.roles()) {
			futures.put(role, CompletableFuture.supplyAsync(() -> {
				try {
					return (Object) Json.LENIENT.readTree(backend.status(role, statusTimeout));
				} catch (final IOException e) {
					return Map.of("state", "offline", "error", String.valueOf(e.getMessage()));
				}
			}, WORKERS));
		}
		final Map<String, Object> clouds = new LinkedHashMap<>();
		final Map<String, Object> out = new LinkedHashMap<>();
		futures.forEach((role, f) -> {
			final Object v = f.join();
			if ("relay".equals(role)) {
				out.put("relay", v);
			} else {
				clouds.put(role.substring("cloud:".length()), v);
			}
		});
		out.put("clouds", clouds);
		return out;
	}

	private SosRequest readRequest(final HttpExchange ex) throws IOException {
		final byte[] body;
		try (InputStream in = ex.getRequestBody()) {
			body = in.readNBytes(MAX_BODY + 1);
		}
		if (body.length > MAX_BODY) {
			json(ex, 413, Map.of("error", "request too large"));
			return null;
		}
		try {
			final JsonNode n = Json.MAPPER.readTree(body);
			return Json.MAPPER.treeToValue(n, SosRequest.class);
		} catch (final IOException e) {
			json(ex, 400, Map.of("error", "invalid request: " + (e instanceof com.fasterxml.jackson.core.JsonProcessingException j ? j.getOriginalMessage() : e.getMessage())));
			return null;
		}
	}

	private boolean authorized(final HttpExchange ex) {
		final List<String> auth = ex.getRequestHeaders().get("Authorization");
		if (token.length == 0 || auth == null || auth.size() != 1 || !auth.get(0).startsWith("Bearer ")) {
			return false;
		}
		return MessageDigest.isEqual(auth.get(0).substring(7).getBytes(StandardCharsets.UTF_8), token);
	}

	private static void serveStatic(final HttpExchange ex, final String name) throws IOException {
		try (InputStream in = ApiServer.class.getResourceAsStream("/web/" + name)) {
			if (in == null) {
				json(ex, 404, Map.of("error", "not found"));
				return;
			}
			send(ex, 200, name.endsWith(".js") ? "text/javascript; charset=utf-8" : "text/html; charset=utf-8", in.readAllBytes());
		}
	}

	private static void json(final HttpExchange ex, final int code, final Object v) throws IOException {
		ex.getResponseHeaders().set("Cache-Control", "no-store");
		send(ex, code, "application/json", Json.write(v).getBytes(StandardCharsets.UTF_8));
	}

	private static void send(final HttpExchange ex, final int code, final String type, final byte[] body) throws IOException {
		ex.getResponseHeaders().set("Content-Type", type);
		ex.sendResponseHeaders(code, body.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(body);
		}
	}
}
