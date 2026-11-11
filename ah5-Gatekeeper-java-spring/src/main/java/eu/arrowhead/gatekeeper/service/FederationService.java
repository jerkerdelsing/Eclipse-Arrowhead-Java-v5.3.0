/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.list;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.obj;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.str;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.strings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.jpa.entity.Cloud;
import eu.arrowhead.gatekeeper.jpa.service.GatekeeperDbService;
import eu.arrowhead.gatekeeper.relay.RelayClient;
import eu.arrowhead.gatekeeper.relay.RelayHandler;
import eu.arrowhead.gatekeeper.service.normalization.GatekeeperNormalization;
import jakarta.annotation.PostConstruct;

// Global service discovery and inter-cloud negotiation, both sides, ported from the native 5.2.2 Gatekeeper with the same
// relay messages and HTTP results. The provider side asks ConsumerAuthorization with authorizationManagement.check-policies
// for the foreign consumer (authorization.verify answers only the provider or the consumer itself).
@Service
public class FederationService implements RelayHandler {

	//=================================================================================================
	// members

	private static final String GENERIC_HTTPS = Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private FederationSettings settings;

	@Autowired
	private GatekeeperDbService dbService;

	@Autowired
	private ArrowheadConsumer consumer;

	@Autowired
	private RelayClient relay;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	public void registerRelayHandler() {
		relay.setHandler(this);
	}

	//-------------------------------------------------------------------------------------------------
	// a peer is trusted when it is the Gatekeeper of a configured neighbour cloud of the expected organisation
	@Override
	public boolean trustedPeer(final String peer) {
		final String[] p = peer.split("\\.");
		if (p.length != 5 || !peer.equals("Gatekeeper." + p[1] + "." + settings.organizationOf(p[1]) + ".arrowhead.eu") || p[1].equals(settings.getCloud())) {
			return false;
		}

		try {
			return dbService.clouds().stream().anyMatch(c -> c.getName().equals(p[1]) && c.getOperator().equals(p[2]));
		} catch (final Exception ex) {
			return false;
		}
	}

	//-------------------------------------------------------------------------------------------------
	// provider side: a gsd-request or icn-request of a trusted peer
	@Override
	public Map<String, Object> incoming(final String peer, final Map<String, Object> message) throws Exception {
		final String[] p = peer.split("\\.");
		final String foreign = p[1] + "|" + p[2];
		final String foreignConsumer = str(message.get("consumer"));
		final String type = str(message.get("type"));

		if (type.equals("gsd-request")) {
			final Map<String, Object> requirement = obj(message.get("serviceRequirement"));
			final String definition = str(requirement.get("serviceDefinition"));
			final List<Object> result = new ArrayList<>();
			for (final Object raw : instances(definition)) {
				final Map<String, Object> instance = obj(raw);
				final Map<String, Object> props = properties(instance);
				final String provider = str(obj(instance.get("provider")).get("name"));
				if (!props.isEmpty() && authorized(provider, foreignConsumer, foreign, definition, strings(requirement.get("operations")))) {
					result.add(m("providerName", provider,
							"serviceDefinition", definition,
							"version", instance.get("version"),
							"metadata", instance.getOrDefault("metadata", m()),
							"operations", new TreeSet<>(obj(props.get("operations")).keySet())));
				}
			}

			return m("cloud", settings.getCloud() + "|" + settings.getOrganization(), "results", result);
		}

		if (!type.equals("icn-request")) {
			throw new ApiError(400, "Unknown relay request");
		}

		final String definition = str(message.get("serviceDefinition"));
		final String provider = str(message.get("providerName"));
		final List<String> requested = strings(message.get("operations"));
		Map<String, Object> instance = m();
		Map<String, Object> props = m();
		for (final Object raw : instances(definition)) {
			final Map<String, Object> candidate = obj(raw);
			if (str(obj(candidate.get("provider")).get("name")).equals(provider)) {
				final Map<String, Object> offered = properties(candidate);
				if (!offered.isEmpty()) {
					instance = candidate;
					props = offered;
					break;
				}
			}
		}

		if (props.isEmpty()) {
			return m("accepted", false, "reason", "Provider not found");
		}

		final Map<String, Object> offered = obj(props.get("operations"));
		final Map<String, Object> negotiated = new LinkedHashMap<>();
		for (final String operation : requested.isEmpty() ? new TreeSet<>(offered.keySet()) : requested) {
			if (!offered.containsKey(operation)) {
				return m("accepted", false, "reason", "Operation not offered");
			}

			negotiated.put(operation, offered.get(operation));
		}

		if (negotiated.isEmpty() || !authorized(provider, foreignConsumer, foreign, definition, new ArrayList<>(negotiated.keySet()))) {
			return m("accepted", false, "reason", "Not authorized by provider cloud");
		}

		final List<String> addresses = strings(props.get("accessAddresses"));
		if (addresses.isEmpty()) {
			return m("accepted", false, "reason", "Provider address missing");
		}

		final Map<String, Object> session = consumer.consume(GatekeeperConstants.SERVICE_DEF_GATE_TUNNEL_CONNECTION, GatekeeperConstants.SERVICE_OP_OPEN_PROVIDER_SESSION, null,
				m("consumer", foreignConsumer,
						"consumerCloud", foreign,
						"provider", provider,
						"serviceDefinition", definition,
						"peer", "GateTunnel." + p[1] + "." + p[2] + ".arrowhead.eu",
						"target", m("address", addresses.getFirst(),
								"port", props.get("accessPort"),
								"basePath", props.get("basePath"),
								"operations", negotiated)));
		final Map<String, Object> accepted = new LinkedHashMap<>(m("accepted", true,
				"sessionId", session.get("sessionId"),
				"providerName", provider,
				"serviceDefinition", definition,
				"version", instance.get("version"),
				"metadata", instance.getOrDefault("metadata", m()),
				"basePath", props.get("basePath"),
				"operations", negotiated));
		// AH-GT-1 (J5): the provider GateTunnel's signed ephemeral key travels to the consumer GateTunnel
		if (session.get("keyExchange") != null) {
			accepted.put("keyExchange", session.get("keyExchange"));
		}

		return accepted;
	}

