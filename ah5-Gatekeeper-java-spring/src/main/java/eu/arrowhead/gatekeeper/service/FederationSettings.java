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

	private Map<String, String> peers = Map.of();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public FederationSettings() {
	}

	//-------------------------------------------------------------------------------------------------
	public FederationSettings(final String cloud, final String organization, final String profile, final String peerOrganizations) {
		this.cloud = cloud;
		this.organization = organization;
		this.profile = profile;
		this.peerOrganizations = peerOrganizations;
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
