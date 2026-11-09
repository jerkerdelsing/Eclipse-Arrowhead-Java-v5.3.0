package eu.arrowhead.localcloudmanager.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** The SoS composition model persisted through PlantDescriptionEngine (server-side port of ui/public/sos-model.js). */
public final class SosModel {

	private SosModel() {
		throw new UnsupportedOperationException();
	}

	private static JsonNode or(final JsonNode v, final JsonNode fallback) {
		return v == null || v.isNull() || v.isMissingNode() || (v.isTextual() && v.asText().isEmpty()) || (v.isBoolean() && !v.asBoolean())
				|| (v.isNumber() && v.asDouble() == 0) ? fallback : v;
	}

	private static String strOr(final JsonNode v, final String fallback) {
		final JsonNode r = or(v, null);
		return r == null ? fallback : r.asText();
	}

	/** model(d): the normalized persisted shape */
	public static ObjectNode model(final JsonNode d) {
		final JsonNode in = d == null || d.isNull() ? Json.obj() : d;
		final ObjectNode out = Json.obj();
		out.put("plantDescription", strOr(in.get("plantDescription"), ""));
		out.put("active", in.path("active").isBoolean() && in.path("active").asBoolean());
		out.set("include", or(in.get("include"), Json.arr()).deepCopy());
		final ArrayNode systems = out.putArray("systems");
		for (final JsonNode s : Json.list(or(in.get("systems"), Json.arr()))) {
			final ObjectNode system = systems.addObject();
			system.put("systemId", strOr(s.get("systemId"), ""));
			final JsonNode name = or(s.get("systemName"), null);
			if (name == null) {
				system.putNull("systemName");
			} else {
				system.set("systemName", name);
			}
			system.set("metadata", or(s.get("metadata"), Json.obj()).deepCopy());
			final ArrayNode ports = system.putArray("ports");
			for (final JsonNode p : Json.list(or(s.get("ports"), Json.arr()))) {
				final ObjectNode port = ports.addObject();
				port.put("portName", strOr(p.get("portName"), ""));
				port.put("serviceDefinition", strOr(p.get("serviceDefinition"), ""));
				final JsonNode intf = or(p.get("serviceInterface"), null);
				if (intf == null) {
					port.putNull("serviceInterface");
				} else {
					port.set("serviceInterface", intf);
				}
				port.put("consumer", p.path("consumer").isBoolean() && p.path("consumer").asBoolean());
				port.set("metadata", or(p.get("metadata"), Json.obj()).deepCopy());
			}
		}
		final ArrayNode connections = out.putArray("connections");
		for (final JsonNode c : Json.list(or(in.get("connections"), Json.arr()))) {
			final ObjectNode connection = connections.addObject();
			connection.set("consumer", c.has("consumer") ? c.get("consumer").deepCopy() : null);
			connection.set("producer", c.has("producer") ? c.get("producer").deepCopy() : null);
			final JsonNode priority = c.get("priority");
			if (priority == null || priority.isNull()) {
				connection.putNull("priority");
			} else {
				connection.set("priority", priority);
			}
		}
		return out;
	}

	/** canonical(v): JSON with sorted object keys */
	public static String canonical(final JsonNode v) {
		if (v == null || v.isMissingNode()) {
			return "undefined";
		}
		if (v.isArray()) {
			final List<String> parts = new ArrayList<>();
			v.forEach(e -> parts.add(canonical(e)));
			return "[" + String.join(",", parts) + "]";
		}
		if (v.isObject()) {
			final Map<String, JsonNode> sorted = new TreeMap<>();
			v.fields().forEachRemaining(e -> sorted.put(e.getKey(), e.getValue()));
			final List<String> parts = new ArrayList<>();
			sorted.forEach((k, x) -> parts.add(Json.MAPPER.valueToTree(k).toString() + ":" + canonical(x)));
			return "{" + String.join(",", parts) + "}";
		}
		return v.toString();
	}

	private record Expanded(List<JsonNode> systems, List<JsonNode> connections, List<String> errors) {
	}

	private static Expanded expand(final JsonNode m, final List<JsonNode> entries) {
		final List<JsonNode> systems = new ArrayList<>(Json.list(m, "systems"));
		final List<JsonNode> connections = new ArrayList<>(Json.list(m, "connections"));
		final List<String> errors = new ArrayList<>();
		final Set<Long> visited = new LinkedHashSet<>();
		for (final JsonNode id : Json.list(m, "include")) {
			walk(id.asLong(), new ArrayList<>(), entries, systems, connections, errors, visited);
		}
		return new Expanded(systems, connections, errors);
	}

