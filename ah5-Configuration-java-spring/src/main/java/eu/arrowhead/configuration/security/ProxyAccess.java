/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.configuration.ConfigurationConstants;
import jakarta.servlet.http.HttpServletRequest;

// configurationProxy reads and writes the configuration of any system. v4.6 let every certified system use it; in 5.2.2
// a consumer needs a ConsumerAuthorization grant (service.authorization.enabled=true) or, without that check, must be sysop.
@Component
public class ProxyAccess {

	@Autowired
	private RequestIdentity identity;

	@Autowired
	private ConfigurationAuthorizationService authorizationService;

	@Value(ConfigurationConstants.$SERVICE_AUTHORIZATION_ENABLED_WD)
	private boolean serviceAuthorizationEnabled;

	public String check(final HttpServletRequest request, final String operation, final String origin) {
		final String requester = identity.requester(request, origin);
		final boolean allowed = serviceAuthorizationEnabled
				? identity.isSysop(request) || authorizationService.isAuthorized(requester, ConfigurationConstants.SERVICE_DEF_CONFIGURATION_PROXY, operation)
				: identity.isSysop(request);
		if (!allowed) {
			throw new ForbiddenException(requester + " is not authorized for " + ConfigurationConstants.SERVICE_DEF_CONFIGURATION_PROXY + "." + operation, origin);
		}
		return requester;
	}
}
