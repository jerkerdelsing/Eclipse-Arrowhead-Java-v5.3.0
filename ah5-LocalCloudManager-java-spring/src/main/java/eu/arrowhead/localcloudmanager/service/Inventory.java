package eu.arrowhead.localcloudmanager.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** The pure functions of the native console API (server.js): names, inventory, registry management, messaging, rules and SenML. */
public final class Inventory {

	public static final List<String> DEFAULT_MANAGED_SERVICES = List.of("serviceregistry", "serviceorchestration-dynamic", "serviceorchestration-simple",
			"consumerauthorization", "authentication", "blacklist", "translation-manager", "device-qos-evaluator", "onboarding", "datamanager", "graphdbmanager",
			"timemanager", "eventhandler", "configuration", "choreographer", "random-data-producer", "sysmonitor", "aigym", "certificate-authority", "mqtt-broker",
			"amqp-broker", "gatekeeper", "gatetunnel");

	public static final Map<String, String> SYSTEM_TO_SERVICE;
	public static final Map<String, String> RUNTIME_DISPLAY_NAMES;
	private static final Set<String> INFRASTRUCTURE = Set.of("mqtt-broker", "amqp-broker", "chiplet-gateway", "chiplet-adapters");

	static {
		final Map<String, String> s = new LinkedHashMap<>();
		final String[][] pairs = { { "ChipletFloorplanGenerator", "chiplet-floorplan-generator" }, { "ResourceRegistry", "resource-registry" },
				{ "ResourceOrchestration", "resource-orchestration" }, { "ResourceScheduler", "resource-scheduler" }, { "NetworkManager", "network-manager" },
				{ "ServiceRegistry", "serviceregistry" }, { "DynamicServiceOrchestration", "serviceorchestration-dynamic" },
				{ "SimpleStoreServiceOrchestration", "serviceorchestration-simple" }, { "ConsumerAuthorization", "consumerauthorization" },
				{ "Authentication", "authentication" }, { "Blacklist", "blacklist" }, { "TranslationManager", "translation-manager" },
				{ "TestTranslation", "test-translation" }, { "DeviceQoSEvaluator", "device-qos-evaluator" }, { "Onboarding", "onboarding" },
				{ "DataManager", "datamanager" }, { "GraphDBManager", "graphdbmanager" }, { "TimeManager", "timemanager" }, { "EventHandler", "eventhandler" },
				{ "PlantDescriptionEngine", "plantdescriptionengine" }, { "Configuration", "configuration" }, { "Choreographer", "choreographer" },
				{ "RandomDataProducer", "random-data-producer" }, { "SysMonitor", "sysmonitor" }, { "AIGym", "aigym" },
				{ "CertificateAuthority", "certificate-authority" }, { "Gatekeeper", "gatekeeper" }, { "GateTunnel", "gatetunnel" },
				{ "LocalCloudConfigurator", "localcloudconfigurator" }, { "LocalCloudManager", "cloud-monitor-api" } };
		for (final String[] p : pairs) {
			s.put(p[0], p[1]);
		}
		SYSTEM_TO_SERVICE = Map.copyOf(s);
		final Map<String, String> d = new LinkedHashMap<>();
		s.forEach((system, service) -> d.put(service, system));
		d.put("mqtt-broker", "MQTTBroker");
		d.put("amqp-broker", "AMQPBroker");
		d.put("cloud-monitor-ui", "LocalCloudManagerUI");
		d.put("system-monitor-publisher", "SysMonitorCompatibilityAdapter");
		RUNTIME_DISPLAY_NAMES = Map.copyOf(d);
	}

	private static final Pattern SYSTEM = Pattern.compile("^[A-Z][0-9A-Za-z]*$");
	private static final Pattern DEFINITION = Pattern.compile("^[a-z][0-9A-Za-z]*$");
	private static final Pattern OPERATION = Pattern.compile("^[a-z](?:[0-9a-z-]*[0-9a-z])?$");

	private Inventory() {
		throw new UnsupportedOperationException();
	}