	private static void walk(final long id, final List<Long> path, final List<JsonNode> entries, final List<JsonNode> systems, final List<JsonNode> connections,
			final List<String> errors, final Set<Long> visited) {
		if (path.contains(id)) {
			errors.add("Included SoS configurations contain a cycle.");
			return;
		}
		if (!visited.add(id)) {
			return;
		}
		JsonNode d = null;
		for (final JsonNode e : entries) {
			if (e.path("id").asLong(Long.MIN_VALUE) == id) {
				d = e;
				break;
			}
		}
		if (d == null) {
			errors.add("Included SoS #" + id + " is unavailable.");
			return;
		}
		systems.addAll(Json.list(d, "systems"));
		connections.addAll(Json.list(d, "connections"));
		for (final JsonNode next : Json.list(d, "include")) {
			final List<Long> p = new ArrayList<>(path);
			p.add(id);
			walk(next.asLong(), p, entries, systems, connections, errors, visited);
		}
	}

	private static boolean metadata(final JsonNode v) {
		if (v == null || !v.isObject() || v.size() > 100) {
			return false;
		}
		for (final String k : Json.fieldNames(v)) {
			final JsonNode x = v.get(k);
			if (k.isEmpty() || k.length() > 200 || !x.isTextual() || x.asText().length() > 2000) {
				return false;
			}
		}
		return true;
	}

	private static boolean safePositiveInteger(final JsonNode id) {
		return id.isIntegralNumber() && id.canConvertToLong() && id.asLong() >= 1 && id.asLong() <= 9007199254740991L
				|| id.isFloatingPointNumber() && id.asDouble() == Math.rint(id.asDouble()) && id.asDouble() >= 1 && id.asDouble() <= 9007199254740991.0;
	}

