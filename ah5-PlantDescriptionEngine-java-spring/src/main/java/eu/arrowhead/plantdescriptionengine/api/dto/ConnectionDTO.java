/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.dto;

// A consumer port connected to a producer port; becomes one or more DSO managed provider rules.
public record ConnectionDTO(
		SystemPortDTO consumer,
		SystemPortDTO producer,
		Integer priority) {
}
