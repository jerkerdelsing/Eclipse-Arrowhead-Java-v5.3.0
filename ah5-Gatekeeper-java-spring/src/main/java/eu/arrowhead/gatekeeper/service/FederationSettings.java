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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.GatekeeperDefaults;
import jakarta.annotation.PostConstruct;

// Own cloud identity and federation profile. Checked at start-up: an unknown profile or an invalid peer mapping refuses to start.
@Component
public class FederationSettings {

	//=================================================================================================
	// members

	@Value("${" + GatekeeperConstants.LOCAL_CLOUD + ":" + GatekeeperDefaults.LOCAL_CLOUD_DEFAULT + "}")
	private String cloud;

	@Value("${" + GatekeeperConstants.ORGANIZATION + ":" + GatekeeperDefaults.ORGANIZATION_DEFAULT + "}")
	private String organization;

	@Value("${" + GatekeeperConstants.FEDERATION_PROFILE + ":" + GatekeeperDefaults.FEDERATION_PROFILE_DEFAULT + "}")
	private String profile;

	@Value("${" + GatekeeperConstants.FEDERATION_PEER_ORGANIZATIONS + ":" + GatekeeperDefaults.FEDERATION_PEER_ORGANIZATIONS_DEFAULT + "}")
	private String peerOrganizations;

	// SOS-7 (2026-11-10): consumer-side inter-cloud requests reach neighbours of the own organisation, and neighbours of
	// another organisation only when listed here ("Cloud|Organisation", matching FEDERATION_PEER_ORGANIZATIONS)
	@Value("${" + GatekeeperConstants.FEDERATION_FOREIGN_TARGETS + ":" + GatekeeperDefaults.FEDERATION_FOREIGN_TARGETS_DEFAULT + "}")
	private String foreignTargets;

	private Map<String, String> peers = Map.of();
	private Set<String> allowedForeign = Set.of();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public FederationSettings() {
	}

	//-------------------------------------------------------------------------------------------------
	public FederationSettings(final String cloud, final String organization, final String profile, final String peerOrganizations) {
		this(cloud, organization, profile, peerOrganizations, "");
	}

	//-------------------------------------------------------------------------------------------------
	public FederationSettings(final String cloud, final String organization, final String profile, final String peerOrganizations, final String foreignTargets) {
		this.cloud = cloud;
		this.organization = organization;
		this.profile = profile;
		this.peerOrganizations = peerOrganizations;
		this.foreignTargets = foreignTargets;
		init();
	}

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	public void init() {
		if (!GatekeeperConstants.FEDERATION_PROFILES.contains(profile)) {
			throw new IllegalArgumentException("Unsupported federation profile: legacy-mtls or ah-gt-1 (J5); never silently downgrade an encrypted peer");
		}

		peers = PeerNames.parse(peerOrganizations);
		organizationOf(cloud); // the own organisation cannot be remapped
		final Set<String> allowed = new TreeSet<>();
		for (final String raw : (foreignTargets == null ? "" : foreignTargets).split(",")) {
			final String entry = raw.trim();
			if (entry.isEmpty()) {
				continue;
			}
			final String[] part = entry.split("\\|", -1);
			if (part.length != 2 || part[0].isBlank() || part[0].equals(cloud) || part[1].equals(organization) || !part[1].equals(organizationOf(part[0]))) {
				throw new IllegalArgumentException(GatekeeperConstants.FEDERATION_FOREIGN_TARGETS + ": \"" + entry
						+ "\" is not Cloud|Organisation of a neighbour of another organisation bound in " + GatekeeperConstants.FEDERATION_PEER_ORGANIZATIONS);
			}
			allowed.add(entry);
		}
		allowedForeign = Set.copyOf(allowed);
	}

	//-------------------------------------------------------------------------------------------------
	// SOS-7: whether this cloud's consumers may reach the neighbour (own organisation, or listed in foreign-targets)
	public boolean targetAllowed(final String peerCloud) {
		final String peerOrganization = organizationOf(peerCloud);
		return peerOrganization.equals(organization) || allowedForeign.contains(peerCloud + "|" + peerOrganization);
	}

	//-------------------------------------------------------------------------------------------------
	public String organizationOf(final String peerCloud) {
		return PeerNames.organization(peers, cloud, organization, peerCloud);
	}

	//-------------------------------------------------------------------------------------------------
	public String commonName(final String system) {
		return system + "." + cloud + "." + organization + ".arrowhead.eu";
	}

	//-------------------------------------------------------------------------------------------------
	public boolean endToEndEncrypted() {
		return GatekeeperConstants.FEDERATION_PROFILE_AH_GT_1.equals(profile);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getCloud() {
		return cloud;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOrganization() {
		return organization;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProfile() {
		return profile;
	}
}
