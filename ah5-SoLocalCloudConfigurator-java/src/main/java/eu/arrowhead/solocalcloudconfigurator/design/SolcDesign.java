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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A System of LocalClouds: local clouds built from stored local-cloud designs, the organisation relay that federates them,
 * the inter-cloud service exchanges between them, and how its deployment package provides the system images.
 *
 * @param name             design name; also the package name
 * @param organization     organisation of every member (the organisation CA cross-signs only its own clouds)
 * @param relay            the organisation relay all members connect to
 * @param members          the local clouds
 * @param exchanges        consumer system of one member uses a service of a provider in another member
 * @param verifyDiscovery  the package also runs a global service discovery from each consumer cloud over the relay
 * @param packaging        image mode and target architectures of the deployment package
 * @param layout           graphical position of each member cloud in the editor
 */
public record SolcDesign(String name, String organization, Relay relay, List<Member> members, List<Exchange> exchanges,
		boolean verifyDiscovery, Packaging packaging, Map<String, Point> layout) {

	/** The relay: an MQTT broker reachable from every member host (host name or address, and port). */
	public record Relay(String host, int port) {
	}

	/**
	 * One local cloud of the SoLC.
	 *
	 * @param cloud         cloud name (CN part of every certificate: <System>.<cloud>.<organization>.arrowhead.eu)
	 * @param lcDesign      the stored LocalCloudConfigurator design it is built from, pinned at a revision
	 * @param host          host name or address of the machine it runs on
	 * @param architecture  CPU architecture of that machine (amd64, arm64)
	 * @param portOffset    added to every published port, for several clouds on one machine (0 on a machine of its own)
	 * @param intercloud    inter-cloud infrastructure this SoLC adds to the cloud on top of its local-cloud design: the
	 *                      policy's inter-cloud components (Gatekeeper, GateTunnel) the design lacks and what they require,
	 *                      resolved by this system from the LocalCloudConfigurator catalog; null or empty when none is added
	 */
	public record Member(String cloud, LcDesignRef lcDesign, String host, String architecture, int portOffset, List<String> intercloud) {

		public List<String> intercloudOrEmpty() {
			return intercloud == null ? List.of() : intercloud;
		}

		/** What the cloud runs: the components of its local-cloud design and the inter-cloud infrastructure added to it. */
		public List<String> components() {
			final Set<String> all = new LinkedHashSet<>();
			if (lcDesign != null && lcDesign.components() != null) {
				all.addAll(lcDesign.components());
			}
			all.addAll(intercloudOrEmpty());
			return List.copyOf(all);
		}

		/** Compose project of the member's deployment. */
		public String composeProject() {
			return "arrowhead-" + cloud.toLowerCase();
		}
	}

	/**
	 * Snapshot of a LocalCloudConfigurator design: its components (the catalog closure of its selection, defaults included)
	 * at the revision the operator chose, so a package is reproducible even if the stored design changes later.
	 */
	public record LcDesignRef(String id, String name, long revision, List<String> components) {
	}

	public record Exchange(String consumerCloud, String consumer, String providerCloud, String provider, String service,
			List<String> operations) {
	}

	/**
	 * @param imageMode      "build": the package pins the source and each host builds its images; "prebuilt": image archives
	 *                       per architecture, added by the host assembler
	 * @param architectures  target CPU architectures (prebuilt: the archives to assemble)
	 */
	public record Packaging(String imageMode, List<String> architectures) {
	}

	public record Point(int x, int y) {
	}

	public List<Member> membersOrEmpty() {
		return members == null ? List.of() : members;
	}

	public List<Exchange> exchangesOrEmpty() {
		return exchanges == null ? List.of() : exchanges;
	}
}
