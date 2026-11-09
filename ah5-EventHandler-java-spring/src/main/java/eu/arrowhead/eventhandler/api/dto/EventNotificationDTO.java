/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.api.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

public record EventNotificationDTO(
		String eventType,
		String payload,
		Map<String, String> metadata,
		// RFC 3339 (IDD), independent of the object mapper used for the outgoing delivery
		@JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
		String sourceSystemName) {
}
