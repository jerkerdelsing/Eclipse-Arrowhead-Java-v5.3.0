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

import static eu.arrowhead.sysmonitor.service.Json.map;

import java.util.Map;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;
import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.init.SysMonitorApplicationInitListener;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;

// Status document of the native 5.2.2 implementation. SysMonitor is "running" (HTTP 200) only with both broker connections,
// otherwise "degraded" (HTTP 503). Also the health source of the system's own sysMonitor observations.
@Service
public class StatusService implements SysMonitorHealthIndicator {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private MonitorDbService store;

	@Autowired
	private AmqpIntake intake;

	@Autowired
	private MqttOutput output;

	@Autowired
	private MonitoringService monitoring;

	@Autowired
	private SysMonitorState state;

	@Autowired
	private PruneTracker prune;

	@Autowired
	private ObjectProvider<SysMonitorApplicationInitListener> initListener;

	@Value(SysMonitorConstants.$ORCHESTRATION_INTERVAL_SECONDS_WD)
	private int orchestrationInterval;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean isRunning() {
		return output.isConnected() && intake.isConnected();
	}

	//-------------------------------------------------------------------------------------------------
	public int healthCode() {
		return isRunning() ? 200 : 503;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> liveness() {
		return map("status", isRunning() ? "ok" : "degraded", "system", SysMonitorConstants.SYSTEM_NAME);
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> status() {
		final Map<String, Object> storage = map();
		try {
			storage.putAll(store.counts());
			storage.putAll(map("retentionDays", monitoring.getRetentionDays(), "lastPrunedAt", prune.getLastPrunedAt(), "lastPruned", prune.getLastPruned()));
		} catch (final RuntimeException ex) {
			logger.debug(ex);
			state.setLastError("History storage unavailable");
		}

		int subscriptions = 0;
		try {
			subscriptions = store.subscriptions().size();
		} catch (final RuntimeException ex) {
			logger.debug(ex);
		}

		final SysMonitorApplicationInitListener listener = initListener.getIfAvailable();
		final Map<String, Object> registration = listener == null ? map("registered", false, "instances", java.util.List.of(), "error", null) : listener.registration();

		return map("status", isRunning() ? "running" : "degraded",
				"system", SysMonitorConstants.SYSTEM_NAME,
				"implementation", "java",
				"version", SysMonitorConstants.SYSTEM_VERSION,
				"amqp", map("connected", intake.isConnected(),
						"broker", intake.getAddress(),
						"port", intake.getPort(),
						"exchange", SysMonitorConstants.EXCHANGE,
						"queue", intake.queue(),
						"bindings", new TreeSet<>(intake.applied()),
						"lastMessageAt", intake.getLastMessageAt(),
						"error", intake.getError()),
				"mqtt", map("connected", output.isConnected(), "broker", output.getAddress(), "port", output.getPort(), "output", output.output()),
				"storage", storage,
				"defaultSubscription", monitoring.getDefaultSubscription(),
				"subscriptions", subscriptions,
				"registration", registration,
				"orchestration", map("intervalSeconds", orchestrationInterval, "providers", monitoring.getProviders(), "error", monitoring.getOrchestrationError()),
				"lastError", state.getLastError(),
				"certificateSecurity", "mutual X.509 TLS 1.2+");
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SysMonitorHealth health() {
		final String detail = "providers=" + monitoring.getProviders().size() + ", bindings=" + safeBindings() + ", amqp=" + intake.isConnected() + ", mqtt=" + output.isConnected();
		return isRunning() ? SysMonitorHealth.alive(detail) : SysMonitorHealth.degraded(detail);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private int safeBindings() {
		try {
			return intake.applied().size();
		} catch (final RuntimeException ex) {
			return 0;
		}
	}
}
