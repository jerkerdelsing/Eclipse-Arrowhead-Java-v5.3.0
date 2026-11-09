/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.dto.enums;

public enum SessionStatus {
	INITIATED, RUNNING, ABORTED, DONE;

	public boolean isActive() {
		return this == INITIATED || this == RUNNING;
	}
}
