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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;

// The periodic workers of the native implementation: broker (re)connection, orchestration of the monitored providers, the
// MQTT outbox and the retention prune. sysmonitor.workers.enabled=false switches them off (tests).
@Component
@ConditionalOnProperty(name = "sysmonitor.workers.enabled", havingValue = "true", matchIfMissing = true)
public class SysMonitorJobs {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private AmqpIntake intake;

	@Autowired
	private MqttOutput output;

	@Autowired
	private MonitoringService monitoring;

	@Autowired
	private MonitorDbService store;

	@Autowired
	private SysMonitorState state;

	@Autowired
	private PruneTracker prune;

	@Value(SysMonitorConstants.$RETENTION_DAYS_WD)
	private double retentionDays;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Scheduled(fixedDelayString = SysMonitorConstants.$CONNECT_INTERVAL_MILLIS_WD)
	public void connectMqtt() {
		try {
			output.connect();
		} catch (final Exception ex) {
			failed("MQTT connection", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Scheduled(fixedDelayString = SysMonitorConstants.$CONNECT_INTERVAL_MILLIS_WD)
	public void connectAmqp() {
		try {
			if (intake.connect()) {
				monitoring.applyIfDiscovered();
			}
		} catch (final Exception ex) {
			failed("AMQP connection", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Scheduled(fixedDelayString = SysMonitorConstants.$ORCHESTRATION_INTERVAL_MILLIS_WD)
	public void orchestrate() {
		try {
			monitoring.orchestrate();
		} catch (final Exception ex) {
			monitoring.orchestrationFailed();
			failed("Monitoring orchestration", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Scheduled(fixedDelayString = SysMonitorConstants.$OUTBOX_INTERVAL_MILLIS_WD)
	public void outbox() {
		try {
			output.publishOutbox();
		} catch (final Exception ex) {
			failed("MQTT outbox", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Scheduled(fixedDelayString = SysMonitorConstants.$PRUNE_INTERVAL_MILLIS_WD)
	public void prune() {
		try {
			prune.pruned(store.prune(retentionDays));
		} catch (final Exception ex) {
			failed("History prune", ex);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void failed(final String job, final Exception ex) {
		state.setLastError(ex.getClass().getSimpleName());
		logger.warn("{} failed: {}", job, ex.getMessage());
		logger.debug(ex);
	}
}
