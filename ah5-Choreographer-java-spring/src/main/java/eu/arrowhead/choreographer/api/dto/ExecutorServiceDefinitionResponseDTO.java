/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

public record ExecutorServiceDefinitionResponseDTO(String serviceDefinitionName, int minVersion, int maxVersion) {
}
