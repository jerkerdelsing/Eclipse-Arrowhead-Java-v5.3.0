/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.api.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.eventhandler.EventHandlerConstants;
import eu.arrowhead.eventhandler.api.dto.EventPublishRequestDTO;
import eu.arrowhead.eventhandler.api.dto.EventPublishResultDTO;
import eu.arrowhead.eventhandler.service.EventHandlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(EventHandlerConstants.HTTP_API_EVENT_PUBLISH_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class EventPublishAPI {

	@Autowired
	private EventHandlerService eventHandlerService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Operation(summary = "Publishes an event to matching subscribers")
	@PostMapping(path = EventHandlerConstants.HTTP_API_OP_PUBLISH_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody EventPublishResultDTO publish(final HttpServletRequest request, @RequestBody final EventPublishRequestDTO dto) {
		logger.debug("publish started");
		final String origin = HttpMethod.POST.name() + " " + EventHandlerConstants.HTTP_API_EVENT_PUBLISH_PATH + EventHandlerConstants.HTTP_API_OP_PUBLISH_PATH;
		return eventHandlerService.publish(HttpUtilities.acquireName(request, origin), dto, origin);
	}
}
