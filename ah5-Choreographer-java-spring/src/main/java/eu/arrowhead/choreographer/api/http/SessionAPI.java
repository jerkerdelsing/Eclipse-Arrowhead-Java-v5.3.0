/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.http;

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

import eu.arrowhead.choreographer.ChoreographerConstants;
import eu.arrowhead.choreographer.api.dto.RunPlanListResponseDTO;
import eu.arrowhead.choreographer.api.dto.StartSessionListRequestDTO;
import eu.arrowhead.choreographer.service.SessionService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.HttpUtilities;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// choreographerSession: application systems start and abort their own sessions by plan name
@RestController
@RequestMapping(ChoreographerConstants.HTTP_API_SESSION_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class SessionAPI {

	@Autowired
	private SessionService sessionService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Operation(summary = "Starts sessions of plans given by name; the requester becomes the owner (choreographerSession.start-session)")
	@PostMapping(path = ChoreographerConstants.HTTP_API_OP_START_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody RunPlanListResponseDTO startSession(final HttpServletRequest httpServletRequest, @RequestBody final StartSessionListRequestDTO dto) {
		logger.debug("startSession started");
		final String origin = HttpMethod.POST.name() + " " + ChoreographerConstants.HTTP_API_SESSION_PATH + ChoreographerConstants.HTTP_API_OP_START_PATH;
		return sessionService.startByClient(dto, HttpUtilities.acquireName(httpServletRequest, origin), origin);
	}

	@Operation(summary = "Aborts a session started by the requester (choreographerSession.abort-session)")
	@DeleteMapping(path = ChoreographerConstants.HTTP_API_OP_ABORT_PATH)
	public ResponseEntity<Void> abortSession(final HttpServletRequest httpServletRequest, @PathVariable final long id) {
		logger.debug("abortSession started");
		final String origin = HttpMethod.DELETE.name() + " " + ChoreographerConstants.HTTP_API_SESSION_PATH + ChoreographerConstants.HTTP_API_OP_ABORT_PATH.replace("{id}", Long.toString(id));
		sessionService.abortByClient(id, HttpUtilities.acquireName(httpServletRequest, origin), origin);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
