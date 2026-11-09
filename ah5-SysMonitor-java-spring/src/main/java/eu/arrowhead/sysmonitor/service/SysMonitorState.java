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

import org.springframework.stereotype.Component;

// The most recent error of any SysMonitor worker, reported by the status operations as "lastError".
@Component
public class SysMonitorState {

	//=================================================================================================
	// members

	private volatile String lastError;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String getLastError() {
		return lastError;
	}

	//-------------------------------------------------------------------------------------------------
	public void setLastError(final String lastError) {
		this.lastError = lastError;
	}
}
