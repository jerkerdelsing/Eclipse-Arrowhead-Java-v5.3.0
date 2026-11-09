/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.dto;

import java.util.List;
import java.util.Map;

// A system of a plant description, identified by system name and/or metadata (v4.6 PdeSystem).
public record PdeSystemDTO(
		String systemId,
		String systemName,
		Map<String, String> metadata,
		List<PortDTO> ports) {
}
