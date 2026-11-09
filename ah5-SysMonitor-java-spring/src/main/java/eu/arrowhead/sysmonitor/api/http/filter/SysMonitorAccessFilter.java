/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.api.http.filter;

import java.io.IOException;

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
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.service.HistoryAuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// The access rules of the native implementation, after authentication:
// - GET sysMonitorHistory operations: ConsumerAuthorization decides (service.authorization.enabled), target sysMonitorHistory,
//   scope list-systems, latest or query;
// - every other /sysmonitor path except generalManagement (/sysmonitor/mgmt/, guarded by the common management filter):
//   LocalCloudManager or Sysop only.
@Component
@Order(SysMonitorConstants.REQUEST_FILTER_ORDER_ACCESS)
public class SysMonitorAccessFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	private static final String HISTORY_PREFIX = SysMonitorConstants.HTTP_API_HISTORY_PATH + "/";
	private static final String GENERAL_MANAGEMENT_PREFIX = SysMonitorConstants.HTTP_API_BASE_PATH + "/mgmt/";
	private static final int SYSTEM_OPERATION_PARTS = 3;

	@Autowired
	private HistoryAuthorizationService authorizationService;

	@Value(SysMonitorConstants.$SERVICE_AUTHORIZATION_ENABLED_WD)
	private boolean authorizationEnabled;

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
	void check(final HttpServletRequest request) {
		final String path = SecurityUtilities.getDecodedUri(request.getRequestURI());
		if (!path.startsWith(SysMonitorConstants.HTTP_API_BASE_PATH + "/") || path.startsWith(GENERAL_MANAGEMENT_PREFIX)) {
			return;
		}

		final String origin = request.getMethod() + " " + path;
		final String caller = caller(request, origin);
		if ("GET".equals(request.getMethod()) && path.startsWith(HISTORY_PREFIX)) {
			final String operation = historyOperation(path.substring(HISTORY_PREFIX.length()));
			if (operation == null) {
				return; // not a history operation: 404
			}

			if (authorizationEnabled && !authorizationService.isAuthorized(caller, operation)) {
				throw new ForbiddenException("History operation not authorized", origin);
			}

			return;
		}

		if (!HttpUtilities.isSysop(request, origin) && !SysMonitorConstants.MANAGERS.contains(caller)) {
			throw new ForbiddenException("Management requires LocalCloudManager or Sysop", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// sysMonitorHistory operation of a path below /sysmonitor/history/, or null
	static String historyOperation(final String rest) {
		if (rest.equals("systems")) {
			return SysMonitorConstants.OP_LIST_SYSTEMS;
		}

		final String[] parts = rest.split("/");
		if (parts.length == SYSTEM_OPERATION_PARTS && parts[0].equals("systems") && !parts[1].isEmpty()) {
			if (parts[2].equals("latest")) {
				return SysMonitorConstants.OP_LATEST;
			}

			if (parts[2].equals("observations")) {
				return SysMonitorConstants.OP_QUERY;
			}
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private static String caller(final HttpServletRequest request, final String origin) {
		final Object name = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (name == null || Constants.UNKNOWN.equals(name) || name.toString().isBlank()) {
			throw new AuthException("Arrowhead identity token is required", origin);
		}

		return name.toString();
	}
}