	/** validate(raw, entries): the error messages (empty when valid) */
	public static List<String> validate(final JsonNode raw, final List<JsonNode> entries) {
		final List<String> e = new ArrayList<>();
		if (raw == null || !raw.isObject()) {
			return List.of("Configuration must be an object.");
		}
		final JsonNode m = raw;
		final JsonNode name = m.get("plantDescription");
		if (name == null || !name.isTextual() || name.asText().trim().isEmpty() || name.asText().length() > 200) {
			e.add("Give the SoS a name of 1–200 characters.");
		}
		if (!m.path("active").isBoolean()) {
			e.add("Active must be boolean.");
		}
		final JsonNode include = m.get("include");
		boolean includeOk = include != null && include.isArray() && include.size() <= 200;
		if (includeOk) {
			final Set<Long> unique = new LinkedHashSet<>();
			for (final JsonNode id : include) {
				includeOk &= safePositiveInteger(id) && unique.add(id.asLong());
			}
		}
		if (!includeOk) {
			e.add("Included SoS IDs must be unique positive integers.");
		}
		if (!m.path("systems").isArray() || m.path("systems").size() > 200 || !m.path("connections").isArray() || m.path("connections").size() > 1000) {
			e.add("Use at most 200 systems and 1000 connections.");
			return e;
		}
		if (!e.isEmpty()) {
			return e;
		}

		final Expanded all = expand(m, entries);
		e.addAll(all.errors());
		final Set<String> ids = new LinkedHashSet<>();
		final Set<String> identities = new LinkedHashSet<>();
		for (final JsonNode s : all.systems()) {
			if (s == null || !s.isObject()) {
				e.add("Invalid system.");
				continue;
			}
			final JsonNode systemId = s.get("systemId");
			final String sid = systemId != null && systemId.isTextual() ? systemId.asText() : null;
			if (sid == null || sid.trim().isEmpty() || sid.length() > 100 || "unknown".equals(sid.toLowerCase()) || ids.contains(sid)) {
				e.add("System identifiers must be unique, nonempty and not unknown.");
			}
			ids.add(sid);
			final JsonNode sn = s.get("systemName");
			if (sn != null && !sn.isNull() && !(sn.isTextual() && sn.asText().isEmpty()) && (!sn.isTextual() || !sn.asText().matches("^[A-Za-z][A-Za-z0-9]*$"))) {
				e.add("Use an Arrowhead system name.");
			}
			final JsonNode md = or(s.get("metadata"), Json.obj());
			if (!metadata(md)) {
				e.add("System metadata must contain string values.");
			}
			final boolean hasName = sn != null && sn.isTextual() && !sn.asText().isEmpty() || sn != null && !sn.isNull() && !sn.isTextual() && !(sn.isBoolean() && !sn.asBoolean());
			if (!hasName && Json.fieldNames(md).isEmpty()) {
				e.add("A system needs a name or matching metadata.");
			}
			final ArrayNode identity = Json.arr();
			identity.add(hasName ? sn : Json.MAPPER.valueToTree(""));
			identity.add(md);
			if (!identities.add(canonical(identity))) {
				e.add("Two systems have indistinguishable name and metadata.");
			}
			final JsonNode ports = s.get("ports");
			if (ports == null || !ports.isArray() || ports.size() > 200) {
				e.add("System ports must be an array of at most 200 entries.");
				continue;
			}
			final Set<String> names = new LinkedHashSet<>();
			for (final JsonNode p : ports) {
				if (p == null || !p.isObject()) {
					e.add("Invalid port.");
					continue;
				}
				final JsonNode pn = p.get("portName");
				if (pn == null || !pn.isTextual() || pn.asText().trim().isEmpty() || pn.asText().length() > 100 || names.contains(pn.asText())) {
					e.add("Port names must be unique within a system.");
				}
				names.add(pn == null ? null : pn.asText());
				final JsonNode sd = p.get("serviceDefinition");
				if (sd == null || !sd.isTextual() || !sd.asText().matches("^[a-z][A-Za-z0-9]*$")) {
					e.add("Service definitions must use camelCase.");
				}
				if (!p.path("consumer").isBoolean()) {
					e.add("Choose required or provided for each port.");
				}
				final JsonNode si = p.get("serviceInterface");
				if (si != null && !si.isNull() && (!si.isTextual() || si.asText().length() > 200)) {
					e.add("Invalid interface template.");
				}
				final JsonNode pm = or(p.get("metadata"), Json.obj());
				if (!metadata(pm) || (p.path("consumer").asBoolean(false) && !Json.fieldNames(pm).isEmpty())) {
					e.add("Consumer ports cannot have matching metadata; metadata values must be strings.");
				}
			}
		}
		if (!e.isEmpty()) {
			return new ArrayList<>(new LinkedHashSet<>(e));
		}

		final Set<String> seen = new LinkedHashSet<>();
		for (final JsonNode c : all.connections()) {
			final JsonNode a = find(all.systems(), c == null ? null : c.get("consumer"));
			final JsonNode b = find(all.systems(), c == null ? null : c.get("producer"));
			if (a == null || b == null) {
				e.add("A connection references a missing system or port.");
				continue;
			}
			if (!a.path("consumer").asBoolean() || b.path("consumer").asBoolean()) {
				e.add("Connect a required port to a provided port.");
			}
			final String ai = Json.or(Json.str(a, "serviceInterface"));
			final String bi = Json.or(Json.str(b, "serviceInterface"));
			if (!a.path("serviceDefinition").asText().equals(b.path("serviceDefinition").asText()) || (ai != null && bi != null && !ai.equals(bi))) {
				e.add("Connected ports must share a service and compatible interface template.");
			}
			final JsonNode priority = c.get("priority");
			if (priority != null && !priority.isNull() && (!priority.isNumber() || priority.asDouble() != Math.rint(priority.asDouble()) || priority.asDouble() < 0 || priority.asDouble() > 2147483647)) {
				e.add("Priority must be a nonnegative integer.");
			}
			final ArrayNode key = Json.arr();
			key.add(c.get("consumer"));
			key.add(c.get("producer"));
			if (!seen.add(canonical(key))) {
				e.add("Duplicate connection.");
			}
		}
		return new ArrayList<>(new LinkedHashSet<>(e));
	}

	private static JsonNode find(final List<JsonNode> systems, final JsonNode ref) {
		if (ref == null || !ref.isObject()) {
			return null;
		}
		for (final JsonNode s : systems) {
			if (s != null && s.isObject() && s.path("systemId").equals(ref.path("systemId")) && !ref.path("systemId").isMissingNode()) {
				for (final JsonNode p : Json.list(s, "ports")) {
					if (p != null && p.isObject() && p.path("portName").equals(ref.path("portName")) && !ref.path("portName").isMissingNode()) {
						return p;
					}
				}
				return null;
			}
		}
		return null;
	}
}
