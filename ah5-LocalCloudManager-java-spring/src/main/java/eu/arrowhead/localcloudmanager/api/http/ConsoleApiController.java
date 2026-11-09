package eu.arrowhead.localcloudmanager.api.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;
import eu.arrowhead.localcloudmanager.client.ProviderGateway;
import eu.arrowhead.localcloudmanager.console.ConsoleAuth;
import eu.arrowhead.localcloudmanager.service.ApiException;
import eu.arrowhead.localcloudmanager.service.Json;
import eu.arrowhead.localcloudmanager.service.ManagerService;
import eu.arrowhead.localcloudmanager.service.PolicyEditor;
import eu.arrowhead.localcloudmanager.service.RegistrationState;
import eu.arrowhead.localcloudmanager.service.RuleEditor;
import eu.arrowhead.localcloudmanager.service.SosModel;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The /api routes and /health of the native console API, with the same paths, JSON shapes, status codes and error bodies
 * ({"error": message}). Routes are matched in the order of the native server (server.js route()).
 */
@Hidden
@RestController
public class ConsoleApiController {

	private static final Pattern SUBSCRIPTION = Pattern.compile("^/api/sysmonitor/subscriptions/([A-Za-z0-9_-]+)$");
	private static final Pattern PLANT = Pattern.compile("^/api/plantdescriptions(?:/(\\d+))?$");
	private static final Pattern RULE = Pattern.compile("(?i)^/api/orchestration/rules/([0-9a-f-]+)$");
	private static final Pattern CONTAINER_ACTION = Pattern.compile("^/api/containers/([a-z0-9-]+)/(start|stop|restart)$");
	private static final Pattern SOS = Pattern.compile("^/api/sos(?:/(\\d+))?$");
	private static final Pattern FLOORPLAN = Pattern.compile("^/api/chiplet/floorplans(?:/(\\d+))?(?:/(check-network))?$");
	private static final Pattern POLICY = Pattern.compile("^/api/orchestration/policies(?:/([^/]+))?$");
	private static final List<String> FLOORPLAN_FIELDS = List.of("name", "description", "interposer", "spacing", "chiplets", "links", "network", "generator", "active");

	private final ManagerService manager;
	private final ConsoleAuth auth;
	private final RegistrationState registration;
	private final boolean chipletFloorplans;
	private final boolean localCloudConfiguration;
	private final String priorityMode;

	public ConsoleApiController(final ManagerService manager, final ConsoleAuth auth, final RegistrationState registration,
			@Value(LocalCloudManagerConstants.$CHIPLET_FLOORPLANS_WD) final boolean chipletFloorplans,
			@Value(LocalCloudManagerConstants.$LOCAL_CLOUD_CONFIGURATION_WD) final boolean localCloudConfiguration,
			@Value(LocalCloudManagerConstants.$PRIORITY_MODE_WD) final String priorityMode) {
		this.manager = manager;
		this.auth = auth;
		this.registration = registration;
		this.chipletFloorplans = chipletFloorplans;
		this.localCloudConfiguration = localCloudConfiguration;
		this.priorityMode = priorityMode;
	}

