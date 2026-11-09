/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import java.util.List;
import java.util.Map;

import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;

public record StepResponseDTO(
		long id,
		String name,
		String serviceDefinition,
		Integer minVersion,
		Integer maxVersion,
		int quantity,
		OrchestrationServiceRequirementDTO serviceRequirement,
		boolean localCloudOnly,
		Map<String, String> staticParameters,
		List<String> nextStepNames,
		String createdAt,
		String updatedAt) {
}
