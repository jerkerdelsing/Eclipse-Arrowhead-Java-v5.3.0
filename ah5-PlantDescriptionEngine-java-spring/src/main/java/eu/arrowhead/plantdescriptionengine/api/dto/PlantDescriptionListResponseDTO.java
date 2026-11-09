/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.dto;

import java.util.List;

public record PlantDescriptionListResponseDTO(
		List<PlantDescriptionResponseDTO> entries,
		long count) {
}
