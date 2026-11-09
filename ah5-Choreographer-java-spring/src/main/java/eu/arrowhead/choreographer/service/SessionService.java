/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import eu.arrowhead.choreographer.api.dto.CheckPlanResponseDTO;
import eu.arrowhead.choreographer.api.dto.RunPlanListRequestDTO;
import eu.arrowhead.choreographer.api.dto.RunPlanListResponseDTO;
import eu.arrowhead.choreographer.api.dto.RunPlanRequestDTO;
import eu.arrowhead.choreographer.api.dto.RunPlanResponseDTO;
import eu.arrowhead.choreographer.api.dto.SessionListResponseDTO;
import eu.arrowhead.choreographer.api.dto.SessionStepListResponseDTO;
import eu.arrowhead.choreographer.api.dto.StartSessionListRequestDTO;
import eu.arrowhead.choreographer.api.dto.StartSessionRequestDTO;
import eu.arrowhead.choreographer.api.dto.WorklogListResponseDTO;
import eu.arrowhead.choreographer.api.dto.enums.SessionStatus;
import eu.arrowhead.choreographer.api.dto.enums.SessionStepStatus;
import eu.arrowhead.choreographer.engine.ChoreographerEngine;
import eu.arrowhead.choreographer.jpa.entity.Plan;
import eu.arrowhead.choreographer.jpa.entity.Session;
import eu.arrowhead.choreographer.jpa.entity.SessionStep;
import eu.arrowhead.choreographer.jpa.entity.Worklog;
import eu.arrowhead.choreographer.jpa.service.PlanDbService;
import eu.arrowhead.choreographer.jpa.service.SessionDbService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.PageDTO;

@Service
public class SessionService {

	@Autowired
	private PlanDbService planDb;

	@Autowired
	private SessionDbService sessionDb;

	@Autowired
	private PlanExecutionChecker checker;

	@Autowired
	private ChoreographerEngine engine;

	@Autowired
	private DTOConverter converter;

	@Autowired
	private PageService pageService;

	// ---------------------------------------------------------------------------------------- start

	public RunPlanListResponseDTO startByManagement(final RunPlanListRequestDTO request, final String requester, final String origin) {
		if (request == null || Utilities.isEmpty(request.sessions())) {
			throw new InvalidParameterException("Session list is empty", origin);
		}
		final List<RunPlanResponseDTO> result = new ArrayList<>();
		for (final RunPlanRequestDTO run : request.sessions()) {
			if (run == null || run.planId() == null || run.planId() <= 0) {
				result.add(new RunPlanResponseDTO(run == null ? null : run.planId(), null, run == null ? null : run.quantity(), null, List.of("Plan id is not valid."), false));
				continue;
			}
			final Plan plan = planDb.getPlan(run.planId());
			result.add(start(plan, run.quantity(), run.allowInterCloud(), run.chooseOptimalExecutor(), run.notifyProtocol(), run.notifyAddress(), run.notifyPort(), run.notifyPath(), requester));
		}
		return new RunPlanListResponseDTO(result);
	}

	public RunPlanListResponseDTO startByClient(final StartSessionListRequestDTO request, final String requester, final String origin) {
		if (request == null || Utilities.isEmpty(request.sessions())) {
			throw new InvalidParameterException("Session list is empty", origin);
		}
		final List<RunPlanResponseDTO> result = new ArrayList<>();
		for (final StartSessionRequestDTO run : request.sessions()) {
			final Optional<Plan> plan = run == null || Utilities.isEmpty(run.planName()) ? Optional.empty() : planDb.findPlanByName(run.planName().trim());
			if (plan.isEmpty()) {
				result.add(new RunPlanResponseDTO(null, null, run == null ? null : run.quantity(), null, List.of("Plan not found: " + (run == null ? null : run.planName())), false));
				continue;
			}
			result.add(start(plan.get(), run.quantity(), run.allowInterCloud(), run.chooseOptimalExecutor(), run.notifyProtocol(), run.notifyAddress(), run.notifyPort(), run.notifyPath(), requester));
		}
		return new RunPlanListResponseDTO(result);
	}

	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	private RunPlanResponseDTO start(final Plan plan, final Long quantityParam, final Boolean allowInterCloudParam, final Boolean chooseOptimalParam, final String notifyProtocol,
			final String notifyAddress, final Integer notifyPort, final String notifyPath, final String owner) {
		final long quantity = quantityParam == null ? 1 : quantityParam;
		final boolean allowInterCloud = Boolean.TRUE.equals(allowInterCloudParam);
		final List<String> errors = new ArrayList<>();
		final String notifyUri = notifyUri(notifyProtocol, notifyAddress, notifyPort, notifyPath, errors);
		final CheckPlanResponseDTO check = checker.check(plan, quantity, allowInterCloud, false);
		errors.addAll(check.errorMessages());
		if (!errors.isEmpty()) {
			return new RunPlanResponseDTO(plan.getId(), null, quantity, null, errors, check.needInterCloud());
		}
		final Session session = sessionDb.initiateSession(plan, quantity, owner, allowInterCloud, Boolean.TRUE.equals(chooseOptimalParam), notifyUri);
		engine.startSessionAsync(session.getId());
		return new RunPlanResponseDTO(plan.getId(), session.getId(), quantity, SessionStatus.INITIATED, List.of(), check.needInterCloud());
	}

