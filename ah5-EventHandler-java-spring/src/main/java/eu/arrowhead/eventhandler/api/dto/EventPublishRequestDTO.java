/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.api.dto;

import java.time.Instant;
import java.util.Map;

public record EventPublishRequestDTO(
		String eventType,
		String payload,
		Map<String, String> metadata,
		Instant timestamp) {
}
