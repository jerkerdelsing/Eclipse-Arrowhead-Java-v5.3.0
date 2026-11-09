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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Explicit cross-organisation bindings (FEDERATION_PEER_ORGANIZATIONS); an ordinary neighbour row is not enough.
// Unchanged from 5.2.2 and identical to the Go and Rust implementations.
public final class PeerNames {

	//=================================================================================================
	// members

	private static final String NAME_PATTERN = "[A-Za-z][A-Za-z0-9_]{0,62}";
	private static final int MAX_BINDINGS = 32;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static Map<String, String> parse(final String raw) {
		final Map<String, String> result = new LinkedHashMap<>();
		if (raw.isBlank()) {
			return result;
		}

		for (final String item : raw.split(",", -1)) {
			final String[] pair = item.trim().split("=", -1);
			if (pair.length != 2
					|| !pair[0].matches(NAME_PATTERN)
					|| !pair[1].matches(NAME_PATTERN)
					|| result.putIfAbsent(pair[0], pair[1]) != null
					|| result.size() > MAX_BINDINGS) {
				throw new IllegalArgumentException("Invalid explicit federation peer organisation mapping");
			}
		}

		return Collections.unmodifiableMap(result);
	}

	//-------------------------------------------------------------------------------------------------
	// "GateTunnel.<Cloud>.<Organisation>.arrowhead.eu" -> "<Cloud>|<Organisation>"
	public static String cloudIdentity(final String peer) {
		final String[] parts = peer.split("\\.");
		if (parts.length != 5) {
			throw new IllegalArgumentException("Invalid peer name");
		}

		return parts[1] + "|" + parts[2];
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private PeerNames() {
		throw new UnsupportedOperationException();
	}
}
