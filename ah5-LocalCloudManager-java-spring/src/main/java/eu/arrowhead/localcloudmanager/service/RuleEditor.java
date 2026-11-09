package eu.arrowhead.localcloudmanager.service;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Priority edits of SimpleStore orchestration rules (port of rule-editor-api.js). */
public final class RuleEditor {

	private RuleEditor() {
		throw new UnsupportedOperationException();
	}

	private static boolean sameNumber(final JsonNode a, final JsonNode b) {
		return a != null && b != null && a.asDouble(Double.NaN) == b.asDouble(Double.NaN);
	}

	public static long validatePriority(final JsonNode old, final JsonNode body) {
		final JsonNode p = body == null ? null : body.get("priority");
		final JsonNode e = body == null ? null : body.get("expected");
		if (p == null || !p.isNumber() || p.asDouble() != Math.rint(p.asDouble()) || p.asDouble() < 1 || p.asDouble() > 2147483647) {
			throw new ApiException(400, "Use a positive integer priority");
		}
		if (e == null || e.isNull()) {
			throw new ApiException(428, "Expected rule snapshot required");
		}
		if (old == null || !old.path("consumer").equals(e.path("consumer")) || !old.path("serviceInstanceId").equals(e.path("serviceInstanceId"))
				|| !sameNumber(old.get("priority"), e.get("priority"))) {
			throw new ApiException(409, "The rule changed; reload before saving");
		}
		return (long) p.asDouble();
	}

	private static JsonNode byId(final List<JsonNode> rows, final JsonNode id) {
		return rows.stream().filter(r -> r.path("id").equals(id)).findFirst().orElse(null);
	}

	private static boolean isCandidate(final JsonNode r, final List<JsonNode> before, final JsonNode old, final long priority) {
		return before.stream().noneMatch(b -> b.path("id").equals(r.path("id"))) && r.path("consumer").equals(old.path("consumer"))
				&& r.path("serviceInstanceId").equals(old.path("serviceInstanceId")) && r.path("priority").asDouble() == priority;
	}

	/** For deployments that authorize create/remove but not native priority management: create the new rule, then remove the old one. */
	public static JsonNode replaceWithExistingOperations(final Supplier<List<JsonNode>> query, final Function<JsonNode, JsonNode> create, final Consumer<String> remove,
			final JsonNode old, final long priority) {
		final List<JsonNode> before = query.get();
		final ObjectNode snapshot = Json.obj().put("priority", priority);
		snapshot.set("expected", old);
		validatePriority(byId(before, old.path("id")), snapshot);
		if (before.stream().anyMatch(r -> !r.path("id").equals(old.path("id")) && r.path("consumer").equals(old.path("consumer"))
				&& r.path("serviceInstanceId").equals(old.path("serviceInstanceId")) && r.path("priority").asDouble() == priority)) {
			throw new ApiException(409, "This connection and priority already exist");
		}
		final ObjectNode candidate = Json.obj();
		candidate.set("consumer", old.get("consumer"));
		candidate.set("serviceInstanceId", old.get("serviceInstanceId"));
		candidate.put("priority", priority);
		JsonNode created = null;
		boolean removeAttempted = false;
		try {
			final JsonNode receipt = create.apply(candidate);
			for (final JsonNode r : Json.list(receipt, "entries")) {
				if (created == null && isCandidate(r, before, old, priority)) {
					created = r;
				}
			}
			final List<JsonNode> current = query.get();
			for (final JsonNode r : current) {
				if (isCandidate(r, before, old, priority)) {
					created = r;
					break;
				}
			}
			if (created == null) {
				throw new ApiException(502, "New rule was not confirmed. Reload to inspect the result");
			}
			validatePriority(byId(current, old.path("id")), snapshot);
			removeAttempted = true;
			remove.accept(old.path("id").asText());
			final ObjectNode out = Json.obj();
			out.putArray("entries").add(created);
			out.put("count", 1);
			return out;
		} catch (final RuntimeException error) {
			if (created != null) {
				try {
					final List<JsonNode> current = query.get();
					if (removeAttempted && current.stream().noneMatch(r -> r.path("id").equals(old.path("id")) || r.path("consumer").equals(old.path("consumer"))
							&& r.path("serviceInstanceId").equals(old.path("serviceInstanceId")) && sameNumber(r.get("priority"), old.get("priority")))) {
						final ObjectNode restore = Json.obj();
						restore.set("consumer", old.get("consumer"));
						restore.set("serviceInstanceId", old.get("serviceInstanceId"));
						restore.put("priority", old.path("priority").asLong());
						create.apply(restore);
					}
					final JsonNode owned = byId(query.get(), created.path("id"));
					if (owned != null && owned.path("consumer").equals(candidate.path("consumer")) && owned.path("serviceInstanceId").equals(candidate.path("serviceInstanceId"))
							&& owned.path("priority").asDouble() == priority) {
						remove.accept(created.path("id").asText());
					}
				} catch (final RuntimeException recovery) {
					throw new ApiException(502, "Priority replacement could not finish recovery. Inspect stored rules before retrying");
				}
			}
			throw error;
		}
	}
}
