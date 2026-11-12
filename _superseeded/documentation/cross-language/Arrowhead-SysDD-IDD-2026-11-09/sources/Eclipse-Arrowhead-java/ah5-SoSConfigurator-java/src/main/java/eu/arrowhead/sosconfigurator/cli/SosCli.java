/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.cli;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.SoSConfiguratorConstants;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.colony.ColonyClient;
import eu.arrowhead.sosconfigurator.colony.ColonyCrypto;
import eu.arrowhead.sosconfigurator.console.ConsolePasswordHash;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;

// Operator command-line tools: keygen, register, plan, apply. They are not part of the running system.
public final class SosCli {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static int run(final String[] args) throws Exception {
		final Map<String, String> options = options(args);
		return switch (args[0]) {
		case "keygen" -> keygen(options);
		case "register" -> register(options);
		case "plan" -> plan(options);
		case "apply" -> apply(options);
		default -> usage();
		};
	}

	//-------------------------------------------------------------------------------------------------
	// file name part of an executor key: relay, or the lower-case cloud name
	public static String roleKey(final String role) {
		return "relay".equals(role) ? "relay" : role.substring("cloud:".length()).toLowerCase();
	}

	//-------------------------------------------------------------------------------------------------
	public static String executorName(final String role) {
		return "sos-" + roleKey(role) + "-executor";
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static int usage() {
		System.err.println("""
				usage: sosconfigurator [serve [--spring.option=value ...]]   the SoSConfigurator support system (default)
				       sosconfigurator executor --sos.catalog.file=FILE --sos.role=relay|cloud:<Name> [--sos.base=DIR] [--sos.scripts=DIR]
				       sosconfigurator keygen   -dir SECRETS -catalog FILE [-force]      ColonyOS keys, DB password, console password and hash
				       sosconfigurator register -dir SECRETS -catalog FILE [-host H -port P]   colony + API and executor identities
				       sosconfigurator plan     -catalog FILE -request FILE              offline policy check and plan
				       sosconfigurator apply    -request FILE [-url URL] [-password-file FILE] [-no-wait]""");
		return 2;
	}

	//-------------------------------------------------------------------------------------------------
	private static Map<String, String> options(final String[] args) {
		final Map<String, String> options = new HashMap<>();
		for (int i = 1; i < args.length; i++) {
			if (!args[i].startsWith("-")) {
				throw new IllegalArgumentException("unexpected argument " + args[i]);
			}

			final String key = args[i].substring(1);
			if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
				options.put(key, args[++i]);
			} else {
				options.put(key, "true");
			}
		}

		return options;
	}

	//-------------------------------------------------------------------------------------------------
	private static String need(final Map<String, String> options, final String key) {
		final String value = options.get(key);
		if (value == null || value.isBlank() || "true".equals(value)) {
			throw new IllegalArgumentException("-" + key + " is required");
		}

		return value;
	}

	//-------------------------------------------------------------------------------------------------
	// fresh ColonyOS keys, database password, console password and its scrypt hash; existing files are kept unless -force
	private static int keygen(final Map<String, String> options) throws IOException {
		final Catalog catalog = Catalog.load(Path.of(need(options, "catalog")));
		final Path dir = Path.of(need(options, "dir"));
		Files.createDirectories(dir);
		Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwx------"));
		final boolean force = options.containsKey("force");
		final SecureRandom random = new SecureRandom();
		final Map<String, String> values = new LinkedHashMap<>();
		final String server = existing(dir, "colonies_server_prvkey", force).orElse(ColonyCrypto.generatePrivateKey());
		values.put("colonies_server_prvkey", server);
		values.put("colonies_server_id", ColonyCrypto.id(server));
		values.put("colonies_colony_prvkey", ColonyCrypto.generatePrivateKey());
		values.put("sos_api_prvkey", ColonyCrypto.generatePrivateKey());
		for (final String role : catalog.roles()) {
			values.put("sos_executor_" + roleKey(role) + "_prvkey", ColonyCrypto.generatePrivateKey());
		}

		for (final String key : new String[] { "colonies_db_password", "sos_db_password", "sos_db_root_password" }) {
			values.put(key, hex(random, 24));
		}

		final String password = existing(dir, "sos_console_password", force).orElse(hex(random, 18));
		values.put("sos_console_password", password);
		if (force || !Files.exists(dir.resolve("sos_console_password_hash")) || !Files.exists(dir.resolve("sos_console_password"))) {
			values.put("sos_console_password_hash", ConsolePasswordHash.create(password));
		}

		int written = 0;
		for (final var entry : values.entrySet()) {
			final Path file = dir.resolve(entry.getKey());
			if (Files.exists(file) && !force && !"sos_console_password_hash".equals(entry.getKey())) {
				continue;
			}

			Files.writeString(file, entry.getValue() + "\n");
			Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
			written++;
		}

		System.out.println("secrets in " + dir + ": " + written + " written, " + (values.size() - written) + " kept");
		return 0;
	}

	//-------------------------------------------------------------------------------------------------
	private static Optional<String> existing(final Path dir, final String name, final boolean force) throws IOException {
		final Path file = dir.resolve(name);
		return !force && Files.exists(file) ? Optional.of(Files.readString(file).trim()) : Optional.empty();
	}

