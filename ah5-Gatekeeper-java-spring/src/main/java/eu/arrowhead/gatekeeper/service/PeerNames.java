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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Explicit cross-organisation bindings; an ordinary neighbour row is not enough. Unchanged from the native 5.2.2 class.
public final class PeerNames {

	//=================================================================================================
	// members

	private static final String NAME = "[A-Za-z][A-Za-z0-9_]{0,62}";
	private static final int MAX_PEERS = 32;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// "<cloud>=<organization>,..."; blank means no explicit binding
	public static Map<String, String> parse(final String raw) {
		final Map<String, String> result = new LinkedHashMap<>();
		if (raw == null || raw.isBlank()) {
			return result;
		}

		for (final String item : raw.split(",", -1)) {
			final String[] pair = item.trim().split("=", -1);
			if (pair.length != 2
					|| !pair[0].matches(NAME)
					|| !pair[1].matches(NAME)
					|| result.putIfAbsent(pair[0], pair[1]) != null
					|| result.size() > MAX_PEERS) {
				throw new IllegalArgumentException("Invalid explicit federation peer organisation mapping");
			}
		}

		return Collections.unmodifiableMap(result);
	}

	//-------------------------------------------------------------------------------------------------
	// organisation of a peer cloud: the explicit binding, else the own organisation
	public static String organization(final Map<String, String> peers, final String ownCloud, final String ownOrganization, final String cloud) {
		if (peers.containsKey(ownCloud) && !peers.get(ownCloud).equals(ownOrganization)) {
			throw new IllegalArgumentException("Own organisation cannot be remapped");
		}

		return peers.getOrDefault(cloud, ownOrganization);
	}

	//-------------------------------------------------------------------------------------------------
	// "<Cloud>|<Organization>" of a five-part peer common name
	public static String cloudIdentity(final String peer) {
		final String[] p = peer.split("\\.");
		if (p.length != 5) {
			throw new IllegalArgumentException("Invalid peer name");
		}

		return p[1] + "|" + p[2];
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private PeerNames() {
		throw new UnsupportedOperationException();
	}
}
