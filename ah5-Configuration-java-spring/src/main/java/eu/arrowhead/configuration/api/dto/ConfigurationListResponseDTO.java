/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.api.dto;

import java.util.List;

public record ConfigurationListResponseDTO(
		long count,
		List<ConfigurationResponseDTO> data) {
}
