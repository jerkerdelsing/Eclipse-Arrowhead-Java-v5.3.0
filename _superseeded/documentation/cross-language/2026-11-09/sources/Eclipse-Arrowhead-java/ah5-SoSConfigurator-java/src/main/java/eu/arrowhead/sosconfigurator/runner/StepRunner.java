package eu.arrowhead.sosconfigurator.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.catalog.Catalog.Cloud;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.PolicyEngine;
import eu.arrowhead.sosconfigurator.policy.PolicyEngine.CloudConfig;
import eu.arrowhead.sosconfigurator.policy.PolicyEngine.Evaluation;
import eu.arrowhead.sosconfigurator.policy.SosRequest;

/**
 * Carries out the steps of one executor role on its host. A process carries only the step kind and the operator's SoS
 * request; the runner evaluates that request against its own catalog again and derives the cloud's configuration
 * itself, so a forged process cannot add a neighbour, a grant or a consumer that the policies refuse.
 */
public class StepRunner {

	/** ColonyOS function names served by the executors. */
	public static final String FUNC_STATUS = "sos.status";
	public static final String FUNC_STEP = "sos.step";

	/** Runs one command in a directory. */
	public interface Commander {
		Result run(Path dir, Map<String, String> env, Duration timeout, List<String> command) throws IOException;
	}

	public record Result(int exit, String output) {
	}