	@GetMapping(path = LocalCloudManagerConstants.HTTP_HEALTH_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<JsonNode> health() {
		final ObjectNode body = Json.obj().put("status", "ok").put("system", LocalCloudManagerConstants.SYSTEM_NAME);
		body.set("registration", registration.get());
		return json(200, body);
	}

	@RequestMapping(path = { "/api", "/api/**" })
	public ResponseEntity<?> route(final HttpServletRequest request) throws IOException {
		try {
			return dispatch(request);
		} catch (final ApiException ex) {
			return error(ex.status(), ex.getMessage());
		} catch (final RuntimeException ex) {
			return error(500, String.valueOf(ex.getMessage()));
		}
	}

	static ResponseEntity<JsonNode> json(final int status, final JsonNode body) {
		return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff").contentType(MediaType.APPLICATION_JSON).body(body);
	}

	static ResponseEntity<JsonNode> error(final int status, final String message) {
		return json(status, Json.obj().put("error", message));
	}

	private static JsonNode readJson(final HttpServletRequest request) throws IOException {
		final byte[] data;
		try (InputStream in = request.getInputStream()) {
			data = in.readNBytes(LocalCloudManagerConstants.MAX_BODY_BYTES + 1);
		}
		if (data.length > LocalCloudManagerConstants.MAX_BODY_BYTES) {
			throw new IllegalStateException("Request body is too large");
		}
		if (data.length == 0) {
			return Json.obj();
		}
		try {
			return Json.MAPPER.readTree(data);
		} catch (final IOException ex) {
			throw new IllegalStateException("Invalid JSON body");
		}
	}

	private static void confirm(final HttpServletRequest request) {
		if (!LocalCloudManagerConstants.ACTION_CONFIRMATION.equals(request.getHeader(LocalCloudManagerConstants.ACTION_HEADER))) {
			throw new ApiException(428, "Missing action confirmation header");
		}
	}

	private ResponseEntity<?> dispatch(final HttpServletRequest request) throws IOException {
		final String path = request.getRequestURI();
		final String method = request.getMethod();

		if (LocalCloudManagerConstants.HTTP_API_SESSION_PATH.equals(path)) {
			return session(request, method);
		}

		if ("GET".equals(method) && "/api/features".equals(path)) {
			return json(200, Json.obj().put("localCloudConfiguration", localCloudConfiguration).put("chipletFloorplans", chipletFloorplans)
					.put("managerName", chipletFloorplans ? "ChipletLocalCloudManager" : "LocalCloudManager"));
		}

		if (path.startsWith("/api/localcloud")) {
			return localCloud(request, path.substring("/api/localcloud".length()), method);
		}

		if (path.startsWith("/api/sos")) {
			return sos(request, path, method);
		}

		if (path.startsWith("/api/chiplet/floorplans")) {
			return floorplans(request, path, method);
		}

		if ("GET".equals(method) && "/api/overview".equals(path)) {
			return json(200, manager.overview());
		}
		if ("GET".equals(method) && "/api/service-registry-management".equals(path)) {
			return json(200, manager.registryManagement());
		}
		if ("GET".equals(method) && "/api/datamanager".equals(path)) {
			return json(200, manager.dataManagerContent(request.getParameter("limit")));
		}
		if ("GET".equals(method) && "/api/consumers".equals(path)) {
			return json(200, manager.serviceConsumers());
		}
		if ("GET".equals(method) && "/api/intercloud".equals(path)) {
			return json(200, manager.interCloud());
		}
		if ("GET".equals(method) && "/api/sysmonitor".equals(path)) {
			final String system = request.getParameter("system");
			return json(200, manager.sysMonitorHistory(system == null ? "" : system, request.getParameter("limit")));
		}
		if ("GET".equals(method) && "/api/sysmonitor/subscriptions".equals(path)) {
			return json(200, manager.sysMonitorSubscriptions());
		}

		final Matcher subscription = SUBSCRIPTION.matcher(path);
		if (subscription.matches() && ("PUT".equals(method) || "DELETE".equals(method))) {
			confirm(request);
			final String target = "/subscriptions/" + ManagerService.encode(subscription.group(1));
			if ("DELETE".equals(method)) {
				manager.sysMonitor("sysMonitorManagement", "unsubscribe", target, null);
				return json(200, Json.obj().put("removed", subscription.group(1)));
			}
			final JsonNode body = readJson(request);
			final ObjectNode payload = Json.obj();
			payload.set("observationTypes", body.get("observationTypes"));
			payload.put("enabled", !(body.path("enabled").isBoolean() && !body.path("enabled").asBoolean()));
			if (payload.get("observationTypes") == null) {
				payload.remove("observationTypes");
			}
			return json(200, manager.sysMonitor("sysMonitorManagement", "subscribe", target, payload));
		}

		if ("GET".equals(method) && "/api/plantdescriptions".equals(path)) {
			return json(200, manager.plantDescriptions());
		}
		final Matcher plant = PLANT.matcher(path);
		if (plant.matches() && List.of("POST", "PUT", "PATCH", "DELETE").contains(method)) {
			confirm(request);
			final String id = plant.group(1);
			if ("POST".equals(method) && id == null) {
				return json(201, manager.plantDescription("create", "/plant-descriptions", readJson(request)));
			}
			if (id == null) {
				return error(404, "Not found");
			}
			if ("DELETE".equals(method)) {
				manager.plantDescription("remove", "/plant-descriptions/" + id, null);
				return json(200, Json.obj().put("removed", Long.parseLong(id)));
			}
			return json(200, manager.plantDescription("PUT".equals(method) ? "replace" : "update", "/plant-descriptions/" + id, readJson(request)));
		}

		if (path.startsWith("/api/orchestration/policies")) {
			return policies(request, path, method);
		}

		final Matcher rule = RULE.matcher(path);
		if ("PUT".equals(method) && rule.matches()) {
			confirm(request);
			final String id = rule.group(1);
			final JsonNode body = readJson(request);
			final JsonNode old = manager.rulesOnly().stream().filter(r -> r.path("id").asText().equals(id)).findFirst().orElse(null);
			final long priority = RuleEditor.validatePriority(old, body);
			if ("replace".equals(priorityMode)) {
				return json(200, RuleEditor.replaceWithExistingOperations(manager::rulesOnly, manager::createRule, manager::removeRule, old, priority));
			}
			final ObjectNode modify = Json.obj().put(id, priority);
			return json(200, manager.orchestration("modify-priorities", modify, null));
		}

		if ("GET".equals(method) && "/api/orchestration/rules".equals(path)) {
			return json(200, manager.orchestrationRules());
		}
		if ("POST".equals(method) && "/api/orchestration/rules".equals(path)) {
			confirm(request);
			return json(201, manager.createRule(readJson(request)));
		}
		if ("DELETE".equals(method) && rule.matches()) {
			confirm(request);
			return json(200, manager.removeRule(rule.group(1)));
		}

		if ("GET".equals(method) && "/api/logs".equals(path)) {
			final String service = request.getParameter("service") == null ? "" : request.getParameter("service");
			final int tail = clampTail(request.getParameter("tail"));
			final ObjectNode out = Json.obj().put("service", service);
			out.set("lines", Json.strings(manager.logs(service, tail)));
			out.put("collectedAt", Json.now());
			return json(200, out);
		}

		final Matcher action = CONTAINER_ACTION.matcher(path);
		if ("POST".equals(method) && action.matches()) {
			confirm(request);
			readJson(request);
			return json(202, manager.performAction(action.group(1), action.group(2)));
		}

		return error(404, "Not found");
	}

	static int clampTail(final String value) {
		double v;
		try {
			v = value == null || value.isEmpty() ? 150 : Double.parseDouble(value);
		} catch (final NumberFormatException ex) {
			v = Double.NaN;
		}
		if (Double.isNaN(v)) {
			v = 1;
		}
		return (int) Math.min(Math.max(v, 1), 500);
	}

	// ----------------------------------------------------------------------------------------------- console session

	private ResponseEntity<?> session(final HttpServletRequest request, final String method) throws IOException {
		if ("GET".equals(method)) {
			return json(200, Json.obj().put("authenticated", auth.authenticated(request)).put("configured", auth.configured()));
		}
		if ("POST".equals(method)) {
			// JSON only: a cross-site form post (text/plain) cannot reach the login, so other pages cannot lock it (J6)
			final String type = request.getContentType() == null ? "" : request.getContentType().toLowerCase(Locale.ROOT);
			if (!type.startsWith("application/json")) {
				return error(415, "Content-Type must be application/json");
			}
			final JsonNode body = readJson(request);
			final JsonNode password = body.get("password");
			final ConsoleAuth.Result result = auth.login(password != null && password.isTextual() ? password.asText() : null, auth.clientOf(request));
			final ResponseEntity.BodyBuilder builder = ResponseEntity.status(result.status()).cacheControl(CacheControl.noStore());
			if (result.cookie() != null) {
				builder.header("Set-Cookie", result.cookie());
			}
			if (result.retryAfter() != null) {
				builder.header("Retry-After", String.valueOf(result.retryAfter()));
			}
			if (result.status() == 204) {
				return builder.build();
			}
			return builder.header("X-Content-Type-Options", "nosniff").contentType(MediaType.APPLICATION_JSON)
					.body(Json.obj().put("error", result.status() == 429 ? "Too many failed logins; try again later" : "Wrong password"));
		}
		if ("DELETE".equals(method)) {
			final String cookie = auth.logout(request);
			return ResponseEntity.status(200).header("Set-Cookie", cookie).cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON)
					.body(Json.obj().put("authenticated", false));
		}
		return error(405, "Method not allowed");
	}

