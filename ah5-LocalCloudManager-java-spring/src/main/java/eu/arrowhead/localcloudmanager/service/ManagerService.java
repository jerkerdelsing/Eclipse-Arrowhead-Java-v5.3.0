package eu.arrowhead.localcloudmanager.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;
import eu.arrowhead.localcloudmanager.client.DockerGateway;
import eu.arrowhead.localcloudmanager.client.ProviderGateway;

/**
 * The provider-facing logic of the console API (port of server.js): every provider is reached through orchestration on behalf of
 * LocalCloudManager (ProviderGateway); the ServiceRegistry management inventory with the identity token; Docker through the proxy.
 */
@Service
public class ManagerService {

	public record Settled(boolean ok, JsonNode value, String error) {
	}

	private final ProviderGateway providers;
	private final DockerGateway docker;
	private final RegistrationState registration;
	private final String projectName;
	private final Set<String> managedServices;
	private final String localCloud;
	private final String release;
	private final String orchestrator;
	private final String crlFile;
	private final Deque<ObjectNode> history = new ArrayDeque<>();

	public ManagerService(final ProviderGateway providers, final DockerGateway docker, final RegistrationState registration,
			@Value(LocalCloudManagerConstants.$PROJECT_NAME_WD) final String projectName,
			@Value(LocalCloudManagerConstants.$MANAGED_SERVICES_WD) final String managedServices,
			@Value(LocalCloudManagerConstants.$LOCAL_CLOUD_WD) final String localCloud,
			@Value(LocalCloudManagerConstants.$RELEASE_WD) final String release,
			@Value(LocalCloudManagerConstants.$ORCHESTRATOR_SYSTEM_WD) final String orchestrator,
			@Value(LocalCloudManagerConstants.$CRL_FILE_WD) final String crlFile) {
		this.providers = providers;
		this.docker = docker;
		this.registration = registration;
		this.projectName = projectName;
		this.managedServices = managedServices == null || managedServices.isBlank() ? new LinkedHashSet<>(Inventory.DEFAULT_MANAGED_SERVICES)
				: Arrays.stream(managedServices.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toCollection(LinkedHashSet::new));
		this.localCloud = localCloud;
		this.release = release;
		this.orchestrator = orchestrator;
		this.crlFile = crlFile;
	}

	public Set<String> managedServices() {
		return managedServices;
	}

	public String localCloud() {
		return localCloud;
	}

