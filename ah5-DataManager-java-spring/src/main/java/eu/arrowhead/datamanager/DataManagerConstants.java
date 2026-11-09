/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager;

public final class DataManagerConstants {

	public static final String SYSTEM_NAME = "DataManager";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.datamanager.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.datamanager.jpa.repository";

	public static final String SERVICE_DEF_DATA_PROXY = "dataProxy";
	public static final String SERVICE_DEF_DATA_HISTORIAN = "dataHistorian";
	public static final String VERSION_DATA_PROXY = "1.0.0";
	public static final String VERSION_DATA_HISTORIAN = "1.0.0";

	public static final String SERVICE_OP_LIST_SYSTEMS = "list-systems";
	public static final String SERVICE_OP_LIST_SERVICES = "list-services";
	public static final String SERVICE_OP_GET_DATA = "get-data";
	public static final String SERVICE_OP_PUT_DATA = "put-data";

	public static final String HTTP_API_BASE_PATH = "/datamanager";
	public static final String HTTP_API_DATA_PROXY_PATH = HTTP_API_BASE_PATH + "/data-proxy";
	public static final String HTTP_API_DATA_HISTORIAN_PATH = HTTP_API_BASE_PATH + "/data-historian";
	public static final String HTTP_API_OP_SYSTEMS_PATH = "/systems";
	public static final String HTTP_PATH_PARAM_SYSTEM_NAME = "{systemName}";
	public static final String HTTP_PATH_PARAM_SERVICE_NAME = "{serviceName}";
	public static final String HTTP_API_OP_SERVICES_PATH = HTTP_API_OP_SYSTEMS_PATH + "/" + HTTP_PATH_PARAM_SYSTEM_NAME + "/services";
	public static final String HTTP_API_OP_DATA_PATH = HTTP_API_OP_SERVICES_PATH + "/" + HTTP_PATH_PARAM_SERVICE_NAME + "/data";

	// Per-operation ConsumerAuthorization check of dataProxy and dataHistorian calls
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":" + DataManagerDefaults.SERVICE_AUTHORIZATION_ENABLED_DEFAULT + "}";
	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30000;
	public static final int REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION = 30;

	public static final int DEFAULT_QUERY_COUNT = 1;
	public static final int MAX_QUERY_COUNT = 1000;
	public static final double RELATIVE_TIMESTAMP_INDICATOR = 268435456D;

	private DataManagerConstants() {
		throw new UnsupportedOperationException();
	}
}
