/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

// Registration of an executor by an operator (choreographerManagement.create-executor).
public record ExecutorCreateRequestDTO(
		String systemName,
		String address,
		Integer port,
		String basePath,
		String serviceDefinitionName,
		Integer minVersion,
		Integer maxVersion) {
}
