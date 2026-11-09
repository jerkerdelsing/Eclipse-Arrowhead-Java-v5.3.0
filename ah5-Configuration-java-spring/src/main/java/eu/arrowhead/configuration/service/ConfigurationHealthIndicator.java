/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.service;

import org.springframework.stereotype.Component;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;

// sysMonitor echo is degraded while the configuration store cannot be read.
@Component
public class ConfigurationHealthIndicator implements SysMonitorHealthIndicator {

	private final ConfigurationService configurationService;

	public ConfigurationHealthIndicator(final ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	@Override
	public SysMonitorHealth health() {
		try {
			return SysMonitorHealth.alive("Stored configurations: " + configurationService.count());
		} catch (final RuntimeException ex) {
			return SysMonitorHealth.degraded("Configuration store is unavailable");
		}
	}
}
