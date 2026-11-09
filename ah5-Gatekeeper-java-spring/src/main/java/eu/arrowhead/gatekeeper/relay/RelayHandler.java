/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.relay;

import java.util.Map;

// The provider side of the relay protocol: which peers are configured neighbours, and the answer to their requests.
public interface RelayHandler {

	//-------------------------------------------------------------------------------------------------
	boolean trustedPeer(final String peerCommonName);

	//-------------------------------------------------------------------------------------------------
	Map<String, Object> incoming(final String peerCommonName, final Map<String, Object> message) throws Exception;
}
