/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.api.http;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import eu.arrowhead.gatetunnel.GateTunnelConstants;
import eu.arrowhead.gatetunnel.service.GateTunnelApiException;
import eu.arrowhead.gatetunnel.service.Wire;

// Request bodies are read as bytes, whatever the Content-Type (as in 5.2.2): empty is {}, more than 1 MiB is 413,
// anything but JSON is 400.
final class ApiSupport {

	//=================================================================================================
	// members

	static final String BASE = GateTunnelConstants.HTTP_API_BASE_PATH;
	static final String LEGACY = GateTunnelConstants.HTTP_API_LEGACY_BASE_PATH;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	static Object body(final byte[] raw) {
		if (raw == null || raw.length == 0) {
			return Wire.m();
		}
		if (raw.length > GateTunnelConstants.MAX_BODY_BYTES) {
			throw new GateTunnelApiException(413, "Request exceeds 1 MiB");
		}

		return Wire.parse(raw);
	}

	//-------------------------------------------------------------------------------------------------
	static Map<String, Object> payload(final byte[] raw) {
		return Wire.obj(body(raw));
	}

	//-------------------------------------------------------------------------------------------------
	static <T> ResponseEntity<T> json(final HttpStatus status, final T body) {
		return json(status.value(), body);
	}

	//-------------------------------------------------------------------------------------------------
	static <T> ResponseEntity<T> json(final int status, final T body) {
		return ResponseEntity.status(status)
				.cacheControl(CacheControl.noStore())
				.header("X-Content-Type-Options", "nosniff")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body);
	}

	//-------------------------------------------------------------------------------------------------
	private ApiSupport() {
		throw new UnsupportedOperationException();
	}
}
