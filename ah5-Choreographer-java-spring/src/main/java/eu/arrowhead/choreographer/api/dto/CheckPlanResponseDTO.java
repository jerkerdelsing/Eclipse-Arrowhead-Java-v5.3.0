/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import java.util.List;

// Result of choreographerManagement.check-plan.
public record CheckPlanResponseDTO(
		long planId,
		List<String> errorMessages,
		boolean needInterCloud) {
}
