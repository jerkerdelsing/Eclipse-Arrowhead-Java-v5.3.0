/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.api.dto;

import java.util.List;

public record SubscriptionListResponseDTO(
		List<SubscriptionResponseDTO> entries,
		long count) {
}
