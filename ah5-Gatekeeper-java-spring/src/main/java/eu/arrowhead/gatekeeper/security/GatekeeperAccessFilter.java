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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// The native Gatekeeper's caller rules, after authentication (identity token bound to the client certificate):
//   management paths (/mgmt/):  LocalCloudManager and the system operator;
//   every other path:            the orchestrators, LocalCloudManager, Gatekeeper and the system operator.
// The common management filter (whitelist policy) checks management paths as well.
@Component
@Order(GatekeeperConstants.REQUEST_FILTER_ORDER_ACCESS)
@ConditionalOnProperty(name = "service.authorization.enabled", havingValue = "true", matchIfMissing = true)
public class GatekeeperAccessFilter extends ArrowheadFilter {

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			check(request);
			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	static void check(final HttpServletRequest request) {
		final String path = RequestPaths.path(request);
		if (RequestPaths.isLiveness(path)) {
			return;
		}

		final String origin = request.getMethod() + " " + request.getRequestURI();
		final Object name = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (name == null || Constants.UNKNOWN.equals(name) || name.toString().isBlank()) {
			throw new AuthException("Arrowhead identity token is required", origin);
		}

		final boolean sysop = HttpUtilities.isSysop(request, origin);
		final boolean management = path.contains(GatekeeperConstants.MGMT_PATH_SEGMENT);
		final boolean allowed = sysop || (management ? GatekeeperConstants.MANAGEMENT_CALLERS : GatekeeperConstants.SERVICE_CALLERS).contains(name.toString());
		if (!allowed) {
			throw new ForbiddenException("Caller is not authorized for this operation", origin);
		}
	}
}
