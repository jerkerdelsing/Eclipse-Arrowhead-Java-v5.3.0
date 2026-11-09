/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.security;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.gatetunnel.GateTunnelConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs right after the authentication filter (which verifies the identity token and binds it to the client certificate)
// and keeps the 5.2.2 caller rules:
// - only system ("sy") and operator ("op") certificates;
// - management paths (/mgmt/): LocalCloudManager and Sysop;
// - all other operations: Gatekeeper, DynamicServiceOrchestration, SimpleStoreServiceOrchestration, LocalCloudManager, Sysop;
// - inter-cloud sessions (intercloud/provider|consumer): the local Gatekeeper only.
// A caller whose identity Authentication marks as sysop is accepted like Sysop. Health needs authentication only.
@Component
@Order(GateTunnelConstants.REQUEST_FILTER_ORDER_CALLER)
public class GateTunnelCallerFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Autowired
	private SystemInfo sysInfo;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			check(request, sysInfo.isSslEnabled());
			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	static void check(final HttpServletRequest request, final boolean sslEnabled) {
		final String path = SecurityUtilities.getDecodedUri(request.getRequestURI());
		final String origin = request.getMethod() + " " + path;
		if (sslEnabled) {
			final X509Certificate[] chain = (X509Certificate[]) request.getAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);
			if (chain == null || chain.length == 0) {
				throw new AuthException("Mutual TLS is required", origin);
			}
			if (!GateTunnelConstants.CALLER_PROFILES.contains(TlsSupport.attribute(chain[0], "DNQ"))) {
				throw new ForbiddenException("Only system/operator certificates may call services", origin);
			}
		}

		final Object name = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (name == null || Constants.UNKNOWN.equals(name) || name.toString().isBlank()) {
			throw new AuthException("Arrowhead identity token is required", origin);
		}
		if (isHealth(path)) {
			return;
		}

		final String caller = name.toString();
		final boolean sysop = HttpUtilities.isSysop(request, origin);
		final List<String> allowed = path.contains("/mgmt/") ? GateTunnelConstants.MANAGEMENT_CALLERS : GateTunnelConstants.CONNECTION_CALLERS;
		if (!sysop && !allowed.contains(caller)) {
			throw new ForbiddenException("Caller is not authorized for this operation", origin);
		}
		if (isInterCloud(path) && !GateTunnelConstants.INTERCLOUD_CALLER.equals(caller)) {
			throw new ForbiddenException("Only the local Gatekeeper opens relay sessions", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static boolean isHealth(final String path) {
		final String trimmed = path.length() > 1 ? path.replaceAll("/+$", "") : path;
		return GateTunnelConstants.HTTP_API_OP_HEALTH.equals(trimmed) || GateTunnelConstants.HTTP_API_LEGACY_HEALTH.equals(trimmed);
	}

	//-------------------------------------------------------------------------------------------------
	private static boolean isInterCloud(final String path) {
		final String trimmed = path.replaceAll("/+$", "");
		return trimmed.endsWith(GateTunnelConstants.HTTP_API_OP_INTERCLOUD_PROVIDER) || trimmed.endsWith(GateTunnelConstants.HTTP_API_OP_INTERCLOUD_CONSUMER);
	}
}