	//-------------------------------------------------------------------------------------------------
	// every requested operation (or the service, when none is named) must be granted to the foreign consumer
	public boolean authorized(final String provider, final String foreignConsumer, final String consumerCloud, final String definition, final List<String> operations) {
		if (provider.isBlank() || foreignConsumer.isBlank() || definition.isBlank()) {
			return false;
		}

		final List<Object> checks = new ArrayList<>();
		for (final String operation : operations.isEmpty() ? List.of("") : operations) {
			final Map<String, Object> check = m("provider", provider, "consumer", foreignConsumer, "cloud", consumerCloud, "targetType", "SERVICE_DEF", "target", definition);
			if (!operation.isEmpty()) {
				check.put("scope", operation);
			}

			checks.add(check);
		}

		try {
			final List<Object> entries = list(consumer.consume(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT, Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES,
					Constants.SYS_NAME_CONSUMER_AUTHORIZATION, m("list", checks)).get("entries"));
			return entries.size() == checks.size() && entries.stream().allMatch(e -> Boolean.TRUE.equals(obj(e).get("granted")));
		} catch (final Exception ex) {
			logger.error("authorization: check-policies unavailable, denying {} from {}: {}", foreignConsumer, consumerCloud, ex.getMessage());
			return false;
		}
	}

	//-------------------------------------------------------------------------------------------------
	// consumer side: global service discovery in the named (or all configured) neighbour clouds that this cloud's policy
	// allows: the own organisation, and another organisation only when listed in foreign-targets (SOS-7)
	public Map<String, Object> gsd(final Map<String, Object> payload, final String localConsumer) {
		final Map<String, Object> requirement = obj(payload.getOrDefault("serviceRequirement", payload.get("requestedService")));
		if (str(requirement.get("serviceDefinition")).isEmpty()) {
			throw new IllegalArgumentException("serviceRequirement.serviceDefinition is required");
		}

		List<Object> targets = list(payload.get("targetClouds"));
		if (targets.isEmpty()) {
			targets = new ArrayList<>();
			for (final Cloud c : dbService.clouds()) {
				if (c.getOperator().equals(settings.organizationOf(c.getName())) && !c.getName().equals(settings.getCloud()) && settings.targetAllowed(c.getName())) {
					targets.add(c.getName());
				}
			}
		}

		final List<Object> results = new ArrayList<>();
		final List<String> refused = new ArrayList<>();
		int failures = 0;
		for (final Object t : targets) {
			try {
				final String name = target(t);
				if (!settings.targetAllowed(name)) {
					refused.add(name + "|" + settings.organizationOf(name));
					failures++;
					continue;
				}
				final Map<String, Object> reply = relay.request(name, m("type", "gsd-request", "consumer", localConsumer, "serviceRequirement", requirement));
				if (!str(reply.get("cloud")).equals(name + "|" + settings.organizationOf(name))) {
					throw new ApiError(502, "Peer cloud identity mismatch");
				}

				for (final Object raw : list(reply.get("results"))) {
					final Map<String, Object> item = new LinkedHashMap<>(obj(raw));
					item.put("cloud", reply.get("cloud"));
					results.add(item);
				}
			} catch (final Exception ex) {
				logger.debug("gsd: {} failed: {}", t, ex.getMessage());
				failures++;
			}
		}

		return m("results", results, "unsuccessfulRequests", failures, "queriedClouds", targets.size() - refused.size(), "refusedClouds", refused);
	}

