package eu.arrowhead.sosconfigurator.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import eu.arrowhead.sosconfigurator.Json;

/**
 * The local clouds an SoS can be built from, the relay that connects them, and what each cloud may offer to and consume
 * from other clouds. The catalog is the authority of the executors: they re-derive every step from it.
 */
public record Catalog(String organization, String colony, Relay relay, List<String> forbiddenConsumers, List<Cloud> clouds) {

	/** The inter-cloud MQTT relay of the organization (Arrowhead-relay). */
	public record Relay(String name, String dir, String composeProject, String service, String executorType, String aclFile,
			List<String> clouds) {
	}

	/** Whether a cloud's implementation can take part in inter-cloud exchanges (Gatekeeper GSD/ICN + GateTunnel). */
	public record Intercloud(boolean supported, String gatekeeperService, String gatetunnelService, String reason) {
	}

	/** Local HTTPS ports of the core and Gatekeeper on the cloud host. */
	public record Ports(int authentication, int authorization, int gatekeeper) {
	}

	/** A service a cloud may offer to consumers of other clouds; services are the Compose services of the provider. */
	public record Export(String provider, String service, List<String> operations, List<String> services) {
	}

	public record Cloud(String name, String organization, String implementation, String dir, String composeProject,
			String executorType, Intercloud intercloud, Ports ports, List<String> consumers, List<Export> exports) {

		public String identifier() {
			return name + "|" + organization;
		}

		public Optional<Export> export(final String provider, final String service) {
			return exports.stream().filter(e -> e.provider().equals(provider) && e.service().equals(service)).findFirst();
		}

		/** Subject DN of a relay client certificate (security/relay-client.ext of the cloud), RFC 2253 order. */
		public String relaySubject(final String system) {
			return "dnQualifier=sy,CN=" + system + "." + name + "." + organization + ".arrowhead.eu,OU=" + name + ",O=" + organization
					+ ",C=SE";
		}
	}

	private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9]{0,62}");
	private static final Pattern SERVICE = Pattern.compile("[a-z][A-Za-z0-9-]{0,62}");
	private static final Pattern COMPOSE = Pattern.compile("[a-z0-9][a-z0-9_-]{0,62}");

	public static Catalog load(final Path file) throws IOException {
		final Catalog c = Json.MAPPER.readValue(Files.readString(file), Catalog.class);
		c.validate();
		return c;
	}

	public Optional<Cloud> cloud(final String name) {
		return clouds.stream().filter(c -> c.name().equals(name)).findFirst();
	}

	/** Executor roles: "relay" and one per cloud ("cloud:<name>"). */
	public List<String> roles() {
		final List<String> roles = new java.util.ArrayList<>();
		roles.add("relay");
		clouds.forEach(c -> roles.add("cloud:" + c.name()));
		return roles;
	}

	public String executorType(final String role) {
		if ("relay".equals(role)) {
			return relay.executorType();
		}
		if (role.startsWith("cloud:")) {
			return cloud(role.substring(6)).map(Cloud::executorType).orElseThrow(() -> new IllegalArgumentException("unknown role " + role));
		}
		throw new IllegalArgumentException("unknown role " + role);
	}

	/**
	 * Names end up in shell arguments, file paths, policy ids and MQTT ACL lines, so the catalog is held to strict
	 * patterns. A management service can never be exported.
	 */
	void validate() {
		require(organization != null && NAME.matcher(organization).matches(), "organization");
		require(colony != null && COMPOSE.matcher(colony).matches(), "colony");
		require(relay != null && COMPOSE.matcher(relay.composeProject()).matches() && COMPOSE.matcher(relay.service()).matches()
				&& COMPOSE.matcher(relay.executorType()).matches() && relay.dir() != null && relay.aclFile() != null, "relay");
		require(forbiddenConsumers != null && forbiddenConsumers.contains("Sysop"), "forbiddenConsumers must contain Sysop");
		final Set<String> names = new HashSet<>();
		final Set<String> types = new HashSet<>(Set.of(relay.executorType()));
		for (final Cloud c : clouds) {
			require(NAME.matcher(c.name()).matches() && names.add(c.name()), "cloud name " + c.name());
			require(NAME.matcher(c.organization()).matches(), c.name() + ": organization");
			require(COMPOSE.matcher(c.composeProject()).matches(), c.name() + ": composeProject");
			require(COMPOSE.matcher(c.executorType()).matches() && types.add(c.executorType()), c.name() + ": executorType");
			require(c.intercloud() != null, c.name() + ": intercloud");
			if (c.intercloud().supported()) {
				require(c.ports() != null && COMPOSE.matcher(c.intercloud().gatekeeperService()).matches()
						&& COMPOSE.matcher(c.intercloud().gatetunnelService()).matches(), c.name() + ": gatekeeper/gatetunnel/ports");
			}
			for (final String s : c.consumers()) {
				require(NAME.matcher(s).matches() && !forbiddenConsumers.contains(s), c.name() + ": consumer " + s);
			}
			final Set<String> exports = new HashSet<>();
			for (final Export e : c.exports()) {
				require(NAME.matcher(e.provider()).matches() && SERVICE.matcher(e.service()).matches()
						&& exports.add(e.provider() + "/" + e.service()), c.name() + ": export " + e.provider() + "/" + e.service());
				require(!e.service().toLowerCase().endsWith("management") && !forbiddenConsumers.contains(e.provider()),
						c.name() + ": management services and core systems cannot be exported (" + e.service() + ")");
				require(e.operations() != null && !e.operations().isEmpty()
						&& e.operations().stream().allMatch(o -> SERVICE.matcher(o).matches()), c.name() + ": export operations");
				require(e.services() != null && e.services().stream().allMatch(s -> COMPOSE.matcher(s).matches()), c.name() + ": export services");
			}
		}
		for (final String r : relay.clouds()) {
			require(names.contains(r), "relay cloud " + r + " is not catalogued");
		}
	}

	private static void require(final boolean ok, final String what) {
		if (!ok) {
			throw new IllegalArgumentException("invalid catalog: " + what);
		}
	}
}
