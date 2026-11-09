/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import java.util.List;

public record ActionResponseDTO(
		long id,
		String name,
		boolean firstAction,
		String nextActionName,
		List<String> firstStepNames,
		List<StepResponseDTO> steps,
		String createdAt,
		String updatedAt) {
}
