package eu.arrowhead.localcloudmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// Ports of the native policies.test.js, rule-priority.test.js and the SoS model checks of sos-api.test.js.
public class EditorsTest {

	private static JsonNode j(final String json) {
		try {
			return Json.MAPPER.readTree(json.replace('\'', '"'));
		} catch (final Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private static final String POLICY = "{'provider':'Provider','targetType':'SERVICE_DEF','target':'temperature','cloud':'LOCAL','description':'editor test',"
			+ "'defaultPolicy':{'policyType':'WHITELIST','policyList':['Consumer']},'scopedPolicies':{'read':{'policyType':'BLACKLIST','policyList':['Other']}}}";
	private static final List<JsonNode> SYSTEMS = Json.list(j("[{'name':'Provider'},{'name':'Consumer'}]"));
	private static final List<JsonNode> SERVICES = Json.list(j("[{'provider':{'name':'Provider'},'serviceDefinition':{'name':'temperature'}}]"));

	private static class FakeAuthority implements PolicyEditor.Authority {
		private final List<JsonNode> rows = new ArrayList<>();
		private final List<String> writes = new ArrayList<>();
		private final boolean legacy;
		private int seq;

		FakeAuthority(final boolean legacy, final JsonNode... initial) {
			this.legacy = legacy;
			for (final JsonNode r : initial) {
				rows.add(r);
			}
		}

		@Override
		public ObjectNode inventory() {
			final ObjectNode out = Json.obj();
			out.putObject("systems").set("entries", Json.MAPPER.valueToTree(SYSTEMS));
			out.putObject("services").set("entries", Json.MAPPER.valueToTree(SERVICES));
			return out;
		}

		@Override
		public List<JsonNode> query() {
			return rows.stream().map(JsonNode::deepCopy).map(n -> (JsonNode) n).toList();
		}

		@Override
		public JsonNode grant(final JsonNode policy) {
			writes.add("grant");
			final ObjectNode row = policy.deepCopy();
			row.put("instanceId", legacy ? "new-" + (++seq) : "MGMT|LOCAL|Provider|SERVICE_DEF|temperature").put("level", "MGMT");
			rows.removeIf(r -> r.path("instanceId").equals(row.path("instanceId")));
			rows.add(row);
			final ObjectNode out = Json.obj();
			out.putArray("entries").add(row);
			return out;
		}

		@Override
		public void revoke(final String instanceId) {
			writes.add("revoke");
			rows.removeIf(r -> r.path("instanceId").asText().equals(instanceId));
		}
	}

	private static ObjectNode withId(final String json, final String id, final String level) {
		return ((ObjectNode) j(json)).put("instanceId", id).put("level", level);
	}

	@Test
	public void localTargetAndScopedPolicyValidationAcceptsTypedLocalLists() {
		assertEquals(PolicyEditor.stable(j(POLICY)).toString(), PolicyEditor.stable(PolicyEditor.validate(j(POLICY), SYSTEMS, SERVICES)).toString());
	}

	@Test
	public void remoteProviderOwnedForeignAndMalformedPoliciesAreRejected() {
		for (final String[] change : new String[][] { { "cloud", "'Other|Company'" }, { "level", "'PROVIDER'" }, { "provider", "'Foreign'" }, { "target", "'unknown'" } }) {
			final ObjectNode p = (ObjectNode) j(POLICY);
			p.set(change[0], j(change[1]));
			assertThrows(ApiException.class, () -> PolicyEditor.validate(p, SYSTEMS, SERVICES));
		}
		final ObjectNode foreignList = (ObjectNode) j(POLICY);
		foreignList.set("defaultPolicy", j("{'policyType':'WHITELIST','policyList':['Consumer@Other']}"));
		assertThrows(ApiException.class, () -> PolicyEditor.validate(foreignList, SYSTEMS, SERVICES));
		final ObjectNode badScope = (ObjectNode) j(POLICY);
		badScope.set("scopedPolicies", j("{'bad/scope':{'policyType':'ALL'}}"));
		assertThrows(ApiException.class, () -> PolicyEditor.validate(badScope, SYSTEMS, SERVICES));
	}

	@Test
	public void listSeparatesEditableLocalManagementAndReadOnlyProviderPolicies() {
		final FakeAuthority f = new FakeAuthority(false, withId(POLICY, "1", "MGMT"), withId(POLICY, "2", "PROVIDER"),
				((ObjectNode) withId(POLICY, "3", "MGMT")).put("cloud", "Other|Company"));
		final ObjectNode out = PolicyEditor.list(f);
		assertEquals(1, out.path("entries").size());
		assertEquals(1, out.path("readOnlyEntries").size());
	}

	@Test
	public void newPolicyRequiresAbsenceAndWritesOnlyValidatedFields() {
		final FakeAuthority f = new FakeAuthority(false);
		final ObjectNode body = Json.obj();
		body.set("policy", ((ObjectNode) j(POLICY)).put("arbitraryURL", "http://invalid"));
		body.putNull("expected");
		final PolicyEditor.Result r = PolicyEditor.save(f, body);
		assertEquals(201, r.status());
		assertFalse(f.rows.get(0).has("arbitraryURL"));
	}

	@Test
	public void existingOrStalePolicyIsNeverSilentlyOverwritten() {
		final ObjectNode old = withId(POLICY, "id", "MGMT");
		final FakeAuthority f = new FakeAuthority(false, old);
		final ObjectNode noExpected = Json.obj();
		noExpected.set("policy", j(POLICY));
		assertEquals(409, assertThrows(ApiException.class, () -> PolicyEditor.save(f, noExpected)).status());
		final ObjectNode stale = Json.obj();
		stale.set("policy", j(POLICY));
		stale.set("expected", old.deepCopy().put("description", "stale"));
		assertEquals(409, assertThrows(ApiException.class, () -> PolicyEditor.save(f, stale)).status());
		assertTrue(f.writes.isEmpty());
	}

	@Test
	public void sameIdentifierUpsertsDoNotRevokeAndLegacyReplacementCreatesFirst() {
		final FakeAuthority f = new FakeAuthority(false);
		final ObjectNode create = Json.obj();
		create.set("policy", j(POLICY));
		final JsonNode saved = PolicyEditor.save(f, create).body();
		final ObjectNode edit = Json.obj();
		edit.set("policy", ((ObjectNode) j(POLICY)).put("description", "edited"));
		edit.set("expected", saved);
		assertEquals(200, PolicyEditor.save(f, edit).status());
		assertFalse(f.writes.contains("revoke"));
		assertEquals("edited", f.rows.get(0).path("description").asText());

		final ObjectNode old = withId(POLICY, "old", "MGMT");
		final FakeAuthority legacy = new FakeAuthority(true, old);
		final ObjectNode replace = Json.obj();
		replace.set("policy", ((ObjectNode) j(POLICY)).put("description", "edited"));
		replace.set("expected", old);
		PolicyEditor.save(legacy, replace);
		assertEquals(List.of("grant", "revoke"), legacy.writes);
		assertEquals(1, legacy.rows.size());
	}

	@Test
	public void deleteRequiresMatchingSnapshotAndCannotDeleteProviderOwnedPolicy() {
		final ObjectNode old = withId(POLICY, "old", "MGMT");
		final FakeAuthority f = new FakeAuthority(false, old);
		assertEquals(428, assertThrows(ApiException.class, () -> PolicyEditor.delete(f, "old", Json.obj())).status());
		final ObjectNode body = Json.obj();
		body.set("expected", old);
		PolicyEditor.delete(f, "old", body);
		assertTrue(f.rows.isEmpty());
		final FakeAuthority owned = new FakeAuthority(false, withId(POLICY, "old", "PROVIDER"));
		assertEquals(404, assertThrows(ApiException.class, () -> PolicyEditor.delete(owned, "old", body)).status());
	}

	@Test
	public void ambiguousDuplicateTargetsAreRefused() {
		assertEquals(409, assertThrows(ApiException.class,
				() -> PolicyEditor.checkExpected(List.of(withId(POLICY, "1", "MGMT"), withId(POLICY, "2", "MGMT")), withId(POLICY, "1", "MGMT"), j(POLICY))).status());
	}

	@Test
	public void duplicateRejectingAuthorityReplacesAndRestoresAFailedReplacement() {
		for (final boolean fail : new boolean[] { false, true }) {
			final List<String> calls = new ArrayList<>();
			final AtomicInteger grants = new AtomicInteger();
			final ObjectNode[] row = { withId(POLICY, "stable", "MGMT") };
			final PolicyEditor.Authority authority = new FakeAuthority(false) {
				@Override
				public List<JsonNode> query() {
					return row[0] == null ? List.of() : List.of(row[0].deepCopy());
				}

				@Override
				public JsonNode grant(final JsonNode p) {
					calls.add("grant");
					grants.incrementAndGet();
					if (row[0] != null) {
						throw new ApiException(400, "A management level policy is already existing");
					}
					if (fail && grants.get() == 2) {
						throw new IllegalStateException("injected failure");
					}
					row[0] = ((ObjectNode) p.deepCopy()).put("instanceId", "stable").put("level", "MGMT");
					final ObjectNode out = Json.obj();
					out.putArray("entries").add(row[0]);
					return out;
				}

				@Override
				public void revoke(final String id) {
					calls.add("revoke");
					row[0] = null;
				}
			};
			final ObjectNode body = Json.obj();
			body.set("policy", ((ObjectNode) j(POLICY)).put("description", "changed"));
			body.set("expected", row[0].deepCopy());
			if (fail) {
				assertTrue(assertThrows(ApiException.class, () -> PolicyEditor.save(authority, body)).getMessage().contains("previous policy was restored"));
				assertEquals("editor test", row[0].path("description").asText());
			} else {
				PolicyEditor.save(authority, body);
				assertEquals("changed", row[0].path("description").asText());
			}
			assertEquals("grant", calls.get(0));
			assertEquals("revoke", calls.get(1));
		}
	}

	// ----------------------------------------------------------------------------------------------- rule priorities

	private static final JsonNode RULE = j("{'id':'123e4567-e89b-12d3-a456-426614174000','consumer':'Consumer','serviceInstanceId':'Provider|temperature|1.0.0','priority':1}");

	private static ObjectNode priority(final Object p, final JsonNode expected) {
		final ObjectNode body = Json.obj();
		body.set("priority", Json.MAPPER.valueToTree(p));
		if (expected != null) {
			body.set("expected", expected);
		}
		return body;
	}

	@Test
	public void priorityEditsRequireACurrentSnapshotAndAPositiveBoundedInteger() {
		assertEquals(2, RuleEditor.validatePriority(RULE, priority(2, RULE)));
		for (final Object p : new Object[] { 0, -1, 1.5, 2147483648L }) {
			assertEquals(400, assertThrows(ApiException.class, () -> RuleEditor.validatePriority(RULE, priority(p, RULE))).status());
		}
		assertEquals(428, assertThrows(ApiException.class, () -> RuleEditor.validatePriority(RULE, priority(2, null))).status());
		assertEquals(409, assertThrows(ApiException.class, () -> RuleEditor.validatePriority(RULE, priority(2, ((ObjectNode) RULE.deepCopy()).put("priority", 3)))).status());
	}

	@Test
	public void existingCreateRemoveWorkflowChangesPriorityAndAFailedCreationKeepsTheRule() {
		final List<JsonNode> rows = new ArrayList<>(List.of(RULE.deepCopy()));
		final List<String> calls = new ArrayList<>();
		final JsonNode result = RuleEditor.replaceWithExistingOperations(() -> rows.stream().map(JsonNode::deepCopy).map(n -> (JsonNode) n).toList(), c -> {
			calls.add("create");
			final ObjectNode r = ((ObjectNode) c.deepCopy()).put("id", "new");
			rows.add(r);
			final ObjectNode out = Json.obj();
			out.putArray("entries").add(r);
			return out;
		}, id -> {
			calls.add("remove");
			rows.removeIf(r -> r.path("id").asText().equals(id));
		}, RULE, 4);
		assertEquals(List.of("create", "remove"), calls);
		assertEquals(1, rows.size());
		assertEquals(4, result.path("entries").get(0).path("priority").asInt());

		final boolean[] removed = { false };
		assertThrows(RuntimeException.class, () -> RuleEditor.replaceWithExistingOperations(() -> List.of(RULE), c -> {
			throw new IllegalStateException("rejected");
		}, id -> removed[0] = true, RULE, 4));
		assertFalse(removed[0]);
	}

	// ----------------------------------------------------------------------------------------------- SoS model

	private static final String SOS = "{'plantDescription':'test','active':false,'include':[],'systems':[],'connections':[]}";

	@Test
	public void sosModelValidatesAndNormalizes() {
		assertEquals(List.of(), SosModel.validate(j(SOS), List.of()));
		final ObjectNode broken = (ObjectNode) j(SOS);
		broken.set("connections", j("[{'consumer':{},'producer':{}}]"));
		assertEquals(List.of("A connection references a missing system or port."), SosModel.validate(broken, List.of()));
		assertEquals(List.of("Configuration must be an object."), SosModel.validate(j("[]"), List.of()));
		final ObjectNode cyclic = ((ObjectNode) j(SOS));
		cyclic.set("include", j("[2]"));
		assertTrue(SosModel.validate(cyclic, Json.list(j("[{'id':2,'include':[3]},{'id':3,'include':[2]}]"))).contains("Included SoS configurations contain a cycle."));
		final String valid = "{'plantDescription':'p','active':true,'include':[],'systems':[{'systemId':'a','systemName':'Consumer','ports':[{'portName':'in','serviceDefinition':'temperature','consumer':true}]},"
				+ "{'systemId':'b','systemName':'Producer','ports':[{'portName':'out','serviceDefinition':'temperature','consumer':false}]}],"
				+ "'connections':[{'consumer':{'systemId':'a','portName':'in'},'producer':{'systemId':'b','portName':'out'},'priority':1}]}";
		assertEquals(List.of(), SosModel.validate(j(valid), List.of()));
		final ObjectNode model = SosModel.model(j(valid));
		assertEquals("{}", model.path("systems").get(0).path("metadata").toString());
		assertTrue(model.path("systems").get(0).path("ports").get(0).path("serviceInterface").isNull());
		assertEquals(SosModel.canonical(SosModel.model(j(valid))), SosModel.canonical(model));
	}
}
