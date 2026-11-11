/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import eu.arrowhead.solocalcloudconfigurator.Json;

/**
 * The organisation's inter-cloud policy for Systems of LocalClouds. A member cloud's inter-cloud abilities follow from the
 * components of its local-cloud design: the systems a component may let consume services of other clouds, and the
 * services a component may offer to them. Everything else (core, operator, inter-cloud and configurator systems,
 * management services) never crosses a cloud boundary.
 *
 * @param organization          default organisation of new designs
 * @param forbiddenConsumers    systems that are never inter-cloud consumers (must contain Sysop)
 * @param intercloudComponents  components a member needs to take part in exchanges (Gatekeeper and GateTunnel)
 * @param components            inter-cloud capabilities per local-cloud catalog component id
 * @param architectures         CPU architectures packages can target
 * @param relayPort             default port of the organisation relay
 */
public record PolicyCatalog(String organization, List<String> forbiddenConsumers, List<String> intercloudComponents,
		Map<String, Capabilities> components, List<String> architectures, int relayPort) {

	/** What a component may consume from and offer to other member clouds. */
	public record Capabilities(List<String> consumers, List<Export> exports) {
	}

	/** A service a component may offer to consumers of other clouds; services are its Compose services. */
	public record Export(String provider, String service, List<String> operations, List<String> services) {
	}

	public static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9]{0,62}");
	public static final Pattern SERVICE = Pattern.compile("[a-z][A-Za-z0-9-]{0,62}");
	public static final Pattern COMPONENT = Pattern.compile("[a-z0-9][a-z0-9-]{0,62}");

	public static PolicyCatalog load(final Path file) throws IOException {
		final PolicyCatalog c = Json.MAPPER.readValue(Files.readString(file), PolicyCatalog.class);
		c.validate();
		return c;
	}

	public Capabilities capabilities(final String component) {
		return components.getOrDefault(component, new Capabilities(List.of(), List.of()));
	}

	/**
	 * Names end up in shell arguments, file paths, policy ids and MQTT ACL lines, so the catalog is held to strict
	 * patterns. A management service can never be exported, and a forbidden consumer never be offered as consumer.
	 */
	void validate() {
		require(organization != null && NAME.matcher(organization).matches(), "organization");
		require(forbiddenConsumers != null && forbiddenConsumers.contains("Sysop"), "forbiddenConsumers must contain Sysop");
		require(intercloudComponents != null && !intercloudComponents.isEmpty()
				&& intercloudComponents.stream().allMatch(c -> COMPONENT.matcher(c).matches()), "intercloudComponents");
		require(architectures != null && !architectures.isEmpty() && architectures.stream().allMatch(a -> Set.of("amd64", "arm64").contains(a)),
				"architectures (amd64, arm64)");
		require(relayPort > 0 && relayPort < 65536, "relayPort");
		require(components != null, "components");
		final Set<String> providers = new HashSet<>();
		components.forEach((id, cap) -> {
			require(COMPONENT.matcher(id).matches() && cap != null && cap.consumers() != null && cap.exports() != null, "component " + id);
			for (final String s : cap.consumers()) {
				require(NAME.matcher(s).matches() && !forbiddenConsumers.contains(s), id + ": consumer " + s);
			}
			for (final Export e : cap.exports()) {
				require(NAME.matcher(e.provider()).matches() && SERVICE.matcher(e.service()).matches()
						&& providers.add(e.provider() + "/" + e.service()), id + ": export " + e.provider() + "/" + e.service());
				require(!e.service().toLowerCase().endsWith("management") && !forbiddenConsumers.contains(e.provider()),
						id + ": management services and core systems cannot be exported (" + e.service() + ")");
				require(e.operations() != null && !e.operations().isEmpty() && e.operations().stream().allMatch(o -> SERVICE.matcher(o).matches()),
						id + ": export operations");
				require(e.services() != null && e.services().stream().allMatch(s -> COMPONENT.matcher(s).matches()), id + ": export services");
			}
		});
	}

	private static void require(final boolean ok, final String what) {
		if (!ok) {
			throw new IllegalArgumentException("invalid policy catalog: " + what);
		}
	}
}