	/** Real processes; output is combined stdout/stderr. */
	public static final class ExecCommander implements Commander {
		@Override
		public Result run(final Path dir, final Map<String, String> env, final Duration timeout, final List<String> command) throws IOException {
			final ProcessBuilder pb = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true);
			pb.environment().putAll(env);
			final Process p = pb.start();
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final Thread reader = Thread.ofVirtual().start(() -> {
				try (InputStream in = p.getInputStream()) {
					in.transferTo(out);
				} catch (final IOException ignored) {
					// process ended
				}
			});
			try {
				if (!p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
					p.destroyForcibly();
					return new Result(124, out.toString(StandardCharsets.UTF_8) + "\ntimeout after " + timeout);
				}
				reader.join(2000);
			} catch (final InterruptedException e) {
				p.destroyForcibly();
				Thread.currentThread().interrupt();
				throw new IOException("interrupted", e);
			}
			return new Result(p.exitValue(), out.toString(StandardCharsets.UTF_8));
		}
	}

	private final Catalog catalog;
	private final String role;
	private final Path base;
	private final Path scripts;
	private final Commander cmd;
	private final PolicyEngine engine;

	public StepRunner(final Catalog catalog, final String role, final Path base, final Path scripts, final Commander cmd) {
		this.catalog = catalog;
		this.role = role;
		this.base = base;
		this.scripts = scripts;
		this.cmd = cmd;
		this.engine = new PolicyEngine(catalog);
		catalog.executorType(role); // unknown roles fail here
	}

	public String role() {
		return role;
	}

	/** Status of this role's cloud or relay as a JSON string. */
	public String status() throws IOException {
		final Map<String, Object> spec = new LinkedHashMap<>();
		if ("relay".equals(role)) {
			spec.put("members", relayMembers(catalog.relay().clouds()));
			return runScript("sos-relay.sh", "status", spec, relayDir(), relayEnv(), Duration.ofSeconds(40)).trim();
		}
		final Cloud c = ownCloud();
		if (!c.intercloud().supported()) {
			return Json.write(Map.of("cloud", c.name(), "intercloud", false, "reason", c.intercloud().reason()));
		}
		spec.putAll(cloudBasics(c));
		return runScript("sos-cloud.sh", "status", spec, cloudDir(c), cloudEnv(c), Duration.ofSeconds(40)).trim();
	}

	/** Executes one step; returns its redacted output, throws with the redacted reason on failure. */
	public String step(final String kind, final String sosJson) throws IOException {
		final SosRequest req = Json.MAPPER.readValue(sosJson, SosRequest.class);
		final Evaluation ev = engine.evaluate(req);
		if (!ev.allowed()) {
			throw new IOException("refused by this executor's policy check: " + ev.violations());
		}
		if ("relay".equals(role)) {
			if (!Planner.RELAY_CHECK.equals(kind)) {
				throw new IOException("step " + kind + " is not served by the relay executor");
			}
			final Map<String, Object> spec = Map.of("members", relayMembers(ev.relay().clouds()));
			return runScript("sos-relay.sh", "check", spec, relayDir(), relayEnv(), Duration.ofMinutes(2));
		}
		final Cloud c = ownCloud();
		final CloudConfig cc = ev.clouds().get(c.name());
		if (cc == null) {
			throw new IOException(c.name() + " is not a member of SoS " + ev.sos());
		}
		final String action = switch (kind) {
		case Planner.CLOUD_CHECK, Planner.NEIGHBOURS, Planner.GRANTS, Planner.VERIFY, Planner.DISCOVER -> kind;
		default -> throw new IOException("unknown step kind " + kind);
		};
		final Map<String, Object> spec = new LinkedHashMap<>(cloudBasics(c));
		spec.put("sos", ev.sos());
		spec.put("config", cc);
		return runScript("sos-cloud.sh", action, spec, cloudDir(c), cloudEnv(c), Duration.ofMinutes(5));
	}

	private Map<String, Object> cloudBasics(final Cloud c) {
		final Map<String, Object> m = new LinkedHashMap<>();
		m.put("cloud", c.name());
		m.put("organization", c.organization());
		m.put("composeProject", c.composeProject());
		m.put("gatekeeperService", c.intercloud().gatekeeperService());
		m.put("gatetunnelService", c.intercloud().gatetunnelService());
		m.put("subjects", Map.of("Gatekeeper", c.relaySubject("Gatekeeper"), "GateTunnel", c.relaySubject("GateTunnel")));
		return m;
	}

	/** Clouds the relay must serve, with their relay subjects and the Gatekeeper to share a network with. */
	private List<Map<String, Object>> relayMembers(final List<String> clouds) {
		final List<Map<String, Object>> members = new ArrayList<>();
		for (final String name : clouds) {
			catalog.cloud(name).filter(c -> c.intercloud().supported()).ifPresent(c -> members.add(Map.of("name", c.name(),
					"composeProject", c.composeProject(), "gatekeeperService", c.intercloud().gatekeeperService(),
					"subjects", List.of(c.relaySubject("Gatekeeper"), c.relaySubject("GateTunnel")))));
		}
		return members;
	}

	private Cloud ownCloud() {
		return catalog.cloud(role.substring("cloud:".length())).orElseThrow();
	}

	private Path cloudDir(final Cloud c) {
		return base.resolve(c.dir()).normalize();
	}

	private Path relayDir() {
		return base.resolve(catalog.relay().dir()).normalize();
	}

	private Map<String, String> cloudEnv(final Cloud c) {
		final Map<String, String> env = new LinkedHashMap<>();
		env.put("CLOUD_DIR", cloudDir(c).toString());
		env.put("COMPOSE_PROJECT", c.composeProject());
		env.put("AUTHENTICATION_URL", "https://127.0.0.1:" + c.ports().authentication());
		env.put("CONSUMER_AUTHORIZATION_URL", "https://127.0.0.1:" + c.ports().authorization());
		env.put("GATEKEEPER_URL", "https://127.0.0.1:" + c.ports().gatekeeper());
		env.put("SECRETS_ENV_FILE", cloudDir(c).resolve(".env").toString());
		return env;
	}

	private Map<String, String> relayEnv() {
		final Map<String, String> env = new LinkedHashMap<>();
		env.put("RELAY_DIR", relayDir().toString());
		env.put("RELAY_PROJECT", catalog.relay().composeProject());
		env.put("RELAY_SERVICE", catalog.relay().service());
		env.put("RELAY_ACL", catalog.relay().aclFile());
		return env;
	}

	private static final Set<String> SCRIPTS = Set.of("sos-cloud.sh", "sos-relay.sh");

	/** Runs one of the configurator's own scripts with the spec in a private temporary file. */
	private String runScript(final String script, final String action, final Object spec, final Path dir, final Map<String, String> env,
			final Duration timeout) throws IOException {
		if (!SCRIPTS.contains(script)) {
			throw new IOException("script " + script + " is not a SoSConfigurator script");
		}
		final Path exe = scripts.resolve(script).toAbsolutePath().normalize();
		if (!Files.isRegularFile(exe)) {
			throw new IOException("missing " + exe);
		}
		if (!Files.isDirectory(dir)) {
			throw new IOException("directory " + dir + " does not exist on this host");
		}
		final Path tmp = Files.createTempDirectory("sos-", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
		final Path file = tmp.resolve("spec.json");
		try {
			Files.writeString(file, Json.write(spec));
			final Result r = cmd.run(dir, env, timeout, List.of("/bin/sh", exe.toString(), action, file.toString()));
			final String out = Redact.redact(r.output());
			if (r.exit() != 0) {
				throw new StepFailed(action + " failed (exit " + r.exit() + ")", out);
			}
			return out;
		} finally {
			Files.deleteIfExists(file);
			Files.deleteIfExists(tmp);
		}
	}

	/** A failed step; carries the redacted output for the ColonyOS process log. */
	public static final class StepFailed extends IOException {
		private static final long serialVersionUID = 1L;
		private final String output;

		public StepFailed(final String message, final String output) {
			super(message);
			this.output = output;
		}

		public String output() {
			return output;
		}
	}

	/** Masks credential-like values in step output before it goes to ColonyOS logs. */
	public static final class Redact {
		private static final Pattern SECRETISH = Pattern
				.compile("(?i)(\"?(password|passwd|token|secret|prvkey|private[_-]?key)\"?\\s*[:=]\\s*)(\"[^\"]*\"|[^\\s,}]+)");
		private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+|IDENTITY-TOKEN//)[^\\s\"]+");

		private Redact() {
		}

		public static String redact(final String s) {
			return BEARER.matcher(SECRETISH.matcher(s).replaceAll("$1***")).replaceAll("$1***");
		}
	}
}
