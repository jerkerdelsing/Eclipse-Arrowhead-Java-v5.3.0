/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.util.List;

import eu.arrowhead.plantdescriptionengine.api.dto.ConnectionDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO;

// The JSON document stored with every plant description.
public record PlantDescriptionBody(
		List<Long> include,
		List<PdeSystemDTO> systems,
		List<ConnectionDTO> connections) {

	public PlantDescriptionBody {
		include = include == null ? List.of() : List.copyOf(include);
		systems = systems == null ? List.of() : List.copyOf(systems);
		connections = connections == null ? List.of() : List.copyOf(connections);
	}
}
