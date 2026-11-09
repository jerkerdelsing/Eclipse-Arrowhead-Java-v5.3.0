/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import org.springframework.stereotype.Component;

import eu.arrowhead.choreographer.engine.ChoreographerEngine;
import eu.arrowhead.choreographer.jpa.repository.PlanRepository;
import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;

// sysMonitor echo is degraded while the database cannot be reached; the detail shows the running sessions.
@Component
public class ChoreographerHealthIndicator implements SysMonitorHealthIndicator {

	private final PlanRepository planRepository;
	private final ChoreographerEngine engine;

	public ChoreographerHealthIndicator(final PlanRepository planRepository, final ChoreographerEngine engine) {
		this.planRepository = planRepository;
		this.engine = engine;
	}

	@Override
	public SysMonitorHealth health() {
		final String sessions = "running sessions: " + engine.runningSessions();
		try {
			final long plans = planRepository.count();
			return SysMonitorHealth.alive("plans: " + plans + ", " + sessions);
		} catch (final RuntimeException ex) {
			return SysMonitorHealth.degraded("database unavailable, " + sessions);
		}
	}
}
