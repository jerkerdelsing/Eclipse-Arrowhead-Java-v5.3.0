/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import java.util.List;

// An action: a group of steps forming a DAG; the next action starts when all its steps are done.
public record ActionRequestDTO(
		String name,
		String nextActionName,
		List<String> firstStepNames,
		List<StepRequestDTO> steps) {
}
