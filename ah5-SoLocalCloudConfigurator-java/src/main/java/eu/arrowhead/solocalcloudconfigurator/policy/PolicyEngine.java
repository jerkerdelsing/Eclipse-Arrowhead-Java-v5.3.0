/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.policy;

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

import eu.arrowhead.solocalcloudconfigurator.catalog.PolicyCatalog;
import eu.arrowhead.solocalcloudconfigurator.catalog.PolicyCatalog.Export;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Exchange;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Member;

/**
 * Checks a System of LocalClouds against the organisation's security and authorization policies and derives, per member
 * cloud, the exact Gatekeeper neighbours and ConsumerAuthorization policies that the exchanges need, and nothing more.
 * Ported from SoSConfigurator's policy engine; the member clouds and their abilities now come from the design: a member's
 * inter-cloud consumers and exports are those of the components of its local-cloud design (PolicyCatalog.components).
 *
 * <p>Security rules (S): S0 the name is a plain identifier; S1 members are listed once; S2 at least one member, two for
 * exchanges; S4 in a federation (two or more members) every member has the inter-cloud components (Gatekeeper and
 * GateTunnel). The package's own checks add the PKI conditions at deployment (cross-certificate, name constraint, relay
 * certificates, CRL). The relay serves every member by construction: the package generates its ACL for exactly the members.
 *
 * <p>Authorization rules (A): A1 consumer and provider are different members; A2 the consumer is offered as inter-cloud
 * consumer by a component of its cloud and is never an operator, core, inter-cloud or configurator system; A3 the provider
 * service is exported by a component of the provider cloud; A4 the operations are a non-empty subset of the export's.
 */
public final class PolicyEngine {

