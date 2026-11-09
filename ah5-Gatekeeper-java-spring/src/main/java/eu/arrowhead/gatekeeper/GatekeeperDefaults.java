/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper;

import eu.arrowhead.common.Defaults;

public final class GatekeeperDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String LOCAL_CLOUD_DEFAULT = "TestCloud";
	public static final String ORGANIZATION_DEFAULT = "Company";
	public static final String FEDERATION_PROFILE_DEFAULT = GatekeeperConstants.FEDERATION_PROFILE_LEGACY;
	public static final String FEDERATION_PEER_ORGANIZATIONS_DEFAULT = "";
	public static final String RELAY_ENABLED_DEFAULT = "true";
	public static final String RELAY_HOST_DEFAULT = "arrowhead-relay";
	public static final String RELAY_PORT_DEFAULT = "8883";
	public static final String RELAY_CHAIN_CERT_DEFAULT = "/certs/gatekeeper-relay-chain.crt";
	public static final String RELAY_KEY_DEFAULT = "/certs/gatekeeper-relay.key";
	public static final String RELAY_CA_CERT_DEFAULT = "/certs/org-ca.crt";
	public static final String RELAY_CRL_FILE_DEFAULT = "";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private GatekeeperDefaults() {
		throw new UnsupportedOperationException();
	}
}
