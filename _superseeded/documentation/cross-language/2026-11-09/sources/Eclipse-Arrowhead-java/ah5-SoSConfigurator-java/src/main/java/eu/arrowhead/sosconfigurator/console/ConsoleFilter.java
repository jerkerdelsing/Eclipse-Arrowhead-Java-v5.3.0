/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.console;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.SoSConfiguratorConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Separates the two connectors.
// Console connector (loopback, plain HTTP, browser): only the UI, the session and the sosConfiguration paths; Host/Origin must be
// local; /api needs a console session. Such requests are marked as already handled by the Arrowhead filter chain (identity
// token, management and authorization filters), because the operator authenticated with the console password instead.
// Arrowhead connector (mutual TLS): the console-only paths do not exist there.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ConsoleFilter extends OncePerRequestFilter {

	//=================================================================================================
	// members

	// "/" belongs to the common Swagger redirect on the Arrowhead connector; on the console it leads to the UI
	private static final Set<String> UI_PATHS = Set.of("/index.html", "/app.js", "/healthz");

	@Autowired
	private ConsoleAuth auth;

	@Autowired
	private ApplicationContext context;

	@Value(SoSConfiguratorConstants.$CONSOLE_PORT_WD)
	private int consolePort;

	private volatile Set<String> arrowheadFilterNames;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
		final String path = request.getRequestURI();
		final boolean consoleOnly = UI_PATHS.contains(path) || SoSConfiguratorConstants.HTTP_API_SESSION_PATH.equals(path);
		if (request.getLocalPort() != consolePort) {
			if (consoleOnly) {
				error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
				return;
			}

			chain.doFilter(request, response);
			return;
		}

		if (!auth.hostAllowed(request.getHeader("Host"), request.getHeader("Origin"))) {
			error(response, HttpServletResponse.SC_FORBIDDEN, "host not allowed");
			return;
		}

		if ("/".equals(path)) {
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setHeader("Location", "/index.html");
			return;
		}

		if (!consoleOnly && !path.startsWith(SoSConfiguratorConstants.HTTP_API_BASE_PATH + "/")) {
			error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
			return;
		}

		if (!consoleOnly) {
			if (!auth.valid(session(request))) {
				error(response, HttpServletResponse.SC_UNAUTHORIZED, "console login required");
				return;
			}

			request.setAttribute(SoSConfiguratorConstants.HTTP_ATTR_CONSOLE_OPERATOR, true);
		}

		for (final String name : arrowheadFilterNames()) {
			request.setAttribute(name + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		}

		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'");
		chain.doFilter(request, response);
	}

	//-------------------------------------------------------------------------------------------------
	static String session(final HttpServletRequest request) {
		final Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		for (final Cookie cookie : cookies) {
			if (SoSConfiguratorConstants.CONSOLE_COOKIE.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private Set<String> arrowheadFilterNames() {
		Set<String> names = arrowheadFilterNames;
		if (names == null) {
			final Map<String, ArrowheadFilter> filters = context.getBeansOfType(ArrowheadFilter.class);
			names = Set.copyOf(filters.keySet());
			arrowheadFilterNames = names;
		}

		return names;
	}

	//-------------------------------------------------------------------------------------------------
	private static void error(final HttpServletResponse response, final int status, final String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader("Cache-Control", "no-store");
		response.getOutputStream().write(Json.write(Map.of("error", message)).getBytes(StandardCharsets.UTF_8));
	}
}