	// ----------------------------------------------------------------------------------------------- LocalCloudConfigurator

	private ResponseEntity<?> localCloud(final HttpServletRequest request, final String path, final String method) throws IOException {
		if (!localCloudConfiguration) {
			throw new ApiException(404, "Local cloud configurator is not enabled");
		}
		final String operation = localCloudOperation(method, path);
		if (operation == null) {
			throw new ApiException(404, "Unknown local cloud configuration operation");
		}
		final boolean write = !"GET".equals(method);
		if (write) {
			confirm(request);
		}
		final JsonNode body = write ? readJson(request) : null;
		final ProviderGateway.Response result = manager.localCloud(operation, path, method, body);
		return json(result.status(), result.body());
	}

	static String localCloudOperation(final String method, final String path) {
		final String config = "^/configurations/[a-f0-9-]{36}$";
		if ("GET".equals(method)) {
			if ("/catalog".equals(path)) {
				return "catalog";
			}
			if ("/status".equals(path)) {
				return "status";
			}
			if ("/configurations".equals(path)) {
				return "list-configurations";
			}
			if (path.matches(config)) {
				return "get-configuration";
			}
			if (path.matches("^/deployments/[A-Za-z0-9-]{1,128}$")) {
				return "get-deployment";
			}
			return null;
		}
		if ("POST".equals(method)) {
			return switch (path) {
			case "/plan" -> "plan";
			case "/deployments" -> "submit-deployment";
			case "/configurations" -> "create-configuration";
			default -> null;
			};
		}
		if ("PUT".equals(method) && path.matches(config)) {
			return "update-configuration";
		}
		if ("DELETE".equals(method) && path.matches(config)) {
			return "remove-configuration";
		}
		return null;
	}

