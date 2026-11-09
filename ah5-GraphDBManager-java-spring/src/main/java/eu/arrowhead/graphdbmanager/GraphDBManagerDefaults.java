/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager;

import eu.arrowhead.common.Defaults;

public final class GraphDBManagerDefaults extends Defaults {

	public static final String SERVICE_AUTHORIZATION_ENABLED_DEFAULT = "false";
	public static final String GRAPHDB_URL_DEFAULT = "http://127.0.0.1:7200";
	public static final String GRAPHDB_REPOSITORY_DEFAULT = "graphdbmanager";

	private GraphDBManagerDefaults() {
		throw new UnsupportedOperationException();
	}
}
