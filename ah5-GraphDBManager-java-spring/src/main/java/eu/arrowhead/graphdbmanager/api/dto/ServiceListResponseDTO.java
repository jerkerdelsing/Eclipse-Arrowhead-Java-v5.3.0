/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.api.dto;

import java.util.List;

public record ServiceListResponseDTO(List<String> services) {

	public ServiceListResponseDTO {
		services = services == null ? List.of() : List.copyOf(services);
	}
}
