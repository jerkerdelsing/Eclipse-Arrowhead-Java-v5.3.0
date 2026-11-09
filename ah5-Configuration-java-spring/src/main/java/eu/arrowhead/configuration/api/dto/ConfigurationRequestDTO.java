/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.api.dto;

// Field names are those of the v4.6 ConfigurationRequestDTO; data is the Base64-encoded file content.
public record ConfigurationRequestDTO(
		String systemName,
		String fileName,
		String contentType,
		String data) {
}
