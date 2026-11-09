/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto;

// Starts a session of a plan (choreographerSession, by plan name).
public record StartSessionRequestDTO(
		String planName,
		Long quantity,
		Boolean allowInterCloud,
		Boolean chooseOptimalExecutor,
		String notifyProtocol,
		String notifyAddress,
		Integer notifyPort,
		String notifyPath) {
}
