/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import eu.arrowhead.solocalcloudconfigurator.catalog.PolicyCatalog;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Exchange;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.LcDesignRef;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Member;

/** The bundled policy catalog and a two-cloud SoLC: RandomDataProducer of Plant stores data in the DataManager of Office. */
public final class TestDesigns {

	public static final List<String> CORE = List.of("serviceregistry", "authentication", "consumerauthorization", "dynamicserviceorchestration");
	public static final String LC_A = "11111111-1111-1111-1111-111111111111";
	public static final String LC_B = "22222222-2222-2222-2222-222222222222";

	private TestDesigns() {
	}

	public static PolicyCatalog catalog() throws Exception {
		return PolicyCatalog.load(Path.of("catalogs/solc-policy.json"));
	}

	public static List<String> components(final String... extra) {
		final java.util.ArrayList<String> c = new java.util.ArrayList<>(CORE);
		c.addAll(List.of(extra));
		return c;
	}

	public static Member member(final String cloud, final String lc, final String host, final String arch, final int offset, final List<String> components) {
		return new Member(cloud, new LcDesignRef(lc, cloud + "-design", 3, components), host, arch, offset);
	}

	public static SolcDesign design(final List<Member> members, final List<Exchange> exchanges) {
		return new SolcDesign("Factory", "Company", new SolcDesign.Relay("relay.example.org", 8893), members, exchanges, true,
				new SolcDesign.Packaging("prebuilt", List.of("amd64", "arm64")), Map.of());
	}

	public static SolcDesign twoClouds() {
		return design(List.of(
				member("Plant", LC_A, "plant.example.org", "arm64", 0, components("gatekeeper", "gatetunnel", "random-data-producer")),
				member("Office", LC_B, "office.example.org", "amd64", 0, components("gatekeeper", "gatetunnel", "datamanager"))),
				List.of(new Exchange("Plant", "RandomDataProducer", "Office", "DataManager", "dataHistorian", List.of("put-data"))));
	}
}
