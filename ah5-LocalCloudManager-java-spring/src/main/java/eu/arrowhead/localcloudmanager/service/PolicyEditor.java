package eu.arrowhead.localcloudmanager.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Local management policies of ConsumerAuthorization (port of policies-api.js): validation, snapshots and safe replacement. */
public final class PolicyEditor {

	private static final String NAME = "^[A-Za-z][A-Za-z0-9_]{0,62}$";

	public interface Authority {
		ObjectNode inventory();

		List<JsonNode> query();

		JsonNode grant(JsonNode policy);

		void revoke(String instanceId);
	}

	private PolicyEditor() {
		throw new UnsupportedOperationException();
	}

	public static JsonNode stable(final JsonNode v) {
		if (v == null) {
			return null;
		}
		if (v.isArray()) {
			final ArrayNode a = Json.arr();
			v.forEach(x -> a.add(stable(x)));
			return a;
		}
		if (v.isObject()) {
			final Map<String, JsonNode> sorted = new TreeMap<>();
			v.fields().forEachRemaining(e -> sorted.put(e.getKey(), stable(e.getValue())));
			final ObjectNode o = Json.obj();
			sorted.forEach(o::set);
			return o;
		}
		return v;
	}

	public static ObjectNode clean(final JsonNode row) {
		final ObjectNode out = Json.obj();
		out.set("provider", row.get("provider"));
		out.set("targetType", row.get("targetType"));
		out.set("target", row.get("target"));
		out.put("cloud", "LOCAL");
		out.put("description", row.hasNonNull("description") && !row.get("description").asText().isEmpty() ? row.get("description").asText() : "");
		out.set("defaultPolicy", row.get("defaultPolicy"));
		out.set("scopedPolicies", row.hasNonNull("scopedPolicies") ? row.get("scopedPolicies") : Json.obj());
		removeMissing(out);
		return out;
	}

	private static void removeMissing(final ObjectNode out) {
		final List<String> nulls = new ArrayList<>();
		out.fields().forEachRemaining(e -> {
			if (e.getValue() == null) {
				nulls.add(e.getKey());
			}
		});
		nulls.forEach(out::remove);
	}

	public static boolean local(final JsonNode row) {
		final String cloud = Json.str(row, "cloud");
		final String level = Json.str(row, "level");
		return (!Json.truthy(cloud) || "LOCAL".equals(cloud)) && (!Json.truthy(level) || "MGMT".equals(level));
	}

	private static ObjectNode policy(final JsonNode p) {
		final String type = p == null ? null : Json.str(p, "policyType");
		final JsonNode list = p == null || p.get("policyList") == null || p.get("policyList").isNull() ? Json.arr() : p.get("policyList");
		boolean ok = type != null && List.of("ALL", "WHITELIST", "BLACKLIST").contains(type) && list.isArray() && list.size() <= 500;
		if (ok) {
			for (final JsonNode n : list) {
				ok &= n.isTextual() && n.asText().matches(NAME);
			}
		}
		if (!ok) {
			throw new ApiException(400, "Use ALL, WHITELIST or BLACKLIST with local system names");
		}
		final ObjectNode out = Json.obj().put("policyType", type);
		final ArrayNode names = out.putArray("policyList");
		if (!"ALL".equals(type)) {
			final LinkedHashSet<String> unique = new LinkedHashSet<>();
			list.forEach(n -> unique.add(n.asText()));
			unique.forEach(names::add);
		}
		return out;
	}

	public static ObjectNode validate(final JsonNode row, final List<JsonNode> systems, final List<JsonNode> services) {
		if (row == null || !row.isObject()) {
			throw new ApiException(400, "A policy is required");
		}
		final String cloud = Json.str(row, "cloud");
		if (Json.truthy(cloud) && !"LOCAL".equals(cloud)) {
			throw new ApiException(400, "Only local-cloud policies can be edited");
		}
		final String level = Json.str(row, "level");
		if (Json.truthy(level) && !"MGMT".equals(level)) {
			throw new ApiException(400, "Provider-owned policies are read-only in this editor");
		}
		final String provider = Json.str(row, "provider");
		if (systems.stream().noneMatch(s -> s.path("name").asText("").equals(provider))) {
			throw new ApiException(409, "Provider is not registered in this local cloud");
		}
		final String targetType = Json.str(row, "targetType");
		final String target = Json.str(row, "target");
		if (!List.of("SERVICE_DEF", "EVENT_TYPE").contains(String.valueOf(targetType)) || provider == null || !provider.matches(NAME) || target == null || !target.matches(NAME)) {
			throw new ApiException(400, "Invalid local policy target");
		}
		if ("SERVICE_DEF".equals(targetType) && services.stream().noneMatch(s -> provider.equals(Json.str(s.path("provider"), "name")) && target.equals(Json.str(s.path("serviceDefinition"), "name")))) {
			throw new ApiException(409, "Service is not registered for this local provider");
		}
		final JsonNode scoped = row.hasNonNull("scopedPolicies") ? row.get("scopedPolicies") : Json.obj();
		if (!scoped.isObject() || scoped.size() > 100) {
			throw new ApiException(400, "Invalid operation scopes");
		}
		for (final String k : Json.fieldNames(scoped)) {
			if (!k.matches("^[A-Za-z0-9_.:-]{1,100}$")) {
				throw new ApiException(400, "Invalid operation scope");
			}
		}
		final JsonNode description = row.get("description");
		if (description != null && !description.isNull() && !(description.isTextual() && description.asText().length() <= 1000) && !(description.isTextual())) {
			throw new ApiException(400, "Description must be at most 1000 characters");
		}
		if (description != null && description.isTextual() && description.asText().length() > 1000) {
			throw new ApiException(400, "Description must be at most 1000 characters");
		}
		final ObjectNode out = clean(row);
		out.set("defaultPolicy", policy(row.get("defaultPolicy")));
		final ObjectNode scopes = Json.obj();
		for (final String k : Json.fieldNames(scoped)) {
			scopes.set(k, policy(scoped.get(k)));
		}
		out.set("scopedPolicies", scopes);
		return out;
	}

