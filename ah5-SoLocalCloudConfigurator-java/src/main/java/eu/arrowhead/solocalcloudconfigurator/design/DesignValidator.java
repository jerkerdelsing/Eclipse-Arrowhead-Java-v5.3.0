/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.design;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import eu.arrowhead.solocalcloudconfigurator.catalog.PolicyCatalog;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Exchange;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Member;

/**
 * Structural checks of a design: names, hosts, ports and package options. These values end up in file names, shell
 * arguments, Compose project names, certificate subjects and MQTT ACL lines, so they are held to strict patterns. Whether the
 * SoLC is allowed (organisation, inter-cloud ability, exchanges) is the policy engine's decision, not this class's.
 */
public final class DesignValidator {

	public static final Pattern DESIGN_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,62}");
	public static final Pattern ID = Pattern.compile("[a-f0-9-]{36}");
	// RFC 1123 host name, IPv4 address, or an IPv6 address without zone
	private static final Pattern HOST = Pattern.compile("(?=.{1,253}$)([A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*");
	private static final Pattern IPV6 = Pattern.compile("[0-9A-Fa-f:]{2,39}");
	public static final Set<String> IMAGE_MODES = Set.of("build", "prebuilt");
	public static final int MAX_MEMBERS = 16;
	public static final int MAX_EXCHANGES = 128;
	// inter-cloud components a SoLC adds to a member, with what they require
	public static final int MAX_INTERCLOUD = 32;
	// published ports of a cloud lie in 5671..8900; offsets keep them below 65536 and clouds on one host apart
	public static final int MAX_PORT_OFFSET = 50000;
	public static final int PORT_OFFSET_STEP = 1000;

	private DesignValidator() {
	}

	/** Problems of the design, empty when it is well formed. */
	public static List<String> problems(final SolcDesign d, final PolicyCatalog catalog) {
		final List<String> p = new ArrayList<>();
		if (d == null) {
			return List.of("design is empty");
		}
		if (d.name() == null || !DESIGN_NAME.matcher(d.name()).matches()) {
			p.add("name must match " + DESIGN_NAME.pattern());
		}
		if (d.organization() == null || !PolicyCatalog.NAME.matcher(d.organization()).matches()) {
			p.add("organization must match " + PolicyCatalog.NAME.pattern());
		}
		if (d.relay() == null || !host(d.relay().host()) || d.relay().port() < 1 || d.relay().port() > 65535) {
			p.add("relay needs a host name or address and a port 1-65535");
		}
		final List<Member> members = d.membersOrEmpty();
		if (members.isEmpty() || members.size() > MAX_MEMBERS) {
			p.add("a System of LocalClouds has 1-" + MAX_MEMBERS + " member clouds");
		}
		final Set<String> clouds = new HashSet<>();
		final Map<String, Set<Integer>> offsets = new HashMap<>();
		for (final Member m : members) {
			if (m == null || m.cloud() == null || !PolicyCatalog.NAME.matcher(m.cloud()).matches()) {
				p.add("member cloud name must match " + PolicyCatalog.NAME.pattern());
				continue;
			}
			if (!clouds.add(m.cloud().toLowerCase())) {
				p.add(m.cloud() + ": cloud names must be unique (case-insensitively: Compose project names)");
			}
			final SolcDesign.LcDesignRef lc = m.lcDesign();
			if (lc == null || lc.id() == null || !ID.matcher(lc.id()).matches() || lc.revision() < 1 || lc.name() == null || lc.name().isBlank()
					|| lc.name().length() > 200 || lc.components() == null || lc.components().isEmpty()
					|| !lc.components().stream().allMatch(c -> c != null && PolicyCatalog.COMPONENT.matcher(c).matches())) {
				p.add(m.cloud() + ": needs a stored local-cloud design (id, name, revision and its components)");
			}
			if (m.intercloudOrEmpty().size() > MAX_INTERCLOUD
					|| !m.intercloudOrEmpty().stream().allMatch(c -> c != null && PolicyCatalog.COMPONENT.matcher(c).matches())) {
				p.add(m.cloud() + ": added inter-cloud infrastructure must be at most " + MAX_INTERCLOUD + " catalog component ids");
			}
			if (!host(m.host())) {
				p.add(m.cloud() + ": host must be a host name or address");
			}
			if (m.architecture() == null || !catalog.architectures().contains(m.architecture())) {
				p.add(m.cloud() + ": architecture must be one of " + catalog.architectures());
			}
			if (m.portOffset() < 0 || m.portOffset() > MAX_PORT_OFFSET || m.portOffset() % PORT_OFFSET_STEP != 0) {
				p.add(m.cloud() + ": port offset must be a multiple of " + PORT_OFFSET_STEP + " in 0-" + MAX_PORT_OFFSET);
			} else if (m.host() != null && !offsets.computeIfAbsent(m.host().toLowerCase(), k -> new HashSet<>()).add(m.portOffset())) {
				p.add(m.cloud() + ": another member on " + m.host() + " uses port offset " + m.portOffset());
			}
		}
		if (d.relay() != null && d.relay().host() != null) {
			final Set<Integer> onRelayHost = offsets.getOrDefault(d.relay().host().toLowerCase(), Set.of());
			for (final int offset : onRelayHost) {
				if (8883 + offset == d.relay().port()) {
					p.add("relay port " + d.relay().port() + " collides with the MQTT broker of a member on the relay host");
				}
			}
		}
		final List<Exchange> exchanges = d.exchangesOrEmpty();
		if (exchanges.size() > MAX_EXCHANGES) {
			p.add("at most " + MAX_EXCHANGES + " exchanges");
		}
		for (final Exchange x : exchanges) {
			if (x == null || !name(x.consumerCloud()) || !name(x.consumer()) || !name(x.providerCloud()) || !name(x.provider())
					|| x.service() == null || !PolicyCatalog.SERVICE.matcher(x.service()).matches() || x.operations() == null
					|| !x.operations().stream().allMatch(o -> o != null && PolicyCatalog.SERVICE.matcher(o).matches())) {
				p.add("exchange: clouds and systems must match " + PolicyCatalog.NAME.pattern() + ", service and operations "
						+ PolicyCatalog.SERVICE.pattern());
			}
		}
		final SolcDesign.Packaging pk = d.packaging();
		if (pk == null || !IMAGE_MODES.contains(pk.imageMode())) {
			p.add("packaging.imageMode must be one of " + IMAGE_MODES);
		} else if (pk.architectures() == null || pk.architectures().isEmpty() || !catalog.architectures().containsAll(pk.architectures())) {
			p.add("packaging.architectures must be a non-empty subset of " + catalog.architectures());
		} else {
			for (final Member m : members) {
				if (m != null && m.architecture() != null && !pk.architectures().contains(m.architecture())) {
					p.add(m.cloud() + ": its architecture " + m.architecture() + " is not among packaging.architectures");
				}
			}
		}
		if (d.layout() != null) {
			d.layout().forEach((k, v) -> {
				if (!clouds.contains(k == null ? "" : k.toLowerCase()) || v == null || v.x() < 0 || v.y() < 0 || v.x() > 10000 || v.y() > 10000) {
					p.add("layout: positions only for member clouds, coordinates 0-10000");
				}
			});
		}
		return p.stream().distinct().toList();
	}

	static boolean host(final String h) {
		return h != null && (HOST.matcher(h).matches() || (h.contains(":") && IPV6.matcher(h).matches()));
	}

	private static boolean name(final String s) {
		return s != null && PolicyCatalog.NAME.matcher(s).matches();
	}
}
