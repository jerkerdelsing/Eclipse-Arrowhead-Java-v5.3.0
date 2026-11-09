/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.api.dto;

import java.time.Instant;
import java.util.Map;

public record SubscriptionResponseDTO(
		Long id,
		String subscriberSystemName,
		String eventType,
		String notifyUri,
		boolean matchMetadata,
		Map<String, String> metadataFilter,
		Instant startsAt,
		Instant expiresAt,
		Instant createdAt,
		Instant updatedAt) {
}
