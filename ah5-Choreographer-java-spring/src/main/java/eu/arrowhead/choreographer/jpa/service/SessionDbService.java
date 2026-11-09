/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.choreographer.api.dto.enums.SessionStatus;
import eu.arrowhead.choreographer.api.dto.enums.SessionStepStatus;
import eu.arrowhead.choreographer.jpa.entity.Action;
import eu.arrowhead.choreographer.jpa.entity.Executor;
import eu.arrowhead.choreographer.jpa.entity.Plan;
import eu.arrowhead.choreographer.jpa.entity.Session;
import eu.arrowhead.choreographer.jpa.entity.SessionStep;
import eu.arrowhead.choreographer.jpa.entity.Step;
import eu.arrowhead.choreographer.jpa.entity.Worklog;
import eu.arrowhead.choreographer.jpa.repository.SessionRepository;
import eu.arrowhead.choreographer.jpa.repository.SessionStepRepository;
import eu.arrowhead.choreographer.jpa.repository.WorklogRepository;
import eu.arrowhead.common.exception.DataNotFoundException;

@Service
public class SessionDbService {

	private static final List<SessionStepStatus> ACTIVE_STEP_STATUSES = List.of(SessionStepStatus.WAITING, SessionStepStatus.RUNNING);

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private SessionStepRepository sessionStepRepository;

	@Autowired
	private WorklogRepository worklogRepository;

	// ---------------------------------------------------------------------------------------- sessions

	@Transactional(rollbackFor = Exception.class)
	public Session initiateSession(final Plan plan, final long quantity, final String owner, final boolean allowInterCloud, final boolean chooseOptimal, final String notifyUri) {
		return sessionRepository.saveAndFlush(new Session(plan, quantity, owner, allowInterCloud, chooseOptimal, notifyUri));
	}

	public Session getSession(final long id) {
		return sessionRepository.findById(id).orElseThrow(() -> new DataNotFoundException("Session not found: " + id));
	}

	public Optional<Session> findSession(final long id) {
		return sessionRepository.findById(id);
	}

	@Transactional(rollbackFor = Exception.class)
	public Session changeSessionStatus(final long id, final SessionStatus status) {
		final Session session = getSession(id);
		session.setStatus(status);
		return sessionRepository.saveAndFlush(session);
	}

	@Transactional(rollbackFor = Exception.class)
	public Session increaseExecutionNumber(final long id) {
		final Session session = getSession(id);
		session.setExecutionNumber(session.getExecutionNumber() + 1);
		return sessionRepository.saveAndFlush(session);
	}

	@Transactional(rollbackFor = Exception.class)
	public Session increaseQuantityDone(final long id) {
		final Session session = getSession(id);
		session.setQuantityDone(session.getQuantityDone() + 1);
		return sessionRepository.saveAndFlush(session);
	}

	// Marks the session and all its active steps aborted; returns the steps that were running.
	@Transactional(rollbackFor = Exception.class)
	public List<SessionStep> abortSession(final long id, final String message) {
		final Session session = getSession(id);
		session.setStatus(SessionStatus.ABORTED);
		sessionRepository.saveAndFlush(session);
		final List<SessionStep> running = new ArrayList<>();
		for (final SessionStep step : sessionStepRepository.findAllBySessionAndStatusIn(session, ACTIVE_STEP_STATUSES)) {
			if (step.getStatus() == SessionStepStatus.RUNNING) {
				running.add(step);
			}
			step.setStatus(SessionStepStatus.ABORTED);
			step.setMessage(message);
		}
		sessionStepRepository.flush();
		return running;
	}

	public List<Session> getActiveSessions() {
		return sessionRepository.findAllByStatusIn(List.of(SessionStatus.INITIATED, SessionStatus.RUNNING));
	}

	public Page<Session> getSessions(final PageRequest page, final Plan plan, final SessionStatus status) {
		if (plan != null && status != null) {
			return sessionRepository.findAllByPlanAndStatus(plan, status, page);
		}
		if (plan != null) {
			return sessionRepository.findAllByPlan(plan, page);
		}
		if (status != null) {
			return sessionRepository.findAllByStatus(status, page);
		}
		return sessionRepository.findAll(page);
	}

	// ---------------------------------------------------------------------------------------- session steps

