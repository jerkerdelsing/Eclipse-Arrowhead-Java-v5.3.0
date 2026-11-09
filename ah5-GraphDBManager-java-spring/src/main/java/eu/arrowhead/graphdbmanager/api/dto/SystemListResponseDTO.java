/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.api.dto;

import java.util.List;

public record SystemListResponseDTO(List<String> systems) {

	public SystemListResponseDTO {
		systems = systems == null ? List.of() : List.copyOf(systems);
	}
}
