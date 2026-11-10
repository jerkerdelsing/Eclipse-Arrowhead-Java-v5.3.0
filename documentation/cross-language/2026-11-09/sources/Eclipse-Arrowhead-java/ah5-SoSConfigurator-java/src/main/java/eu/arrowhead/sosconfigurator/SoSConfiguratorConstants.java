/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator;

public final class SoSConfiguratorConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "SoSConfigurator";
	public static final String SYSTEM_VERSION = "5.2.2";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.sosconfigurator.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.sosconfigurator.jpa.repository";

	// the ColonyOS executor type of the API identity
	public static final String API_EXECUTOR_TYPE = "sos-api";

	// sosConfiguration service; the /api paths are those of the native 5.2.2 API and its operator UI
	public static final String SERVICE_DEF_SOS_CONFIGURATION = "sosConfiguration";
	public static final String SERVICE_VERSION = "1.0.0";
	public static final String SERVICE_OP_CATALOG = "catalog";
	public static final String SERVICE_OP_STATUS = "status";
	public static final String SERVICE_OP_PLAN = "plan";
	public static final String SERVICE_OP_SUBMIT_DEPLOYMENT = "submit-deployment";
	public static final String SERVICE_OP_LIST_DEPLOYMENTS = "list-deployments";
	public static final String SERVICE_OP_GET_DEPLOYMENT = "get-deployment";

	public static final String HTTP_API_BASE_PATH = "/api";
	public static final String HTTP_API_OP_CATALOG_PATH = "/catalog";
	public static final String HTTP_API_OP_STATUS_PATH = "/status";
	public static final String HTTP_API_OP_PLAN_PATH = "/plan";
	public static final String HTTP_API_OP_DEPLOYMENTS_PATH = "/deployments";
	public static final String HTTP_API_OP_DEPLOYMENT_PATH = "/deployments/{id}";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = "/sosconfigurator/general/mgmt";

	// console (browser) connector only
	public static final String HTTP_API_SESSION_PATH = "/api/session";
	public static final String CONSOLE_COOKIE = "ah_sos_console";
	public static final String HTTP_ATTR_CONSOLE_OPERATOR = "sosconfigurator.console.operator";

	// configuration
	public static final String CATALOG_FILE = "sos.catalog.file";
	public static final String $CATALOG_FILE_WD = "${" + CATALOG_FILE + ":" + SoSConfiguratorDefaults.CATALOG_FILE_DEFAULT + "}";
	public static final String STATUS_TIMEOUT_SECONDS = "sos.status.timeout.seconds";
	public static final String $STATUS_TIMEOUT_SECONDS_WD = "${" + STATUS_TIMEOUT_SECONDS + ":" + SoSConfiguratorDefaults.STATUS_TIMEOUT_SECONDS_DEFAULT + "}";
	public static final String API_WHITELIST = "sos.api.whitelist";
	public static final String $API_WHITELIST_WD = "${" + API_WHITELIST + ":" + SoSConfiguratorDefaults.API_WHITELIST_DEFAULT + "}";
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":" + SoSConfiguratorDefaults.SERVICE_AUTHORIZATION_ENABLED_DEFAULT + "}";
	public static final String CONSOLE_PORT = "console.port";
	public static final String $CONSOLE_PORT_WD = "${" + CONSOLE_PORT + ":" + SoSConfiguratorDefaults.CONSOLE_PORT_DEFAULT + "}";
	public static final String CONSOLE_ADDRESS = "console.address";
	public static final String $CONSOLE_ADDRESS_WD = "${" + CONSOLE_ADDRESS + ":" + SoSConfiguratorDefaults.CONSOLE_ADDRESS_DEFAULT + "}";
	public static final String CONSOLE_PASSWORD_HASH = "console.password.hash";
	public static final String $CONSOLE_PASSWORD_HASH_WD = "${" + CONSOLE_PASSWORD_HASH + ":}";
	public static final String CONSOLE_ALLOWED_HOSTS = "console.allowed.hosts";
	public static final String $CONSOLE_ALLOWED_HOSTS_WD = "${" + CONSOLE_ALLOWED_HOSTS + ":" + SoSConfiguratorDefaults.CONSOLE_ALLOWED_HOSTS_DEFAULT + "}";
	public static final String CONSOLE_SESSION_SECONDS = "console.session.seconds";
	public static final String $CONSOLE_SESSION_SECONDS_WD = "${" + CONSOLE_SESSION_SECONDS + ":" + SoSConfiguratorDefaults.CONSOLE_SESSION_SECONDS_DEFAULT + "}";

	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30000;
	public static final int REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION = 30;
	public static final int MAX_BODY_BYTES = 64 << 10;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SoSConfiguratorConstants() {
		throw new UnsupportedOperationException();
	}
}
