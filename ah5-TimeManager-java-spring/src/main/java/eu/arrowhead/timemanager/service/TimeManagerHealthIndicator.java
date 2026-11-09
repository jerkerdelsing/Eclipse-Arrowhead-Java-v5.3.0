/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.service;

import org.springframework.stereotype.Component;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;

// sysMonitor echo is degraded while the local time cannot be trusted.
@Component
public class TimeManagerHealthIndicator implements SysMonitorHealthIndicator {

	private final TimeTrustService trustService;

	public TimeManagerHealthIndicator(final TimeTrustService trustService) {
		this.trustService = trustService;
	}

	@Override
	public SysMonitorHealth health() {
		final TimeTrustService.TrustState state = trustService.state();
		return state.trusted() ? SysMonitorHealth.alive(state.detail()) : SysMonitorHealth.degraded(state.detail());
	}
}
