/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import java.util.List;

public record PlanResponseDTO(
		long id,
		String name,
		String firstActionName,
		List<ActionResponseDTO> actions,
		String createdAt,
		String updatedAt) {
}
