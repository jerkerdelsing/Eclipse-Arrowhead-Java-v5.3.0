/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.choreographer.api.dto.ActionRequestDTO;
import eu.arrowhead.choreographer.api.dto.PlanRequestDTO;
import eu.arrowhead.choreographer.api.dto.StepRequestDTO;
import eu.arrowhead.choreographer.graph.StepGraph;
import eu.arrowhead.choreographer.graph.StepGraphUtils;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;

// Validates and normalizes plans with the v4.6 rules: existing and unique names, reachable actions and steps,
// no cycles, and normalized step graphs (implied next-step edges removed).
@Service
public class PlanValidator {

	public static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.-]{0,62}$");

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefinitionNameNormalizer;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	public PlanRequestDTO validateAndNormalize(final PlanRequestDTO request, final String origin) {
		if (request == null) {
			throw new InvalidParameterException("Request is null", origin);
		}
		final String name = name(request.name(), "Plan name", origin);
		final String firstAction = name(request.firstActionName(), "First action name", origin);
		if (Utilities.isEmpty(request.actions())) {
			throw new InvalidParameterException("Action list is empty", origin);
		}

		final Map<String, ActionRequestDTO> actions = new LinkedHashMap<>();
		for (final ActionRequestDTO action : request.actions()) {
			final ActionRequestDTO validated = validateAction(action, origin);
			if (actions.put(validated.name(), validated) != null) {
				throw new InvalidParameterException("Action name duplication found: " + validated.name(), origin);
			}
		}
		if (!actions.containsKey(firstAction)) {
			throw new InvalidParameterException("First action is not found in the action list: " + firstAction, origin);
		}
		for (final ActionRequestDTO action : actions.values()) {
			if (action.nextActionName() != null && !actions.containsKey(action.nextActionName())) {
				throw new InvalidParameterException("Action not found: " + action.nextActionName(), origin);
			}
		}
		// actions form one chain from the first action: no cycle, every action reachable
		final Set<String> visited = new HashSet<>();
		String current = firstAction;
		while (current != null) {
			if (!visited.add(current)) {
				throw new InvalidParameterException("An action references a previous action as its next action (or itself): " + current, origin);
			}
			current = actions.get(current).nextActionName();
		}
		for (final String action : actions.keySet()) {
			if (!visited.contains(action)) {
				throw new InvalidParameterException("Unreachable action detected: " + action, origin);
			}
		}

		return new PlanRequestDTO(name, firstAction, new ArrayList<>(actions.values()));
	}

	//-------------------------------------------------------------------------------------------------
	private ActionRequestDTO validateAction(final ActionRequestDTO action, final String origin) {
		if (action == null) {
			throw new InvalidParameterException("Action is null", origin);
		}
		final String name = name(action.name(), "Action name", origin);
		final String next = Utilities.isEmpty(action.nextActionName()) ? null : name(action.nextActionName(), "Next action name", origin);
		if (name.equals(next)) {
			throw new InvalidParameterException("Action references itself as next action: " + name, origin);
		}
		if (Utilities.isEmpty(action.firstStepNames())) {
			throw new InvalidParameterException("First step list of action " + name + " is empty", origin);
		}
		if (Utilities.isEmpty(action.steps())) {
			throw new InvalidParameterException("Step list of action " + name + " is empty", origin);
		}
		final List<String> firstSteps = new ArrayList<>();
		for (final String step : action.firstStepNames()) {
			firstSteps.add(name(step, "First step name", origin));
		}

		final Map<String, StepRequestDTO> steps = new LinkedHashMap<>();
		for (final StepRequestDTO step : action.steps()) {
			final StepRequestDTO validated = validateStep(step, origin);
			if (steps.put(validated.name(), validated) != null) {
				throw new InvalidParameterException("Step name duplication found in action " + name + ": " + validated.name(), origin);
			}
		}
		for (final String first : firstSteps) {
			if (!steps.containsKey(first)) {
				throw new InvalidParameterException("First step " + first + " is not found in the step list of action " + name, origin);
			}
		}

		final StepGraph graph = new StepGraph();
		steps.keySet().forEach(graph::addNode);
		for (final StepRequestDTO step : steps.values()) {
			for (final String nextStep : step.nextStepNames()) {
				if (!steps.containsKey(nextStep)) {
					throw new InvalidParameterException("Step not found: " + nextStep, origin);
				}
				graph.addEdge(step.name(), nextStep);
			}
		}
		if (StepGraphUtils.hasCycle(graph)) {
			throw new InvalidParameterException("Circular reference detected between the steps of action: " + name, origin);
		}
		final Map<String, Integer> inDegree = graph.inDegrees();
		for (final String step : steps.keySet()) {
			final boolean first = firstSteps.contains(step);
			if (first && inDegree.get(step) > 0) {
				throw new InvalidParameterException("First step " + step + " of action " + name + " is also the next step of another step", origin);
			}
			if (!first && inDegree.get(step) == 0) {
				throw new InvalidParameterException("Step is unreachable: " + step, origin);
			}
		}

		final StepGraph normalized = StepGraphUtils.normalize(graph);
		final List<StepRequestDTO> result = new ArrayList<>();
		for (final StepRequestDTO step : steps.values()) {
			result.add(new StepRequestDTO(step.name(), step.quantity(), step.serviceRequirement(), step.localCloudOnly(), step.staticParameters(),
					List.copyOf(normalized.nextOf(step.name()))));
		}

		return new ActionRequestDTO(name, next, firstSteps, result);
	}

	//-------------------------------------------------------------------------------------------------
	private StepRequestDTO validateStep(final StepRequestDTO step, final String origin) {
		if (step == null) {
			throw new InvalidParameterException("Step is null", origin);
		}
		final String name = name(step.name(), "Step name", origin);
		final int quantity = step.quantity() == null ? 1 : step.quantity();
		if (quantity < 1) {
			throw new InvalidParameterException("Quantity of step " + name + " must be at least 1", origin);
		}
		final OrchestrationServiceRequirementDTO requirement = step.serviceRequirement();
		if (requirement == null) {
			throw new InvalidParameterException("Service requirement of step " + name + " is missing", origin);
		}
		if (Utilities.isEmpty(requirement.serviceDefinition())) {
			throw new InvalidParameterException("Service definition of step " + name + " is missing", origin);
		}
		final String serviceDefinition = serviceDefinitionNameNormalizer.normalize(requirement.serviceDefinition());
		try {
			serviceDefinitionNameValidator.validateServiceDefinitionName(serviceDefinition);
			Versions.window(requirement.versions());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException("Step " + name + ": " + ex.getMessage(), origin);
		}
		final OrchestrationServiceRequirementDTO normalized = new OrchestrationServiceRequirementDTO(
				serviceDefinition, requirement.operations(), requirement.versions(), requirement.alivesAt(), requirement.metadataRequirements(),
				requirement.interfaceTemplateNames(), requirement.interfaceAddressTypes(), requirement.interfacePropertyRequirements(),
				requirement.securityPolicies(), requirement.preferredProviders());
		final List<String> nextSteps = new ArrayList<>();
		if (step.nextStepNames() != null) {
			for (final String next : step.nextStepNames()) {
				nextSteps.add(name(next, "Next step name", origin));
			}
		}
		final Map<String, String> parameters = step.staticParameters() == null ? new HashMap<>() : step.staticParameters();

		return new StepRequestDTO(name, quantity, normalized, Boolean.TRUE.equals(step.localCloudOnly()), parameters, nextSteps);
	}

	//-------------------------------------------------------------------------------------------------
	static String name(final String value, final String what, final String origin) {
		if (Utilities.isEmpty(value)) {
			throw new InvalidParameterException(what + " is null or blank", origin);
		}
		final String trimmed = value.trim();
		if (!NAME_PATTERN.matcher(trimmed).matches()) {
			throw new InvalidParameterException(what + " has an invalid format (letters, digits, '_', '.', '-'; at most 63 characters): " + trimmed, origin);
		}
		return trimmed;
	}
}
