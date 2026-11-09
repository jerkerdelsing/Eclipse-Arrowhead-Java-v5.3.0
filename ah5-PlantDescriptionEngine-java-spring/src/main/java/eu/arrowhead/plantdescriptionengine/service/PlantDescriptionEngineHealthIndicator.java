/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import org.springframework.stereotype.Component;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;

// sysMonitor echo is degraded while the plant cannot be synchronized or while unacknowledged mismatches exist.
@Component
public class PlantDescriptionEngineHealthIndicator implements SysMonitorHealthIndicator {

	private final PlantSynchronizer synchronizer;
	private final AlarmService alarmService;

	public PlantDescriptionEngineHealthIndicator(final PlantSynchronizer synchronizer, final AlarmService alarmService) {
		this.synchronizer = synchronizer;
		this.alarmService = alarmService;
	}

	@Override
	public SysMonitorHealth health() {
		final PlantSynchronizer.SyncState state = synchronizer.state();
		if (!state.ok()) {
			return SysMonitorHealth.degraded(state.detail());
		}
		final long alarms = alarmService.activeCount();
		final String detail = state.detail() + ", " + state.rules() + " rule(s), " + alarms + " active alarm(s)";
		return alarms == 0 ? SysMonitorHealth.alive(detail) : SysMonitorHealth.degraded(detail);
	}
}
