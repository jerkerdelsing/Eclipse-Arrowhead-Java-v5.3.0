/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.security;

import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;

// The requester identity established by the common authentication filters, bound to the client certificate when TLS is used.
@Component
public class RequestIdentity {

	@Autowired
	private SystemInfo sysInfo;

	public String requester(final HttpServletRequest request, final String origin) {
		final String requester = (String) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (requester == null) {
			throw new AuthException("No authenticated Arrowhead system", origin);
		}
		if (sysInfo.isSslEnabled()) {
			final X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);
			final String certificateSystem = certificates == null || certificates.length == 0
					? null
					: systemNameFromSubject(certificates[0].getSubjectX500Principal().getName(X500Principal.RFC2253));
			if (!requester.equals(certificateSystem)) {
				throw new AuthException("Client certificate does not belong to the authenticated system", origin);
			}
		}
		return requester;
	}

	public boolean isSysop(final HttpServletRequest request) {
		return Boolean.TRUE.equals(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST));
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
}
