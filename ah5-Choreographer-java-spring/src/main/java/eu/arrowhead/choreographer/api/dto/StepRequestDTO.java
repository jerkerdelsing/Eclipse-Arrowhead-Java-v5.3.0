/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import java.util.List;
import java.util.Map;

import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;

// A step: one service consumption performed by an executor; serviceRequirement selects the provider (v5 orchestration requirement).
public record StepRequestDTO(
		String name,
		Integer quantity,
		OrchestrationServiceRequirementDTO serviceRequirement,
		Boolean localCloudOnly,
		Map<String, String> staticParameters,
		List<String> nextStepNames) {
}