	// ----------------------------------------------------------------------------------------------- SoS

	private ResponseEntity<?> sos(final HttpServletRequest request, final String path, final String method) throws IOException {
		final Matcher match = SOS.matcher(path);
		if (!match.matches()) {
			throw new ApiException(404, "Unknown SoS operation");
		}
		final String id = match.group(1);
		if ("GET".equals(method)) {
			final ObjectNode data = manager.sosConfigurations();
			if (id == null) {
				return json(200, data);
			}
			return json(200, Json.list(data, "entries").stream().filter(e -> e.path("id").asText().equals(id)).findFirst()
					.orElseThrow(() -> new ApiException(404, "SoS not found")));
		}
		if (!List.of("POST", "PUT", "DELETE").contains(method)) {
			throw new ApiException(405, "Method not allowed");
		}
		confirm(request);
		if ("POST".equals(method) && id != null || !"POST".equals(method) && id == null) {
			throw new ApiException(405, "Invalid SoS operation");
		}
		final JsonNode body = readJson(request);
		final ObjectNode data = manager.sosConfigurations();
		final List<JsonNode> entries = Json.list(data, "entries");
		final JsonNode old = id == null ? null : entries.stream().filter(e -> e.path("id").asText().equals(id)).findFirst().orElse(null);
		if (id != null && old == null) {
			throw new ApiException(404, "SoS not found");
		}
		if (id != null) {
			final JsonNode expected = body.get("expected");
			if (expected == null || expected.isNull() || !expected.path("updatedAt").equals(old.path("updatedAt"))
					|| !SosModel.canonical(SosModel.model(expected)).equals(SosModel.canonical(SosModel.model(old)))) {
				throw new ApiException(409, "This SoS changed since it was opened. Reload before saving.");
			}
		}
		if ("DELETE".equals(method)) {
			manager.plantDescription("remove", "/plant-descriptions/" + id, null);
			return json(200, Json.obj().put("removed", Long.parseLong(id)));
		}
		final JsonNode config = body.get("configuration");
		final List<String> errors = SosModel.validate(config, entries);
		if (!errors.isEmpty()) {
			throw new ApiException(400, String.join(" ", errors));
		}
		if (id != null) {
			for (final JsonNode included : config.path("include")) {
				if (included.asLong() == Long.parseLong(id)) {
					throw new ApiException(400, "An SoS cannot include itself.");
				}
			}
		}
		if (config.path("active").asBoolean() && !(body.path("activationConfirmed").isBoolean() && body.path("activationConfirmed").asBoolean())) {
			throw new ApiException(428, "Confirm saving an active SoS; it affects orchestration.");
		}
		final boolean create = "POST".equals(method);
		return json(create ? 201 : 200, manager.plantDescription(create ? "create" : "replace", id != null ? "/plant-descriptions/" + id : "/plant-descriptions", SosModel.model(config)));
	}

	// ----------------------------------------------------------------------------------------------- floorplans

	static ObjectNode floorplanPayload(final JsonNode body) {
		final JsonNode plan = body == null ? null : body.get("floorplan");
		if (plan == null || !plan.isObject()) {
			throw new ApiException(400, "floorplan object required");
		}
		final ObjectNode out = Json.obj();
		for (final String k : FLOORPLAN_FIELDS) {
			if (plan.has(k)) {
				out.set(k, plan.get(k));
			}
		}
		final JsonNode name = out.get("name");
		if (name == null || !name.isTextual() || name.asText().trim().isEmpty() || name.asText().length() > 200) {
			throw new ApiException(400, "A name of 1-200 characters is required");
		}
		final JsonNode chiplets = out.get("chiplets");
		if (chiplets == null || !chiplets.isArray() || chiplets.isEmpty() || chiplets.size() > 64) {
			throw new ApiException(400, "A floorplan must contain 1-64 chiplets");
		}
		final JsonNode links = out.get("links");
		if (links != null && (!links.isArray() || links.size() > 1024)) {
			throw new ApiException(400, "Invalid links");
		}
		return out;
	}

