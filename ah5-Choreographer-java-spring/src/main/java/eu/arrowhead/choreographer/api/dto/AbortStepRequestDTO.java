/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

// POST <executor>/abort
public record AbortStepRequestDTO(long sessionId, long sessionStepId) {
}
