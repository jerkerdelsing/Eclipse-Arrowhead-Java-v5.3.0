/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator;

import eu.arrowhead.common.Defaults;

public final class SoLocalCloudConfiguratorDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String SERVER_PORT_DEFAULT = "8496";
	public static final String CATALOG_FILE_DEFAULT = "/app/catalogs/solc-policy.json";
	// systems besides Sysop that may use soLocalCloudConfiguration without a ConsumerAuthorization grant
	public static final String API_WHITELIST_DEFAULT = "";
	public static final String SERVICE_AUTHORIZATION_ENABLED_DEFAULT = "true";
	public static final String SOURCE_REPOSITORY_DEFAULT = "https://github.com/jerkerdelsing/Eclipse-Arrowhead-java.git";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SoLocalCloudConfiguratorDefaults() {
		throw new UnsupportedOperationException();
	}
}
