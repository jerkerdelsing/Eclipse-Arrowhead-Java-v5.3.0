/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.security;

import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import jakarta.servlet.http.HttpServletRequest;

final class RequestPaths {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// decoded path without trailing slashes, as the native implementation compared it
	static String path(final HttpServletRequest request) {
		final String path = SecurityUtilities.getDecodedUri(request.getRequestURI());
		return path.length() > 1 ? path.replaceAll("/+$", "") : path;
	}

	//-------------------------------------------------------------------------------------------------
	static boolean isLiveness(final String path) {
		return GatekeeperConstants.HTTP_API_HEALTH.equals(path) || GatekeeperConstants.HTTP_API_LEGACY_HEALTH.equals(path);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private RequestPaths() {
		throw new UnsupportedOperationException();
	}
}
