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

import eu.arrowhead.common.Defaults;

public final class SoSConfiguratorDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String SERVER_PORT_DEFAULT = "8494";
	public static final String CATALOG_FILE_DEFAULT = "/app/catalogs/sos.json";
	public static final String STATUS_TIMEOUT_SECONDS_DEFAULT = "20";
	// systems besides Sysop that may use sosConfiguration without a ConsumerAuthorization grant
	public static final String API_WHITELIST_DEFAULT = "";
	public static final String SERVICE_AUTHORIZATION_ENABLED_DEFAULT = "true";
	// the browser console: plain HTTP with password login, never exposed beyond the host's loopback interface
	public static final String CONSOLE_PORT_DEFAULT = "8495";
	public static final String CONSOLE_ADDRESS_DEFAULT = "127.0.0.1";
	public static final String CONSOLE_ALLOWED_HOSTS_DEFAULT = "127.0.0.1,localhost,[::1]";
	public static final String CONSOLE_SESSION_SECONDS_DEFAULT = "28800";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SoSConfiguratorDefaults() {
		throw new UnsupportedOperationException();
	}
}
