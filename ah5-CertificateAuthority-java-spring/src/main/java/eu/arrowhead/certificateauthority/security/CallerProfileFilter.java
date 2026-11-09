/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.security;

import java.io.IOException;
import java.security.cert.X509Certificate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.certificateauthority.CertificateAuthorityConstants;
import eu.arrowhead.certificateauthority.service.CertificateSupport;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs right after the authentication filter (which binds the identity token to the client certificate): only system ("sy")
// and operator ("op") certificates may call the CA. On-boarding and device certificates are refused.
@Component
@Order(CertificateAuthorityConstants.REQUEST_FILTER_ORDER_CALLER_PROFILE)
public class CallerProfileFilter extends ArrowheadFilter {

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
			if (sysInfo.isSslEnabled()) {
				check(request);
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	static void check(final HttpServletRequest request) {
		final String origin = request.getMethod() + " " + request.getRequestURI();
		final X509Certificate[] chain = (X509Certificate[]) request.getAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);
		if (chain == null || chain.length == 0) {
			throw new AuthException("Mutual TLS is required", origin);
		}

		if (!CertificateAuthorityConstants.CALLER_PROFILES.contains(CertificateSupport.attribute(chain[0], "DNQ"))) {
			throw new ForbiddenException("Only system/operator certificates may call services", origin);
		}
	}
}
