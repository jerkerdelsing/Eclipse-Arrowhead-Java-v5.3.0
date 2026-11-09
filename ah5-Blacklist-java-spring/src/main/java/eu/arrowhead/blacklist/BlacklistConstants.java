/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.blacklist;

import eu.arrowhead.common.jpa.ArrowheadEntity;

public final class BlacklistConstants {

	//=================================================================================================
	// members

	// System info related
	public static final String VERSION_DISCOVERY = "1.0.0";
	public static final String VERSION_MANAGEMENT = "1.0.0";
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_MONITOR = "1.0.0";

	// JPA related

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.blacklist.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.blacklist.jpa.repository";

	// HTTP API paths

	public static final String HTTP_API_BASE_PATH = "/blacklist";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_OP_CHECK = "/check";
	public static final String HTTP_API_PARAM_NAME = "{systemName}";
	public static final String HTTP_API_OP_CHECK_PATH = HTTP_API_OP_CHECK + "/" + HTTP_API_PARAM_NAME;
	public static final String HTTP_API_OP_LOOKUP = "/lookup";
	public static final String HTTP_API_OP_QUERY = "/query";
	public static final String HTTP_API_OP_CREATE = "/create";
	public static final String HTTP_API_OP_REMOVE = "/remove";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_OP_ECHO = "/echo";

	// MQTT API topics

	public static final String MQTT_API_BASE_TOPIC = "arrowhead/blacklist";
	public static final String MQTT_API_DISCOVERY_BASE_TOPIC = MQTT_API_BASE_TOPIC + "/";
	public static final String MQTT_API_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC + "/management/";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC + "/general/management/";
	public static final String MQTT_API_MONITOR_BASE_TOPIC = MQTT_API_BASE_TOPIC + "/monitor/";

	// DTO field length limitations

	public static final int REASON_LENGTH = ArrowheadEntity.VARCHAR_LARGE;

	// Blacklist parameters

	public static final String WHITELIST = "whitelist";
	public static final String $WHITELIST_WD = "#{'${" + WHITELIST + ":" + BlacklistDefaults.WHITELIST_DEFAULT + "}'.split(',')}";

	//=================================================================================================
	// assistant methods

	private BlacklistConstants() {
		throw new UnsupportedOperationException();
	}
}