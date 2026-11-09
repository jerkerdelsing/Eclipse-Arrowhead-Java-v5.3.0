/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.choreographer.ChoreographerConstants;
import eu.arrowhead.choreographer.api.dto.CheckPlanResponseDTO;
import eu.arrowhead.choreographer.engine.ExecutorData;
import eu.arrowhead.choreographer.engine.ExecutorSelector;
import eu.arrowhead.choreographer.jpa.entity.Plan;
import eu.arrowhead.choreographer.jpa.entity.Step;
import eu.arrowhead.choreographer.jpa.service.ExecutorDbService;
import eu.arrowhead.choreographer.jpa.service.PlanDbService;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;

// Checks whether a plan can be executed (port of the v4.6 ChoreographerPlanExecutionChecker): executors exist for every
// step, ServiceRegistry knows providers for every step and, optionally, the selected executors' dependencies are satisfiable.
@Service
public class PlanExecutionChecker {

	public static final String EXECUTOR_NOT_FOUND = "Executor not found for step: ";
	public static final String PROVIDER_NOT_FOUND = "Provider not found for step: ";
	public static final String CONNECTION_PROBLEM = "Something happened when connecting to other core services: ";

	@Autowired
	private PlanDbService planDb;

	@Autowired
	private ExecutorDbService executorDb;

	@Autowired
	private ExecutorSelector selector;

	@Autowired
	private ChoreographerDriver driver;

	@Value(ChoreographerConstants.$MAX_PLAN_ITERATION_WD)
	private long maxIteration;

	public CheckPlanResponseDTO check(final Plan plan, final long quantity, final boolean allowInterCloud, final boolean dependencyCheck) {
		final List<String> errors = new ArrayList<>();
		if (quantity <= 0) {
			errors.add("Quantity must be greater than 0.");
		}
		if (quantity > maxIteration) {
			errors.add("Quantity could not be greater than " + maxIteration + ".");
		}

		boolean needInterCloud = false;
		final List<Step> withExecutors = new ArrayList<>();
		for (final Step step : planDb.collectStepsFromPlan(plan)) {
			if (executorDb.getExecutorsFor(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion()).isEmpty()) {
				errors.add(EXECUTOR_NOT_FOUND + fullName(step));
			} else {
				withExecutors.add(step);
			}
			try {
				if (!driver.isServiceAvailable(Utilities.fromJson(step.getServiceRequirement(), OrchestrationServiceRequirementDTO.class))) {
					if (allowInterCloud && !step.isLocalCloudOnly()) {
						needInterCloud = true;
					} else {
						errors.add(PROVIDER_NOT_FOUND + fullName(step));
					}
				}
			} catch (final Exception ex) {
				errors.add(CONNECTION_PROBLEM + ex.getMessage());
			}
		}

		if (dependencyCheck) {
			for (final Step step : withExecutors) {
				final ExecutorData selected = selector.select(step.getServiceDefinition(), step.getMinVersion(), step.getMaxVersion(), null, allowInterCloud, false);
				if (selected == null) {
					errors.add(EXECUTOR_NOT_FOUND + fullName(step));
				} else if (selected.usesOtherClouds()) {
					needInterCloud = true;
				}
			}
		}

		return new CheckPlanResponseDTO(plan.getId(), errors, needInterCloud);
	}

	static String fullName(final Step step) {
		return step.getAction().getPlan().getName() + ChoreographerConstants.DOT + step.getAction().getName() + ChoreographerConstants.DOT + step.getName();
	}
}
