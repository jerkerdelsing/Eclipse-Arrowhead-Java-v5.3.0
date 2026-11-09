/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel;

import eu.arrowhead.common.Defaults;

// Field names follow SystemInfo's rule (key upper-cased, '.' -> '_', '-' -> '__', suffix _DEFAULT) for the public config.
public final class GateTunnelDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String GATETUNNEL_FEDERATION_PROFILE_DEFAULT = GateTunnelConstants.PROFILE_LEGACY_MTLS;
	public static final String GATETUNNEL_NEIGHBOR_CLOUDS_DEFAULT = "GoCloud";
	public static final String GATETUNNEL_FEDERATION_PEER_ORGANIZATIONS_DEFAULT = "";
	public static final String GATETUNNEL_LOCAL_CLOUD_DEFAULT = "TestCloud";
	public static final String GATETUNNEL_ORGANIZATION_DEFAULT = "Company";
	public static final String GATETUNNEL_ADVERTISED_ADDRESS_DEFAULT = "gatetunnel";
	public static final int GATETUNNEL_SESSION_MAX_AGE_SECONDS_DEFAULT = 43200;
	public static final int GATETUNNEL_SESSION_IDLE_SECONDS_DEFAULT = 3600;
	public static final int GATETUNNEL_SESSION_CLOCK_SKEW_SECONDS_DEFAULT = 120;
	public static final int GATETUNNEL_INTERCLOUD_MIN_PORT_DEFAULT = 8200;
	public static final int GATETUNNEL_INTERCLOUD_MAX_PORT_DEFAULT = 8299;
	public static final int GATETUNNEL_CONTROL_MIN_PORT_DEFAULT = 8000;
	public static final int GATETUNNEL_CONTROL_MAX_PORT_DEFAULT = 8099;
	public static final String GATETUNNEL_RELAY_HOST_DEFAULT = "arrowhead-relay";
	public static final int GATETUNNEL_RELAY_PORT_DEFAULT = 8883;
	public static final String GATETUNNEL_RELAY_CHAIN_CERT_DEFAULT = "/certs/gatetunnel-relay-chain.crt";
	public static final String GATETUNNEL_RELAY_KEY_DEFAULT = "/certs/gatetunnel-relay.key";
	public static final String GATETUNNEL_RELAY_CA_CERT_DEFAULT = "/certs/org-ca.crt";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private GateTunnelDefaults() {
		throw new UnsupportedOperationException();
	}
}
