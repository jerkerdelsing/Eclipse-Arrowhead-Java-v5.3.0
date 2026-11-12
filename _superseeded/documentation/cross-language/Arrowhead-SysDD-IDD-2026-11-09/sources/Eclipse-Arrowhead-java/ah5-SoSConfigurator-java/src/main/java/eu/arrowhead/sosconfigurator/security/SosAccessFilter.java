/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.sosconfigurator.SoSConfiguratorConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs after the authentication filter on the Arrowhead connector. A sosConfiguration operation changes several local clouds, so
// only Sysop, the systems of sos.api.whitelist, and systems with a ConsumerAuthorization grant for the operation may call it.
// (Console requests are authenticated by the console password and skip this filter; see ConsoleFilter.)
@Component
@Order(SoSConfiguratorConstants.REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION)
public class SosAccessFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Autowired
	private SosAuthorizationService authorizationService;

	@Value(SoSConfiguratorConstants.$SERVICE_AUTHORIZATION_ENABLED_WD)
	private boolean authorizationEnabled;

	private final Set<String> whitelist;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SosAccessFilter(@Value(SoSConfiguratorConstants.$API_WHITELIST_WD) final String whitelist) {
		this.whitelist = Arrays.stream(whitelist.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
	}

	//-------------------------------------------------------------------------------------------------
	// the sosConfiguration operation of a request, or null for any other path
	static String operation(final String method, final String path) {
		final String base = SoSConfiguratorConstants.HTTP_API_BASE_PATH;
		if ("GET".equals(method) && (base + SoSConfiguratorConstants.HTTP_API_OP_CATALOG_PATH).equals(path)) {
			return SoSConfiguratorConstants.SERVICE_OP_CATALOG;
		}

		if ("GET".equals(method) && (base + SoSConfiguratorConstants.HTTP_API_OP_STATUS_PATH).equals(path)) {
			return SoSConfiguratorConstants.SERVICE_OP_STATUS;
		}

		if ("POST".equals(method) && (base + SoSConfiguratorConstants.HTTP_API_OP_PLAN_PATH).equals(path)) {
			return SoSConfiguratorConstants.SERVICE_OP_PLAN;
		}

		final String deployments = base + SoSConfiguratorConstants.HTTP_API_OP_DEPLOYMENTS_PATH;
		if (deployments.equals(path)) {
			return "POST".equals(method) ? SoSConfiguratorConstants.SERVICE_OP_SUBMIT_DEPLOYMENT : "GET".equals(method) ? SoSConfiguratorConstants.SERVICE_OP_LIST_DEPLOYMENTS : null;
		}

		if ("GET".equals(method) && path.startsWith(deployments + "/") && path.indexOf('/', deployments.length() + 1) < 0) {
			return SoSConfiguratorConstants.SERVICE_OP_GET_DEPLOYMENT;
		}

		return null;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			final String operation = operation(request.getMethod(), request.getRequestURI());
			if (operation != null) {
				check(request, operation);
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void check(final HttpServletRequest request, final String operation) {
		final String origin = request.getMethod() + " " + request.getRequestURI();
		final Object system = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (system == null || Constants.UNKNOWN.equals(system)) {
			throw new AuthException("No authenticated Arrowhead system", origin);
		}

		final String consumer = system.toString();
		if (HttpUtilities.isSysop(request, origin) || whitelist.contains(consumer)) {
			return;
		}

		if (!authorizationEnabled) {
			// development profile only: the security profile guard refuses service.authorization.enabled=false otherwise
			return;
		}

		if (!authorizationService.isAuthorized(consumer, SoSConfiguratorConstants.SERVICE_DEF_SOS_CONFIGURATION, operation)) {
			throw new ForbiddenException(consumer + " is not authorized for " + SoSConfiguratorConstants.SERVICE_DEF_SOS_CONFIGURATION + "." + operation, origin);
		}
	}
}
