/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.choreographer.ChoreographerConstants;
import eu.arrowhead.choreographer.api.dto.AbortStepRequestDTO;
import eu.arrowhead.choreographer.api.dto.ExecuteStepRequestDTO;
import eu.arrowhead.choreographer.api.dto.ExecutedStepResultDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorDependencyDTO;
import eu.arrowhead.choreographer.api.dto.SessionNotificationDTO;
import eu.arrowhead.choreographer.api.dto.enums.SessionStatus;
import eu.arrowhead.choreographer.api.dto.enums.SessionStepStatus;
import eu.arrowhead.choreographer.jpa.entity.Action;
import eu.arrowhead.choreographer.jpa.entity.Plan;
import eu.arrowhead.choreographer.jpa.entity.Session;
import eu.arrowhead.choreographer.jpa.entity.SessionStep;
import eu.arrowhead.choreographer.jpa.entity.Step;
import eu.arrowhead.choreographer.jpa.service.PlanDbService;
import eu.arrowhead.choreographer.jpa.service.SessionDbService;
import eu.arrowhead.choreographer.service.ChoreographerDriver;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;

// Executes sessions (port of the v4.6 ChoreographerService): selects executors for all steps, runs the first steps of
// each action, orchestrates every step's provider on behalf of its executor, starts the executor and continues when
// executors report their steps done; repeats the plan quantityGoal times.
@Component
public class ChoreographerEngine {

	public static final String START_SESSION_MSG = "Session is started.";
	public static final String START_PLAN_MSG = "Plan execution is started.";
	public static final String FINISH_PLAN_MSG = "Plan execution is finished.";
	public static final String FINISH_SESSION_MSG = "Session is finished.";
	public static final String ABORT_SESSION_MSG = "Session is aborted.";

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private PlanDbService planDb;

	@Autowired
	private SessionDbService sessionDb;

	@Autowired
	private ExecutorSelector selector;

	@Autowired
	private ChoreographerDriver driver;

	@Autowired
	@Qualifier(EngineConfig.WORKER)
	private TaskExecutor worker;

	private final Map<Long, SessionExecutorCache> caches = new ConcurrentHashMap<>();
	private final Map<Long, Object> locks = new ConcurrentHashMap<>();

	//=================================================================================================
	// entry points

	public void startSessionAsync(final long sessionId) {
		submit(sessionId, () -> startSession(sessionId));
	}

	public void stepDoneAsync(final ExecutedStepResultDTO result) {
		submit(result.sessionId(), () -> stepDone(result));
	}

	public int runningSessions() {
		return caches.size();
	}

	// Aborts the session, tells the executors of its running steps to abort (except the step that caused the abort).
	public void abortSession(final long sessionId, final Long causingSessionStepId, final String message) {
		final Optional<Session> optional = sessionDb.findSession(sessionId);
		if (optional.isEmpty() || !optional.get().getStatus().isActive()) {
			return;
		}
		final Session session = optional.get();
		final String text = message == null ? "" : message.trim();
		final List<SessionStep> running;
		synchronized (lock(sessionId)) {
			running = sessionDb.abortSession(sessionId, text);
			final SessionExecutorCache cache = caches.remove(sessionId);
			if (cache != null) {
				cache.finish();
			}
		}
		for (final SessionStep step : running) {
			if (causingSessionStepId != null && causingSessionStepId == step.getId() || step.getExecutor() == null) {
				continue;
			}
			try {
				driver.abortExecutor(step.getExecutor(), new AbortStepRequestDTO(sessionId, step.getId()));
			} catch (final Exception ex) {
				logger.warn("Unable to send abort message to executor {}: {}", step.getExecutor().getName(), ex.getMessage());
				sessionDb.worklog(session.getPlan().getName(), step.getStep().getAction().getName(), step.getStep().getName(), sessionId, session.getExecutionNumber(),
						"Unable to send abort message to the executor", ex.getMessage());
			}
		}
		locks.remove(sessionId);
		sessionDb.worklog(session.getPlan().getName(), sessionId, session.getExecutionNumber(), ABORT_SESSION_MSG + " " + text, null);
		notify(sessionId, ABORT_SESSION_MSG, "Execution: " + session.getExecutionNumber() + "/" + session.getQuantityGoal() + ". " + text);
	}

	// Sessions that were active when the Choreographer stopped cannot be continued: their executor state is lost.
	@EventListener(ApplicationReadyEvent.class)
	public void abortSessionsOfPreviousRun() {
		for (final Session session : sessionDb.getActiveSessions()) {
			abortSession(session.getId(), null, "The Choreographer restarted; the session cannot be continued.");
		}
	}

