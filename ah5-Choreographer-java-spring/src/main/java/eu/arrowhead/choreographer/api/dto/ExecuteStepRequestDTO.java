/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import java.util.List;
import java.util.Map;

import eu.arrowhead.dto.OrchestrationResultDTO;

// POST <executor>/start: orchestration results were obtained on behalf of the executor.
public record ExecuteStepRequestDTO(
		long sessionId,
		long sessionStepId,
		List<OrchestrationResultDTO> preconditionOrchestrationResults,
		OrchestrationResultDTO mainOrchestrationResult,
		int quantity,
		Map<String, String> staticParameters) {
}
