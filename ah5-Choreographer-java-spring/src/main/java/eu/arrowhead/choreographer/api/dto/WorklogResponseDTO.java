/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

public record WorklogResponseDTO(
		long id,
		String entryDate,
		String planName,
		String actionName,
		String stepName,
		Long sessionId,
		Long executionNumber,
		String message,
		String exception) {
}
