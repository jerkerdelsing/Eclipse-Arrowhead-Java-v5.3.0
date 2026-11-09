/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.api.dto;

// Field names are those of the v4.6 ConfigurationResponseDTO; size (bytes of the decoded content) is additive.
public record ConfigurationResponseDTO(
		long id,
		String systemName,
		String fileName,
		String contentType,
		String data,
		long size,
		String createdAt,
		String updatedAt) {
}
