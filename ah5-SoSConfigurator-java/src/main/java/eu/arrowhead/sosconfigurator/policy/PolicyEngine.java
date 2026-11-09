package eu.arrowhead.sosconfigurator.policy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.catalog.Catalog.Cloud;
import eu.arrowhead.sosconfigurator.catalog.Catalog.Export;
import eu.arrowhead.sosconfigurator.policy.SosRequest.Exchange;

/**
 * Checks an SoS request against the security and authorization constraints of the organization and derives, per member
 * cloud, the exact Gatekeeper neighbours and ConsumerAuthorization policies that the exchanges need, and nothing more.
 *
 * <p>Security rules (S): only catalogued clouds of the relay's organization, whose implementation supports inter-cloud
 * exchanges and which the relay serves. The runtime checks of the cloud and relay executors add the PKI and ACL
 * conditions (cross-certificate, name constraint, relay certificates, CRL, ACL subjects).
 *
 * <p>Authorization rules (A): a consumer must be a catalogued inter-cloud consumer of its cloud and never an operator,
 * core or inter-cloud system; a provider service must be exported by its cloud; the operations must be offered by the
 * export. Grants are whitelists of exactly the requested consumers per consumer cloud, and grants of the catalogued
 * exports to clouds outside the SoS are revoked.
 */
public final class PolicyEngine {

