/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.api.dto;

import java.time.Instant;
import java.util.Map;

public record SubscriptionRequestDTO(
		String eventType,
		String notifyUri,
		Boolean matchMetadata,
		Map<String, String> metadataFilter,
		Instant startsAt,
		Instant expiresAt) {
}
