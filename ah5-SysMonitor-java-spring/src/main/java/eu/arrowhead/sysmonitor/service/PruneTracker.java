/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import static eu.arrowhead.sysmonitor.service.Json.now;

import org.springframework.stereotype.Component;

// Result of the last retention prune, reported in the status document.
@Component
public class PruneTracker {

	//=================================================================================================
	// members

	private volatile String lastPrunedAt;
	private volatile int lastPruned;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void pruned(final int count) {
		lastPruned = count;
		lastPrunedAt = now();
	}

	//-------------------------------------------------------------------------------------------------
	public String getLastPrunedAt() {
		return lastPrunedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public int getLastPruned() {
		return lastPruned;
	}
}
