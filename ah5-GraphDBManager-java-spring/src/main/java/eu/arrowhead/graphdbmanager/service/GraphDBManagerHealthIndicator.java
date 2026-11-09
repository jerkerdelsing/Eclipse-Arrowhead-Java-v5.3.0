/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.service;

import org.springframework.stereotype.Component;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;

// Liveness reported in the sysMonitor echo observation: degraded while the historian storage cannot be queried.
@Component
public class GraphDBManagerHealthIndicator implements SysMonitorHealthIndicator {

	private final ProxyService proxyService;
	private final HistorianService historianService;

	public GraphDBManagerHealthIndicator(final ProxyService proxyService, final HistorianService historianService) {
		this.proxyService = proxyService;
		this.historianService = historianService;
	}

	@Override
	public SysMonitorHealth health() {
		final int proxySystems = proxyService.getSystems().size();
		try {
			final int historianSystems = historianService.getSystems().size();
			return SysMonitorHealth.alive("dataProxy systems: " + proxySystems + ", dataHistorian systems: " + historianSystems);
		} catch (final RuntimeException ex) {
			return SysMonitorHealth.degraded("dataHistorian storage is unavailable");
		}
	}
}
