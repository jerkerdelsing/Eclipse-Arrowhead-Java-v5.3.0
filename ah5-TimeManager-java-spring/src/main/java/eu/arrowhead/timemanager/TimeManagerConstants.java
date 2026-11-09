/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager;

public final class TimeManagerConstants {

	public static final String SYSTEM_NAME = "TimeManager";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.timemanager.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.timemanager.jpa.repository";

	// v4.6 core service "time" keeps its definition name; its single operation follows the v5 kebab-case convention.
	public static final String SERVICE_DEF_TIME = "time";
	public static final String VERSION_TIME = "1.0.0";
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String SERVICE_OP_GET_TIME = "get-time";

	// The v4.6 URL GET /timemanager/time is preserved.
	public static final String HTTP_API_BASE_PATH = "/timemanager";
	public static final String HTTP_API_OP_TIME_PATH = "/time";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";

	public static final String TIME_TIMEZONE = "time.timezone";
	public static final String $TIME_TIMEZONE_WD = "${" + TIME_TIMEZONE + ":" + TimeManagerDefaults.TIME_TIMEZONE_DEFAULT + "}";
	public static final String NTP_SERVER_LIST = "ntp.server.list";
	public static final String $NTP_SERVER_LIST_WD = "${" + NTP_SERVER_LIST + ":" + TimeManagerDefaults.NTP_SERVER_LIST_DEFAULT + "}";
	public static final String TIME_OFFSET_THRESHOLD = "time.offset.threshold";
	public static final String $TIME_OFFSET_THRESHOLD_WD = "${" + TIME_OFFSET_THRESHOLD + ":" + TimeManagerDefaults.TIME_OFFSET_THRESHOLD_DEFAULT + "}";
	public static final String NTP_CHECK_INTERVAL = "ntp.check.interval";
	public static final String $NTP_CHECK_INTERVAL_WD = "${" + NTP_CHECK_INTERVAL + ":" + TimeManagerDefaults.NTP_CHECK_INTERVAL_DEFAULT + "}";
	// Per-call ConsumerAuthorization check of time.get-time (authorization.verify), cached for 30 s.
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":" + TimeManagerDefaults.SERVICE_AUTHORIZATION_ENABLED_DEFAULT + "}";
	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30000;
	public static final int REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION = 30;

	public static final String NTP_TIMEOUT = "ntp.timeout";
	public static final String $NTP_TIMEOUT_WD = "${" + NTP_TIMEOUT + ":" + TimeManagerDefaults.NTP_TIMEOUT_DEFAULT + "}";

	private TimeManagerConstants() {
		throw new UnsupportedOperationException();
	}
}
