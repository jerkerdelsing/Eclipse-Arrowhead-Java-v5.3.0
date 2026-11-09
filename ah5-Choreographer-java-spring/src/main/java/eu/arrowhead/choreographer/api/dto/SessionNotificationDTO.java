/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import eu.arrowhead.choreographer.api.dto.enums.SessionStatus;

// Posted to the notify URI of a session on start, plan completion, session completion and abort.
public record SessionNotificationDTO(
		long sessionId,
		long planId,
		String planName,
		String timestamp,
		SessionStatus status,
		String message,
		String details) {
}