	// v4.6 rules: protocol http or https, a valid port, an empty path by default; no address means no notification
	static String notifyUri(final String protocol, final String address, final Integer port, final String path, final List<String> errors) {
		if (Utilities.isEmpty(address)) {
			return null;
		}
		if (!Constants.HTTP.equalsIgnoreCase(protocol) && !Constants.HTTPS.equalsIgnoreCase(protocol)) {
			errors.add("Invalid notify protocol.");
		}
		if (port == null || port < Constants.MIN_PORT || port > Constants.MAX_PORT) {
			errors.add("Notify port must be between " + Constants.MIN_PORT + " and " + Constants.MAX_PORT + ".");
		}
		if (!errors.isEmpty()) {
			return null;
		}
		final String normalizedPath = Utilities.isEmpty(path) ? "" : (path.startsWith("/") ? path : "/" + path);
		return protocol.toLowerCase() + "://" + address.trim() + ":" + port + normalizedPath;
	}

	// ---------------------------------------------------------------------------------------- abort

	public void abortByManagement(final long id, final String requester) {
		sessionDb.getSession(id);
		engine.abortSession(id, null, "Aborted by " + requester + ".");
	}

	// a consumer may abort only the sessions it started
	public void abortByClient(final long id, final String requester, final String origin) {
		final Session session = sessionDb.getSession(id);
		if (!session.getOwner().equals(requester)) {
			throw new ForbiddenException("Session " + id + " was not started by " + requester, origin);
		}
		engine.abortSession(id, null, "Aborted by " + requester + ".");
	}

	// ---------------------------------------------------------------------------------------- queries

	public SessionListResponseDTO querySessions(final PageDTO page, final Long planId, final String status, final String origin) {
		final Plan plan = planId == null ? null : planDb.getPlan(planId);
		final Page<Session> sessions = sessionDb.getSessions(pageService.getPageRequest(page, Session.SORTABLE_FIELDS_BY, "id", origin), plan, parse(status, SessionStatus.class, origin));
		return new SessionListResponseDTO(sessions.getContent().stream().map(converter::session).toList(), sessions.getTotalElements());
	}

	public SessionStepListResponseDTO querySessionSteps(final PageDTO page, final Long sessionId, final String status, final String origin) {
		final Session session = sessionId == null ? null : sessionDb.getSession(sessionId);
		final Page<SessionStep> steps = sessionDb.getSessionSteps(pageService.getPageRequest(page, SessionStep.SORTABLE_FIELDS_BY, "id", origin), session,
				parse(status, SessionStepStatus.class, origin));
		return new SessionStepListResponseDTO(steps.getContent().stream().map(converter::sessionStep).toList(), steps.getTotalElements());
	}

	public WorklogListResponseDTO queryWorklog(final PageDTO page, final Long sessionId, final String planName, final String actionName, final String stepName, final String origin) {
		final Page<Worklog> entries = sessionDb.getWorklogs(pageService.getPageRequest(page, Worklog.SORTABLE_FIELDS_BY, "id", origin), sessionId, planName, actionName, stepName);
		return new WorklogListResponseDTO(entries.getContent().stream().map(converter::worklog).toList(), entries.getTotalElements());
	}

	private static <E extends Enum<E>> E parse(final String value, final Class<E> type, final String origin) {
		if (Utilities.isEmpty(value)) {
			return null;
		}
		try {
			return Enum.valueOf(type, value.trim().toUpperCase());
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException("Invalid status: " + value, origin);
		}
	}
}
