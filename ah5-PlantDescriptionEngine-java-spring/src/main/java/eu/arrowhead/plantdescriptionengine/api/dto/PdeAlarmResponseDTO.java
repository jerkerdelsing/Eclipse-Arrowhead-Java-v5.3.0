/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.dto;

public record PdeAlarmResponseDTO(
		long id,
		String systemId,
		String systemName,
		boolean acknowledged,
		String severity,
		String cause,
		String description,
		String raisedAt,
		String updatedAt,
		String clearedAt,
		String acknowledgedAt) {
}
