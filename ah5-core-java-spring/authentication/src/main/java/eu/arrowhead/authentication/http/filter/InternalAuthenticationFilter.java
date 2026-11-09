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
package eu.arrowhead.authentication.http.filter;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;

import eu.arrowhead.authentication.AuthenticationConstants;
import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.http.filter.authentication.IAuthenticationPolicyFilter;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.security.TokenCertificateBinding;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Order(Constants.REQUEST_FILTER_ORDER_AUTHENTICATION)
public class InternalAuthenticationFilter extends ArrowheadFilter implements IAuthenticationPolicyFilter {

	//=================================================================================================
	// members

	private static final List<String> tokenlessOperationPaths = List.of(
			AuthenticationConstants.HTTP_API_OP_LOGIN_PATH,
			AuthenticationConstants.HTTP_API_OP_LOGOUT_PATH,
			AuthenticationConstants.HTTP_API_OP_CHANGE_PATH,
			Constants.HTTP_API_OP_ECHO_PATH);

	@Autowired
	private IdentityDbService dbService;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			initializeRequestAttributes(request);
			final String requestTarget = SecurityUtilities.getDecodedUri(request.getRequestURL().toString());

			if (needTokenCheck(requestTarget)) {
				final String token = processAuthHeader(request);

				Optional<ActiveSession> sessionOpt = Optional.empty();
				try {
					sessionOpt = dbService.getSessionByToken(token.trim());
				} catch (final InternalServerError ex) {
					throw new AuthException("Session problem", ex);
				}

				if (sessionOpt.isEmpty()) {
					throw new AuthException("Invalid identity token");
				}

				final ActiveSession session = sessionOpt.get();
				final ZonedDateTime now = Utilities.utcNow();
				if (session.getExpirationTime().isBefore(now)) {
					// expired session
					throw new AuthException("Invalid identity token");
				}

				TokenCertificateBinding.check(request, session.getSystem().getName(), systemNameNormalizer);
				request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, session.getSystem().getName());
				request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST, session.getSystem().isSysop());
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private boolean needTokenCheck(final String url) {
		for (final String path : tokenlessOperationPaths) {
			if (url.contains(path)) {
				return false;
			}
		}

		return true;
	}

	//-------------------------------------------------------------------------------------------------
	private String processAuthHeader(final HttpServletRequest request) {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (Utilities.isEmpty(authHeader)) {
			throw new AuthException("No authorization header has been provided");
		}

		String[] split = authHeader.trim().split(" ");
		if (split.length != 2 || !split[0].equals(Constants.AUTHENTICATION_SCHEMA)) {
			throw new AuthException("Invalid authorization header");
		}

		split = split[1].split(Constants.AUTHENTICATION_KEY_DELIMITER);
		if (split.length != 2 || !split[0].equals(Constants.AUTHENTICATION_PREFIX_IDENTITY_TOKEN)) {
			throw new AuthException("Invalid authorization header");
		}

		return split[1];
	}
}