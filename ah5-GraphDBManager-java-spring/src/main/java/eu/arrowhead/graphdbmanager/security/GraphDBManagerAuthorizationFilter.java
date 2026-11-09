/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.security;

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
import eu.arrowhead.graphdbmanager.GraphDBManagerConstants;
import eu.arrowhead.graphdbmanager.security.GraphDBManagerOperationResolver.ServiceOperation;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs after the authentication filter: binds the authenticated system to the client certificate
// and lets ConsumerAuthorization decide every dataProxy and dataHistorian operation.
@Component
@Order(GraphDBManagerConstants.REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION)
@ConditionalOnProperty(name = GraphDBManagerConstants.SERVICE_AUTHORIZATION_ENABLED, havingValue = "true")
public class GraphDBManagerAuthorizationFilter extends ArrowheadFilter {

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private GraphDBManagerAuthorizationService authorizationService;

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			final Optional<ServiceOperation> target = GraphDBManagerOperationResolver.resolve(request.getMethod(), request.getRequestURI());
			if (target.isPresent()) {
				final String origin = request.getMethod() + " " + request.getRequestURI();
				final String consumer = (String) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
				if (consumer == null) {
					throw new AuthException("No authenticated Arrowhead system", origin);
				}

				if (sysInfo.isSslEnabled() && !consumer.equals(certificateSystemName(request))) {
					throw new AuthException("Client certificate does not belong to the authenticated system", origin);
				}

				final ServiceOperation operation = target.get();
				if (!authorizationService.isAuthorized(consumer, operation.serviceDefinition(), operation.operation())) {
					throw new ForbiddenException(consumer + " is not authorized for " + operation.serviceDefinition() + "." + operation.operation(), origin);
				}
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

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

	private String certificateSystemName(final HttpServletRequest request) {
		final X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);
		if (certificates == null || certificates.length == 0) {
			return null;
		}

		return systemNameFromSubject(certificates[0].getSubjectX500Principal().getName(X500Principal.RFC2253));
	}
}
