/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.dto;

import java.util.List;

public record PlantDescriptionResponseDTO(
		long id,
		String plantDescription,
		boolean active,
		List<Long> include,
		List<PdeSystemDTO> systems,
		List<ConnectionDTO> connections,
		String createdAt,
		String updatedAt) {
}
