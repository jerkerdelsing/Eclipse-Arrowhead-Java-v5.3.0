/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;

// A service the executor itself consumes while executing a step.
public record ExecutorDependencyDTO(
		OrchestrationServiceRequirementDTO serviceRequirement,
		Boolean localCloudOnly) {
}
