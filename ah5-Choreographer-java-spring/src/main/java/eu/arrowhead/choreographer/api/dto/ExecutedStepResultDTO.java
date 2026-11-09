/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import eu.arrowhead.choreographer.api.dto.enums.ExecutedStepStatus;

// choreographerExecutor.notify-step-done
public record ExecutedStepResultDTO(
		Long sessionId,
		Long sessionStepId,
		ExecutedStepStatus status,
		String message,
		String exception) {
}
