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
package eu.arrowhead.serviceregistry.api.http.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(ServiceRegistryConstants.REQUEST_FILTER_ORDER_SERVICE_LOOKUP)
public class ServiceLookupFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Autowired
	private ServiceRegistrySystemInfo sysInfo;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			final String requestTarget = Utilities.stripEndSlash(SecurityUtilities.getDecodedUri(request.getRequestURL().toString()));
			if (requestTarget.endsWith(ServiceRegistryConstants.HTTP_API_SERVICE_DISCOVERY_PATH + ServiceRegistryConstants.HTTP_API_OP_LOOKUP_PATH)) {
				final ServiceDiscoveryPolicy policy = sysInfo.getServiceDiscoveryPolicy();
				if (policy == ServiceDiscoveryPolicy.RESTRICTED) {
					final Boolean isSysop = Boolean.valueOf(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST).toString());
					final String systemName = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM).toString();

					final boolean isRestricted = !(isSysop || sysInfo.hasClientDirectAccess(systemName));
					request.setAttribute(ServiceRegistryConstants.REQUEST_ATTR_RESTRICTED_SERVICE_LOOKUP, isRestricted);
				} else if (policy == ServiceDiscoveryPolicy.OPEN) {
					request.setAttribute(ServiceRegistryConstants.REQUEST_ATTR_RESTRICTED_SERVICE_LOOKUP, false);
				}
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}
}