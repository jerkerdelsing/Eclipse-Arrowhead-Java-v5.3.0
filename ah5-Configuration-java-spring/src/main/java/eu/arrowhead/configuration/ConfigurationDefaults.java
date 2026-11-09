/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration;

import eu.arrowhead.common.Defaults;

public final class ConfigurationDefaults extends Defaults {

	public static final String SERVER_PORT_DEFAULT = "8459";
	public static final String SERVICE_AUTHORIZATION_ENABLED_DEFAULT = "false";
	public static final String CONFIGURATION_MAX_SIZE_DEFAULT = "1048576";

	private ConfigurationDefaults() {
		throw new UnsupportedOperationException();
	}
}
