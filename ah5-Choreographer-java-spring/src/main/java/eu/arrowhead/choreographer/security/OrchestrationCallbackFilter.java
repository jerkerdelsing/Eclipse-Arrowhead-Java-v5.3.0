/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.security;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import eu.arrowhead.choreographer.ChoreographerConstants;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.http.filter.authentication.IAuthenticationPolicyFilter;
import eu.arrowhead.common.security.CertificateProfileType;
import eu.arrowhead.common.security.SecurityUtilities.CommonNameAndType;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.security.auth.x500.X500Principal;

// Takes the place of the authentication policy filter.
// DynamicServiceOrchestration pushes orchestration results without an Authorization header, so the callback path
// cannot pass the outsourced or self-declared filters. It is accepted when
//  - SSL is on: the client certificate is a local-cloud system certificate of DynamicServiceOrchestration;
//  - SSL is off: unconditionally (the unguessable correlation id in the path is the only protection, as for any
//    non-SSL Arrowhead cloud).
// The callback controller then accepts only correlation ids of pending orchestrations.
// Every other path is handled by the configured authentication policy filter.
public class OrchestrationCallbackFilter extends ArrowheadFilter {

	private static final Set<String> CALLBACK_SENDERS = Set.of(Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION);

	private final IAuthenticationPolicyFilter delegate;

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	public OrchestrationCallbackFilter(final IAuthenticationPolicyFilter delegate) {
		this.delegate = delegate;
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		if (!isCallback(request)) {
			delegate.doFilter(request, response, chain);
			return;
		}

		try {
			initializeRequestAttributes(request);
			request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, sysInfo.isSslEnabled() ? checkSender(request) : Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION);
			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	static boolean isCallback(final HttpServletRequest request) {
		final String path = SecurityUtilities.getDecodedUri(request.getRequestURI());
		return "POST".equalsIgnoreCase(request.getMethod()) && path.startsWith(ChoreographerConstants.HTTP_API_ORCHESTRATION_RESULT_PATH + "/");
	}

	//-------------------------------------------------------------------------------------------------
	private String checkSender(final HttpServletRequest request) {
		final String commonName = clientCommonName(request);
		if (commonName == null) {
			throw new AuthException("Unauthenticated orchestration result push");
		}
		final String cloudCN = SecurityUtilities.getCloudCN(serverCommonName());
		if (!SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, commonName, cloudCN)) {
			throw new ForbiddenException("Orchestration result push from foreign cloud");
		}
		final String name = systemNameNormalizer.normalize(SecurityUtilities.getClientNameFromClientCN(commonName));
		if (!CALLBACK_SENDERS.contains(name)) {
			throw new ForbiddenException(name + " is not allowed to push orchestration results");
		}
		return name;
	}

	//-------------------------------------------------------------------------------------------------
	// The framework stores the server CN only under certificate authentication; the server certificate whenever SSL is on.
	private String serverCommonName() {
		final Object commonName = arrowheadContext.get(Constants.SERVER_COMMON_NAME);
		if (commonName != null) {
			return (String) commonName;
		}
		final X509Certificate certificate = (X509Certificate) arrowheadContext.get(Constants.SERVER_CERTIFICATE);
		if (certificate == null) {
			throw new AuthException("Server certificate is not available");
		}
		return SecurityUtilities.getCommonNameFromSubjectDN(certificate.getSubjectX500Principal().getName(X500Principal.RFC2253));
	}

	//-------------------------------------------------------------------------------------------------
	// System CN of the TLS client certificate. Certificates with an Arrowhead profile type (dnQualifier) must be system
	// certificates; certificates without one (e.g. clouds that use outsourced authentication) are taken by their CN.
	private static String clientCommonName(final HttpServletRequest request) {
		final CommonNameAndType typed = SecurityUtilities.getIdentificationDataFromRequest(request);
		if (typed != null) {
			return CertificateProfileType.SYSTEM == typed.profileType() ? typed.commonName() : null;
		}
		final X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);
		if (certificates == null) {
			return null;
		}
		for (final X509Certificate certificate : certificates) {
			final String dn = certificate.getSubjectX500Principal().getName(X500Principal.RFC2253);
			if (SecurityUtilities.getIdentificationDataFromSubjectDN(dn) == null) {
				final String commonName = SecurityUtilities.getCommonNameFromSubjectDN(dn);
				if (SecurityUtilities.isValidSystemCommonName(commonName)) {
					return commonName;
				}
			}
		}
		return null;
	}
}
