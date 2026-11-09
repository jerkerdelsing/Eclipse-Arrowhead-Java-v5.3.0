/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import eu.arrowhead.choreographer.api.dto.enums.SessionStatus;

public record SessionResponseDTO(
		long id,
		long planId,
		String planName,
		SessionStatus status,
		long quantityDone,
		long quantityGoal,
		long executionNumber,
		String owner,
		String notifyUri,
		String startedAt,
		String updatedAt) {
}
