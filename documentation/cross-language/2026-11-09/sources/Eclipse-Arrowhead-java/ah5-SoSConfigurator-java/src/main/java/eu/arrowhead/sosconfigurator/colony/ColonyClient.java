package eu.arrowhead.sosconfigurator.colony;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.arrowhead.sosconfigurator.Json;

/**
 * Minimal ColonyOS (colonies v1.9.10) RPC client: signed messages posted to {@code /api} of the Colonies server. It
 * covers what the configurator needs: workflows, processes, logs, assignment, colony and executor registration.
 */
public class ColonyClient {

	public static final int WAITING = 0, RUNNING = 1, SUCCESS = 2, FAILED = 3;

	/** Server location and the identity of this process. */
	public record Config(String host, int port, boolean tls, String colonyName, String prvKey) {

		/** Reads COLONIES_SERVER_HOST/PORT, COLONIES_TLS, COLONIES_COLONY_NAME and COLONIES_PRVKEY(_FILE). */
		public static Config fromEnv() throws IOException {
			String prv = System.getenv("COLONIES_PRVKEY");
			final String file = System.getenv("COLONIES_PRVKEY_FILE");
			if (file != null && !file.isBlank()) {
				prv = Files.readString(Path.of(file)).trim();
			}
			final String colony = System.getenv("COLONIES_COLONY_NAME");
			if (colony == null || colony.isBlank() || prv == null || prv.isBlank()) {
				throw new IllegalStateException("COLONIES_COLONY_NAME and COLONIES_PRVKEY(_FILE) are required");
			}
			return new Config(env("COLONIES_SERVER_HOST", "localhost"), Integer.parseInt(env("COLONIES_SERVER_PORT", "50080")),
					"true".equals(env("COLONIES_TLS", "false")), colony, prv);
		}

		private static String env(final String k, final String d) {
			final String v = System.getenv(k);
			return v == null || v.isBlank() ? d : v;
		}
	}

	/** An error answered by the Colonies server. */
	public static class ColonyException extends IOException {
		private static final long serialVersionUID = 1L;
		private final int status;

		public ColonyException(final int status, final String message) {
			super(message);
			this.status = status;
		}

		public int status() {
			return status;
		}
	}

	public record Process(String id, int state, String nodeName, Map<String, Object> kwargs, String funcName, List<Object> output,
			List<String> errors) {
	}

	public record StepState(String node, String processId, String kind, String target, String state, List<String> errors, String output) {
	}

	public record Deployment(String id, String state, List<StepState> steps) {
	}

	private final Config cfg;
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	public ColonyClient(final Config cfg) {
		this.cfg = cfg;
	}

	public Config config() {
		return cfg;
	}

	public static String stateName(final int s) {
		return switch (s) {
		case WAITING -> "waiting";
		case RUNNING -> "running";
		case SUCCESS -> "success";
		case FAILED -> "failed";
		default -> "unknown";
		};
	}

	// ------------------------------------------------------------------------------------------------- transport