	private static final Pattern SOLC_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,62}");

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

	/** Everything one member cloud applies and verifies (clouds/<Cloud>/federation-spec.json of the package). */
	public record CloudConfig(String cloud, List<Neighbour> neighbours, List<Neighbour> removeNeighbours, List<Grant> grants,
			List<String> revokes, List<Check> preconditions, List<Check> verify, List<Discovery> discovery, List<String> exportServices) {
	}

	/** The relay: member clouds and the relay subjects of their Gatekeeper and GateTunnel (the ACL is generated from them). */
	public record RelayConfig(List<String> clouds, List<String> subjects) {
	}

	public record Evaluation(String solc, List<String> members, List<Violation> violations, Map<String, CloudConfig> clouds,
			RelayConfig relay) {

		public boolean allowed() {
			return violations.isEmpty();
		}
	}

	/** A member's inter-cloud abilities, derived from the components of its local-cloud design. */
	record Cloud(String name, String organization, boolean intercloud, List<String> missing, Set<String> consumers, List<Export> exports) {

		String identifier() {
			return name + "|" + organization;
		}

		Optional<Export> export(final String provider, final String service) {
			return exports.stream().filter(e -> e.provider().equals(provider) && e.service().equals(service)).findFirst();
		}
	}

	private final PolicyCatalog catalog;

	public PolicyEngine(final PolicyCatalog catalog) {
		this.catalog = catalog;
	}

	/** Subject DN of a relay client certificate (kit security/relay-client.ext), RFC 2253 order. */
	public static String relaySubject(final String system, final String cloud, final String organization) {
		return "dnQualifier=sy,CN=" + system + "." + cloud + "." + organization + ".arrowhead.eu,OU=" + cloud + ",O=" + organization + ",C=SE";
	}

	Cloud cloud(final Member m, final String organization) {
		final List<String> components = m.lcDesign() == null || m.lcDesign().components() == null ? List.of() : m.lcDesign().components();
		final List<String> missing = catalog.intercloudComponents().stream().filter(c -> !components.contains(c)).toList();
		final Set<String> consumers = new TreeSet<>();
		final List<Export> exports = new ArrayList<>();
		for (final String c : components) {
			consumers.addAll(catalog.capabilities(c).consumers());
			exports.addAll(catalog.capabilities(c).exports());
		}
		return new Cloud(m.cloud(), organization, missing.isEmpty(), missing, consumers, exports);
	}

	public Evaluation evaluate(final SolcDesign design) {
		final List<Violation> v = new ArrayList<>();
		if (design.name() == null || !SOLC_NAME.matcher(design.name()).matches()) {
			v.add(new Violation("S0", "SoLC name must match " + SOLC_NAME.pattern()));
		}
		final String org = design.organization();

		// --- security: member clouds
		final Map<String, Cloud> members = new LinkedHashMap<>();
		for (final Member m : design.membersOrEmpty()) {
			if (members.containsKey(m.cloud())) {
				v.add(new Violation("S1", "cloud " + m.cloud() + " is listed twice"));
				continue;
			}
			members.put(m.cloud(), cloud(m, org));
		}
		if (members.isEmpty()) {
			v.add(new Violation("S2", "a System of LocalClouds needs at least one member cloud"));
		}
		if (!design.exchangesOrEmpty().isEmpty() && members.size() < 2) {
			v.add(new Violation("S2", "inter-cloud exchanges need at least two member clouds"));
		}
		final boolean federation = members.size() >= 2;
		if (federation) {
			members.values().stream().filter(c -> !c.intercloud()).forEach(c -> v.add(new Violation("S4", c.name()
					+ " cannot take part in the federation: its local-cloud design lacks " + String.join(", ", c.missing()))));
		}

		// --- authorization: exchanges
		// provider cloud -> (consumer cloud identifier, provider, service) -> consumers
		final Map<String, Map<List<String>, Set<String>>> grants = new TreeMap<>();
		final Map<String, Set<String>> peers = new TreeMap<>();
		final Map<String, Set<String>> opsUsed = new TreeMap<>(); // "<providerCloud>|<provider>|<service>" -> operations
		final Map<String, List<Check>> verify = new TreeMap<>();
		final Map<String, List<Discovery>> discovery = new TreeMap<>();
		int i = 0;
		for (final Exchange x : design.exchangesOrEmpty()) {
			i++;
			final String at = "exchange " + i + " (" + x.consumerCloud() + "/" + x.consumer() + " -> " + x.providerCloud() + "/" + x.provider()
					+ "." + x.service() + ")";
			if (!members.containsKey(x.consumerCloud()) || !members.containsKey(x.providerCloud())) {
				v.add(new Violation("A1", at + ": both clouds must be members of the SoLC"));
				continue;
			}
			if (x.consumerCloud().equals(x.providerCloud())) {
				v.add(new Violation("A1", at + ": consumer and provider are in the same cloud; that is local orchestration, not an inter-cloud exchange"));
				continue;
			}
			final Cloud cc = members.get(x.consumerCloud());
			final Cloud pc = members.get(x.providerCloud());
			if (catalog.forbiddenConsumers().contains(x.consumer())) {
				v.add(new Violation("A2", at + ": " + x.consumer() + " is an operator, core, inter-cloud or configurator system and never an inter-cloud consumer"));
				continue;
			}
			if (!cc.consumers().contains(x.consumer())) {
				v.add(new Violation("A2", at + ": " + x.consumer() + " is not an inter-cloud consumer of " + cc.name() + " (no component of its design offers it)"));
				continue;
			}
			final Optional<Export> e = pc.export(x.provider(), x.service());
			if (e.isEmpty()) {
				v.add(new Violation("A3", at + ": " + pc.name() + " does not export " + x.provider() + "." + x.service() + " (no component of its design offers it)"));
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
			if (design.verifyDiscovery()) {
				discovery.computeIfAbsent(cc.name(), k -> new ArrayList<>())
						.add(new Discovery(x.consumer(), x.service(), pc.identifier(), x.provider()));
			}
		}

		// --- per member cloud of the federation: neighbours, grants, revocations, preconditions
		final Map<String, CloudConfig> configs = new LinkedHashMap<>();
		final List<String> subjects = new ArrayList<>();
		for (final Cloud c : members.values()) {
			if (!federation || !c.intercloud()) {
				continue;
			}
			subjects.add(relaySubject("Gatekeeper", c.name(), org));
			subjects.add(relaySubject("GateTunnel", c.name(), org));
			final Set<String> myPeers = peers.getOrDefault(c.name(), Set.of());
			final List<Neighbour> add = new ArrayList<>();
			final List<Neighbour> remove = new ArrayList<>();
			final List<String> revokes = new ArrayList<>();
			final List<Check> verifyHere = new ArrayList<>(verify.getOrDefault(c.name(), List.of()));
			for (final Cloud other : members.values()) {
				if (other.name().equals(c.name())) {
					continue;
				}
				if (myPeers.contains(other.name())) {
					add.add(new Neighbour(other.name(), org));
				} else {
					remove.add(new Neighbour(other.name(), org));
				}
			}
			final Map<List<String>, Set<String>> mine = grants.getOrDefault(c.name(), Map.of());
			final List<Grant> grantList = new ArrayList<>();
			mine.forEach((k, consumers) -> grantList.add(new Grant(k.get(0), k.get(1), k.get(2), List.copyOf(consumers))));
			for (final Export e : c.exports()) {
				for (final Cloud other : members.values()) {
					if (other.name().equals(c.name())) {
						continue;
					}
					if (!mine.containsKey(List.of(other.identifier(), e.provider(), e.service()))) {
						revokes.add(new Grant(other.identifier(), e.provider(), e.service(), List.of()).instanceId());
						// the revocation is verified: an inter-cloud consumer of that cloud must be denied
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
				if (p[0].equals(c.name())) {
					ops.forEach(op -> pre.add(new Check(p[1], "GateTunnel", "LOCAL", p[2], op, true)));
					c.export(p[1], p[2]).ifPresent(e -> exportServices.add(String.join(",", e.services())));
				}
			});
			configs.put(c.name(), new CloudConfig(c.name(), add, remove, grantList, revokes, pre, verifyHere,
					discovery.getOrDefault(c.name(), List.of()), exportServices));
		}
		return new Evaluation(design.name(), List.copyOf(new LinkedHashSet<>(members.keySet())), v, configs,
				new RelayConfig(List.copyOf(configs.keySet()), subjects));
	}

	private static int compareKeys(final List<String> a, final List<String> b) {
		return String.join("|", a).compareTo(String.join("|", b));
	}
}