	public static String encode(final String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	public static Settled settled(final Supplier<JsonNode> call) {
		try {
			final JsonNode value = call.get();
			// a provider may answer without a body
			return new Settled(true, value == null ? com.fasterxml.jackson.databind.node.NullNode.getInstance() : value, null);
		} catch (final RuntimeException ex) {
			return new Settled(false, null, String.valueOf(ex.getMessage()));
		}
	}

	private static <T> CompletableFuture<T> async(final Supplier<T> call) {
		return CompletableFuture.supplyAsync(call);
	}

	private static <T> T join(final CompletableFuture<T> future) {
		try {
			return future.join();
		} catch (final java.util.concurrent.CompletionException ex) {
			if (ex.getCause() instanceof RuntimeException r) {
				throw r;
			}
			throw ex;
		}
	}

	// ----------------------------------------------------------------------------------------------- containers (Docker proxy)

	public List<JsonNode> listContainers() {
		final String filters = encode("{\"label\":[\"com.docker.compose.project=" + projectName + "\"]}");
		final JsonNode containers;
		try {
			containers = Json.MAPPER.readTree(docker.request("/containers/json?all=1&filters=" + filters, "GET"));
		} catch (final RuntimeException ex) {
			throw ex;
		} catch (final Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
		final List<JsonNode> out = new ArrayList<>();
		final Pattern healthPattern = Pattern.compile("\\((healthy|unhealthy|starting)\\)");
		for (final JsonNode c : Json.list(containers)) {
			final String service = Json.or(Json.str(c.path("Labels"), "com.docker.compose.service"), "unknown");
			final Matcher m = healthPattern.matcher(c.path("Status").asText(""));
			final ObjectNode n = Json.obj();
			n.put("id", c.path("Id").asText("").substring(0, Math.min(12, c.path("Id").asText("").length())));
			n.put("name", c.path("Names").path(0).asText("").replaceFirst("^/", ""));
			n.put("service", service);
			n.put("displayName", Inventory.RUNTIME_DISPLAY_NAMES.getOrDefault(service, service));
			n.set("image", c.get("Image"));
			n.set("state", c.get("State"));
			n.set("status", c.get("Status"));
			if (m.find()) {
				n.put("health", m.group(1));
			} else {
				n.putNull("health");
			}
			n.put("created", Json.iso(Instant.ofEpochSecond(c.path("Created").asLong())));
			n.put("managed", managedServices.contains(service));
			out.add(n);
		}
		out.sort(Comparator.comparing(n -> n.path("service").asText()));
		return out;
	}

	private JsonNode containerFor(final String service) {
		return listContainers().stream().filter(c -> service.equals(c.path("service").asText())).findFirst().orElse(null);
	}

	public List<String> logs(final String service, final int tail) {
		final JsonNode container = containerFor(service);
		if (container == null) {
			throw new ApiException(404, "Unknown project service");
		}
		try {
			final String text = Inventory.demuxDockerLogs(docker.request("/containers/" + container.path("id").asText() + "/logs?stdout=1&stderr=1&timestamps=1&tail=" + tail, "GET"));
			final List<String> lines = Arrays.stream(text.split("\n")).filter(l -> !l.isEmpty()).toList();
			return lines.subList(Math.max(0, lines.size() - tail), lines.size());
		} catch (final RuntimeException ex) {
			throw ex;
		} catch (final Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	public ObjectNode performAction(final String service, final String action) {
		if (!managedServices.contains(service)) {
			throw new ApiException(403, "This service is not in the management allowlist");
		}
		if (!List.of("start", "stop", "restart").contains(action)) {
			throw new ApiException(400, "Unsupported lifecycle action");
		}
		final JsonNode container = containerFor(service);
		if (container == null) {
			throw new ApiException(404, "Unknown project service");
		}
		final String suffix = "stop".equals(action) ? "/stop?t=20" : "restart".equals(action) ? "/restart?t=20" : "/start";
		try {
			docker.request("/containers/" + container.path("id").asText() + suffix, "POST");
		} catch (final RuntimeException ex) {
			throw ex;
		} catch (final Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
		return Json.obj().put("service", service).put("action", action).put("accepted", true).put("at", Json.now());
	}

	// ----------------------------------------------------------------------------------------------- ServiceRegistry inventory

	public ObjectNode inventory() {
		final CompletableFuture<JsonNode> devices = async(() -> providers.serviceRegistry("/serviceregistry/mgmt/devices/query", Json.obj()));
		final CompletableFuture<JsonNode> systems = async(() -> providers.serviceRegistry("/serviceregistry/mgmt/systems/query", Json.obj()));
		final JsonNode d = join(devices);
		final JsonNode s = join(systems);
		final List<String> names = Json.list(s, "entries").stream().map(e -> e.path("name").asText()).toList();
		JsonNode services = Json.obj().<ObjectNode>set("entries", Json.arr()).put("count", 0);
		if (!names.isEmpty()) {
			final ObjectNode query = Json.obj();
			query.set("providerNames", Json.strings(names));
			services = providers.serviceRegistry("/serviceregistry/mgmt/service-instances/query", query);
		}
		final ObjectNode out = Json.obj();
		out.set("devices", d);
		out.set("systems", s);
		out.set("services", services);
		return out;
	}

	private static ObjectNode emptyInventory() {
		final ObjectNode out = Json.obj();
		for (final String k : List.of("devices", "systems", "services")) {
			out.putObject(k).put("count", 0).putArray("entries");
		}
		return out;
	}

	// ----------------------------------------------------------------------------------------------- overview

	public ObjectNode overview() {
		final String collectedAt = Json.now();
		final CompletableFuture<List<JsonNode>> containersFuture = async(this::listContainers);
		final CompletableFuture<ObjectNode> inventoryFuture = async(this::inventory);
		List<JsonNode> containers = List.of();
		String dockerError = null;
		try {
			containers = join(containersFuture);
		} catch (final RuntimeException ex) {
			dockerError = String.valueOf(ex.getMessage());
		}
		ObjectNode inventory;
		String arrowheadError = null;
		try {
			inventory = join(inventoryFuture);
		} catch (final RuntimeException ex) {
			arrowheadError = String.valueOf(ex.getMessage());
			inventory = emptyInventory();
		}

		final List<JsonNode> serviceEntries = Json.list(inventory.path("services"), "entries");
		final Map<String, Integer> serviceCounts = new LinkedHashMap<>();
		for (final JsonNode service : serviceEntries) {
			serviceCounts.merge(Json.or(Json.str(service.path("provider"), "name"), "Unknown"), 1, Integer::sum);
		}
		final List<JsonNode> systems = new ArrayList<>();
		for (final JsonNode system : Json.list(inventory.path("systems"), "entries")) {
			systems.add(Inventory.systemStatus(system, containers, serviceCounts));
		}
		final List<JsonNode> devices = Json.list(inventory.path("devices"), "entries");
		final ObjectNode registryManagement = Inventory.buildRegistryManagement(devices, systems, serviceEntries, containers);
		final List<String> naming = Inventory.validateInventoryNames(Json.list(inventory.path("systems"), "entries"), serviceEntries);
		final ObjectNode messaging = Inventory.buildMessagingOverview(containers, serviceEntries);
		final long running = containers.stream().filter(c -> "running".equals(c.path("state").asText(null))).count();
		final ObjectNode revocation = CrlStatus.status(crlFile, System.currentTimeMillis());
		final long unhealthy = containers.stream().filter(c -> "unhealthy".equals(c.path("health").asText(null))).count();
		final Set<String> registered = systems.stream().map(s -> Json.str(s, "composeService")).filter(Json::truthy).collect(Collectors.toSet());
		final Set<String> infrastructure = Set.of("mqtt-broker", "amqp-broker", "chiplet-gateway", "chiplet-adapters");
		final List<String> unregistered = containers.stream()
				.filter(c -> c.path("managed").asBoolean() && !infrastructure.contains(c.path("service").asText()) && "running".equals(c.path("state").asText(null))
						&& !registered.contains(c.path("service").asText()))
				.map(c -> c.path("service").asText()).toList();

		final ObjectNode sample = Json.obj();
		sample.put("at", collectedAt);
		sample.put("running", running);
		sample.put("containers", containers.size());
		sample.put("registeredSystems", systems.size());
		sample.put("registeredServices", inventory.path("services").path("count").asLong(0));
		sample.put("unhealthy", unhealthy);
		sample.put("incidents", unhealthy + unregistered.size() + naming.size() + ("ok".equals(revocation.path("state").asText()) ? 0 : 1));
		sample.set("unregisteredServices", Json.strings(unregistered));
		final ArrayNode samples;
		synchronized (history) {
			history.addLast(sample);
			while (history.size() > LocalCloudManagerConstants.HISTORY_LIMIT) {
				history.removeFirst();
			}
			samples = Json.MAPPER.valueToTree(new ArrayList<>(history));
		}

		final ObjectNode out = Json.obj();
		out.put("collectedAt", collectedAt);
		out.putObject("cloud").put("name", projectName).put("release", release).put("registryReachable", arrowheadError == null).put("dockerReachable", dockerError == null);
		out.set("summary", sample);
		out.set("devices", Json.MAPPER.valueToTree(devices));
		out.set("systems", Json.MAPPER.valueToTree(systems));
		out.set("services", Json.MAPPER.valueToTree(serviceEntries));
		out.set("registryManagement", registryManagement);
		final ObjectNode namingNode = out.putObject("naming").put("valid", naming.isEmpty());
		namingNode.set("violations", Json.strings(naming));
		out.set("revocation", revocation);
		out.set("infrastructure", messaging.get("infrastructure"));
		out.set("orchestrations", messaging.get("orchestrations"));
		out.set("containers", Json.MAPPER.valueToTree(containers));
		out.set("history", samples);
		out.set("registration", registration.get());
		final ObjectNode errors = out.putObject("errors");
		errors.put("docker", dockerError);
		errors.put("arrowhead", arrowheadError);
		errors.put("naming", naming.isEmpty() ? null : "Arrowhead naming violations: " + String.join(", ", naming));
		return out;
	}

	public ObjectNode registryManagement() {
		final ObjectNode overview = overview();
		final ObjectNode out = Json.obj();
		out.set("collectedAt", overview.get("collectedAt"));
		out.set("cloud", overview.get("cloud"));
		overview.path("registryManagement").fields().forEachRemaining(e -> out.set(e.getKey(), e.getValue()));
		out.set("errors", overview.get("errors"));
		return out;
	}

	// ----------------------------------------------------------------------------------------------- DataManager

	private JsonNode dataManager(final String operation, final String path) {
		return providers.call("dataHistorian", operation, null, path, null, null);
	}

	public ObjectNode dataManagerContent(final String limit) {
		final int safe = clamp(limit, 240, 1, 1000);
		final List<String[]> pairs = new ArrayList<>();
		for (final JsonNode system : Json.list(dataManager("list-systems", null), "systems")) {
			for (final JsonNode service : Json.list(dataManager("list-services", "/systems/" + encode(system.asText()) + "/services"), "services")) {
				pairs.add(new String[] { system.asText(), service.asText() });
			}
		}
		final List<ObjectNode> all = new ArrayList<>();
		for (final String[] pair : pairs) {
			try {
				final JsonNode records = dataManager("get-data", "/systems/" + encode(pair[0]) + "/services/" + encode(pair[1]) + "/data?count=" + safe);
				all.addAll(Inventory.resolveSenMLRecords(Json.list(records), pair[0], pair[1]));
			} catch (final ApiException ex) {
				if (ex.status() != 404) {
					throw ex;
				}
			}
		}
		all.sort(Comparator.comparingDouble((ObjectNode e) -> e.path("time").asDouble()).reversed());
		final List<ObjectNode> entries = all.subList(0, Math.min(safe, all.size()));
		final Map<String, ObjectNode> latest = new LinkedHashMap<>();
		for (final ObjectNode e : entries) {
			latest.putIfAbsent(e.path("name").asText(), e);
		}
		final Map<String, ArrayNode> series = new LinkedHashMap<>();
		final Map<String, ObjectNode> meta = new LinkedHashMap<>();
		for (int i = entries.size() - 1; i >= 0; i--) {
			final ObjectNode e = entries.get(i);
			final Double value = Inventory.numericValue(e);
			if (value == null) {
				continue;
			}
			final String name = e.path("name").asText();
			meta.putIfAbsent(name, Inventory.seriesLabel(e));
			final ObjectNode point = series.computeIfAbsent(name, k -> Json.arr()).addObject();
			point.set("time", e.get("time"));
			Inventory.putNumber(point, "value", value);
			point.set("unit", e.get("unit"));
		}
		final ObjectNode out = Json.obj();
		out.put("collectedAt", Json.now());
		out.put("count", entries.size());
		final List<ObjectNode> latestSorted = new ArrayList<>(latest.values());
		latestSorted.sort(Comparator.comparing(e -> e.path("name").asText()));
		out.set("latest", Json.MAPPER.valueToTree(latestSorted));
		out.set("entries", Json.MAPPER.valueToTree(entries));
		final ArrayNode s = out.putArray("series");
		series.forEach((name, points) -> {
			final ObjectNode row = s.addObject().put("name", name);
			row.set("points", points);
			row.setAll(meta.get(name));
		});
		return out;
	}

	static int clamp(final String value, final int fallback, final int min, final int max) {
		double v;
		try {
			v = value == null || value.isBlank() ? fallback : Double.parseDouble(value);
		} catch (final NumberFormatException ex) {
			v = fallback;
		}
		if (Double.isNaN(v) || v == 0) {
			v = fallback;
		}
		return (int) Math.min(Math.max(v, min), max);
	}

	// ----------------------------------------------------------------------------------------------- SysMonitor

	public JsonNode sysMonitor(final String serviceDefinition, final String operation, final String path, final JsonNode body) {
		return providers.call(serviceDefinition, operation, null, path, null, body);
	}

	public ObjectNode sysMonitorSubscriptions() {
		final JsonNode p = sysMonitor("sysMonitorManagement", "providers", null, null);
		final JsonNode s = sysMonitor("sysMonitorManagement", "subscriptions", null, null);
		final JsonNode status = sysMonitor("sysMonitorManagement", "status", null, null);
		final ObjectNode out = Json.obj();
		out.put("collectedAt", Json.now());
		out.set("defaultSubscription", p.get("defaultSubscription"));
		out.set("providers", p.has("list") ? p.get("list") : Json.arr());
		out.set("subscriptions", s.has("list") ? s.get("list") : Json.arr());
		out.set("intake", status.hasNonNull("amqp") ? status.get("amqp") : null);
		return out;
	}

	public ObjectNode sysMonitorHistory(final String system, final String limit) {
		final int safe = clamp(limit, 120, 1, 1000);
		final JsonNode listed = sysMonitor("sysMonitorHistory", "list-systems", null, null);
		final List<ObjectNode> rows = new ArrayList<>();
		for (final JsonNode item : Json.list(listed, "systems")) {
			final ObjectNode row = item.deepCopy();
			try {
				final JsonNode latest = sysMonitor("sysMonitorHistory", "latest", "/systems/" + encode(item.path("systemName").asText()) + "/latest", null);
				row.set("latest", latest.hasNonNull("latest") ? latest.get("latest") : Json.obj());
			} catch (final RuntimeException ex) {
				// one unreachable system must not hide the others
				row.putObject("latest");
				row.put("error", ex instanceof ApiException a && a.status() == 404 ? null : ex.getMessage());
			}
			rows.add(row);
		}
		final boolean known = Json.list(listed, "systems").stream().anyMatch(i -> i.path("systemName").asText().equals(system));
		final String selected = known ? system : rows.isEmpty() ? "" : rows.get(0).path("systemName").asText("");
		final JsonNode observations = selected.isEmpty() ? Json.arr()
				: sysMonitor("sysMonitorHistory", "query", "/systems/" + encode(selected) + "/observations?count=" + safe, null).path("observations");
		final ObjectNode out = Json.obj();
		out.put("collectedAt", Json.now());
		out.set("retentionDays", listed.has("retentionDays") ? listed.get("retentionDays") : null);
		out.put("storedObservations", rows.stream().mapToLong(r -> r.path("observationCount").asLong(0)).sum());
		out.set("systems", Json.MAPPER.valueToTree(rows));
		out.put("selectedSystem", selected);
		out.set("observations", observations.isMissingNode() ? Json.arr() : observations);
		return out;
	}

	// ----------------------------------------------------------------------------------------------- inter-cloud

	public ObjectNode interCloud() {
		final Settled gatekeeper = settled(() -> providers.call("gatekeeperManagement", "status", null, null, null, null));
		final Settled gatetunnel = settled(() -> providers.call("gateTunnelManagement", "status", null, null, null, null));
		final Settled clouds = settled(() -> providers.call("gatekeeperManagement", "clouds", null, null, null, null));
		final Settled producer = settled(() -> providers.call("randomNumberProducer", "latest", null, null, null, null));
		final ObjectNode out = Json.obj();
		out.put("collectedAt", Json.now());
		out.put("localCloud", localCloud);
		final ArrayNode neighbours = out.putArray("neighbourClouds");
		if (clouds.ok()) {
			Json.list(clouds.value(), "entries").forEach(c -> neighbours.add(c.path("name").asText() + "|" + c.path("operator").asText()));
		}
		out.put("neighbourError", clouds.ok() ? null : clouds.error());
		final ObjectNode relay = out.putObject("relay");
		relay.set("gatekeeper", gatekeeper.ok() ? gatekeeper.value().get("relay") : Json.obj().put("connected", false).put("error", gatekeeper.error()));
		relay.set("gatetunnel", gatetunnel.ok() ? gatetunnel.value().get("relay") : Json.obj().put("connected", false).put("error", gatetunnel.error()));
		out.set("sessions", gatetunnel.ok() ? gatetunnel.value().get("interCloud") : null);
		out.set("producer", producer.ok() ? producer.value().get("interCloud") : Json.obj().put("status", "unavailable").put("lastError", producer.error()));
		return out;
	}

	// ----------------------------------------------------------------------------------------------- PlantDescriptionEngine and SoS

	public JsonNode plantDescription(final String operation, final String path, final JsonNode body) {
		return providers.call("plantDescriptionManagement", operation, null, path, null, body);
	}

	public ObjectNode plantDescriptions() {
		final CompletableFuture<JsonNode> descriptionsFuture = async(() -> plantDescription("query", "/plant-descriptions?page=0&size=200", null));
		final CompletableFuture<ObjectNode> inventoryFuture = async(this::inventory);
		final JsonNode descriptions = join(descriptionsFuture);
		ObjectNode registry;
		String registryError = null;
		try {
			registry = join(inventoryFuture);
		} catch (final RuntimeException ex) {
			registry = emptyInventory();
			registryError = ex.getMessage();
		}
		final ObjectNode out = Json.obj();
		out.put("collectedAt", Json.now());
		final ArrayNode entries = out.putArray("entries");
		Json.list(descriptions, "entries").forEach(entries::add);
		if (descriptions.hasNonNull("count")) {
			out.set("count", descriptions.get("count"));
		} else {
			out.put("count", entries.size());
		}
		final List<String> systems = new ArrayList<>(Json.list(registry.path("systems"), "entries").stream().map(s -> s.path("name").asText(null)).toList());
		systems.sort(Comparator.nullsLast(Comparator.naturalOrder()));
		out.set("systems", Json.MAPPER.valueToTree(systems));
		final ArrayNode services = out.putArray("services");
		for (final JsonNode service : Json.list(registry.path("services"), "entries")) {
			final ObjectNode s = services.addObject();
			s.put("provider", Json.or(Json.str(service.path("provider"), "name"), Json.part(Json.str(service, "instanceId"), 0)));
			s.put("serviceDefinition", Json.or(Json.str(service.path("serviceDefinition"), "name"), Json.part(Json.str(service, "instanceId"), 1)));
			final Set<String> templates = new LinkedHashSet<>();
			Json.list(service, "interfaces").forEach(i -> {
				if (Json.truthy(Json.str(i, "templateName"))) {
					templates.add(i.path("templateName").asText());
				}
			});
			s.set("interfaces", Json.strings(templates));
		}
		out.put("registryError", registryError);
		return out;
	}

	public ObjectNode sosConfigurations() {
		final ObjectNode data = plantDescriptions();
		final ArrayNode entries = (ArrayNode) data.get("entries");
		for (int page = 1; entries.size() < data.path("count").asLong(); page++) {
			if (page >= 10) {
				throw new ApiException(503, "SoS view supports up to 2000 stored configurations");
			}
			final List<JsonNode> rows = Json.list(plantDescription("query", "/plant-descriptions?page=" + page + "&size=200", null), "entries");
			if (rows.isEmpty()) {
				throw new ApiException(409, "SoS catalog changed while reading; reload");
			}
			rows.forEach(entries::add);
		}
		return data;
	}

	// ----------------------------------------------------------------------------------------------- orchestration rules and consumers

	public JsonNode orchestration(final String operation, final JsonNode body, final String path) {
		return providers.call("serviceOrchestrationSimpleStoreManagement", operation, null, path, null, body);
	}

	public ObjectNode catalog(final ObjectNode inventory) {
		return Inventory.buildOrchestrationCatalog(Json.list(inventory.path("systems"), "entries"), Json.list(inventory.path("services"), "entries"));
	}

	public List<JsonNode> rulesOnly() {
		final ObjectNode catalog = catalog(inventory());
		final List<JsonNode> consumers = Json.list(catalog, "consumers");
		if (consumers.isEmpty()) {
			return List.of();
		}
		final ObjectNode query = Json.obj();
		query.set("consumerNames", Json.strings(consumers.stream().map(c -> c.path("name").asText()).toList()));
		return Json.list(orchestration("query", query, null), "entries");
	}

	public ObjectNode orchestrationRules() {
		final ObjectNode catalog = catalog(inventory());
		final List<JsonNode> consumers = Json.list(catalog, "consumers");
		JsonNode rules = Json.obj().<ObjectNode>set("entries", Json.arr()).put("count", 0);
		if (!consumers.isEmpty()) {
			final ObjectNode query = Json.obj();
			query.set("consumerNames", Json.strings(consumers.stream().map(c -> c.path("name").asText()).toList()));
			rules = orchestration("query", query, null);
		}
		final Settled dynamic = settled(this::dynamicOrchestrations);
		final ObjectNode out = Json.obj();
		out.set("catalog", catalog);
		out.set("rules", rules.has("entries") ? rules.get("entries") : Json.arr());
		out.put("count", rules.path("count").asLong(0));
		out.set("dynamic", dynamic.ok() ? dynamic.value() : Json.arr());
		out.put("dynamicError", dynamic.ok() ? null : dynamic.error());
		out.put("collectedAt", Json.now());
		return out;
	}

	public JsonNode createRule(final JsonNode candidate) {
		final ObjectNode normalized = Inventory.validateRuleCandidate(catalog(inventory()), candidate);
		final ObjectNode body = Json.obj();
		body.putArray("candidates").add(normalized);
		return orchestration("create", body, null);
	}

	public ObjectNode removeRule(final String id) {
		if (id == null || !id.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) {
			throw new ApiException(400, "Invalid orchestration rule id");
		}
		orchestration("remove", null, "/remove?uuids=" + encode(id));
		return Json.obj().put("id", id).put("removed", true).put("at", Json.now());
	}

	private static final Pattern RESULT = Pattern.compile("^(\\d+) (local|inter-cloud) result(?:: (.*))?$");

	/** DynamicServiceOrchestration history aggregated per consumer, service and kind, plus active GateTunnel consumer sessions. */
	public ArrayNode dynamicOrchestrations() {
		final ObjectNode body = Json.obj();
		body.putObject("pagination").put("page", 0).put("size", 500).put("direction", "DESC").put("sortField", "createdAt");
		final JsonNode history = providers.call("serviceOrchestrationHistoryManagement", "query", orchestrator, null, null, body);
		final Map<String, ObjectNode> groups = new LinkedHashMap<>();
		for (final JsonNode job : Json.list(history, "entries")) {
			final String consumer = Json.or(Json.str(job, "targetSystem"), Json.str(job, "requesterSystem"));
			final String message = Json.or(Json.str(job, "message"), "");
			final String kind = message.contains("inter-cloud") ? "inter-cloud" : "local";
			final String key = consumer + "|" + job.path("serviceDefinition").asText(null) + "|" + kind;
			final ObjectNode group = groups.computeIfAbsent(key, k -> {
				final ObjectNode g = Json.obj().put("consumer", consumer);
				g.set("serviceDefinition", job.get("serviceDefinition"));
				g.put("count", 0);
				g.putNull("providers");
				g.putNull("kind");
				g.put("lastAt", Json.or(Json.str(job, "finishedAt"), Json.str(job, "createdAt")));
				g.set("lastStatus", job.get("status"));
				g.set("lastMessage", job.get("message"));
				g.set("type", job.get("type"));
				return g;
			});
			group.put("count", group.path("count").asInt() + 1);
			final Matcher m = RESULT.matcher(message);
			if (group.get("providers").isNull() && m.matches() && Json.truthy(m.group(3))) {
				group.put("kind", m.group(2));
				final ArrayNode providerList = Json.arr();
				for (final String item : m.group(3).split(", ")) {
					final String[] parts = item.split("@", 2);
					final ObjectNode p = providerList.addObject().put("provider", parts[0]);
					if (parts.length > 1 && !parts[1].isEmpty()) {
						p.put("cloud", parts[1]);
					} else {
						p.putNull("cloud");
					}
				}
				group.set("providers", providerList);
			}
		}
		final Settled tunnel = settled(() -> providers.call("gateTunnelManagement", "status", null, null, null, null));
		if (tunnel.ok()) {
			for (final JsonNode s : Json.list(tunnel.value().path("interCloud"), "sessions")) {
				if (!"consumer".equals(Json.str(s, "side"))) {
					continue;
				}
				final String key = s.path("consumer").asText() + "|" + s.path("serviceDefinition").asText() + "|inter-cloud";
				ObjectNode group = groups.get(key);
				if (group == null) {
					group = Json.obj();
					group.set("consumer", s.get("consumer"));
					group.set("serviceDefinition", s.get("serviceDefinition"));
					group.put("count", 0).put("type", "PULL");
				}
				group.put("kind", "inter-cloud");
				final ObjectNode p = Json.obj();
				p.set("provider", s.get("provider"));
				p.set("cloud", s.get("providerCloud"));
				group.set("providers", Json.arr().add(p));
				if (!Json.truthy(Json.str(group, "lastAt"))) {
					group.put("lastAt", Json.iso(Instant.ofEpochSecond(s.path("createdAt").asLong())));
				}
				group.put("lastStatus", "session active (" + s.path("requests").asText() + " requests)");
				if (!Json.truthy(Json.str(group, "lastMessage"))) {
					group.put("lastMessage", "GateTunnel session");
				}
				groups.put(key, group);
			}
		}
		final List<ObjectNode> rows = new ArrayList<>();
		for (final ObjectNode g : groups.values()) {
			if (!g.has("providers") || g.get("providers").isNull()) {
				g.putArray("providers");
			}
			rows.add(g);
		}
		rows.sort(Comparator.<ObjectNode, String>comparing(g -> g.path("consumer").asText()).thenComparing(g -> g.path("serviceDefinition").asText()));
		return Json.MAPPER.valueToTree(rows);
	}

	public List<JsonNode> authorizationPolicies(final String level) {
		final ObjectNode body = Json.obj();
		body.putObject("pagination").put("page", 0).put("size", 1000);
		body.put("level", level);
		return Json.list(providers.call("authorizationManagement", "query-policies", null, null, null, body), "entries");
	}

	private static String policyText(final JsonNode policy) {
		if (policy == null || policy.isNull() || policy.isMissingNode()) {
			return "—";
		}
		final String type = Json.str(policy, "policyType");
		final List<String> names = Json.list(policy, "policyList").stream().map(JsonNode::asText).toList();
		if ("ALL".equals(type)) {
			return "everyone";
		}
		if ("WHITELIST".equals(type)) {
			return names.isEmpty() ? "nobody" : String.join(", ", names);
		}
		if ("BLACKLIST".equals(type)) {
			return "everyone except " + String.join(", ", names);
		}
		return type;
	}

	/** Per provider service: who may consume it (ConsumerAuthorization) and who is orchestrated to it. */
	public ObjectNode serviceConsumers() {
		final Settled mgmt = settled(() -> Json.MAPPER.valueToTree(authorizationPolicies("MGMT")));
		final Settled provider = settled(() -> Json.MAPPER.valueToTree(authorizationPolicies("PROVIDER")));
		final Settled dynamic = settled(this::dynamicOrchestrations);
		final Settled rules = settled(() -> Json.MAPPER.valueToTree(rulesOnly()));
		final Settled plant = settled(() -> providers.call("serviceOrchestrationPlantManagement", "query", "DynamicServiceOrchestration", null, null, Json.obj()));
		final Map<String, ObjectNode> rows = new LinkedHashMap<>();
		final RowFactory row = (p, service, cloud) -> rows.computeIfAbsent(p + "|" + service + "|" + cloud, k -> {
			final ObjectNode r = Json.obj().put("provider", p).put("serviceDefinition", service).put("providerCloud", cloud);
			r.putArray("authorized");
			r.putArray("orchestrated");
			return r;
		});
		final List<JsonNode> policies = new ArrayList<>();
		if (mgmt.ok()) {
			policies.addAll(Json.list(mgmt.value()));
		}
		if (provider.ok()) {
			policies.addAll(Json.list(provider.value()));
		}
		for (final JsonNode policy : policies) {
			if (!"SERVICE_DEF".equals(Json.str(policy, "targetType"))) {
				continue;
			}
			final String cloud = Json.str(policy, "cloud");
			final String consumerCloud = !Json.truthy(cloud) || "LOCAL".equals(cloud) ? localCloud : cloud.split("\\|")[0];
			final ObjectNode entry = row.get(Json.str(policy, "provider"), Json.str(policy, "target"), localCloud);
			final ObjectNode a = ((ArrayNode) entry.get("authorized")).addObject().put("consumerCloud", consumerCloud);
			a.set("level", policy.get("level"));
			a.put("consumers", policyText(policy.get("defaultPolicy")));
			final ArrayNode scoped = a.putArray("scoped");
			policy.path("scopedPolicies").fields().forEachRemaining(e -> scoped.add(e.getKey() + ": " + policyText(e.getValue())));
		}
		if (dynamic.ok()) {
			for (final JsonNode group : Json.list(dynamic.value())) {
				for (final JsonNode p : Json.list(group, "providers")) {
					final String cloud = Json.truthy(Json.str(p, "cloud")) ? p.path("cloud").asText().split("\\|")[0] : localCloud;
					final ObjectNode o = ((ArrayNode) row.get(Json.str(p, "provider"), Json.str(group, "serviceDefinition"), cloud).get("orchestrated")).addObject();
					o.set("consumer", group.get("consumer"));
					o.put("consumerCloud", localCloud);
					o.put("via", Json.truthy(Json.str(p, "cloud")) ? "DynamicServiceOrchestration (inter-cloud, GateTunnel)" : "DynamicServiceOrchestration");
					o.set("count", group.get("count"));
					o.set("lastAt", group.get("lastAt"));
				}
			}
		}
		final Settled tunnel = settled(() -> providers.call("gateTunnelManagement", "status", null, null, null, null));
		if (tunnel.ok()) {
			for (final JsonNode s : Json.list(tunnel.value().path("interCloud"), "sessions")) {
				if ("provider".equals(Json.str(s, "side"))) {
					final ObjectNode o = ((ArrayNode) row.get(Json.str(s, "provider"), Json.str(s, "serviceDefinition"), localCloud).get("orchestrated")).addObject();
					o.set("consumer", s.get("consumer"));
					o.put("consumerCloud", s.path("consumerCloud").asText("").split("\\|")[0]);
					o.put("via", "inter-cloud GateTunnel session (" + s.path("requests").asText() + " requests)");
				}
			}
		}
		if (rules.ok()) {
			for (final JsonNode rule : Json.list(rules.value())) {
				final String id = rule.path("serviceInstanceId").asText("");
				final String p = Json.part(id, 0);
				final String service = Json.part(id, 1);
				if (Json.truthy(p) && Json.truthy(service)) {
					final ObjectNode o = ((ArrayNode) row.get(p, service, localCloud).get("orchestrated")).addObject();
					o.set("consumer", rule.hasNonNull("consumerName") ? rule.get("consumerName") : rule.get("consumer"));
					o.put("consumerCloud", localCloud);
					o.put("via", "SimpleStore rule (priority " + rule.path("priority").asText() + ")");
				}
			}
		}
		if (plant.ok()) {
			for (final JsonNode scope : Json.list(plant.value(), "rules")) {
				for (final JsonNode p : Json.list(scope, "providers")) {
					final String providerName = Json.part(p.path("serviceInstanceId").asText(), 0);
					final ObjectNode o = ((ArrayNode) row.get(providerName, Json.str(scope, "serviceDefinition"), localCloud).get("orchestrated")).addObject();
					o.set("consumer", scope.get("consumer"));
					o.put("consumerCloud", localCloud);
					o.put("via", "DSO plant rule (priority " + p.path("priority").asText() + ")");
				}
			}
		}
		final ObjectNode out = Json.obj();
		out.put("collectedAt", Json.now());
		out.put("localCloud", localCloud);
		final ArrayNode errors = out.putArray("errors");
		for (final Settled s : List.of(mgmt, provider, dynamic, rules, plant)) {
			if (!s.ok()) {
				errors.add(s.error());
			}
		}
		final List<ObjectNode> sorted = new ArrayList<>(rows.values());
		sorted.sort(Comparator.<ObjectNode, String>comparing(r -> r.path("provider").asText()).thenComparing(r -> r.path("serviceDefinition").asText()));
		out.set("rows", Json.MAPPER.valueToTree(sorted));
		return out;
	}

	@FunctionalInterface
	private interface RowFactory {
		ObjectNode get(String provider, String service, String cloud);
	}

	// ----------------------------------------------------------------------------------------------- policies

	public PolicyEditor.Authority policyAuthority() {
		return new PolicyEditor.Authority() {
			@Override
			public ObjectNode inventory() {
				return ManagerService.this.inventory();
			}

			@Override
			public List<JsonNode> query() {
				final List<JsonNode> all = new ArrayList<>(authorizationPolicies("MGMT"));
				all.addAll(authorizationPolicies("PROVIDER"));
				return all;
			}

			@Override
			public JsonNode grant(final JsonNode policy) {
				final ObjectNode body = Json.obj();
				body.putArray("list").add(policy);
				return providers.call("authorizationManagement", "grant-policies", "ConsumerAuthorization", null, null, body);
			}

			@Override
			public void revoke(final String instanceId) {
				providers.call("authorizationManagement", "revoke-policies", "ConsumerAuthorization", "/revoke?instanceIds=" + encode(instanceId), null, null);
			}
		};
	}

	// ----------------------------------------------------------------------------------------------- floorplans and LocalCloudConfigurator

	public JsonNode floorplan(final String definition, final String operation, final String id, final String query, final JsonNode body) {
		String path = providers.operationPath(definition, operation, "ChipletFloorplanGenerator");
		if (id != null) {
			path = path.replaceAll("\\{[^}]+\\}", Matcher.quoteReplacement(encode(id)));
		}
		if (path.matches(".*\\{[^}]+\\}.*")) {
			throw new ApiException(502, "Incomplete floorplan route");
		}
		if (query != null) {
			path += (path.contains("?") ? "&" : "?") + query;
		}
		return providers.call(definition, operation, "ChipletFloorplanGenerator", path, null, body);
	}

	/** localCloudConfiguration of LocalCloudConfigurator, consumed through orchestration; status and body are passed through. */
	public ProviderGateway.Response localCloud(final String operation, final String path, final String method, final JsonNode body) {
		return providers.exchange("localCloudConfiguration", operation, "LocalCloudConfigurator", path, method, body);
	}
}
