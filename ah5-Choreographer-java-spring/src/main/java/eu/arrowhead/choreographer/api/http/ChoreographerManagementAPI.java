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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.choreographer.ChoreographerConstants;
import eu.arrowhead.choreographer.api.dto.CheckPlanResponseDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorCreateRequestDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorListResponseDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorResponseDTO;
import eu.arrowhead.choreographer.api.dto.PlanListResponseDTO;
import eu.arrowhead.choreographer.api.dto.PlanRequestDTO;
import eu.arrowhead.choreographer.api.dto.PlanResponseDTO;
import eu.arrowhead.choreographer.api.dto.RunPlanListRequestDTO;
import eu.arrowhead.choreographer.api.dto.RunPlanListResponseDTO;
import eu.arrowhead.choreographer.api.dto.SessionListResponseDTO;
import eu.arrowhead.choreographer.api.dto.SessionStepListResponseDTO;
import eu.arrowhead.choreographer.api.dto.WorklogListResponseDTO;
import eu.arrowhead.choreographer.service.ExecutorService;
import eu.arrowhead.choreographer.service.PlanService;
import eu.arrowhead.choreographer.service.SessionService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.dto.PageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// choreographerManagement: plans, sessions and executors (v4.6 /mgmt endpoints)
@RestController
@RequestMapping(ChoreographerConstants.HTTP_API_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ChoreographerManagementAPI {

	@Autowired
	private PlanService planService;

	@Autowired
	private SessionService sessionService;

	@Autowired
	private ExecutorService executorService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	// ---------------------------------------------------------------------------------------- plans

	@Operation(summary = "Returns plans (choreographerManagement.query-plans)")
	@GetMapping(path = ChoreographerConstants.HTTP_API_OP_PLANS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PlanListResponseDTO queryPlans(
			@RequestParam(required = false) final Integer page,
			@RequestParam(required = false) final Integer size,
			@RequestParam(required = false) final String direction,
			@RequestParam(required = false) final String sortField) {
		logger.debug("queryPlans started");
		return planService.query(new PageDTO(page, size, direction, sortField), origin(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_PLANS_PATH));
	}

	@Operation(summary = "Returns a plan (choreographerManagement.get-plan)")
	@GetMapping(path = ChoreographerConstants.HTTP_API_OP_PLAN_BY_ID_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PlanResponseDTO getPlan(@PathVariable final long id) {
		logger.debug("getPlan started");
		return planService.get(id);
	}

	@Operation(summary = "Creates a plan (choreographerManagement.create-plan)")
	@PostMapping(path = ChoreographerConstants.HTTP_API_OP_PLANS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody PlanResponseDTO createPlan(@RequestBody final PlanRequestDTO dto) {
		logger.debug("createPlan started");
		return planService.create(dto, origin(HttpMethod.POST, ChoreographerConstants.HTTP_API_OP_PLANS_PATH));
	}

	@Operation(summary = "Removes a plan with its sessions (choreographerManagement.remove-plan)")
	@DeleteMapping(path = ChoreographerConstants.HTTP_API_OP_PLAN_BY_ID_PATH)
	public ResponseEntity<Void> removePlan(@PathVariable final long id) {
		logger.debug("removePlan started");
		planService.remove(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Operation(summary = "Checks whether a plan can be executed now (choreographerManagement.check-plan)")
	@GetMapping(path = ChoreographerConstants.HTTP_API_OP_CHECK_PLAN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody CheckPlanResponseDTO checkPlan(@PathVariable final long id, @RequestParam(required = false, defaultValue = "false") final boolean allowInterCloud) {
		logger.debug("checkPlan started");
		return planService.check(id, allowInterCloud);
	}

	// ---------------------------------------------------------------------------------------- sessions

	@Operation(summary = "Starts sessions of plans given by id (choreographerManagement.start-sessions)")
	@PostMapping(path = ChoreographerConstants.HTTP_API_OP_START_SESSIONS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody RunPlanListResponseDTO startSessions(final HttpServletRequest httpServletRequest, @RequestBody final RunPlanListRequestDTO dto) {
		logger.debug("startSessions started");
		final String origin = origin(HttpMethod.POST, ChoreographerConstants.HTTP_API_OP_START_SESSIONS_PATH);
		return sessionService.startByManagement(dto, HttpUtilities.acquireName(httpServletRequest, origin), origin);
	}

	@Operation(summary = "Aborts a session (choreographerManagement.abort-session)")
	@DeleteMapping(path = ChoreographerConstants.HTTP_API_OP_SESSION_BY_ID_PATH)
	public ResponseEntity<Void> abortSession(final HttpServletRequest httpServletRequest, @PathVariable final long id) {
		logger.debug("abortSession started");
		final String origin = origin(HttpMethod.DELETE, ChoreographerConstants.HTTP_API_OP_SESSION_BY_ID_PATH.replace("{id}", Long.toString(id)));
		sessionService.abortByManagement(id, HttpUtilities.acquireName(httpServletRequest, origin));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Operation(summary = "Returns sessions, optionally filtered by plan and status (choreographerManagement.query-sessions)")
	@GetMapping(path = ChoreographerConstants.HTTP_API_OP_SESSIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody SessionListResponseDTO querySessions(
			@RequestParam(required = false) final Integer page,
			@RequestParam(required = false) final Integer size,
			@RequestParam(required = false) final String direction,
			@RequestParam(required = false) final String sortField,
			@RequestParam(required = false) final Long planId,
			@RequestParam(required = false) final String status) {
		logger.debug("querySessions started");
		return sessionService.querySessions(new PageDTO(page, size, direction, sortField), planId, status, origin(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_SESSIONS_PATH));
	}

	@Operation(summary = "Returns session steps, optionally filtered by session and status (choreographerManagement.query-session-steps)")
	@GetMapping(path = ChoreographerConstants.HTTP_API_OP_SESSION_STEPS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody SessionStepListResponseDTO querySessionSteps(
			@RequestParam(required = false) final Integer page,
			@RequestParam(required = false) final Integer size,
			@RequestParam(required = false) final String direction,
			@RequestParam(required = false) final String sortField,
			@RequestParam(required = false) final Long sessionId,
			@RequestParam(required = false) final String status) {
		logger.debug("querySessionSteps started");
		return sessionService.querySessionSteps(new PageDTO(page, size, direction, sortField), sessionId, status,
				origin(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_SESSION_STEPS_PATH));
	}

	@Operation(summary = "Returns worklog entries (choreographerManagement.query-worklog)")
	@GetMapping(path = ChoreographerConstants.HTTP_API_OP_WORKLOG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody WorklogListResponseDTO queryWorklog(
			@RequestParam(required = false) final Integer page,
			@RequestParam(required = false) final Integer size,
			@RequestParam(required = false) final String direction,
			@RequestParam(required = false) final String sortField,
			@RequestParam(required = false) final Long sessionId,
			@RequestParam(required = false) final String planName,
			@RequestParam(required = false) final String actionName,
			@RequestParam(required = false) final String stepName) {
		logger.debug("queryWorklog started");
		return sessionService.queryWorklog(new PageDTO(page, size, direction, sortField), sessionId, planName, actionName, stepName,
				origin(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_WORKLOG_PATH));
	}

	// ---------------------------------------------------------------------------------------- executors

	@Operation(summary = "Returns executors (choreographerManagement.query-executors)")
	@GetMapping(path = ChoreographerConstants.HTTP_API_OP_EXECUTORS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ExecutorListResponseDTO queryExecutors(
			@RequestParam(required = false) final Integer page,
			@RequestParam(required = false) final Integer size,
			@RequestParam(required = false) final String direction,
			@RequestParam(required = false) final String sortField) {
		logger.debug("queryExecutors started");
		return executorService.query(new PageDTO(page, size, direction, sortField), origin(HttpMethod.GET, ChoreographerConstants.HTTP_API_OP_EXECUTORS_PATH));
	}

	@Operation(summary = "Returns an executor (choreographerManagement.get-executor)")
	@GetMapping(path = ChoreographerConstants.HTTP_API_OP_EXECUTOR_BY_ID_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ExecutorResponseDTO getExecutor(@PathVariable final long id) {
		logger.debug("getExecutor started");
		return executorService.get(id);
	}

	@Operation(summary = "Creates an executor for a registered system (choreographerManagement.create-executor)")
	@PostMapping(path = ChoreographerConstants.HTTP_API_OP_EXECUTORS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody ExecutorResponseDTO createExecutor(@RequestBody final ExecutorCreateRequestDTO dto) {
		logger.debug("createExecutor started");
		return executorService.create(dto, origin(HttpMethod.POST, ChoreographerConstants.HTTP_API_OP_EXECUTORS_PATH));
	}

	@Operation(summary = "Removes an executor (choreographerManagement.remove-executor)")
	@DeleteMapping(path = ChoreographerConstants.HTTP_API_OP_EXECUTOR_BY_ID_PATH)
	public ResponseEntity<Void> removeExecutor(@PathVariable final long id) {
		logger.debug("removeExecutor started");
		executorService.remove(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	//-------------------------------------------------------------------------------------------------
	private static String origin(final HttpMethod method, final String path) {
		return method.name() + " " + ChoreographerConstants.HTTP_API_MANAGEMENT_PATH + path;
	}
}