	private static final Pattern SOS_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,62}");

	public record Violation(String rule, String message) {
	}

	public record Neighbour(String name, String operator) {
	}

	/** One ConsumerAuthorization policy in the provider cloud for the consumers of one neighbour cloud. */
	public record Grant(String cloud, String provider, String service, List<String> consumers) {

		public String instanceId() {
			return "MGMT|" + cloud + "|" + provider + "|SERVICE_DEF|" + service;
		}
	}

	/** A ConsumerAuthorization check: is the consumer (of cloud, or LOCAL) granted the service (operation scope)? */
	public record Check(String provider, String consumer, String cloud, String service, String scope, boolean expected) {
	}

	/** A global service discovery from the consumer cloud's Gatekeeper that must find the provider. */
	public record Discovery(String consumer, String service, String targetCloud, String provider) {
	}

	/** Everything one member cloud's executor applies and verifies. */
	public record CloudConfig(String cloud, List<Neighbour> neighbours, List<Neighbour> removeNeighbours, List<Grant> grants,
			List<String> revokes, List<Check> preconditions, List<Check> verify, List<Discovery> discovery, List<String> exportServices) {
	}

	/** What the relay executor checks: the ACL subjects and CRL sources of all member clouds. */
	public record RelayConfig(List<String> clouds, List<String> subjects) {
	}

	public record Evaluation(String sos, List<String> members, List<Violation> violations, Map<String, CloudConfig> clouds,
			RelayConfig relay) {

		public boolean allowed() {
			return violations.isEmpty();
		}
	}

	private final Catalog catalog;

	public PolicyEngine(final Catalog catalog) {
		this.catalog = catalog;
	}

	public Evaluation evaluate(final SosRequest req) {
		final List<Violation> v = new ArrayList<>();
		if (req.name() == null || !SOS_NAME.matcher(req.name()).matches()) {
			v.add(new Violation("S0", "SoS name must match " + SOS_NAME.pattern()));
		}

		// --- security: member clouds
		final Set<String> members = new LinkedHashSet<>();
		for (final String name : req.cloudsOrEmpty()) {
			final Optional<Cloud> c = catalog.cloud(name);
			if (c.isEmpty()) {
				v.add(new Violation("S1", "cloud " + name + " is not in the catalog"));
				continue;
			}
			if (!members.add(name)) {
				v.add(new Violation("S1", "cloud " + name + " is listed twice"));
				continue;
			}
			final Cloud cloud = c.get();
			if (!catalog.organization().equals(cloud.organization())) {
				v.add(new Violation("S3", name + " belongs to organization " + cloud.organization() + "; the relay and the organization PKI ("
						+ catalog.organization() + ") only federate clouds of the own organization"));
			}
			if (!cloud.intercloud().supported()) {
				v.add(new Violation("S4", name + " cannot exchange services with other clouds: " + cloud.intercloud().reason()));
			}
			if (!catalog.relay().clouds().contains(name)) {
				v.add(new Violation("S5", name + " is not served by " + catalog.relay().name() + " (no ACL subjects, CRL source or network)"));
			}
		}
		if (members.isEmpty()) {
			v.add(new Violation("S2", "an SoS needs at least one member cloud"));
		}
		if (!req.exchangesOrEmpty().isEmpty() && members.size() < 2) {
			v.add(new Violation("S2", "inter-cloud exchanges need at least two member clouds"));
		}

		// --- authorization: exchanges
		// provider cloud -> (consumer cloud identifier, provider, service) -> consumers
		final Map<String, Map<List<String>, Set<String>>> grants = new TreeMap<>();
		final Map<String, Set<String>> peers = new TreeMap<>();
		final Map<String, Set<String>> opsUsed = new TreeMap<>(); // "<providerCloud>|<provider>|<service>" -> operations
		final Map<String, List<Check>> verify = new TreeMap<>();
		final Map<String, List<Discovery>> discovery = new TreeMap<>();
		int i = 0;
		for (final Exchange x : req.exchangesOrEmpty()) {
			i++;
			final String at = "exchange " + i + " (" + x.consumerCloud() + "/" + x.consumer() + " -> " + x.providerCloud() + "/" + x.provider()
					+ "." + x.service() + ")";
			if (!members.contains(x.consumerCloud()) || !members.contains(x.providerCloud())) {
				v.add(new Violation("A1", at + ": both clouds must be members of the SoS"));
				continue;
			}
			if (x.consumerCloud().equals(x.providerCloud())) {
				v.add(new Violation("A1", at + ": consumer and provider are in the same cloud; that is local orchestration, not an SoS exchange"));
				continue;
			}
			final Cloud cc = catalog.cloud(x.consumerCloud()).orElseThrow();
			final Cloud pc = catalog.cloud(x.providerCloud()).orElseThrow();
			if (catalog.forbiddenConsumers().contains(x.consumer())) {
				v.add(new Violation("A2", at + ": " + x.consumer() + " is an operator, core or inter-cloud system and never an inter-cloud consumer"));
				continue;
			}
			if (!cc.consumers().contains(x.consumer())) {
				v.add(new Violation("A2", at + ": " + x.consumer() + " is not a catalogued inter-cloud consumer of " + cc.name()));
				continue;
			}
			final Optional<Export> e = pc.export(x.provider(), x.service());
			if (e.isEmpty()) {
				v.add(new Violation("A3", at + ": " + pc.name() + " does not export " + x.provider() + "." + x.service()));
				continue;
			}
			final List<String> ops = x.operations() == null ? List.of() : x.operations();
			if (ops.isEmpty() || !e.get().operations().containsAll(ops)) {
				v.add(new Violation("A4", at + ": operations " + ops + " must be a non-empty subset of the exported " + e.get().operations()));
				continue;
			}
			// ConsumerAuthorization refuses an empty default whitelist, so an inter-cloud grant is per service
			// definition; the operations are checked against the export above and verified per operation below.
			grants.computeIfAbsent(pc.name(), k -> new TreeMap<>(PolicyEngine::compareKeys))
					.computeIfAbsent(List.of(cc.identifier(), x.provider(), x.service()), k -> new TreeSet<>()).add(x.consumer());
			peers.computeIfAbsent(pc.name(), k -> new TreeSet<>()).add(cc.name());
			peers.computeIfAbsent(cc.name(), k -> new TreeSet<>()).add(pc.name());
			opsUsed.computeIfAbsent(pc.name() + "|" + x.provider() + "|" + x.service(), k -> new TreeSet<>()).addAll(ops);
			for (final String op : ops) {
				verify.computeIfAbsent(pc.name(), k -> new ArrayList<>())
						.add(new Check(x.provider(), x.consumer(), cc.identifier(), x.service(), op, true));
			}
			if (req.verifyDiscovery()) {
				discovery.computeIfAbsent(cc.name(), k -> new ArrayList<>())
						.add(new Discovery(x.consumer(), x.service(), pc.identifier(), x.provider()));
			}
		}

		// --- per member cloud: neighbours, grants, revocations, preconditions
		final Map<String, CloudConfig> configs = new LinkedHashMap<>();
		final List<String> subjects = new ArrayList<>();
		for (final String name : members) {
			final Cloud c = catalog.cloud(name).orElseThrow();
			if (!c.intercloud().supported() || !catalog.organization().equals(c.organization())) {
				continue;
			}
			subjects.add(c.relaySubject("Gatekeeper"));
			subjects.add(c.relaySubject("GateTunnel"));
			final Set<String> myPeers = peers.getOrDefault(name, Set.of());
			final List<Neighbour> add = new ArrayList<>();
			final List<Neighbour> remove = new ArrayList<>();
			final List<String> revokes = new ArrayList<>();
			final List<Check> verifyHere = new ArrayList<>(verify.getOrDefault(name, List.of()));
			for (final Cloud other : catalog.clouds()) {
				if (other.name().equals(name) || !other.organization().equals(catalog.organization())) {
					continue;
				}
				if (myPeers.contains(other.name())) {
					add.add(new Neighbour(other.name(), other.organization()));
				} else {
					remove.add(new Neighbour(other.name(), other.organization()));
				}
			}
			final Map<List<String>, Set<String>> mine = grants.getOrDefault(name, Map.of());
			final List<Grant> grantList = new ArrayList<>();
			mine.forEach((k, consumers) -> grantList.add(new Grant(k.get(0), k.get(1), k.get(2), List.copyOf(consumers))));
			for (final Export e : c.exports()) {
				for (final Cloud other : catalog.clouds()) {
					if (other.name().equals(name) || !other.organization().equals(catalog.organization())) {
						continue;
					}
					if (!mine.containsKey(List.of(other.identifier(), e.provider(), e.service()))) {
						revokes.add(new Grant(other.identifier(), e.provider(), e.service(), List.of()).instanceId());
						// the revocation is verified: a catalogued consumer of that cloud must be denied
						other.consumers().stream().findFirst().ifPresent(consumer -> verifyHere.add(
								new Check(e.provider(), consumer, other.identifier(), e.service(), e.operations().get(0), false)));
					}
				}
			}
			// Preconditions before anything is changed: the Gatekeeper may check inter-cloud consumers, and the
			// provider-side GateTunnel may call each provider operation in use on behalf of remote consumers.
			final List<Check> pre = new ArrayList<>();
			pre.add(new Check("ConsumerAuthorization", "Gatekeeper", "LOCAL", "authorizationManagement", "check-policies", true));
			final List<String> exportServices = new ArrayList<>();
			opsUsed.forEach((key, ops) -> {
				final String[] p = key.split("\\|");
				if (p[0].equals(name)) {
					ops.forEach(op -> pre.add(new Check(p[1], "GateTunnel", "LOCAL", p[2], op, true)));
					c.export(p[1], p[2]).ifPresent(e -> exportServices.add(String.join(",", e.services())));
				}
			});
			configs.put(name, new CloudConfig(name, add, remove, grantList, revokes, pre, verifyHere,
					discovery.getOrDefault(name, List.of()), exportServices));
		}
		return new Evaluation(req.name(), List.copyOf(members), v, configs, new RelayConfig(List.copyOf(configs.keySet()), subjects));
	}

	private static int compareKeys(final List<String> a, final List<String> b) {
		return String.join("|", a).compareTo(String.join("|", b));
	}
}
