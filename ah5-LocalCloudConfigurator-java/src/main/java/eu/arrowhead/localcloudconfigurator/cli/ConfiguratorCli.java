package eu.arrowhead.localcloudconfigurator.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.arrowhead.localcloudconfigurator.Catalog;
import eu.arrowhead.localcloudconfigurator.CloudColony;
import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.Planner;
import eu.arrowhead.localcloudconfigurator.Runner;
import eu.arrowhead.localcloudconfigurator.colony.ColonyClient;
import eu.arrowhead.localcloudconfigurator.colony.ColonyCrypto;

/**
 * Operator tools of the native 5.2.2 configurator, unchanged: plan (print a plan for a request file) and the retained ColonyOS
 * tools keygen, register and executor. The shared operator token of the native API no longer exists (keygen creates none).
 */
public final class ConfiguratorCli {

	private ConfiguratorCli() {
		throw new UnsupportedOperationException();
	}

	public static void run(final String[] args) throws Exception {
		final Map<String, String> o = options(args);
		switch (args[0]) {
		case "keygen" -> {
			final Path dir = Path.of(need(o, "dir"));
			Files.createDirectories(dir);
			Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwx------"));
			for (final String name : List.of("colony-key", "api-key", "executor-key")) {
				final Path p = dir.resolve(name);
				if (!Files.exists(p)) {
					Files.writeString(p, ColonyCrypto.generatePrivateKey() + "\n", StandardOpenOption.CREATE_NEW);
					Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rw-------"));
				}
			}
			System.out.println("LocalCloudConfigurator ColonyOS credentials ready");
		}
		case "register" -> {
			final Catalog c = Catalog.load(Path.of(need(o, "catalog")));
			final Path dir = Path.of(need(o, "dir"));
			final String host = o.getOrDefault("host", "127.0.0.1");
			final int port = Integer.parseInt(o.getOrDefault("port", "50081"));
			final String colonyKey = Files.readString(dir.resolve("colony-key")).trim();
			final ColonyClient client = new ColonyClient(new ColonyClient.Config(host, port, false, c.colony(), Files.readString(dir.resolve("executor-key")).trim()));
			client.ensureColony(Files.readString(Path.of(need(o, "server-key-file"))).trim(), colonyKey);
			client.ensureExecutor(colonyKey, Files.readString(dir.resolve("api-key")).trim(), "java-lcc-api", "java-lcc-api");
			final ColonyClient apiClient = new ColonyClient(new ColonyClient.Config(host, port, false, c.colony(), Files.readString(dir.resolve("api-key")).trim()));
			final ColonyClient.Process probe = apiClient.assign(1);
			if (probe != null) {
				apiClient.fail(probe.id(), List.of("API identity does not execute deployment functions"));
			}
			client.ensureExecutor(colonyKey, Files.readString(dir.resolve("executor-key")).trim(), c.executorType(), "java-lcc-executor");
			System.out.println("Java local-cloud colony and executor identities registered");
		}
		case "executor" -> {
			final Catalog c = Catalog.load(Path.of(need(o, "catalog")));
			final Runner r = new Runner(c, Path.of(need(o, "cloud-dir")), new Runner.Exec(), o.getOrDefault("docker", "docker"));
			System.out.println("Java LocalCloudConfigurator executor ready for " + c.cloud());
			new CloudColony(new ColonyClient(ColonyClient.Config.fromEnv()), c).serve(r);
		}
		case "plan" -> {
			final Catalog c = Catalog.load(Path.of(need(o, "catalog")));
			final Planner.Request req = Json.MAPPER.readValue(Files.readString(Path.of(need(o, "request"))), Planner.Request.class);
			System.out.println(Json.write(Planner.build(c, req, List.of())));
		}
		default -> throw new IllegalArgumentException("Unknown command");
		}
	}

	static Map<String, String> options(final String[] args) {
		final Map<String, String> o = new HashMap<>();
		for (int i = 1; i < args.length; i++) {
			if (!args[i].startsWith("-")) {
				throw new IllegalArgumentException("Expected option");
			}
			final String k = args[i].replaceFirst("^-+", "");
			if (i + 1 == args.length || args[i + 1].startsWith("-")) {
				o.put(k, "true");
			} else {
				o.put(k, args[++i]);
			}
		}
		return o;
	}

	static String need(final Map<String, String> o, final String k) {
		if (!o.containsKey(k) || "true".equals(o.get(k))) {
			throw new IllegalArgumentException("Missing -" + k);
		}
		return o.get(k);
	}
}
