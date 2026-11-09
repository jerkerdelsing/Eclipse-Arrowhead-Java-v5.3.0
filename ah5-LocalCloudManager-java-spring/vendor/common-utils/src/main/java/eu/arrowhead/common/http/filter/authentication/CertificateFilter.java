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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.security.CertificateProfileType;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.security.SecurityUtilities.CommonNameAndType;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Order(Constants.REQUEST_FILTER_ORDER_AUTHENTICATION)
public class CertificateFilter extends ArrowheadFilter implements IAuthenticationPolicyFilter {

	//=================================================================================================
	// members

	@Autowired
	private ApplicationContext appContext;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		log.debug("Checking access in CertificateFilter...");

		try {
			initializeRequestAttributes(request);
			final String requestTarget = Utilities.stripEndSlash(request.getRequestURL().toString());
			final CommonNameAndType requesterData = SecurityUtilities.getIdentificationDataFromRequest(request);
			if (requesterData == null) {
				log.error("Unauthenticated access attempt: {}", requestTarget);
				throw new AuthException("Unauthenticated access attempt: " + requestTarget);
			}

			checkClientAuthorized(requesterData, requestTarget);
			fillRequestAttributes(request, requesterData);

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void checkClientAuthorized(final CommonNameAndType requesterData, final String requestTarget) {
		log.debug("CertificateFilter.checkClientAuthenticated started...");

		if (CertificateProfileType.SYSTEM != requesterData.profileType() && CertificateProfileType.OPERATOR != requesterData.profileType()) {
			log.error("Unauthorized access: {}, invalid certificate type: {}", requestTarget, requesterData.profileType());
			throw new ForbiddenException("Unauthorized access: " + requestTarget + ", invalid certificate type: " + requesterData.profileType());
		}

		final String serverCN = (String) arrowheadContext.get(Constants.SERVER_COMMON_NAME);
		final String cloudCN = SecurityUtilities.getCloudCN(serverCN);
		if (!SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, requesterData.commonName(), cloudCN)) {
			log.error("Unauthorized access: {}, from foreign cloud", requestTarget);
			throw new ForbiddenException("Unauthorized access: " + requestTarget + ", from foreign cloud");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void fillRequestAttributes(final HttpServletRequest request, final CommonNameAndType requesterData) {
		log.debug("CertificateFilter.checkClientAuthenticated started...");

		final String clientName = systemNameNormalizer.normalize(SecurityUtilities.getClientNameFromClientCN(requesterData.commonName()));

		request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, clientName);
		request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST, CertificateProfileType.OPERATOR == requesterData.profileType());
	}
}