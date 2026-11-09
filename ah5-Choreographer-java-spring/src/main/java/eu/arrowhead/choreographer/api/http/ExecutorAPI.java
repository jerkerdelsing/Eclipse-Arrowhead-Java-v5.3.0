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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.choreographer.ChoreographerConstants;
import eu.arrowhead.choreographer.api.dto.ExecutedStepResultDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorRegistrationRequestDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorResponseDTO;
import eu.arrowhead.choreographer.service.ExecutorService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.HttpUtilities;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// choreographerExecutor: executor systems register themselves and report step results
@RestController
@RequestMapping(ChoreographerConstants.HTTP_API_EXECUTOR_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ExecutorAPI {

	@Autowired
	private ExecutorService executorService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Operation(summary = "Registers the requester as executor of a step service definition (choreographerExecutor.register)")
	@PostMapping(path = ChoreographerConstants.HTTP_API_OP_REGISTER_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody ExecutorResponseDTO register(final HttpServletRequest httpServletRequest, @RequestBody final ExecutorRegistrationRequestDTO dto) {
		logger.debug("register started");
		final String origin = HttpMethod.POST.name() + " " + ChoreographerConstants.HTTP_API_EXECUTOR_PATH + ChoreographerConstants.HTTP_API_OP_REGISTER_PATH;
		return executorService.register(HttpUtilities.acquireName(httpServletRequest, origin), dto, origin);
	}

	@Operation(summary = "Unregisters the requester as executor (choreographerExecutor.unregister)")
	@DeleteMapping(path = ChoreographerConstants.HTTP_API_OP_UNREGISTER_PATH)
	public ResponseEntity<Void> unregister(final HttpServletRequest httpServletRequest) {
		logger.debug("unregister started");
		final String origin = HttpMethod.DELETE.name() + " " + ChoreographerConstants.HTTP_API_EXECUTOR_PATH + ChoreographerConstants.HTTP_API_OP_UNREGISTER_PATH;
		executorService.unregister(HttpUtilities.acquireName(httpServletRequest, origin));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Operation(summary = "Reports the result of an executed step (choreographerExecutor.notify-step-done)")
	@PostMapping(path = ChoreographerConstants.HTTP_API_OP_NOTIFY_STEP_DONE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> notifyStepDone(final HttpServletRequest httpServletRequest, @RequestBody final ExecutedStepResultDTO dto) {
		logger.debug("notifyStepDone started");
		final String origin = HttpMethod.POST.name() + " " + ChoreographerConstants.HTTP_API_EXECUTOR_PATH + ChoreographerConstants.HTTP_API_OP_NOTIFY_STEP_DONE_PATH;
		executorService.notifyStepDone(HttpUtilities.acquireName(httpServletRequest, origin), dto, origin);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
