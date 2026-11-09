/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import java.util.List;

import eu.arrowhead.choreographer.api.dto.enums.SessionStatus;

public record RunPlanResponseDTO(
		Long planId,
		Long sessionId,
		Long quantity,
		SessionStatus status,
		List<String> errorMessages,
		boolean needInterCloud) {
}