	@Transactional(rollbackFor = Exception.class)
	public SessionStep registerSessionStep(final Session session, final Step step, final Executor executor) {
		final Session current = getSession(session.getId());
		final Optional<SessionStep> existing = sessionStepRepository.findBySessionAndStepAndExecutionNumber(current, step, current.getExecutionNumber());
		if (existing.isPresent()) {
			existing.get().setExecutor(executor);
			return sessionStepRepository.saveAndFlush(existing.get());
		}
		return sessionStepRepository.saveAndFlush(new SessionStep(current, step, executor, current.getExecutionNumber()));
	}

	public SessionStep getSessionStep(final long id) {
		return sessionStepRepository.findById(id).orElseThrow(() -> new DataNotFoundException("Session step not found: " + id));
	}

	public Optional<SessionStep> findCurrentSessionStep(final Session session, final Step step) {
		final Session current = getSession(session.getId());
		return sessionStepRepository.findBySessionAndStepAndExecutionNumber(current, step, current.getExecutionNumber());
	}

	public List<SessionStep> getCurrentSessionSteps(final Session session, final List<Step> steps) {
		final Session current = getSession(session.getId());
		return steps.isEmpty() ? List.of() : sessionStepRepository.findAllBySessionAndExecutionNumberAndStepIn(current, current.getExecutionNumber(), steps);
	}

	public List<SessionStep> getCurrentSessionStepsOfAction(final Session session, final Action action) {
		final Session current = getSession(session.getId());
		return sessionStepRepository.findAllBySessionAndExecutionNumber(current, current.getExecutionNumber())
				.stream()
				.filter(s -> s.getStep().getAction().getId() == action.getId())
				.toList();
	}

	@Transactional(rollbackFor = Exception.class)
	public SessionStep changeSessionStepStatus(final long sessionStepId, final SessionStepStatus status, final String message) {
		final SessionStep step = getSessionStep(sessionStepId);
		step.setStatus(status);
		step.setMessage(message == null ? "" : message);
		return sessionStepRepository.saveAndFlush(step);
	}

	// Moves the current session step of 'step' from WAITING to RUNNING; false if it is not waiting (already claimed).
	@Transactional(rollbackFor = Exception.class)
	public boolean claimSessionStep(final Session session, final Step step, final String message) {
		final Optional<SessionStep> optional = findCurrentSessionStep(session, step);
		if (optional.isEmpty() || optional.get().getStatus() != SessionStepStatus.WAITING) {
			return false;
		}
		optional.get().setStatus(SessionStepStatus.RUNNING);
		optional.get().setMessage(message);
		sessionStepRepository.saveAndFlush(optional.get());
		return true;
	}

	@Transactional(rollbackFor = Exception.class)
	public SessionStep changeSessionStepExecutor(final long sessionStepId, final Executor executor) {
		final SessionStep step = getSessionStep(sessionStepId);
		step.setExecutor(executor);
		return sessionStepRepository.saveAndFlush(step);
	}

	public Page<SessionStep> getSessionSteps(final PageRequest page, final Session session, final SessionStepStatus status) {
		if (session != null && status != null) {
			return sessionStepRepository.findAllBySessionAndStatus(session, status, page);
		}
		if (session != null) {
			return sessionStepRepository.findAllBySession(session, page);
		}
		if (status != null) {
			return sessionStepRepository.findAllByStatus(status, page);
		}
		return sessionStepRepository.findAll(page);
	}

	// ---------------------------------------------------------------------------------------- worklog

	@Transactional(rollbackFor = Exception.class)
	public void worklog(final String planName, final String actionName, final String stepName, final Long sessionId, final Long executionNumber, final String message, final String exception) {
		worklogRepository.saveAndFlush(new Worklog(planName, actionName, stepName, sessionId, executionNumber, message, exception));
	}

	public void worklog(final String planName, final Long sessionId, final Long executionNumber, final String message, final String exception) {
		worklog(planName, null, null, sessionId, executionNumber, message, exception);
	}

	public Page<Worklog> getWorklogs(final PageRequest page, final Long sessionId, final String planName, final String actionName, final String stepName) {
		final Worklog probe = new Worklog(planName, actionName, stepName, sessionId, null, null, null);
		final ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues().withIgnorePaths("id", "createdAt");
		return worklogRepository.findAll(Example.of(probe, matcher), page);
	}
}
