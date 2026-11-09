/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager;

import eu.arrowhead.common.Defaults;

public final class TimeManagerDefaults extends Defaults {

	public static final String SERVER_PORT_DEFAULT = "8463";
	public static final String TIME_TIMEZONE_DEFAULT = "UTC";
	public static final String NTP_SERVER_LIST_DEFAULT = "pool.ntp.org";
	public static final String TIME_OFFSET_THRESHOLD_DEFAULT = "5000";
	public static final String NTP_CHECK_INTERVAL_DEFAULT = "60000";
	public static final String NTP_TIMEOUT_DEFAULT = "10000";
	public static final String SERVICE_AUTHORIZATION_ENABLED_DEFAULT = "false";

	private TimeManagerDefaults() {
		throw new UnsupportedOperationException();
	}
}
