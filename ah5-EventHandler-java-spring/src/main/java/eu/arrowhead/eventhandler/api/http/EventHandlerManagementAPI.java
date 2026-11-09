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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.eventhandler.EventHandlerConstants;
import eu.arrowhead.eventhandler.api.dto.SubscriptionListResponseDTO;
import eu.arrowhead.eventhandler.api.dto.SubscriptionRequestDTO;
import eu.arrowhead.eventhandler.api.dto.SubscriptionResponseDTO;
import eu.arrowhead.eventhandler.jpa.entity.Subscription;
import eu.arrowhead.eventhandler.service.EventHandlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(EventHandlerConstants.HTTP_API_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class EventHandlerManagementAPI {

	@Autowired
	private EventHandlerService eventHandlerService;

	@Autowired
	private PageService pageService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Operation(summary = "Returns event subscriptions")
	@GetMapping(path = EventHandlerConstants.HTTP_API_OP_SUBSCRIPTIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody SubscriptionListResponseDTO querySubscriptions(
			@RequestParam(required = false) final Integer page,
			@RequestParam(required = false) final Integer size,
			@RequestParam(required = false) final String direction,
			@RequestParam(required = false) final String sortField) {
		logger.debug("querySubscriptions started");
		final String origin = HttpMethod.GET.name() + " " + EventHandlerConstants.HTTP_API_MANAGEMENT_PATH + EventHandlerConstants.HTTP_API_OP_SUBSCRIPTIONS_PATH;
		final PageDTO pageDTO = new PageDTO(page, size, direction, sortField);
		return eventHandlerService.querySubscriptions(pageService.getPageRequest(pageDTO, Subscription.SORTABLE_FIELDS_BY, Subscription.DEFAULT_SORT_FIELD, origin));
	}

	@Operation(summary = "Updates an event subscription")
	@PutMapping(path = EventHandlerConstants.HTTP_API_OP_SUBSCRIPTION_BY_ID_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody SubscriptionResponseDTO updateSubscription(@PathVariable final long subscriptionId, @RequestBody final SubscriptionRequestDTO dto) {
		logger.debug("updateSubscription started");
		final String path = EventHandlerConstants.HTTP_API_OP_SUBSCRIPTION_BY_ID_PATH.replace("{subscriptionId}", Long.toString(subscriptionId));
		final String origin = HttpMethod.PUT.name() + " " + EventHandlerConstants.HTTP_API_MANAGEMENT_PATH + path;
		return eventHandlerService.updateSubscription(subscriptionId, dto, origin);
	}

	@Operation(summary = "Removes an event subscription")
	@DeleteMapping(path = EventHandlerConstants.HTTP_API_OP_SUBSCRIPTION_BY_ID_PATH)
	public ResponseEntity<Void> removeSubscription(@PathVariable final long subscriptionId) {
		logger.debug("removeSubscription started");
		final String path = EventHandlerConstants.HTTP_API_OP_SUBSCRIPTION_BY_ID_PATH.replace("{subscriptionId}", Long.toString(subscriptionId));
		final String origin = HttpMethod.DELETE.name() + " " + EventHandlerConstants.HTTP_API_MANAGEMENT_PATH + path;
		final boolean removed = eventHandlerService.removeSubscription(subscriptionId, origin);
		return new ResponseEntity<>(removed ? HttpStatus.OK : HttpStatus.NO_CONTENT);
	}
}