	public static List<String> validateInventoryNames(final List<JsonNode> systems, final List<JsonNode> services) {
		final Set<String> violations = new TreeSet<>();
		for (final JsonNode system : systems) {
			final String name = Json.str(system, "name");
			if (!SYSTEM.matcher(name == null ? "" : name).matches()) {
				violations.add("system:" + (Json.truthy(name) ? name : "<missing>"));
			}
			if ("SystemMonitorPublisher".equals(name)) {
				violations.add("legacy-system:SystemMonitorPublisher");
			}
		}
		for (final JsonNode service : services) {
			final String definition = Json.or(Json.str(service.path("serviceDefinition"), "name"), "");
			if (!DEFINITION.matcher(definition).matches()) {
				violations.add("service-definition:" + (definition.isEmpty() ? "<missing>" : definition));
			}
			if ("monitor".equals(definition) || "monitoring".equals(definition)) {
				violations.add("legacy-service-definition:" + definition);
			}
			for (final JsonNode intf : Json.list(service, "interfaces")) {
				final JsonNode operations = intf.path("properties").get("operations");
				final List<String> names = new ArrayList<>();
				if (operations != null && operations.isArray()) {
					operations.forEach(o -> names.add(o.asText()));
				} else {
					names.addAll(Json.fieldNames(operations));
				}
				for (final String operation : names) {
					if (!OPERATION.matcher(operation).matches()) {
						violations.add("operation:" + (operation.isEmpty() ? "<missing>" : operation));
					}
				}
			}
		}
		// JavaScript sorts by UTF-16 code units, like the natural order of String
		return new ArrayList<>(violations);
	}

