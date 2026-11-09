package eu.arrowhead.localcloudmanager.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** JSON helpers for the JavaScript-shaped responses of the native console API. */
public final class Json {

	public static final ObjectMapper MAPPER = new ObjectMapper();
	private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

	private Json() {
		throw new UnsupportedOperationException();
	}

	public static ObjectNode obj() {
		return JsonNodeFactory.instance.objectNode();
	}

	public static ArrayNode arr() {
		return JsonNodeFactory.instance.arrayNode();
	}

	/** Date.prototype.toISOString */
	public static String iso(final Instant instant) {
		return ISO.format(instant);
	}

	public static String now() {
		return iso(Instant.now());
	}

	/** A string field, or null when absent, null or not textual. */
	public static String str(final JsonNode node, final String field) {
		final JsonNode v = node == null ? null : node.get(field);
		return v != null && v.isTextual() ? v.asText() : null;
	}

	/** JavaScript truthiness of a string value. */
	public static boolean truthy(final String s) {
		return s != null && !s.isEmpty();
	}

	public static String or(final String... values) {
		for (final String v : values) {
			if (truthy(v)) {
				return v;
			}
		}
		return null;
	}

	/** The elements of an array field (empty when absent). */
	public static List<JsonNode> list(final JsonNode node, final String field) {
		final JsonNode v = node == null ? null : node.get(field);
		final List<JsonNode> out = new ArrayList<>();
		if (v != null && v.isArray()) {
			v.forEach(out::add);
		}
		return out;
	}

	public static List<JsonNode> list(final JsonNode array) {
		final List<JsonNode> out = new ArrayList<>();
		if (array != null && array.isArray()) {
			array.forEach(out::add);
		}
		return out;
	}

	public static List<String> fieldNames(final JsonNode node) {
		final List<String> out = new ArrayList<>();
		if (node != null && node.isObject()) {
			for (final Iterator<String> it = node.fieldNames(); it.hasNext();) {
				out.add(it.next());
			}
		}
		return out;
	}

	/** instanceId "provider|service|version" part i, or null */
	public static String part(final String instanceId, final int i) {
		if (instanceId == null) {
			return null;
		}
		final String[] p = instanceId.split("\\|", -1);
		return i < p.length ? p[i] : null;
	}

	public static ArrayNode strings(final Iterable<String> values) {
		final ArrayNode a = arr();
		values.forEach(a::add);
		return a;
	}
}
