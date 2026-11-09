/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager;

public final class GraphDBManagerConstants {

	public static final String SYSTEM_NAME = "GraphDBManager";

	// GraphDB (RDF triple store, RDF4J HTTP protocol) holding the historian
	public static final String GRAPHDB_URL = "graphdb.url";
	public static final String $GRAPHDB_URL_WD = "${" + GRAPHDB_URL + ":" + GraphDBManagerDefaults.GRAPHDB_URL_DEFAULT + "}";
	public static final String GRAPHDB_REPOSITORY = "graphdb.repository";
	public static final String $GRAPHDB_REPOSITORY_WD = "${" + GRAPHDB_REPOSITORY + ":" + GraphDBManagerDefaults.GRAPHDB_REPOSITORY_DEFAULT + "}";
	public static final String GRAPHDB_USERNAME = "graphdb.username";
	public static final String $GRAPHDB_USERNAME_WD = "${" + GRAPHDB_USERNAME + ":}";
	public static final String GRAPHDB_PASSWORD = "graphdb.password";
	public static final String $GRAPHDB_PASSWORD_WD = "${" + GRAPHDB_PASSWORD + ":}";

	public static final String SERVICE_DEF_DATA_PROXY = "graphDataProxy";
	public static final String SERVICE_DEF_DATA_HISTORIAN = "graphDataHistorian";
	public static final String VERSION_DATA_PROXY = "1.0.0";
	public static final String VERSION_DATA_HISTORIAN = "1.0.0";

	public static final String SERVICE_OP_LIST_SYSTEMS = "list-systems";
	public static final String SERVICE_OP_LIST_SERVICES = "list-services";
	public static final String SERVICE_OP_GET_DATA = "get-data";
	public static final String SERVICE_OP_PUT_DATA = "put-data";

	public static final String HTTP_API_BASE_PATH = "/graphdbmanager";
	public static final String HTTP_API_DATA_PROXY_PATH = HTTP_API_BASE_PATH + "/data-proxy";
	public static final String HTTP_API_DATA_HISTORIAN_PATH = HTTP_API_BASE_PATH + "/data-historian";
	public static final String HTTP_API_OP_SYSTEMS_PATH = "/systems";
	public static final String HTTP_PATH_PARAM_SYSTEM_NAME = "{systemName}";
	public static final String HTTP_PATH_PARAM_SERVICE_NAME = "{serviceName}";
	public static final String HTTP_API_OP_SERVICES_PATH = HTTP_API_OP_SYSTEMS_PATH + "/" + HTTP_PATH_PARAM_SYSTEM_NAME + "/services";
	public static final String HTTP_API_OP_DATA_PATH = HTTP_API_OP_SERVICES_PATH + "/" + HTTP_PATH_PARAM_SERVICE_NAME + "/data";

	// Per-operation ConsumerAuthorization check of dataProxy and dataHistorian calls
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":" + GraphDBManagerDefaults.SERVICE_AUTHORIZATION_ENABLED_DEFAULT + "}";
	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30000;
	public static final int REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION = 30;

	public static final int DEFAULT_QUERY_COUNT = 1;
	public static final int MAX_QUERY_COUNT = 1000;
	public static final double RELATIVE_TIMESTAMP_INDICATOR = 268435456D;

	private GraphDBManagerConstants() {
		throw new UnsupportedOperationException();
	}
}
