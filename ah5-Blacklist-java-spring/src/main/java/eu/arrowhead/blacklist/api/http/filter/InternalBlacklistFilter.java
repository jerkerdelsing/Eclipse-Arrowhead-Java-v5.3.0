/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.blacklist.api.http.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.blacklist.BlacklistConstants;
import eu.arrowhead.blacklist.service.DiscoveryService;
import eu.arrowhead.blacklist.service.normalization.Normalization;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.security.SecurityUtilities;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Constants.REQUEST_FILTER_ORDER_AUTHORIZATION_BLACKLIST)
public class InternalBlacklistFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Autowired
	private DiscoveryService discoveryService;

	private static final String SLASH = "/";

	@Autowired
	private Normalization normalizer;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		log.debug("InternalBlacklistFilter is active");
		final int index = request.getRequestURL().indexOf(BlacklistConstants.HTTP_API_BASE_PATH);
		final String origin = request.getMethod() + " " + (index > 0 ? request.getRequestURL().substring(index) : request.getRequestURL());

		try {
			final String systemName = HttpUtilities.acquireName(request, origin);

			if (!HttpUtilities.isSysop(request, origin)
					&& !isSelfCheck(request, systemName)
					&& !isLookup(request)
					&& discoveryService.check(systemName, origin)) {
				throw new ForbiddenException(systemName + " system is blacklisted", origin);
			}
		} catch (final InvalidParameterException ex) {
			// no system name in attributes
			throw new AuthException("Unknown requester system", origin);
		}

		chain.doFilter(request, response);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isLookup(final HttpServletRequest request) {
		final String requestTarget = Utilities.stripEndSlash(SecurityUtilities.getDecodedUri(request.getRequestURL().toString()));
		if (requestTarget.endsWith(BlacklistConstants.HTTP_API_BASE_PATH + BlacklistConstants.HTTP_API_OP_LOOKUP)) {
			return true;
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isSelfCheck(final HttpServletRequest request, final String systemName) {
		final String requestTarget = Utilities.stripEndSlash(SecurityUtilities.getDecodedUri(request.getRequestURI().toString()));
		if (!requestTarget.contains(SLASH)) {
			return false;
		}

		// rebuilding the request target so it contains a normalized system name as path parameter
		final String path = requestTarget.substring(0, requestTarget.lastIndexOf(SLASH) + 1);
		final String pathParam = requestTarget.substring(requestTarget.lastIndexOf(SLASH) + 1);
		final String normalizedRequestTarget = path + normalizer.normalizeSystemName(pathParam);

		return normalizedRequestTarget.endsWith(BlacklistConstants.HTTP_API_BASE_PATH + BlacklistConstants.HTTP_API_OP_CHECK + SLASH + normalizer.normalizeSystemName(systemName));
	}
}