	//-------------------------------------------------------------------------------------------------
	// consumer side: inter-cloud negotiation with one provider of a neighbour cloud, then the local GateTunnel session
	public Map<String, Object> icn(final Map<String, Object> payload, final String localConsumer) throws Exception {
		final Object target = payload.getOrDefault("targetCloud", payload.get("providerCloud"));
		final String foreign = target(target);
		final String provider = str(payload.get("providerName"));
		final String definition = str(payload.get("serviceDefinition"));
		if (foreign.isEmpty() || provider.isEmpty() || definition.isEmpty()) {
			throw new IllegalArgumentException("targetCloud, providerName and serviceDefinition are required");
		}
		if (!settings.targetAllowed(foreign)) {
			throw new ApiError(403, "Target cloud " + foreign + "|" + settings.organizationOf(foreign) + " belongs to another organisation and is not allowed by this cloud's policy ("
					+ GatekeeperConstants.FEDERATION_FOREIGN_TARGETS + ")");
		}

		final Map<String, Object> reply = relay.request(foreign, m("type", "icn-request",
				"consumer", localConsumer,
				"providerName", provider,
				"serviceDefinition", definition,
				"operations", payload.getOrDefault("operations", List.of())));
		if (!Boolean.TRUE.equals(reply.get("accepted"))) {
			return m("negotiated", false, "targetCloud", target, "reason", reply.get("reason"));
		}

		final boolean e2e = settings.endToEndEncrypted();
		if (!e2e && reply.get("keyExchange") != null) {
			throw new ApiError(502, "Peer requires AH-GT-1; legacy transport will not downgrade it");
		}

		if (e2e && reply.get("keyExchange") == null) {
			throw new ApiError(502, "Peer does not offer AH-GT-1; it is not downgraded to legacy-mTLS");
		}

		final String organization = settings.organizationOf(foreign);
		final String providerCloud = foreign + "|" + organization;
		final Map<String, Object> tunnel = consumer.consume(GatekeeperConstants.SERVICE_DEF_GATE_TUNNEL_CONNECTION, GatekeeperConstants.SERVICE_OP_OPEN_CONSUMER_SESSION, null,
				m("sessionId", reply.get("sessionId"),
						"consumer", localConsumer,
						"providerCloud", providerCloud,
						"provider", reply.get("providerName"),
						"serviceDefinition", reply.get("serviceDefinition"),
						"peer", "GateTunnel." + foreign + "." + organization + ".arrowhead.eu",
						"keyExchange", reply.get("keyExchange")));

		return m("negotiated", true,
				"providerCloud", providerCloud,
				"providerName", reply.get("providerName"),
				"serviceDefinition", reply.get("serviceDefinition"),
				"version", reply.get("version"),
				"metadata", reply.get("metadata"),
				"sessionId", reply.get("sessionId"),
				"interface", m("templateName", GENERIC_HTTPS,
						"protocol", "https",
						"policy", "CERT_AUTH",
						"properties", m("accessAddresses", List.of(tunnel.get("address")),
								"accessPort", tunnel.get("port"),
								"basePath", reply.get("basePath"),
								"operations", reply.get("operations"))));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// local instances of a service definition with a generic_https interface (needs ServiceRegistry direct access)
	List<Object> instances(final String definition) {
		if (definition.isBlank()) {
			throw new IllegalArgumentException("serviceDefinition is required");
		}

		return list(consumer.lookup(m("serviceDefinitionNames", List.of(definition), "interfaceTemplateNames", List.of(GENERIC_HTTPS))).get("entries"));
	}

	//-------------------------------------------------------------------------------------------------
	static Map<String, Object> properties(final Map<String, Object> instance) {
		for (final Object raw : list(instance.get("interfaces"))) {
			final Map<String, Object> intf = obj(raw);
			if (str(intf.get("templateName")).equals(GENERIC_HTTPS) && str(intf.get("protocol")).equalsIgnoreCase("https")) {
				return obj(intf.get("properties"));
			}
		}

		return m();
	}

	//-------------------------------------------------------------------------------------------------
	// cloud name of a target; a named organisation must be the configured one of that cloud
	String target(final Object value) {
		final String name = GatekeeperNormalization.targetName(value);
		final String requested = GatekeeperNormalization.targetOrganization(value);
		if (!requested.isEmpty() && !requested.equals(settings.organizationOf(name))) {
			throw new ApiError(400, "Target organisation does not match configured peer");
		}

		return name;
	}
}
