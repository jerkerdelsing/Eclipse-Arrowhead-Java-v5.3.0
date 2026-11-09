/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.init;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.sysmonitor.service.Json;

// Registration (system, sysMonitorManagement, sysMonitorHistory, sysMonitorData, generalManagement and the system's own
// sysMonitor offer) is done by the common ApplicationInitListener; this exposes its outcome for the status document.
@Component
public class SysMonitorApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	private volatile boolean initialized = false;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> registration() {
		final List<String> instances = new ArrayList<>(registeredServices);
		return Json.map("registered", initialized && (standaloneMode || !instances.isEmpty()), "instances", instances, "error", null);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException {
		initialized = true;
	}
}
