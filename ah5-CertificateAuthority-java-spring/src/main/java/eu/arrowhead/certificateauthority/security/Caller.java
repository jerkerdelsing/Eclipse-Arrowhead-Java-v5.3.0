/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.security;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;

// The system name the authentication filter verified for this request.
public final class Caller {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static String of(final HttpServletRequest request, final String origin) {
		final Object name = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (name == null || Constants.UNKNOWN.equals(name) || name.toString().isBlank()) {
			throw new AuthException("Arrowhead identity token is required", origin);
		}

		return name.toString();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Caller() {
		throw new UnsupportedOperationException();
	}
}