	private ResponseEntity<?> floorplans(final HttpServletRequest request, final String path, final String method) throws IOException {
		if (!chipletFloorplans) {
			return error(404, "Chiplet floorplan editing is not enabled");
		}
		if ("/api/chiplet/floorplans/library".equals(path) && "GET".equals(method)) {
			return json(200, manager.floorplan("floorplanDiscovery", "query-floorplans", null, "library=true", null));
		}
		final Matcher match = FLOORPLAN.matcher(path);
		if (!match.matches()) {
			return error(404, "Unknown floorplan operation");
		}
		final String id = match.group(1);
		final boolean check = match.group(2) != null;
		if ("GET".equals(method) && !check) {
			if (id != null) {
				return json(200, manager.floorplan("floorplanDiscovery", "get-floorplan", id, null, null));
			}
			final long page;
			final long size;
			try {
				page = request.getParameter("page") == null ? 0 : Long.parseLong(request.getParameter("page"));
				size = request.getParameter("size") == null ? 200 : Long.parseLong(request.getParameter("size"));
			} catch (final NumberFormatException ex) {
				throw new ApiException(400, "Invalid page/size");
			}
			if (page < 0 || page > 1000000 || size < 1 || size > 200) {
				throw new ApiException(400, "Invalid page/size");
			}
			return json(200, manager.floorplan("floorplanDiscovery", "query-floorplans", null, "page=" + page + "&size=" + size, null));
		}
		if (!List.of("POST", "PUT", "DELETE").contains(method)) {
			return error(405, "Method not allowed");
		}
		confirm(request);
		if (check && "POST".equals(method) && id == null) {
			final ObjectNode plan = floorplanPayload(readJson(request));
			if (!plan.hasNonNull("network") || plan.get("network").isBoolean() && !plan.get("network").asBoolean()) {
				throw new ApiException(400, "No network configuration to validate");
			}
			return json(200, manager.floorplan("floorplanManagement", "check-network", null, null, plan));
		}
		if ("POST".equals(method) && id == null && !check) {
			final ObjectNode plan = floorplanPayload(readJson(request));
			plan.put("active", false);
			return json(201, manager.floorplan("floorplanManagement", "create", null, null, plan));
		}
		if (id != null && "PUT".equals(method) && !check) {
			final JsonNode body = readJson(request);
			final ObjectNode plan = floorplanPayload(body);
			// best-effort stale-edit protection; the upstream service remains the authoritative writer and validator
			final JsonNode current = manager.floorplan("floorplanDiscovery", "get-floorplan", id, null, null);
			final JsonNode expected = body.get("expectedUpdatedAt");
			if (expected == null || expected.isNull() || expected.asText().isEmpty() || !expected.equals(current.get("updatedAt"))) {
				throw new ApiException(409, "This floorplan changed since you opened it. Reload it before saving.");
			}
			return json(200, manager.floorplan("floorplanManagement", "replace", id, null, plan));
		}
		if (id != null && "DELETE".equals(method) && !check) {
			manager.floorplan("floorplanManagement", "remove", id, null, null);
			return json(200, Json.obj().put("removed", Long.parseLong(id)));
		}
		return error(405, "Method not allowed");
	}

	// ----------------------------------------------------------------------------------------------- policies

	private ResponseEntity<?> policies(final HttpServletRequest request, final String path, final String method) throws IOException {
		final Matcher match = POLICY.matcher(path);
		if (!match.matches()) {
			return error(404, "Not found");
		}
		final String id = match.group(1) == null ? null : java.net.URLDecoder.decode(match.group(1), java.nio.charset.StandardCharsets.UTF_8);
		final PolicyEditor.Authority authority = manager.policyAuthority();
		if ("GET".equals(method) && id == null) {
			return json(200, PolicyEditor.list(authority));
		}
		if (!List.of("POST", "DELETE").contains(method) || "DELETE".equals(method) && id == null || "POST".equals(method) && id != null) {
			return error(405, "Method not allowed");
		}
		confirm(request);
		final JsonNode body = readJson(request);
		final PolicyEditor.Result result = "DELETE".equals(method) ? PolicyEditor.delete(authority, id, body) : PolicyEditor.save(authority, body);
		return json(result.status(), result.body());
	}
}
