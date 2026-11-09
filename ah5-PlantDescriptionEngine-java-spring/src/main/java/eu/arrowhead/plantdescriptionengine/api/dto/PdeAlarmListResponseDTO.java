/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.dto;

import java.util.List;

public record PdeAlarmListResponseDTO(
		List<PdeAlarmResponseDTO> entries,
		long count) {
}
