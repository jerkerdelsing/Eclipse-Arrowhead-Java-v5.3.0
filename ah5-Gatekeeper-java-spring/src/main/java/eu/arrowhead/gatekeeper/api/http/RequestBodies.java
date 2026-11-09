/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.api.http;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.parse;

import java.io.IOException;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.service.ApiError;
import jakarta.servlet.http.HttpServletRequest;

final class RequestBodies {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// JSON body of at most 1 MiB (413 otherwise); an empty body is {}
	static Object json(final HttpServletRequest request) throws IOException {
		final byte[] body = request.getInputStream().readNBytes(GatekeeperConstants.MAX_REQUEST_BYTES + 1);
		if (body.length > GatekeeperConstants.MAX_REQUEST_BYTES) {
			throw new ApiError(413, "Request exceeds 1 MiB");
		}

		return body.length == 0 ? m() : parse(body);
	}

	//-------------------------------------------------------------------------------------------------
	// the system the authentication filter verified
	static String caller(final HttpServletRequest request) {
		final Object name = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (name == null || Constants.UNKNOWN.equals(name) || name.toString().isBlank()) {
			throw new AuthException("Arrowhead identity token is required", request.getMethod() + " " + request.getRequestURI());
		}

		return name.toString();
	}

	//-------------------------------------------------------------------------------------------------
	static boolean sysop(final HttpServletRequest request) {
		return Boolean.TRUE.equals(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private RequestBodies() {
		throw new UnsupportedOperationException();
	}
}
