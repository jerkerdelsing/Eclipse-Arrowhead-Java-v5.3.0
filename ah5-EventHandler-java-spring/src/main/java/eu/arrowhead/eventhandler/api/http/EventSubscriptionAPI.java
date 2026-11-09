/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.api.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.eventhandler.EventHandlerConstants;
import eu.arrowhead.eventhandler.api.dto.SubscriptionRequestDTO;
import eu.arrowhead.eventhandler.api.dto.SubscriptionResponseDTO;
import eu.arrowhead.eventhandler.service.EventHandlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(EventHandlerConstants.HTTP_API_EVENT_SUBSCRIPTION_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class EventSubscriptionAPI {

	@Autowired
	private EventHandlerService eventHandlerService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Operation(summary = "Creates an event subscription owned by the authenticated requester")
	@PostMapping(path = EventHandlerConstants.HTTP_API_OP_SUBSCRIBE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody SubscriptionResponseDTO subscribe(final HttpServletRequest request, @RequestBody final SubscriptionRequestDTO dto) {
		logger.debug("subscribe started");
		final String origin = HttpMethod.POST.name() + " " + EventHandlerConstants.HTTP_API_EVENT_SUBSCRIPTION_PATH + EventHandlerConstants.HTTP_API_OP_SUBSCRIBE_PATH;
		return eventHandlerService.subscribe(HttpUtilities.acquireName(request, origin), dto, origin);
	}

	@Operation(summary = "Removes an event subscription owned by the authenticated requester")
	@DeleteMapping(path = EventHandlerConstants.HTTP_API_OP_UNSUBSCRIBE_PATH)
	public ResponseEntity<Void> unsubscribe(final HttpServletRequest request, @PathVariable final long subscriptionId) {
		logger.debug("unsubscribe started");
		final String path = EventHandlerConstants.HTTP_API_OP_UNSUBSCRIBE_PATH.replace("{subscriptionId}", Long.toString(subscriptionId));
		final String origin = HttpMethod.DELETE.name() + " " + EventHandlerConstants.HTTP_API_EVENT_SUBSCRIPTION_PATH + path;
		final boolean removed = eventHandlerService.unsubscribe(subscriptionId, HttpUtilities.acquireName(request, origin), origin);
		return new ResponseEntity<>(removed ? HttpStatus.OK : HttpStatus.NO_CONTENT);
	}
}
