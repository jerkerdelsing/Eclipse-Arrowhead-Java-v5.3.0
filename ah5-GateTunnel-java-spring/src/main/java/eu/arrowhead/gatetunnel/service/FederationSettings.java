/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.gatetunnel.GateTunnelConstants;
import eu.arrowhead.gatetunnel.GateTunnelDefaults;

// Federation and session settings. The environment names of 5.2.2 (FEDERATION_PROFILE, NEIGHBOR_CLOUDS, RELAY_*, ...) are
// mapped onto these keys in application.properties. Invalid settings refuse the start.
@Component
public class FederationSettings {

	//=================================================================================================
	// members

	private final String cloud;
	private final String organization;
	private final String profile;
	private final Set<String> neighborClouds;
	private final Map<String, String> peerOrganizations;
	private final String advertisedAddress;
	private final int sessionMaxAgeSeconds;
	private final int sessionIdleSeconds;
	private final int clockSkewSeconds;
	private final int interCloudMinPort;
	private final int interCloudMaxPort;
	private final int controlMinPort;
	private final int controlMaxPort;
	private final String relayHost;
	private final int relayPort;
	private final String relayChainCert;
	private final String relayKey;
	private final String relayCaCert;
	private final String crlFile;
	private final List<String> relayCrlFiles;
	private final String relayCrlFile;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumber")
	public FederationSettings(
			@Value("${" + GateTunnelConstants.LOCAL_CLOUD + ":" + GateTunnelDefaults.GATETUNNEL_LOCAL_CLOUD_DEFAULT + "}") final String cloud,
			@Value("${" + GateTunnelConstants.ORGANIZATION + ":" + GateTunnelDefaults.GATETUNNEL_ORGANIZATION_DEFAULT + "}") final String organization,
			@Value("${" + GateTunnelConstants.FEDERATION_PROFILE + ":" + GateTunnelDefaults.GATETUNNEL_FEDERATION_PROFILE_DEFAULT + "}") final String profile,
			@Value("${" + GateTunnelConstants.NEIGHBOR_CLOUDS + ":" + GateTunnelDefaults.GATETUNNEL_NEIGHBOR_CLOUDS_DEFAULT + "}") final String neighborClouds,
			@Value("${" + GateTunnelConstants.PEER_ORGANIZATIONS + ":}") final String peerOrganizations,
			@Value("${" + GateTunnelConstants.ADVERTISED_ADDRESS + ":" + GateTunnelDefaults.GATETUNNEL_ADVERTISED_ADDRESS_DEFAULT + "}") final String advertisedAddress,
			@Value("${" + GateTunnelConstants.SESSION_MAX_AGE_SECONDS + ":" + GateTunnelDefaults.GATETUNNEL_SESSION_MAX_AGE_SECONDS_DEFAULT + "}") final int sessionMaxAgeSeconds,
			@Value("${" + GateTunnelConstants.SESSION_IDLE_SECONDS + ":" + GateTunnelDefaults.GATETUNNEL_SESSION_IDLE_SECONDS_DEFAULT + "}") final int sessionIdleSeconds,
			@Value("${" + GateTunnelConstants.SESSION_CLOCK_SKEW_SECONDS + ":" + GateTunnelDefaults.GATETUNNEL_SESSION_CLOCK_SKEW_SECONDS_DEFAULT + "}") final int clockSkewSeconds,
			@Value("${" + GateTunnelConstants.INTERCLOUD_MIN_PORT + ":" + GateTunnelDefaults.GATETUNNEL_INTERCLOUD_MIN_PORT_DEFAULT + "}") final int interCloudMinPort,
			@Value("${" + GateTunnelConstants.INTERCLOUD_MAX_PORT + ":" + GateTunnelDefaults.GATETUNNEL_INTERCLOUD_MAX_PORT_DEFAULT + "}") final int interCloudMaxPort,
			@Value("${" + GateTunnelConstants.CONTROL_MIN_PORT + ":" + GateTunnelDefaults.GATETUNNEL_CONTROL_MIN_PORT_DEFAULT + "}") final int controlMinPort,
			@Value("${" + GateTunnelConstants.CONTROL_MAX_PORT + ":" + GateTunnelDefaults.GATETUNNEL_CONTROL_MAX_PORT_DEFAULT + "}") final int controlMaxPort,
			@Value("${" + GateTunnelConstants.RELAY_HOST + ":" + GateTunnelDefaults.GATETUNNEL_RELAY_HOST_DEFAULT + "}") final String relayHost,
			@Value("${" + GateTunnelConstants.RELAY_PORT + ":" + GateTunnelDefaults.GATETUNNEL_RELAY_PORT_DEFAULT + "}") final int relayPort,
			@Value("${" + GateTunnelConstants.RELAY_CHAIN_CERT + ":" + GateTunnelDefaults.GATETUNNEL_RELAY_CHAIN_CERT_DEFAULT + "}") final String relayChainCert,
			@Value("${" + GateTunnelConstants.RELAY_KEY + ":" + GateTunnelDefaults.GATETUNNEL_RELAY_KEY_DEFAULT + "}") final String relayKey,
			@Value("${" + GateTunnelConstants.RELAY_CA_CERT + ":" + GateTunnelDefaults.GATETUNNEL_RELAY_CA_CERT_DEFAULT + "}") final String relayCaCert,
			@Value("${" + GateTunnelConstants.CRL_FILE + ":}") final String crlFile,
			@Value("${" + GateTunnelConstants.RELAY_CRL_FILES + ":}") final String relayCrlFiles,
			@Value("${" + GateTunnelConstants.RELAY_CRL_FILE + ":}") final String relayCrlFile) {
		if (!Set.of(GateTunnelConstants.PROFILE_LEGACY_MTLS, GateTunnelConstants.PROFILE_AH_GT_1).contains(profile)) {
			throw new IllegalArgumentException("Unsupported federation profile");
		}

		this.cloud = cloud;
		this.organization = organization;
		this.profile = profile;
		this.neighborClouds = Wire.csv(neighborClouds);
		this.peerOrganizations = PeerNames.parse(peerOrganizations == null ? "" : peerOrganizations);
		if (this.peerOrganizations.containsKey(cloud) && !this.peerOrganizations.get(cloud).equals(organization)) {
			throw new IllegalArgumentException("Own organisation cannot be remapped");
		}
		this.advertisedAddress = advertisedAddress;
		this.sessionMaxAgeSeconds = sessionMaxAgeSeconds;
		this.sessionIdleSeconds = sessionIdleSeconds;
		this.clockSkewSeconds = clockSkewSeconds;
		this.interCloudMinPort = interCloudMinPort;
		this.interCloudMaxPort = interCloudMaxPort;
		this.controlMinPort = controlMinPort;
		this.controlMaxPort = controlMaxPort;
		this.relayHost = relayHost;
		this.relayPort = relayPort;
		this.relayChainCert = relayChainCert;
		this.relayKey = relayKey;
		this.relayCaCert = relayCaCert;
		this.crlFile = crlFile;
		this.relayCrlFiles = List.copyOf(Wire.csv(relayCrlFiles == null ? "" : relayCrlFiles));
		// SOS-6: the relay's server certificate (organisation CA) is checked against the organisation CRL; no unchecked relay
		if (relayCrlFile == null || relayCrlFile.isBlank()) {
			throw new IllegalArgumentException(GateTunnelConstants.RELAY_CRL_FILE + " (the organisation CA's CRL) is required");
		}
		this.relayCrlFile = relayCrlFile.trim();
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isEndToEnd() {
		return GateTunnelConstants.PROFILE_AH_GT_1.equals(profile);
	}

	//-------------------------------------------------------------------------------------------------
	public String commonName(final String system) {
		return system + "." + cloud + "." + organization + ".arrowhead.eu";
	}

	//-------------------------------------------------------------------------------------------------
	public String ownCommonName() {
		return commonName(GateTunnelConstants.SYSTEM_NAME);
	}

	//-------------------------------------------------------------------------------------------------
	// The organisation of a peer cloud: an explicit binding, otherwise the own organisation.
	public String organizationOf(final String peerCloud) {
		return peerOrganizations.getOrDefault(peerCloud, organization);
	}

	//-------------------------------------------------------------------------------------------------
	// A configured neighbour's GateTunnel (exact 5-label relay identity, not the own cloud).
	public boolean trustedPeer(final String peer) {
		final String[] parts = peer.split("\\.");
		return parts.length == 5
				&& peer.equals(GateTunnelConstants.SYSTEM_NAME + "." + parts[1] + "." + organizationOf(parts[1]) + ".arrowhead.eu")
				&& !parts[1].equals(cloud)
				&& neighborClouds.contains(parts[1]);
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

	//-------------------------------------------------------------------------------------------------
	public String getAdvertisedAddress() {
		return advertisedAddress;
	}

	//-------------------------------------------------------------------------------------------------
	public int getSessionMaxAgeSeconds() {
		return sessionMaxAgeSeconds;
	}

	//-------------------------------------------------------------------------------------------------
	public int getSessionIdleSeconds() {
		return sessionIdleSeconds;
	}

	//-------------------------------------------------------------------------------------------------
	public int getClockSkewSeconds() {
		return clockSkewSeconds;
	}

	//-------------------------------------------------------------------------------------------------
	public int getInterCloudMinPort() {
		return interCloudMinPort;
	}

	//-------------------------------------------------------------------------------------------------
	public int getInterCloudMaxPort() {
		return interCloudMaxPort;
	}

	//-------------------------------------------------------------------------------------------------
	public int getControlMinPort() {
		return controlMinPort;
	}

	//-------------------------------------------------------------------------------------------------
	public int getControlMaxPort() {
		return controlMaxPort;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRelayHost() {
		return relayHost;
	}

	//-------------------------------------------------------------------------------------------------
	public int getRelayPort() {
		return relayPort;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRelayChainCert() {
		return relayChainCert;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRelayKey() {
		return relayKey;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRelayCaCert() {
		return relayCaCert;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCrlFile() {
		return crlFile;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getRelayCrlFiles() {
		return relayCrlFiles;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRelayCrlFile() {
		return relayCrlFile;
	}
}
