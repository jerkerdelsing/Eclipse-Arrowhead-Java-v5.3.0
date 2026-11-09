/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.eventhandler.EventHandlerConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs after the authentication filter (which already binds the identity token to the client certificate) and lets
// ConsumerAuthorization decide eventPublish.publish, eventSubscription.subscribe and eventSubscription.unsubscribe.
// Management operations are protected by the common management filter.
@Component
@Order(EventHandlerConstants.REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION)
@ConditionalOnProperty(name = EventHandlerConstants.SERVICE_AUTHORIZATION_ENABLED, havingValue = "true")
public class EventHandlerAuthorizationFilter extends ArrowheadFilter {

	@Autowired
	private EventHandlerAuthorizationService authorizationService;

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			final String[] target = target(request);
			if (target != null) {
				final String origin = request.getMethod() + " " + request.getRequestURI();
				final String consumer = (String) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
				if (consumer == null) {
					throw new AuthException("No authenticated Arrowhead system", origin);
				}
				if (!authorizationService.isAuthorized(consumer, target[0], target[1])) {
					throw new ForbiddenException(consumer + " is not authorized for " + target[0] + "." + target[1], origin);
				}
			}
			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	// {service definition, operation} of an operation that needs a ConsumerAuthorization decision, or null
	static String[] target(final HttpServletRequest request) {
		final String method = request.getMethod();
		final String uri = request.getRequestURI();
		if ("POST".equals(method) && (EventHandlerConstants.HTTP_API_EVENT_PUBLISH_PATH + EventHandlerConstants.HTTP_API_OP_PUBLISH_PATH).equals(uri)) {
			return new String[] { EventHandlerConstants.SERVICE_DEF_EVENT_PUBLISH, EventHandlerConstants.SERVICE_OP_PUBLISH };
		}
		if ("POST".equals(method) && (EventHandlerConstants.HTTP_API_EVENT_SUBSCRIPTION_PATH + EventHandlerConstants.HTTP_API_OP_SUBSCRIBE_PATH).equals(uri)) {
			return new String[] { EventHandlerConstants.SERVICE_DEF_EVENT_SUBSCRIPTION, EventHandlerConstants.SERVICE_OP_SUBSCRIBE };
		}
		if ("DELETE".equals(method) && uri != null && uri.startsWith(EventHandlerConstants.HTTP_API_EVENT_SUBSCRIPTION_PATH + "/unsubscribe/")) {
			return new String[] { EventHandlerConstants.SERVICE_DEF_EVENT_SUBSCRIPTION, EventHandlerConstants.SERVICE_OP_UNSUBSCRIBE };
		}
		return null;
	}
}
