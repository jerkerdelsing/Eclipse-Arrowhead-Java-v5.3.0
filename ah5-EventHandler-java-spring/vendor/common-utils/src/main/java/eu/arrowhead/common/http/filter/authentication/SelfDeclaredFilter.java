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
package eu.arrowhead.common.http.filter.authentication;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Order(Constants.REQUEST_FILTER_ORDER_AUTHENTICATION)
public class SelfDeclaredFilter extends ArrowheadFilter implements IAuthenticationPolicyFilter {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		log.debug("checking access in SelfDeclaredFilter...");

		try {
			initializeRequestAttributes(request);

			final String systemName = processAuthHeader(request);
			request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, systemName);
			request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST, Constants.SYSOP.equals(systemName));

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private String processAuthHeader(final HttpServletRequest request) {
		log.debug("SelfDeclaredFilter.processAuthHeader started...");

		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (Utilities.isEmpty(authHeader)) {
			throw new AuthException("No authorization header has been provided");
		}

		String[] split = authHeader.trim().split(" ");
		if (split.length != 2 || !split[0].equals(Constants.AUTHENTICATION_SCHEMA)) {
			throw new AuthException("Invalid authorization header");
		}

		split = split[1].split(Constants.AUTHENTICATION_KEY_DELIMITER);
		if (split.length != 2 || !split[0].equals(Constants.AUTHENTICATION_PREFIX_SYSTEM)) {
			throw new AuthException("Invalid authorization header");
		}

		return systemNameNormalizer.normalize(split[1]);
	}
}