	private static boolean same(final JsonNode a, final JsonNode b) {
		return stable(clean(a)).toString().equals(stable(clean(b)).toString());
	}

	public static JsonNode checkExpected(final List<JsonNode> rows, final JsonNode expected, final JsonNode policy) {
		final List<JsonNode> matches = rows.stream().filter(r -> r.path("provider").equals(policy.path("provider")) && r.path("targetType").equals(policy.path("targetType"))
				&& r.path("target").equals(policy.path("target"))).toList();
		if (matches.size() > 1) {
			throw new ApiException(409, "Multiple policies share this target; resolve them before editing");
		}
		final JsonNode old = matches.isEmpty() ? null : matches.get(0);
		final boolean hasExpected = expected != null && !expected.isNull();
		if (!hasExpected && old != null) {
			throw new ApiException(409, "Policy already exists; reload before editing");
		}
		if (hasExpected && (old == null || !old.path("instanceId").equals(expected.path("instanceId")) || !same(old, expected))) {
			throw new ApiException(409, "Policy changed; reload before saving");
		}
		return old;
	}

	public record Result(int status, JsonNode body) {
	}

	/** GET: editable local management policies and read-only local provider policies. */
	public static ObjectNode list(final Authority authority) {
		final List<JsonNode> entries = authority.query();
		final ObjectNode out = Json.obj();
		final ArrayNode editable = out.putArray("entries");
		final ArrayNode readOnly = out.putArray("readOnlyEntries");
		for (final JsonNode p : entries) {
			final String cloud = Json.str(p, "cloud");
			if (local(p)) {
				editable.add(p);
			} else if (!Json.truthy(cloud) || "LOCAL".equals(cloud)) {
				readOnly.add(p);
			}
		}
		out.put("collectedAt", Json.now());
		return out;
	}

	public static Result delete(final Authority authority, final String id, final JsonNode body) {
		final List<JsonNode> entries = authority.query().stream().filter(PolicyEditor::local).toList();
		final JsonNode row = entries.stream().filter(r -> id.equals(r.path("instanceId").asText(null))).findFirst().orElse(null);
		if (row == null) {
			throw new ApiException(404, "Local management policy not found");
		}
		final JsonNode expected = body == null ? null : body.get("expected");
		if (expected == null || expected.isNull()) {
			throw new ApiException(428, "Expected policy snapshot required");
		}
		if (!expected.path("instanceId").equals(row.path("instanceId")) || !same(row, expected)) {
			throw new ApiException(409, "Policy changed; reload before deleting");
		}
		authority.revoke(id);
		return new Result(200, Json.obj().put("removed", true).put("id", id));
	}

	public static Result save(final Authority authority, final JsonNode body) {
		final List<JsonNode> entries = authority.query().stream().filter(PolicyEditor::local).toList();
		final ObjectNode inventory = authority.inventory();
		final ObjectNode policy = validate(body == null ? null : body.get("policy"), Json.list(inventory.path("systems"), "entries"), Json.list(inventory.path("services"), "entries"));
		final JsonNode expected = body == null ? null : body.get("expected");
		final JsonNode old = checkExpected(entries, expected, policy);
		JsonNode result;
		try {
			result = authority.grant(policy);
		} catch (final ApiException error) {
			// Java's management API rejects an existing target instead of upserting it; use its revoke/grant protocol only for that error
			final String message = String.valueOf(error.getMessage());
			if (old == null || (error.status() != 400 && error.status() != 409) || !message.toLowerCase().matches("(?s).*(policy.*already exist|already exist.*policy).*")) {
				throw error;
			}
			checkExpected(authority.query().stream().filter(PolicyEditor::local).toList(), expected, policy);
			authority.revoke(old.path("instanceId").asText());
			try {
				result = authority.grant(policy);
			} catch (final RuntimeException replacement) {
				try {
					authority.grant(clean(old));
				} catch (final RuntimeException restore) {
					throw new ApiException(502, "Replacement failed and the old policy could not be restored. Reload and recover this policy before retrying");
				}
				throw new ApiException(502, "Replacement failed; the previous policy was restored");
			}
		}
		final JsonNode created = result != null && result.path("entries").isArray() ? result.path("entries").get(0) : result;
		if (created == null || !created.hasNonNull("instanceId") || created.get("instanceId").asText().isEmpty()) {
			throw new ApiException(502, "Authority did not return a policy identifier; reload to inspect the result");
		}
		if (old != null && !old.path("instanceId").equals(created.path("instanceId"))) {
			try {
				authority.revoke(old.path("instanceId").asText());
			} catch (final RuntimeException ex) {
				try {
					authority.revoke(created.path("instanceId").asText());
				} catch (final RuntimeException ignored) {
					// reported below
				}
				throw new ApiException(502, "Policy replacement could not finish; reload policies before retrying");
			}
		}
		return new Result(old != null ? 200 : 201, created);
	}
}
