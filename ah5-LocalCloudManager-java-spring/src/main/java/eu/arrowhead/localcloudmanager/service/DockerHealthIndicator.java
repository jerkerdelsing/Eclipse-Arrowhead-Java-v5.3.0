package eu.arrowhead.localcloudmanager.service;

import org.springframework.stereotype.Component;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;

// sysMonitor echo: degraded while the Docker API (through the proxy) cannot be reached.
@Component
public class DockerHealthIndicator implements SysMonitorHealthIndicator {

	private final ManagerService manager;

	public DockerHealthIndicator(final ManagerService manager) {
		this.manager = manager;
	}

	@Override
	public SysMonitorHealth health() {
		try {
			return SysMonitorHealth.alive("Docker API reachable; " + manager.listContainers().size() + " containers");
		} catch (final RuntimeException ex) {
			return SysMonitorHealth.degraded("Docker API unavailable: " + ex.getMessage());
		}
	}
}