	//=================================================================================================
	// session start

	private void startSession(final long sessionId) {
		Session session = sessionDb.changeSessionStatus(sessionId, SessionStatus.RUNNING);
		final Plan plan = session.getPlan();
		sessionDb.worklog(plan.getName(), sessionId, session.getExecutionNumber(), START_SESSION_MSG, null);
		notify(sessionId, START_SESSION_MSG, "Quantity goal: " + session.getQuantityGoal());

		session = sessionDb.increaseExecutionNumber(sessionId);
		final SessionExecutorCache cache = new SessionExecutorCache(session.isAllowInterCloud(), session.isChooseOptimalExecutor());
		caches.put(sessionId, cache);
		registerSessionSteps(session, cache);
		sessionDb.worklog(plan.getName(), sessionId, session.getExecutionNumber(), "Found executors to all steps.", null);

		final List<Step> toRun;
		synchronized (lock(sessionId)) {
			sessionDb.worklog(plan.getName(), sessionId, session.getExecutionNumber(), START_PLAN_MSG, null);
			notify(sessionId, START_PLAN_MSG, "Execution: " + session.getExecutionNumber() + "/" + session.getQuantityGoal());
			toRun = claimFirstSteps(session, planDb.getAction(plan, plan.getFirstActionName()));
		}
		dispatch(sessionId, toRun);
	}

	// Selects (or reuses) an executor for every step and creates the session steps of the current execution.
	private void registerSessionSteps(final Session session, final SessionExecutorCache cache) {
		for (final Step step : planDb.collectStepsFromPlan(session.getPlan())) {
			ExecutorData data = cache.get(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion());
			if (data == null) {
				data = selector.select(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion(), cache.getExclusions(), cache.isAllowInterCloud(), cache.isChooseOptimalExecutor());
				if (data == null) {
					throw new ChoreographerSessionException(session.getId(), null, "Can't find properly working executor for step: " + fullName(step));
				}
				cache.put(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion(), data);
			}
			sessionDb.registerSessionStep(session, step, data.executor());
		}
	}

	private List<Step> claimFirstSteps(final Session session, final Action action) {
		final List<Step> claimed = new ArrayList<>();
		for (final Step step : planDb.getFirstSteps(action)) {
			if (sessionDb.claimSessionStep(session, step, "Running step: " + fullName(step))) {
				claimed.add(step);
			}
		}
		return claimed;
	}

	private void dispatch(final long sessionId, final List<Step> steps) {
		for (final Step step : steps) {
			submit(sessionId, () -> executeStep(sessionId, step));
		}
	}

	//=================================================================================================
	// step execution

	private void executeStep(final long sessionId, final Step step) {
		final SessionExecutorCache cache = caches.get(sessionId);
		if (cache == null || cache.isFinished()) {
			return;
		}
		final Session session = sessionDb.getSession(sessionId);
		final SessionStep sessionStep = sessionDb.findCurrentSessionStep(session, step)
				.orElseThrow(() -> new ChoreographerSessionException(sessionId, null, "Session step not found for step: " + fullName(step)));
		if (sessionStep.getStatus() != SessionStepStatus.RUNNING) {
			return;
		}

		while (true) {
			final ExecutorData data = executorFor(sessionId, sessionStep, step, cache);
			final String executorName = data.executor().getName();

			final List<OrchestrationResultDTO> preconditions;
			try {
				preconditions = orchestrateDependencies(executorName, data.dependencies(), cache);
			} catch (final Exception ex) {
				logger.warn("Precondition orchestration for executor {} failed: {}", executorName, ex.getMessage());
				exclude(cache, step, data);
				continue;
			}

			final OrchestrationResponseDTO main;
			try {
				main = driver.orchestrateFor(executorName, requirement(step), cache.isAllowInterCloud() && !step.isLocalCloudOnly());
			} catch (final Exception ex) {
				throw new ChoreographerSessionException(sessionId, sessionStep.getId(), "Problem occurred while orchestration for step " + fullName(step) + ".", ex);
			}
			if (main == null || Utilities.isEmpty(main.results())) {
				// no provider the executor is authorized to use: another executor may be
				sessionDb.worklog(session.getPlan().getName(), step.getAction().getName(), step.getName(), sessionId, session.getExecutionNumber(),
						"No providers found for executor " + executorName, null);
				exclude(cache, step, data);
				continue;
			}

			final ExecuteStepRequestDTO payload = new ExecuteStepRequestDTO(sessionId, sessionStep.getId(), preconditions, main.results().get(0), step.getQuantity(), parameters(step));
			try {
				driver.startExecutor(data.executor(), payload);
				return;
			} catch (final Exception ex) {
				logger.warn("Unable to start executor {}: {}", executorName, ex.getMessage());
				exclude(cache, step, data); // try again with another executor
			}
		}
	}

