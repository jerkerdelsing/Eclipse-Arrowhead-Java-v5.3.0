/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto.enums;

// Result an executor reports through choreographerExecutor.notify-step-done (unchanged from v4.6).
public enum ExecutedStepStatus {
	SUCCESS, ERROR, FATAL_ERROR, ABORTED;

	public boolean isError() {
		return this == ERROR || this == FATAL_ERROR;
	}

	public boolean isFatal() {
		return this == FATAL_ERROR;
	}
}
