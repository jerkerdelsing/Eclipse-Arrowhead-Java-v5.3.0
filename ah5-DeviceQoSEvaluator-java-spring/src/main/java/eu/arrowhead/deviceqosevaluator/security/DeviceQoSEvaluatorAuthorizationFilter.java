/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.deviceqosevaluator.security;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs after the authentication filter: binds the authenticated system to the client certificate and lets
// ConsumerAuthorization decide every qualityEvaluation call (filter, sort). Anything else under the qualityEvaluation
// path is refused. Management is protected by the common management filter. Until release 5.3.0 any authenticated
// system could use qualityEvaluation.
@Component
@Order(DeviceQoSEvaluatorConstants.REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION)
@ConditionalOnProperty(name = DeviceQoSEvaluatorConstants.SERVICE_AUTHORIZATION_ENABLED, havingValue = "true", matchIfMissing = true)
public class DeviceQoSEvaluatorAuthorizationFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	private static final String QUALITY_EVALUATION_PREFIX = DeviceQoSEvaluatorConstants.HTTP_API_QUALITY_EVALUATION_PATH + "/";

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private DeviceQoSEvaluatorAuthorizationService authorizationService;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			final String path = SecurityUtilities.getDecodedUri(request.getRequestURI());
			if (isQualityEvaluation(path)) {
				final String origin = request.getMethod() + " " + request.getRequestURI();
				final String consumer = (String) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
				if (consumer == null || Constants.UNKNOWN.equals(consumer)) {
					throw new AuthException("No authenticated Arrowhead system", origin);
				}

				if (sysInfo.isSslEnabled() && !consumer.equals(certificateSystemName(request))) {
					throw new AuthException("Client certificate does not belong to the authenticated system", origin);
				}

				final Optional<String> operation = operation(request.getMethod(), path);
				if (operation.isEmpty()) {
					throw new ForbiddenException("Not a " + Constants.SERVICE_DEF_QUALITY_EVALUATION + " operation", origin);
				}

				if (!authorizationService.isAuthorized(consumer, Constants.SERVICE_DEF_QUALITY_EVALUATION, operation.get())) {
					throw new ForbiddenException(consumer + " is not authorized for " + Constants.SERVICE_DEF_QUALITY_EVALUATION + "." + operation.get(), origin);
				}
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	static boolean isQualityEvaluation(final String decodedPath) {
		return decodedPath != null
				&& (decodedPath.equals(DeviceQoSEvaluatorConstants.HTTP_API_QUALITY_EVALUATION_PATH) || decodedPath.startsWith(QUALITY_EVALUATION_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	static Optional<String> operation(final String method, final String decodedPath) {
		if (!"POST".equals(method)) {
			return Optional.empty();
		}

		if ((DeviceQoSEvaluatorConstants.HTTP_API_QUALITY_EVALUATION_PATH + DeviceQoSEvaluatorConstants.HTTP_API_OP_FILTER_PATH).equals(decodedPath)) {
			return Optional.of(Constants.SERVICE_OP_FILTER);
		}

		if ((DeviceQoSEvaluatorConstants.HTTP_API_QUALITY_EVALUATION_PATH + DeviceQoSEvaluatorConstants.HTTP_API_OP_SORT_PATH).equals(decodedPath)) {
			return Optional.of(Constants.SERVICE_OP_SORT);
		}

		return Optional.empty();
	}

	//-------------------------------------------------------------------------------------------------
	static String systemNameFromSubject(final String subjectDN) {
		try {
			for (final Rdn rdn : new LdapName(subjectDN).getRdns()) {
				if ("CN".equalsIgnoreCase(rdn.getType())) {
					final String commonName = rdn.getValue().toString();
					final int dot = commonName.indexOf('.');
					return dot < 0 ? commonName : commonName.substring(0, dot);
				}
			}
		} catch (final InvalidNameException ex) {
			return null;
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private String certificateSystemName(final HttpServletRequest request) {
		final X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);
		if (certificates == null || certificates.length == 0) {
			return null;
		}

		return systemNameFromSubject(certificates[0].getSubjectX500Principal().getName(X500Principal.RFC2253));
	}
}
