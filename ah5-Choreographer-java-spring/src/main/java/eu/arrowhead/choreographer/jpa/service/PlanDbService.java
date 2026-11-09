/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.choreographer.api.dto.ActionRequestDTO;
import eu.arrowhead.choreographer.api.dto.PlanRequestDTO;
import eu.arrowhead.choreographer.api.dto.StepRequestDTO;
import eu.arrowhead.choreographer.api.dto.enums.SessionStatus;
import eu.arrowhead.choreographer.jpa.entity.Action;
import eu.arrowhead.choreographer.jpa.entity.Plan;
import eu.arrowhead.choreographer.jpa.entity.Session;
import eu.arrowhead.choreographer.jpa.entity.Step;
import eu.arrowhead.choreographer.jpa.entity.StepConnection;
import eu.arrowhead.choreographer.jpa.repository.ActionRepository;
import eu.arrowhead.choreographer.jpa.repository.PlanRepository;
import eu.arrowhead.choreographer.jpa.repository.SessionRepository;
import eu.arrowhead.choreographer.jpa.repository.SessionStepRepository;
import eu.arrowhead.choreographer.jpa.repository.StepConnectionRepository;
import eu.arrowhead.choreographer.jpa.repository.StepRepository;
import eu.arrowhead.choreographer.service.Versions;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;

@Service
public class PlanDbService {

	@Autowired
	private PlanRepository planRepository;

	@Autowired
	private ActionRepository actionRepository;

	@Autowired
	private StepRepository stepRepository;

	@Autowired
	private StepConnectionRepository connectionRepository;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private SessionStepRepository sessionStepRepository;

	// The request must be validated and normalized (PlanValidator).
	@Transactional(rollbackFor = Exception.class)
	public Plan createPlan(final PlanRequestDTO plan) {
		if (planRepository.findByName(plan.name()).isPresent()) {
			throw new InvalidParameterException("Plan with name " + plan.name() + " already exists");
		}
		final Plan saved = planRepository.saveAndFlush(new Plan(plan.name(), plan.firstActionName()));
		for (final ActionRequestDTO actionDTO : plan.actions()) {
			final Action action = actionRepository.saveAndFlush(new Action(saved, actionDTO.name(), actionDTO.name().equals(plan.firstActionName()), actionDTO.nextActionName()));
			final Map<String, Step> steps = new HashMap<>();
			for (final StepRequestDTO stepDTO : actionDTO.steps()) {
				final int[] window = Versions.window(stepDTO.serviceRequirement().versions());
				steps.put(stepDTO.name(), stepRepository.save(new Step(action, stepDTO.name(), actionDTO.firstStepNames().contains(stepDTO.name()),
						stepDTO.serviceRequirement().serviceDefinition(), window == null ? null : window[0], window == null ? null : window[1],
						Utilities.toJson(stepDTO.serviceRequirement()), Boolean.TRUE.equals(stepDTO.localCloudOnly()),
						Utilities.toJson(stepDTO.staticParameters()), stepDTO.quantity())));
			}
			stepRepository.flush();
			final List<StepConnection> connections = new ArrayList<>();
			for (final StepRequestDTO stepDTO : actionDTO.steps()) {
				for (final String next : stepDTO.nextStepNames()) {
					connections.add(new StepConnection(steps.get(stepDTO.name()), steps.get(next)));
				}
			}
			connectionRepository.saveAllAndFlush(connections);
		}
		return saved;
	}

	public Plan getPlan(final long id) {
		return planRepository.findById(id).orElseThrow(() -> new DataNotFoundException("Plan not found: " + id));
	}

	public Optional<Plan> findPlanByName(final String name) {
		return planRepository.findByName(name);
	}

	public Page<Plan> getPlans(final PageRequest page) {
		return planRepository.findAll(page);
	}

	public List<Action> getActions(final Plan plan) {
		return actionRepository.findAllByPlan(plan);
	}

	public Action getAction(final Plan plan, final String name) {
		return actionRepository.findByPlanAndName(plan, name).orElseThrow(() -> new DataNotFoundException("Action not found: " + name));
	}

	public List<Step> getSteps(final Action action) {
		return stepRepository.findAllByAction(action);
	}

	public List<Step> getFirstSteps(final Action action) {
		return stepRepository.findAllByActionAndFirstStepTrue(action);
	}

	public List<Step> collectStepsFromPlan(final Plan plan) {
		final List<Action> actions = actionRepository.findAllByPlan(plan);
		return actions.isEmpty() ? List.of() : stepRepository.findAllByActionIn(actions);
	}

	public List<Step> getNextSteps(final Step step) {
		return connectionRepository.findAllByFrom(step).stream().map(StepConnection::getTo).toList();
	}

	public List<Step> getPreviousSteps(final Step step) {
		return connectionRepository.findAllByTo(step).stream().map(StepConnection::getFrom).toList();
	}

	@Transactional(rollbackFor = Exception.class)
	public void removePlan(final long id) {
		final Optional<Plan> optional = planRepository.findById(id);
		if (optional.isEmpty()) {
			return;
		}
		final Plan plan = optional.get();
		if (!sessionRepository.findAllByPlanAndStatusIn(plan, List.of(SessionStatus.INITIATED, SessionStatus.RUNNING)).isEmpty()) {
			throw new LockedException("Plan " + plan.getName() + " has active sessions");
		}
		for (final Session session : sessionRepository.findAllByPlan(plan)) {
			sessionStepRepository.deleteAll(sessionStepRepository.findAllBySession(session));
		}
		sessionRepository.deleteAll(sessionRepository.findAllByPlan(plan));
		final List<Step> steps = collectStepsFromPlan(plan);
		if (!steps.isEmpty()) {
			connectionRepository.deleteAll(connectionRepository.findAllByFromIn(steps));
			stepRepository.deleteAll(steps);
		}
		actionRepository.deleteAll(actionRepository.findAllByPlan(plan));
		planRepository.delete(plan);
		planRepository.flush();
	}
}
