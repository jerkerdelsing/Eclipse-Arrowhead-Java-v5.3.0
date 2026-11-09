/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor;

import eu.arrowhead.common.Defaults;

public final class SysMonitorDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String MQTT_ADDRESS_DEFAULT = "mqtt-broker";
	public static final String MQTT_PORT_DEFAULT = "8883";
	public static final String RETENTION_DAYS_DEFAULT = "30";
	public static final String PRUNE_INTERVAL_SECONDS_DEFAULT = "3600";
	public static final String DEFAULT_SUBSCRIPTION_DEFAULT = "all";
	public static final String ORCHESTRATION_INTERVAL_SECONDS_DEFAULT = "30";
	public static final String OUTBOX_INTERVAL_MILLIS_DEFAULT = "1000";
	public static final String CONNECT_INTERVAL_SECONDS_DEFAULT = "5";
	public static final String BROKER_SSL_ENABLED_DEFAULT = "true";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SysMonitorDefaults() {
		throw new UnsupportedOperationException();
	}
}
