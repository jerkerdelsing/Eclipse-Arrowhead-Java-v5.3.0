/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.api.http;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;

// Response and request helpers shared by the SysMonitor controllers.
final class Responses {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	static <T> ResponseEntity<T> json(final int status, final T body) {
		return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON).body(body);
	}

	//-------------------------------------------------------------------------------------------------
	static <T> ResponseEntity<T> json(final HttpStatus status, final T body) {
		return json(status.value(), body);
	}

	//-------------------------------------------------------------------------------------------------
	static String origin(final HttpServletRequest request) {
		return request.getMethod() + " " + request.getRequestURI();
	}

	//-------------------------------------------------------------------------------------------------
	// authenticated system name (set by the authentication filter)
	static String caller(final HttpServletRequest request) {
		final Object name = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (name == null || Constants.UNKNOWN.equals(name) || name.toString().isBlank()) {
			throw new AuthException("Arrowhead identity token is required", origin(request));
		}

		return name.toString();
	}

	//-------------------------------------------------------------------------------------------------
	private Responses() {
		throw new UnsupportedOperationException();
	}
}
