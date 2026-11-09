/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import eu.arrowhead.choreographer.api.dto.enums.SessionStepStatus;

public record SessionStepResponseDTO(
		long id,
		long sessionId,
		long stepId,
		String planName,
		String actionName,
		String stepName,
		String executorName,
		SessionStepStatus status,
		long executionNumber,
		String message,
		String startedAt,
		String updatedAt) {
}