	private ExecutorData executorFor(final long sessionId, final SessionStep sessionStep, final Step step, final SessionExecutorCache cache) {
		ExecutorData data = cache.get(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion());
		if (data == null) {
			data = selector.select(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion(), cache.getExclusions(), cache.isAllowInterCloud(), cache.isChooseOptimalExecutor());
			if (data == null) {
				throw new ChoreographerSessionException(sessionId, sessionStep.getId(), "Can't find properly working executor for step: " + fullName(step));
			}
			cache.put(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion(), data);
		}
		if (sessionStep.getExecutor() == null || sessionStep.getExecutor().getId() != data.executor().getId()) {
			sessionDb.changeSessionStepExecutor(sessionStep.getId(), data.executor());
		}
		return data;
	}

	private List<OrchestrationResultDTO> orchestrateDependencies(final String executorName, final List<ExecutorDependencyDTO> dependencies, final SessionExecutorCache cache) {
		final List<OrchestrationResultDTO> results = new ArrayList<>();
		for (final ExecutorDependencyDTO dependency : dependencies) {
			final OrchestrationResponseDTO response = driver.orchestrateFor(executorName, dependency.serviceRequirement(),
					cache.isAllowInterCloud() && !Boolean.TRUE.equals(dependency.localCloudOnly()));
			if (response == null || Utilities.isEmpty(response.results())) {
				throw new IllegalStateException("No provider for dependency " + dependency.serviceRequirement().serviceDefinition());
			}
			results.add(response.results().get(0));
		}
		return results;
	}

	private static void exclude(final SessionExecutorCache cache, final Step step, final ExecutorData data) {
		cache.remove(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion());
		cache.getExclusions().add(data.executor().getId());
	}

	//=================================================================================================
	// step results

	private void stepDone(final ExecutedStepResultDTO result) {
		final SessionStep sessionStep = sessionDb.getSessionStep(result.sessionStepId());
		switch (result.status()) {
		case SUCCESS -> handleSuccess(sessionStep);
		case ABORTED -> sessionDb.worklog(sessionStep.getSession().getPlan().getName(), sessionStep.getStep().getAction().getName(), sessionStep.getStep().getName(),
				result.sessionId(), sessionStep.getSession().getExecutionNumber(), "The execution of this step has aborted successfully.", null);
		case ERROR, FATAL_ERROR -> handleError(sessionStep, result);
		default -> throw new IllegalArgumentException("Invalid status: " + result.status());
		}
	}

	private void handleError(final SessionStep sessionStep, final ExecutedStepResultDTO result) {
		final long sessionId = sessionStep.getSession().getId();
		final String detail = result.message() + (result.exception() == null ? "" : " " + result.exception());
		if (result.status().isFatal()) {
			abortSession(sessionId, sessionStep.getId(), detail);
			return;
		}
		// not fatal: another executor may be able to do the step
		final Step step = sessionStep.getStep();
		sessionDb.worklog(sessionStep.getSession().getPlan().getName(), step.getAction().getName(), step.getName(), sessionId, sessionStep.getSession().getExecutionNumber(),
				result.message(), result.exception());
		final SessionExecutorCache cache = caches.get(sessionId);
		if (cache == null || cache.isFinished()) {
			return;
		}
		cache.remove(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion());
		if (sessionStep.getExecutor() != null) {
			cache.getExclusions().add(sessionStep.getExecutor().getId());
		}
		final ExecutorData replacement = selector.select(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion(), cache.getExclusions(),
				cache.isAllowInterCloud(), cache.isChooseOptimalExecutor());
		if (replacement == null) {
			abortSession(sessionId, sessionStep.getId(), detail);
			return;
		}
		cache.put(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion(), replacement);
		sessionDb.changeSessionStepExecutor(sessionStep.getId(), replacement.executor());
		executeStep(sessionId, step);
	}

