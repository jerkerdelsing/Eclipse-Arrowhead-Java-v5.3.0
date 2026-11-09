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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.gatekeeper.GatekeeperConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Outermost: the native response headers on every answer, and the native "error" field in every JSON error body. The
// common filters answer with the Arrowhead ErrorMessageDTO; its fields are kept and "error" (the message) is added, so
// clients of the native {"error": ...} body keep working.
@Component
@Order(GatekeeperConstants.REQUEST_FILTER_ORDER_LEGACY_ERROR_BODY)
public class NativeResponseFilter extends OncePerRequestFilter {

	//=================================================================================================
	// members

	private final ObjectMapper mapper = new ObjectMapper();

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("X-Content-Type-Options", "nosniff");
		final ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
		try {
			chain.doFilter(request, wrapper);
		} finally {
			if (wrapper.getStatus() >= HttpServletResponse.SC_BAD_REQUEST) {
				addErrorField(wrapper);
			}

			wrapper.copyBodyToResponse();
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private void addErrorField(final ContentCachingResponseWrapper wrapper) {
		final byte[] body = wrapper.getContentAsByteArray();
		if (body.length == 0 || body[0] != '{') {
			return;
		}

		try {
			final Map<String, Object> error = mapper.readValue(body, LinkedHashMap.class);
			if (!error.containsKey("error") && error.get("errorMessage") != null) {
				final Map<String, Object> amended = new LinkedHashMap<>();
				amended.put("error", error.get("errorMessage"));
				amended.putAll(error);
				wrapper.resetBuffer();
				wrapper.getOutputStream().write(mapper.writeValueAsBytes(amended));
			}
		} catch (final Exception ex) {
			// not a JSON object: left as it is
		}
	}
}
