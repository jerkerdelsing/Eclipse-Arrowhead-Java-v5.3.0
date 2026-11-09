/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.init.GatekeeperApplicationInitListener;
import eu.arrowhead.gatekeeper.relay.RelayClient;

// The status document of /gatekeeper/mgmt/status and /gatekeeper/mgmt/general/status, the token-free liveness, and the
// health of the sysMonitor echo observation (degraded while the relay link is down, as the native publisher reported it).
@Service
public class StatusService implements SysMonitorHealthIndicator {

	//=================================================================================================
	// members

	@Autowired
	private RelayClient relay;

	@Autowired
	private GatekeeperApplicationInitListener initListener;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> status() {
		final boolean registered = initListener.isRegistered();
		return m("status", "ok",
				"system", GatekeeperConstants.SYSTEM_NAME,
				"version", GatekeeperConstants.SYSTEM_VERSION,
				"implementation", GatekeeperConstants.IMPLEMENTATION,
				"x509", true,
				"registration", m("registered", registered, "instances", initListener.getRegisteredInstances(), "error", null),
				"relay", relay.status());
	}

	//-------------------------------------------------------------------------------------------------
	// status only (J18)
	public Map<String, Object> liveness() {
		return m("status", "ok", "system", GatekeeperConstants.SYSTEM_NAME);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SysMonitorHealth health() {
		return relay.isConnected() ? SysMonitorHealth.alive("relay connected") : SysMonitorHealth.degraded("relay not connected");
	}
}
