/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.dto;

import java.util.Map;

// A port of a plant system: the service it consumes or produces (v4.6 Port).
public record PortDTO(
		String portName,
		String serviceDefinition,
		String serviceInterface,
		Map<String, String> metadata,
		Boolean consumer) {
}
