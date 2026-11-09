/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.dto;

// A port of a plant system, addressed by plant system id and port name.
public record SystemPortDTO(
		String systemId,
		String portName) {
}