	//-------------------------------------------------------------------------------------------------
	private static String secret(final Path dir, final String name) throws IOException {
		return Files.readString(dir.resolve(name)).trim();
	}

	//-------------------------------------------------------------------------------------------------
	private static String hex(final SecureRandom random, final int bytes) {
		final byte[] value = new byte[bytes];
		random.nextBytes(value);
		return HexFormat.of().formatHex(value);
	}

	//-------------------------------------------------------------------------------------------------
	private static int register(final Map<String, String> options) throws IOException {
		final Catalog catalog = Catalog.load(Path.of(need(options, "catalog")));
		final Path dir = Path.of(need(options, "dir"));
		final String colonyKey = secret(dir, "colonies_colony_prvkey");
		final ColonyClient client = new ColonyClient(new ColonyClient.Config(options.getOrDefault("host", "127.0.0.1"),
				Integer.parseInt(options.getOrDefault("port", "50081")), false, catalog.colony(), colonyKey));
		client.ensureColony(secret(dir, "colonies_server_prvkey"), colonyKey);
		client.ensureExecutor(colonyKey, secret(dir, "sos_api_prvkey"), SoSConfiguratorConstants.API_EXECUTOR_TYPE, "sos-api");
		for (final String role : catalog.roles()) {
			client.ensureExecutor(colonyKey, secret(dir, "sos_executor_" + roleKey(role) + "_prvkey"), catalog.executorType(role), executorName(role));
		}

		System.out.println("colony " + catalog.colony() + ": sos-api and " + catalog.roles().size() + " executors registered and approved");
		return 0;
	}

	//-------------------------------------------------------------------------------------------------
	private static int plan(final Map<String, String> options) throws IOException {
		final Catalog catalog = Catalog.load(Path.of(need(options, "catalog")));
		final SosRequest request = Json.MAPPER.readValue(Files.readString(Path.of(need(options, "request"))), SosRequest.class);
		final Planner.Plan plan = new Planner(catalog).build(request);
		System.out.println(Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(plan));
		return plan.allowed() ? 0 : 3;
	}

	//-------------------------------------------------------------------------------------------------
	// submits through the operator console (loopback, password login) and follows the workflow
	private static int apply(final Map<String, String> options) throws Exception {
		final String url = options.getOrDefault("url", "http://127.0.0.1:8495");
		final String password = Files.readString(Path.of(options.getOrDefault("password-file", "deploy/colonyos/secrets/sos_console_password"))).trim();
		final String body = Files.readString(Path.of(need(options, "request")));
		final HttpClient http = HttpClient.newHttpClient();

		final HttpResponse<String> login = http.send(HttpRequest.newBuilder(URI.create(url + SoSConfiguratorConstants.HTTP_API_SESSION_PATH))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(Json.write(Map.of("password", password))))
				.build(), HttpResponse.BodyHandlers.ofString());
		if (login.statusCode() != 200) {
			System.err.println("console login refused (HTTP " + login.statusCode() + ")");
			return 3;
		}

		final String cookie = login.headers().firstValue("Set-Cookie").map(c -> c.split(";", 2)[0]).orElseThrow();
		final HttpResponse<String> submitted = http.send(HttpRequest.newBuilder(URI.create(url + "/api/deployments")).header("Cookie", cookie)
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
		final JsonNode result = Json.LENIENT.readTree(submitted.body());
		if (submitted.statusCode() != 202) {
			System.err.println("refused (HTTP " + submitted.statusCode() + "): " + result.path("error").asText());
			for (final JsonNode violation : result.path("plan").path("violations")) {
				System.err.println("  " + violation.path("rule").asText() + ": " + violation.path("message").asText());
			}
			return 3;
		}

		final String id = result.path("id").asText();
		System.out.println("submitted SoS " + result.path("plan").path("sos").asText() + " as workflow " + id);
		if (options.containsKey("no-wait")) {
			return 0;
		}

		String last = "";
		while (true) {
			Thread.sleep(1000);
			final HttpResponse<String> polled = http.send(HttpRequest.newBuilder(URI.create(url + "/api/deployments/" + URLEncoder.encode(id, StandardCharsets.UTF_8)))
					.header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
			final JsonNode deployment = Json.LENIENT.readTree(polled.body());
			final StringBuilder steps = new StringBuilder();
			for (final JsonNode step : deployment.path("steps")) {
				steps.append(String.format("  %-34s %s%n", step.path("node").asText(), step.path("state").asText()));
			}

			if (!steps.toString().equals(last)) {
				System.out.print(steps);
				System.out.println("  --");
				last = steps.toString();
			}

			final String state = deployment.path("state").asText();
			if ("success".equals(state)) {
				System.out.println("SoS applied");
				return 0;
			}

			if ("failed".equals(state)) {
				for (final JsonNode step : deployment.path("steps")) {
					if ("failed".equals(step.path("state").asText())) {
						System.err.println(step.path("node").asText() + ": " + step.path("errors"));
					}
				}
				return 1;
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private SosCli() {
		throw new UnsupportedOperationException();
	}
}
