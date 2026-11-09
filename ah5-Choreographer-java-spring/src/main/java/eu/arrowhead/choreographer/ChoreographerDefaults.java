/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer;

import eu.arrowhead.common.Defaults;

public final class ChoreographerDefaults extends Defaults {

	public static final String SERVER_PORT_DEFAULT = "8457";
	public static final String MAX_PLAN_ITERATION_DEFAULT = "1000";
	public static final String ORCHESTRATION_TIMEOUT_DEFAULT = "30";
	public static final String SESSION_WORKER_THREADS_DEFAULT = "8";

	private ChoreographerDefaults() {
		throw new UnsupportedOperationException();
	}
}
