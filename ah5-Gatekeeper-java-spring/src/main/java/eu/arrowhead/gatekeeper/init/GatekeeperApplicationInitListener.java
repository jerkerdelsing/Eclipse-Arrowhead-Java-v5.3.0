/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.init;

import java.util.List;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.init.ApplicationInitListener;

// Registration in the ServiceRegistry (system, the four Gatekeeper services and the native sysMonitor offer) is the common
// listener's job; this one records the result for the status document.
@Component
public class GatekeeperApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	private volatile boolean registered = false;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean isRegistered() {
		return registered;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getRegisteredInstances() {
		return List.copyOf(registeredServices);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		logger.debug("customInit started...");
		registered = !standaloneMode;
	}
}