	private void handleSuccess(final SessionStep finished) {
		final long sessionId = finished.getSession().getId();
		final List<Step> toRun = new ArrayList<>();
		synchronized (lock(sessionId)) {
			final SessionExecutorCache cache = caches.get(sessionId);
			final SessionStep current = sessionDb.getSessionStep(finished.getId());
			if (cache == null || cache.isFinished() || current.getStatus() != SessionStepStatus.RUNNING) {
				return; // session over, or duplicate notification
			}
			sessionDb.changeSessionStepStatus(current.getId(), SessionStepStatus.DONE, "Step finished successfully.");
			Session session = sessionDb.getSession(sessionId);
			final Step step = current.getStep();
			final List<Step> nextSteps = planDb.getNextSteps(step);

			if (!nextSteps.isEmpty()) {
				for (final Step next : nextSteps) {
					// a next step is executable when all of its previous steps are done
					final List<SessionStep> previous = sessionDb.getCurrentSessionSteps(session, planDb.getPreviousSteps(next));
					if (previous.stream().allMatch(s -> s.getStatus() == SessionStepStatus.DONE)
							&& sessionDb.claimSessionStep(session, next, "Running step: " + fullName(next))) {
						toRun.add(next);
					}
				}
			} else if (sessionDb.getCurrentSessionStepsOfAction(session, step.getAction()).stream().allMatch(s -> s.getStatus() == SessionStepStatus.DONE)) {
				final Action action = step.getAction();
				if (action.getNextActionName() != null) {
					toRun.addAll(claimFirstSteps(session, planDb.getAction(session.getPlan(), action.getNextActionName())));
				} else {
					// last step of the last action
					session = sessionDb.increaseQuantityDone(sessionId);
					sessionDb.worklog(session.getPlan().getName(), sessionId, session.getExecutionNumber(), FINISH_PLAN_MSG, null);
					notify(sessionId, FINISH_PLAN_MSG, "Quantity: " + session.getQuantityDone() + "/" + session.getQuantityGoal());
					if (session.getQuantityDone() < session.getQuantityGoal()) {
						session = sessionDb.increaseExecutionNumber(sessionId);
						registerSessionSteps(session, cache);
						sessionDb.worklog(session.getPlan().getName(), sessionId, session.getExecutionNumber(), START_PLAN_MSG, null);
						notify(sessionId, START_PLAN_MSG, "Execution: " + session.getExecutionNumber() + "/" + session.getQuantityGoal());
						toRun.addAll(claimFirstSteps(session, planDb.getAction(session.getPlan(), session.getPlan().getFirstActionName())));
					} else {
						sessionDone(session, cache);
					}
				}
			}
		}
		dispatch(sessionId, toRun);
	}

	private void sessionDone(final Session session, final SessionExecutorCache cache) {
		final Session done = sessionDb.changeSessionStatus(session.getId(), SessionStatus.DONE);
		cache.finish();
		caches.remove(session.getId());
		sessionDb.worklog(done.getPlan().getName(), done.getId(), done.getExecutionNumber(), FINISH_SESSION_MSG, null);
		notify(done.getId(), FINISH_SESSION_MSG, "Number of executions: " + done.getQuantityDone());
	}

	//=================================================================================================
	// assistant methods

	private void submit(final long sessionId, final Runnable task) {
		worker.execute(() -> {
			try {
				task.run();
			} catch (final ChoreographerSessionException ex) {
				logger.warn("Session {} aborted: {}", ex.getSessionId(), ex.getDetailedMessage());
				abortSession(ex.getSessionId(), ex.getSessionStepId(), ex.getDetailedMessage());
			} catch (final Exception ex) {
				logger.error("Unexpected error in session {}", sessionId, ex);
				abortSession(sessionId, null, "Unexpected error: " + ex.getMessage());
			}
		});
	}

	private Object lock(final long sessionId) {
		return locks.computeIfAbsent(sessionId, id -> new Object());
	}

	private void notify(final long sessionId, final String message, final String details) {
		try {
			final Session session = sessionDb.getSession(sessionId);
			if (!Utilities.isEmpty(session.getNotifyUri())) {
				driver.sendSessionNotification(session.getNotifyUri(), new SessionNotificationDTO(session.getId(), session.getPlan().getId(), session.getPlan().getName(),
						Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow()), session.getStatus(), message, details));
			}
		} catch (final Exception ex) {
			// notification problems must not affect the plan execution
			logger.warn("Unable to send session notification: {}", ex.getMessage());
		}
	}

	static String fullName(final Step step) {
		return step.getAction().getPlan().getName() + ChoreographerConstants.DOT + step.getAction().getName() + ChoreographerConstants.DOT + step.getName();
	}

	private static OrchestrationServiceRequirementDTO requirement(final Step step) {
		return Utilities.fromJson(step.getServiceRequirement(), OrchestrationServiceRequirementDTO.class);
	}

	private static Map<String, String> parameters(final Step step) {
		final Map<String, String> parameters = Utilities.fromJson(step.getStaticParameters(), new TypeReference<Map<String, String>>() { });
		return parameters == null ? Map.of() : parameters;
	}
}