	public static boolean hasX509Identity(final JsonNode system) {
		final JsonNode metadata = system.path("metadata");
		for (final String key : Json.fieldNames(metadata)) {
			final JsonNode v = metadata.get(key);
			if (key.toLowerCase().matches(".*x509.*public.*key.*") && v.isTextual() && !v.asText().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public static ObjectNode buildOrchestrationCatalog(final List<JsonNode> systems, final List<JsonNode> services) {
		final Map<String, Set<String>> templates = new LinkedHashMap<>();
		for (final JsonNode service : services) {
			final String provider = Json.str(service.path("provider"), "name");
			if (!Json.truthy(provider)) {
				continue;
			}
			final Set<String> set = templates.computeIfAbsent(provider, k -> new TreeSet<>());
			for (final JsonNode intf : Json.list(service, "interfaces")) {
				final String t = Json.str(intf, "templateName");
				if (Json.truthy(t)) {
					set.add(t);
				}
			}
		}
		for (final JsonNode system : systems) {
			final String name = Json.str(system, "name");
			final Set<String> set = templates.computeIfAbsent(name, k -> new TreeSet<>());
			final String declared = Json.str(system.path("metadata"), "consumerInterfaceTemplates");
			if (declared != null) {
				for (final String t : declared.split(",")) {
					if (t.trim().matches("^[a-z0-9_]+$")) {
						set.add(t.trim());
					}
				}
			}
		}

		final List<ObjectNode> consumers = new ArrayList<>();
		for (final JsonNode system : systems) {
			final String name = Json.str(system, "name");
			final ObjectNode c = Json.obj().put("name", name);
			final String version = Json.str(system, "version");
			if (Json.truthy(version)) {
				c.put("version", version);
			} else {
				c.putNull("version");
			}
			c.put("x509", hasX509Identity(system));
			c.set("interfaceTemplates", Json.strings(templates.getOrDefault(name, Set.of())));
			consumers.add(c);
		}
		consumers.sort(Comparator.comparing(c -> c.path("name").asText()));

		final List<ObjectNode> producers = new ArrayList<>();
		for (final JsonNode service : services) {
			final String provider = Json.or(Json.str(service.path("provider"), "name"), "Unknown");
			final ArrayNode interfaces = Json.arr();
			for (final JsonNode intf : Json.list(service, "interfaces")) {
				final ObjectNode i = interfaces.addObject();
				i.set("templateName", intf.get("templateName"));
				i.set("protocol", intf.get("protocol"));
				i.set("policy", intf.get("policy"));
			}
			final ArrayNode compatible = Json.arr();
			for (final ObjectNode consumer : consumers) {
				final String name = consumer.path("name").asText();
				if (name.equals(provider)) {
					continue;
				}
				final Set<String> own = new LinkedHashSet<>();
				consumer.path("interfaceTemplates").forEach(t -> own.add(t.asText()));
				boolean ok = false;
				for (final JsonNode intf : interfaces) {
					if (own.contains(intf.path("templateName").asText(null)) && (!"CERT_AUTH".equals(intf.path("policy").asText(null)) || consumer.path("x509").asBoolean())) {
						ok = true;
					}
				}
				if (ok) {
					compatible.add(name);
				}
			}
			if (compatible.isEmpty()) {
				continue;
			}
			final ObjectNode p = Json.obj();
			p.set("instanceId", service.get("instanceId"));
			p.put("provider", provider);
			p.put("serviceDefinition", Json.or(Json.str(service.path("serviceDefinition"), "name"), "Unknown"));
			p.set("version", service.get("version"));
			p.set("interfaces", interfaces);
			p.set("compatibleConsumers", compatible);
			producers.add(p);
		}
		producers.sort(Comparator.<ObjectNode, String>comparing(p -> p.path("serviceDefinition").asText()).thenComparing(p -> p.path("provider").asText()));

		final ObjectNode out = Json.obj();
		out.set("producers", Json.MAPPER.valueToTree(producers));
		out.set("consumers", Json.MAPPER.valueToTree(consumers));
		return out;
	}

	public static ObjectNode validateRuleCandidate(final JsonNode catalog, final JsonNode candidate) {
		final String consumer = candidate != null && candidate.path("consumer").isTextual() ? candidate.path("consumer").asText().trim() : "";
		final String instance = candidate != null && candidate.path("serviceInstanceId").isTextual() ? candidate.path("serviceInstanceId").asText().trim() : "";
		final JsonNode p = candidate == null ? null : candidate.get("priority");
		final boolean integer = p != null && p.isNumber() && p.asDouble() == Math.rint(p.asDouble()) && p.asDouble() > 0
				|| p != null && p.isTextual() && p.asText().matches("\\d+") && Long.parseLong(p.asText()) > 0;
		if (consumer.isEmpty() || instance.isEmpty() || !integer) {
			throw new ApiException(400, "Consumer, service instance and a positive integer priority are required");
		}
		JsonNode producer = null;
		for (final JsonNode entry : Json.list(catalog, "producers")) {
			if (instance.equals(entry.path("instanceId").asText(null))) {
				producer = entry;
			}
		}
		if (producer == null) {
			throw new ApiException(409, "The selected producer is no longer available");
		}
		boolean compatible = false;
		for (final JsonNode c : producer.path("compatibleConsumers")) {
			compatible |= consumer.equals(c.asText());
		}
		if (!compatible) {
			throw new ApiException(409, "The selected consumer is not interoperable with this producer");
		}
		return Json.obj().put("consumer", consumer).put("serviceInstanceId", instance).put("priority", (long) (p.isNumber() ? p.asDouble() : Long.parseLong(p.asText())));
	}

	public static ObjectNode systemStatus(final JsonNode system, final List<JsonNode> containers, final Map<String, Integer> serviceCounts) {
		final ObjectNode out = system.deepCopy();
		final String composeService = SYSTEM_TO_SERVICE.get(Json.str(system, "name"));
		JsonNode container = null;
		for (final JsonNode c : containers) {
			if (composeService != null && composeService.equals(c.path("service").asText(null))) {
				container = c;
				break;
			}
		}
		if (composeService == null) {
			out.putNull("composeService");
		} else {
			out.put("composeService", composeService);
		}
		out.put("serviceCount", serviceCounts.getOrDefault(Json.str(system, "name"), 0));
		if (container == null) {
			out.putNull("runtime");
		} else {
			final ObjectNode runtime = out.putObject("runtime");
			runtime.set("state", container.get("state"));
			runtime.set("status", container.get("status"));
			runtime.set("health", container.get("health"));
			runtime.set("managed", container.get("managed"));
		}
		return out;
	}

	private static String providerOf(final JsonNode service) {
		return Json.or(Json.str(service.path("provider"), "name"), Json.part(Json.str(service, "instanceId"), 0));
	}

	private static String definitionOf(final JsonNode service) {
		return Json.or(Json.str(service.path("serviceDefinition"), "name"), Json.part(Json.str(service, "instanceId"), 1));
	}

	private static boolean running(final JsonNode system) {
		return "running".equals(system.path("runtime").path("state").asText(null));
	}

	public static ObjectNode buildRegistryManagement(final List<JsonNode> devices, final List<JsonNode> systems, final List<JsonNode> services, final List<JsonNode> containers) {
		final Map<String, List<JsonNode>> byProvider = new LinkedHashMap<>();
		for (final JsonNode service : services) {
			byProvider.computeIfAbsent(Json.or(providerOf(service), "Unknown"), k -> new ArrayList<>()).add(service);
		}

		final Map<String, ObjectNode> groups = new LinkedHashMap<>();
		for (final JsonNode device : devices) {
			final ObjectNode g = Json.obj();
			g.set("name", device.get("name"));
			g.set("addresses", device.has("addresses") ? device.get("addresses") : Json.arr());
			g.set("metadata", device.has("metadata") ? device.get("metadata") : Json.obj());
			g.put("registered", true);
			g.putArray("systems");
			groups.put(device.path("name").asText(), g);
		}
		final ObjectNode unassigned = Json.obj().put("name", "No registered device");
		unassigned.putArray("addresses");
		unassigned.putObject("metadata");
		unassigned.put("registered", false);
		unassigned.putArray("systems");

		for (final JsonNode system : systems) {
			final String deviceName = Json.or(Json.str(system.path("device"), "name"), Json.str(system, "deviceName"));
			final ObjectNode entry = system.deepCopy();
			if (deviceName == null) {
				entry.putNull("deviceName");
			} else {
				entry.put("deviceName", deviceName);
			}
			final boolean deployed = running(system);
			entry.put("deployed", deployed);
			final ArrayNode own = entry.putArray("services");
			for (final JsonNode service : byProvider.getOrDefault(system.path("name").asText(), List.of())) {
				own.add(((ObjectNode) service.deepCopy()).put("deployed", deployed));
			}
			if (deviceName != null) {
				final ObjectNode g = groups.computeIfAbsent(deviceName, k -> {
					final ObjectNode n = Json.obj().put("name", k);
					n.putArray("addresses");
					n.putObject("metadata");
					n.put("registered", false);
					n.putArray("systems");
					return n;
				});
				((ArrayNode) g.get("systems")).add(entry);
			} else {
				((ArrayNode) unassigned.get("systems")).add(entry);
			}
		}

		final Set<String> registered = new LinkedHashSet<>();
		for (final JsonNode system : systems) {
			final String cs = Json.str(system, "composeService");
			if (Json.truthy(cs)) {
				registered.add(cs);
			}
		}
		final ArrayNode runtimeOnly = Json.arr();
		for (final JsonNode c : containers) {
			final String service = c.path("service").asText();
			if (c.path("managed").asBoolean() && !INFRASTRUCTURE.contains(service) && !registered.contains(service)) {
				final ObjectNode r = runtimeOnly.addObject();
				r.put("name", Json.or(Json.str(c, "displayName"), service));
				r.put("composeService", service);
				r.put("deployed", "running".equals(c.path("state").asText(null)));
				r.set("runtime", c);
				r.put("registered", false);
				r.putArray("services");
			}
		}

		final List<ObjectNode> sorted = new ArrayList<>(groups.values());
		sorted.sort(Comparator.comparing(g -> g.path("name").asText()));
		final ArrayNode outGroups = Json.arr();
		for (final ObjectNode g : sorted) {
			outGroups.add(summarize(g));
		}
		if (!unassigned.get("systems").isEmpty()) {
			outGroups.add(summarize(unassigned));
		}

		final ObjectNode summary = Json.obj();
		summary.put("registeredDevices", devices.size());
		int deployedDevices = 0;
		for (final JsonNode g : outGroups) {
			if (g.path("registered").asBoolean() && g.path("deployed").asBoolean()) {
				deployedDevices++;
			}
		}
		summary.put("deployedDevices", deployedDevices);
		summary.put("registeredSystems", systems.size());
		summary.put("deployedSystems", systems.stream().filter(Inventory::running).count());
		summary.put("registeredServices", services.size());
		summary.put("deployedServices", services.stream().filter(s -> systems.stream().anyMatch(sys -> sys.path("name").asText().equals(providerOf(s)) && running(sys))).count());
		summary.put("runtimeOnly", runtimeOnly.size());

		final ObjectNode out = Json.obj();
		out.set("devices", outGroups);
		out.set("runtimeOnly", runtimeOnly);
		out.set("summary", summary);
		return out;
	}

	private static ObjectNode summarize(final ObjectNode device) {
		final ObjectNode out = device.deepCopy();
		boolean deployed = false;
		int count = 0;
		for (final JsonNode s : device.path("systems")) {
			deployed |= s.path("deployed").asBoolean();
			count += s.path("services").size();
		}
		out.put("deployed", deployed);
		out.put("serviceCount", count);
		return out;
	}

	public static ObjectNode buildMessagingOverview(final List<JsonNode> containers, final List<JsonNode> services) {
		final Function<String, JsonNode> runtime = service -> {
			for (final JsonNode c : containers) {
				if (service.equals(c.path("service").asText(null))) {
					final ObjectNode r = Json.obj();
					for (final String k : List.of("service", "state", "status", "health", "managed")) {
						r.set(k, c.get(k));
					}
					return r;
				}
			}
			return null;
		};
		final Set<String> instances = new LinkedHashSet<>();
		services.forEach(s -> instances.add(s.path("instanceId").asText(null)));
		final Function<String, Boolean> isRunning = s -> {
			final JsonNode r = runtime.apply(s);
			return r != null && "running".equals(r.path("state").asText(null));
		};

		final List<ObjectNode> producers = new ArrayList<>();
		for (final JsonNode service : services) {
			if (!"sysMonitor".equals(definitionOf(service))) {
				continue;
			}
			final String provider = providerOf(service);
			final boolean delegated = "delegated".equals(service.path("metadata").path("publisherMode").asText(null));
			final String providerService = SYSTEM_TO_SERVICE.get(provider);
			final ObjectNode flow = Json.obj();
			flow.put("id", "sysmonitor-publish-" + provider.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
			flow.put("source", provider);
			flow.put("target", "AMQP Broker");
			flow.put("label", "sysMonitor.observe · " + (delegated ? "delegated" : "native"));
			flow.put("protocol", "AMQPS · CERT_AUTH · confirmed");
			flow.put("active", isRunning.apply("amqp-broker") && (delegated ? isRunning.apply("system-monitor-publisher") : providerService == null || isRunning.apply(providerService)));
			producers.add(flow);
		}
		producers.sort(Comparator.comparing(f -> f.path("source").asText()));

		final ArrayNode infrastructure = Json.arr();
		final ObjectNode amqp = infrastructure.addObject().put("id", "amqp-broker").put("name", "AMQP Broker").put("kind", "Messaging infrastructure · sysMonitor")
				.put("endpoint", "amqps://amqp-broker:5671 · exchange arrowhead").put("protocol", "AMQP 0-9-1").put("security", "Mutual X.509 · SASL EXTERNAL · topic permissions");
		amqp.set("runtime", runtime.apply("amqp-broker"));
		final ObjectNode mqtt = infrastructure.addObject().put("id", "mqtt-broker").put("name", "MQTT Broker").put("kind", "Messaging infrastructure · sysMonitorData")
				.put("endpoint", "mqtts://mqtt-broker:8883").put("protocol", "MQTT 5").put("security", "Mutual X.509 · ACL");
		mqtt.set("runtime", runtime.apply("mqtt-broker"));

		final ArrayNode flows = Json.arr();
		producers.forEach(flows::add);
		flows.addObject().put("id", "sysmonitor-consume").put("source", "AMQP Broker").put("target", "SysMonitor")
				.put("label", "sysMonitor observation subscription · selected in sysMonitorManagement").put("protocol", "AMQPS · X.509")
				.put("active", isRunning.apply("amqp-broker") && isRunning.apply("sysmonitor"));
		flows.addObject().put("id", "sysmonitor-orchestration").put("source", "SysMonitor").put("target", "DynamicServiceOrchestration")
				.put("label", "Pull sysMonitor.observe · 30s").put("protocol", "HTTPS · token + mTLS")
				.put("active", isRunning.apply("sysmonitor") && isRunning.apply("serviceorchestration-dynamic") && !producers.isEmpty());
		flows.addObject().put("id", "aigym-sysmonitor-data").put("source", "MQTT Broker").put("target", "AIGym").put("label", "sysMonitorData.subscribe")
				.put("protocol", "DSO + MQTTS · CERT_AUTH")
				.put("active", isRunning.apply("aigym") && isRunning.apply("sysmonitor") && instances.contains("SysMonitor|sysMonitorData|1.0.0"));
		flows.addObject().put("id", "sysmonitor-history").put("source", "SysMonitor").put("target", "LocalCloudManager")
				.put("label", "sysMonitorHistory.query · stored in SysMonitor").put("protocol", "HTTPS · token + mTLS")
				.put("active", isRunning.apply("sysmonitor") && instances.contains("SysMonitor|sysMonitorHistory|1.0.0"));

		final ObjectNode out = Json.obj();
		out.set("infrastructure", infrastructure);
		out.set("orchestrations", flows);
		return out;
	}

	public static Double numericValue(final JsonNode entry) {
		for (final String field : List.of("v", "s")) {
			final JsonNode v = entry.path("value").get(field);
			if (v != null && v.isNumber()) {
				return v.asDouble();
			}
		}
		return null;
	}

	private static final List<String> SENML_VALUE_FIELDS = List.of("v", "vs", "vb", "vd", "s");
	private static final double SENML_RELATIVE_TIME_LIMIT = 268435456;

	/** Resolves the flat record list returned by dataHistorian get-data into the measurement entries used by the dashboard. */
	public static List<ObjectNode> resolveSenMLRecords(final List<JsonNode> records, final String systemName, final String serviceName) {
		final List<ObjectNode> entries = new ArrayList<>();
		String baseName = "";
		double baseTime = 0;
		JsonNode baseUnit = null;
		for (final JsonNode record : records) {
			if (record.hasNonNull("bn")) {
				baseName = record.get("bn").asText();
			}
			if (record.hasNonNull("bt")) {
				baseTime = record.get("bt").asDouble();
			}
			if (record.hasNonNull("bu")) {
				baseUnit = record.get("bu");
			}
			String field = null;
			for (final String f : SENML_VALUE_FIELDS) {
				if (record.hasNonNull(f)) {
					field = f;
					break;
				}
			}
			if (field == null) {
				continue;
			}
			final String name = baseName + (record.hasNonNull("n") ? record.get("n").asText() : "");
			final double offset = record.path("t").asDouble(0);
			final double time = Math.abs(offset) < SENML_RELATIVE_TIME_LIMIT ? baseTime + offset : offset;
			final ObjectNode e = Json.obj();
			e.put("id", systemName + "|" + serviceName + "|" + name + "|" + number(time));
			e.put("name", name);
			putNumber(e, "time", time);
			final JsonNode unit = record.hasNonNull("u") && !record.get("u").asText().isEmpty() ? record.get("u") : baseUnit;
			if (unit == null) {
				e.putNull("unit");
			} else {
				e.set("unit", unit);
			}
			e.putObject("value").set(field, record.get(field));
			e.set("record", record);
			e.put("system", systemName);
			e.put("service", serviceName);
			entries.add(e);
		}
		return entries;
	}

	static void putNumber(final ObjectNode node, final String field, final double value) {
		if (value == Math.rint(value) && Math.abs(value) < 9e15) {
			node.put(field, (long) value);
		} else {
			node.put(field, value);
		}
	}

	static String number(final double value) {
		return value == Math.rint(value) && Math.abs(value) < 9e15 ? Long.toString((long) value) : Double.toString(value);
	}

	private static final Pattern SOURCE_CLOUD = Pattern.compile("^urn:arrowhead:([A-Z][A-Za-z0-9]*):");

	/** A stream label: stored system/service and, for inter-cloud data, the source cloud of the SenML base name. */
	public static ObjectNode seriesLabel(final JsonNode entry) {
		final String bn = Json.or(Json.str(entry.path("record"), "bn"), Json.str(entry, "name"), "");
		final Matcher m = SOURCE_CLOUD.matcher(bn);
		final String sourceCloud = m.find() ? m.group(1) : null;
		final String n = Json.or(Json.str(entry.path("record"), "n"), Json.str(entry, "name"), "");
		final String measurement = n.substring(n.lastIndexOf(':') + 1);
		final ObjectNode out = Json.obj();
		out.set("system", entry.get("system"));
		out.set("service", entry.get("service"));
		if (sourceCloud == null) {
			out.putNull("sourceCloud");
		} else {
			out.put("sourceCloud", sourceCloud);
		}
		out.put("label", entry.path("system").asText() + " · " + measurement + " — " + (sourceCloud != null ? "from " + sourceCloud + " (inter-cloud)" : "local"));
		return out;
	}

	/** Docker multiplexed log stream frames to text. */
	public static String demuxDockerLogs(final byte[] buffer) {
		final StringBuilder lines = new StringBuilder();
		int offset = 0;
		boolean framed = false;
		while (offset + 8 <= buffer.length) {
			final int length = ((buffer[offset + 4] & 0xff) << 24) | ((buffer[offset + 5] & 0xff) << 16) | ((buffer[offset + 6] & 0xff) << 8) | (buffer[offset + 7] & 0xff);
			if (length < 0 || offset + 8 + length > buffer.length) {
				break;
			}
			lines.append(new String(buffer, offset + 8, length, StandardCharsets.UTF_8));
			framed = true;
			offset += 8 + length;
		}
		return (framed ? lines.toString() : new String(buffer, StandardCharsets.UTF_8)).replace("\u0000", "");
	}
}
