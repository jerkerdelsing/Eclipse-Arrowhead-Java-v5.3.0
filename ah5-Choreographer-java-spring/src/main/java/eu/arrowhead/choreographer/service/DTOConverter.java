/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.choreographer.api.dto.ActionResponseDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorResponseDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorServiceDefinitionResponseDTO;
import eu.arrowhead.choreographer.api.dto.PlanResponseDTO;
import eu.arrowhead.choreographer.api.dto.SessionResponseDTO;
import eu.arrowhead.choreographer.api.dto.SessionStepResponseDTO;
import eu.arrowhead.choreographer.api.dto.StepResponseDTO;
import eu.arrowhead.choreographer.api.dto.WorklogResponseDTO;
import eu.arrowhead.choreographer.jpa.entity.Action;
import eu.arrowhead.choreographer.jpa.entity.Executor;
import eu.arrowhead.choreographer.jpa.entity.Plan;
import eu.arrowhead.choreographer.jpa.entity.Session;
import eu.arrowhead.choreographer.jpa.entity.SessionStep;
import eu.arrowhead.choreographer.jpa.entity.Step;
import eu.arrowhead.choreographer.jpa.entity.Worklog;
import eu.arrowhead.choreographer.jpa.service.ExecutorDbService;
import eu.arrowhead.choreographer.jpa.service.PlanDbService;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;

@Component
public class DTOConverter {

	@Autowired
	private PlanDbService planDb;

	@Autowired
	private ExecutorDbService executorDb;

	public PlanResponseDTO plan(final Plan plan) {
		final List<ActionResponseDTO> actions = planDb.getActions(plan).stream().map(this::action).toList();
		return new PlanResponseDTO(plan.getId(), plan.getName(), plan.getFirstActionName(), actions, time(plan.getCreatedAt()), time(plan.getUpdatedAt()));
	}

	public ActionResponseDTO action(final Action action) {
		final List<Step> steps = planDb.getSteps(action);
		return new ActionResponseDTO(action.getId(), action.getName(), action.isFirstAction(), action.getNextActionName(),
				steps.stream().filter(Step::isFirstStep).map(Step::getName).toList(), steps.stream().map(this::step).toList(),
				time(action.getCreatedAt()), time(action.getUpdatedAt()));
	}

	public StepResponseDTO step(final Step step) {
		final Map<String, String> parameters = Utilities.fromJson(step.getStaticParameters(), new TypeReference<Map<String, String>>() { });
		return new StepResponseDTO(step.getId(), step.getName(), step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion(), step.getQuantity(),
				Utilities.fromJson(step.getServiceRequirement(), OrchestrationServiceRequirementDTO.class), step.isLocalCloudOnly(), parameters,
				planDb.getNextSteps(step).stream().map(Step::getName).toList(), time(step.getCreatedAt()), time(step.getUpdatedAt()));
	}

	public SessionResponseDTO session(final Session session) {
		return new SessionResponseDTO(session.getId(), session.getPlan().getId(), session.getPlan().getName(), session.getStatus(), session.getQuantityDone(),
				session.getQuantityGoal(), session.getExecutionNumber(), session.getOwner(), session.getNotifyUri(), time(session.getCreatedAt()), time(session.getUpdatedAt()));
	}

	public SessionStepResponseDTO sessionStep(final SessionStep step) {
		return new SessionStepResponseDTO(step.getId(), step.getSession().getId(), step.getStep().getId(), step.getSession().getPlan().getName(),
				step.getStep().getAction().getName(), step.getStep().getName(), step.getExecutor() == null ? null : step.getExecutor().getName(),
				step.getStatus(), step.getExecutionNumber(), step.getMessage(), time(step.getCreatedAt()), time(step.getUpdatedAt()));
	}

	public WorklogResponseDTO worklog(final Worklog entry) {
		return new WorklogResponseDTO(entry.getId(), time(entry.getCreatedAt()), entry.getPlanName(), entry.getActionName(), entry.getStepName(),
				entry.getSessionId(), entry.getExecutionNumber(), entry.getMessage(), entry.getException());
	}

	public ExecutorResponseDTO executor(final Executor executor) {
		final List<ExecutorServiceDefinitionResponseDTO> definitions = executorDb.getServiceDefinitions(executor)
				.stream()
				.map(d -> new ExecutorServiceDefinitionResponseDTO(d.getServiceDefinition(), d.getMinVersion(), d.getMaxVersion()))
				.toList();
		return new ExecutorResponseDTO(executor.getId(), executor.getName(), executor.getAddress(), executor.getPort(), executor.getBasePath(), definitions,
				time(executor.getCreatedAt()), time(executor.getUpdatedAt()));
	}

	private static String time(final java.time.ZonedDateTime time) {
		return time == null ? null : Utilities.convertZonedDateTimeToUTCString(time);
	}
}
