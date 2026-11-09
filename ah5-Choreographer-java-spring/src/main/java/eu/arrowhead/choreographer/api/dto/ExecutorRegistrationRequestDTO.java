/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

// Registration of the requesting system as executor (choreographerExecutor.register).
public record ExecutorRegistrationRequestDTO(
		String address,
		Integer port,
		String basePath,
		String serviceDefinitionName,
		Integer minVersion,
		Integer maxVersion) {
}
