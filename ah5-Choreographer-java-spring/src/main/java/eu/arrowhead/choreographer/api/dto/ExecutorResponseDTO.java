/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

import java.util.List;

public record ExecutorResponseDTO(
		long id,
		String name,
		String address,
		int port,
		String basePath,
		List<ExecutorServiceDefinitionResponseDTO> serviceDefinitions,
		String createdAt,
		String updatedAt) {
}