	JsonNode send(final String type, final ObjectNode msg, final String prvKey, final Duration timeout) throws IOException {
		msg.put("msgtype", type);
		final String payload = Base64.getEncoder().encodeToString(Json.write(msg).getBytes(StandardCharsets.UTF_8));
		final ObjectNode rpc = Json.MAPPER.createObjectNode();
		rpc.put("signature", ColonyCrypto.sign(payload, prvKey));
		rpc.put("payloadtype", type);
		rpc.put("payload", payload);
		final HttpRequest req = HttpRequest.newBuilder(URI.create((cfg.tls() ? "https" : "http") + "://" + cfg.host() + ":" + cfg.port() + "/api"))
				.timeout(timeout).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(Json.write(rpc))).build();
		final HttpResponse<String> resp;
		try {
			resp = http.send(req, HttpResponse.BodyHandlers.ofString());
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("interrupted", e);
		}
		final JsonNode reply;
		try {
			reply = Json.LENIENT.readTree(resp.body());
		} catch (final IOException e) {
			throw new IOException("expected a Colonies RPC reply, got HTTP " + resp.statusCode());
		}
		final String body = new String(Base64.getDecoder().decode(reply.path("payload").asText("")), StandardCharsets.UTF_8);
		if (reply.path("error").asBoolean(false)) {
			final JsonNode f = Json.LENIENT.readTree(body.isEmpty() ? "{}" : body);
			throw new ColonyException(f.path("status").asInt(resp.statusCode()), f.path("message").asText(body));
		}
		return body.isEmpty() ? Json.LENIENT.nullNode() : Json.LENIENT.readTree(body);
	}

	private JsonNode send(final String type, final ObjectNode msg) throws IOException {
		return send(type, msg, cfg.prvKey(), Duration.ofSeconds(30));
	}

	private static ObjectNode obj() {
		return Json.MAPPER.createObjectNode();
	}

	// ------------------------------------------------------------------------------------------------- functions

	/** A function spec for executors of one type; field names follow core.FunctionSpec. */
	public ObjectNode functionSpec(final String funcName, final String executorType, final Map<String, Object> kwargs, final int maxWait,
			final int maxExec, final String label) {
		final ObjectNode fs = obj();
		fs.put("nodename", "");
		fs.put("funcname", funcName);
		fs.putArray("args");
		fs.set("kwargs", Json.MAPPER.valueToTree(kwargs));
		fs.put("priority", 0);
		fs.put("maxwaittime", maxWait);
		fs.put("maxexectime", maxExec);
		fs.put("maxretries", 0);
		final ObjectNode c = fs.putObject("conditions");
		c.put("colonyname", cfg.colonyName());
		c.putArray("executornames");
		c.put("executortype", executorType);
		c.putArray("dependencies");
		fs.put("label", label);
		fs.putObject("env");
		return fs;
	}

	/** Submits a chain of function specs (each depends on the previous) as one workflow; returns the graph id. */
	public String submitChain(final List<ObjectNode> specs, final List<String> nodeNames) throws IOException {
		final ObjectNode wf = obj();
		wf.put("colonyname", cfg.colonyName());
		final ArrayNode arr = wf.putArray("functionspecs");
		String prev = null;
		for (int i = 0; i < specs.size(); i++) {
			final ObjectNode fs = specs.get(i);
			fs.put("nodename", nodeNames.get(i));
			if (prev != null) {
				((ObjectNode) fs.get("conditions")).withArray("dependencies").add(prev);
			}
			prev = nodeNames.get(i);
			arr.add(fs);
		}
		final ObjectNode msg = obj();
		msg.set("spec", wf);
		return send("submitworkflowspecmsg", msg).path("processgraphid").asText();
	}

	public Process submit(final ObjectNode spec) throws IOException {
		final ObjectNode msg = obj();
		msg.set("spec", spec);
		return process(send("submitfuncspecmsg", msg));
	}

	public Process getProcess(final String id) throws IOException {
		final ObjectNode msg = obj();
		msg.put("processid", id);
		return process(send("getprocessmsg", msg));
	}

	public Deployment deployment(final String graphId) throws IOException {
		final ObjectNode msg = obj();
		msg.put("processgraphid", graphId);
		final JsonNode g = send("getprocessgraphmsg", msg);
		final List<StepState> steps = new ArrayList<>();
		for (final JsonNode pid : g.path("processids")) {
			final Process p = getProcess(pid.asText());
			final Object out = p.output().isEmpty() ? null : p.output().get(0);
			steps.add(new StepState(p.nodeName(), p.id(), String.valueOf(p.kwargs().get("kind")), String.valueOf(p.kwargs().get("target")),
					stateName(p.state()), p.errors(), out instanceof String s ? s : null));
		}
		steps.sort(Comparator.comparing(StepState::node));
		return new Deployment(g.path("processgraphid").asText(), stateName(g.path("state").asInt()), steps);
	}

	/** Waits up to timeoutSeconds for a process of this executor's type; null when nothing was queued. */
	public Process assign(final int timeoutSeconds) throws IOException {
		final ObjectNode msg = obj();
		msg.put("colonyname", cfg.colonyName());
		msg.put("timeout", timeoutSeconds);
		msg.put("availablecpu", "");
		msg.put("availablemem", "");
		try {
			return process(send("assignprocessmsg", msg, cfg.prvKey(), Duration.ofSeconds(timeoutSeconds + 15L)));
		} catch (final ColonyException e) {
			final String m = String.valueOf(e.getMessage()).toLowerCase();
			if (m.contains("timeout") || m.contains("no processes")) {
				return null;
			}
			throw e;
		}
	}

	public void close(final String processId, final List<Object> output) throws IOException {
		final ObjectNode msg = obj();
		msg.put("processid", processId);
		msg.set("out", Json.MAPPER.valueToTree(output));
		send("closesuccessfulmsg", msg);
	}

	public void fail(final String processId, final List<String> errors) throws IOException {
		final ObjectNode msg = obj();
		msg.put("processid", processId);
		msg.set("errors", Json.MAPPER.valueToTree(errors));
		send("closefailedmsg", msg);
	}

	public void addLog(final String processId, final String message) throws IOException {
		final ObjectNode msg = obj();
		msg.put("processid", processId);
		msg.put("message", message);
		send("addlogmsg", msg);
	}

	@SuppressWarnings("unchecked")
	private static Process process(final JsonNode p) {
		final JsonNode spec = p.path("spec");
		final Map<String, Object> kwargs = spec.path("kwargs").isObject() ? Json.LENIENT.convertValue(spec.path("kwargs"), LinkedHashMap.class)
				: Map.of();
		final List<Object> out = p.path("out").isArray() ? Json.LENIENT.convertValue(p.path("out"), List.class) : List.of();
		final List<String> errors = p.path("errors").isArray() ? Json.LENIENT.convertValue(p.path("errors"), List.class) : List.of();
		return new Process(p.path("processid").asText(), p.path("state").asInt(), spec.path("nodename").asText(), kwargs,
				spec.path("funcname").asText(), out, errors);
	}

	// ------------------------------------------------------------------------------------------------- setup

	/** Creates the colony if missing (signed with the server key). */
	public void ensureColony(final String serverPrvKey, final String colonyPrvKey) throws IOException {
		final ObjectNode get = obj();
		get.put("colonyname", cfg.colonyName());
		try {
			send("getcolonymsg", get, serverPrvKey, Duration.ofSeconds(15));
			return;
		} catch (final ColonyException missing) {
			// add below
		}
		final ObjectNode msg = obj();
		msg.putObject("colony").put("colonyid", ColonyCrypto.id(colonyPrvKey)).put("name", cfg.colonyName());
		send("addcolonymsg", msg, serverPrvKey, Duration.ofSeconds(15));
	}

	/** Registers and approves an executor identity (signed with the colony key); idempotent. */
	public void ensureExecutor(final String colonyPrvKey, final String executorPrvKey, final String type, final String name) throws IOException {
		final String id = ColonyCrypto.id(executorPrvKey);
		final ObjectNode get = obj();
		get.put("colonyname", cfg.colonyName());
		get.put("executorname", name);
		try {
			final JsonNode existing = send("getexecutormsg", get, colonyPrvKey, Duration.ofSeconds(15));
			if (existing.path("executorid").asText().equals(id) && existing.path("executortype").asText().equals(type)) {
				if (existing.path("state").asInt() != 1) {
					approve(colonyPrvKey, name);
				}
				return;
			}
			if (!existing.isNull() && existing.has("executorid")) {
				send("removeexecutormsg", get.deepCopy(), colonyPrvKey, Duration.ofSeconds(15));
			}
		} catch (final ColonyException missing) {
			// add below
		}
		final ObjectNode msg = obj();
		final ObjectNode ex = msg.putObject("executor");
		final String now = Instant.now().toString();
		ex.put("executorid", id);
		ex.put("executortype", type);
		ex.put("executorname", name);
		ex.put("colonyname", cfg.colonyName());
		ex.put("state", 0);
		ex.put("requirefuncreg", false);
		ex.put("commissiontime", now);
		ex.put("lastheardfromtime", now);
		try {
			send("addexecutormsg", msg, colonyPrvKey, Duration.ofSeconds(15));
		} catch (final ColonyException e) {
			if (!String.valueOf(e.getMessage()).contains("already")) {
				throw e;
			}
		}
		approve(colonyPrvKey, name);
	}

	private void approve(final String colonyPrvKey, final String name) throws IOException {
		final ObjectNode msg = obj();
		msg.put("colonyname", cfg.colonyName());
		msg.put("executorname", name);
		send("approveexecutormsg", msg, colonyPrvKey, Duration.ofSeconds(15));
	}
}
