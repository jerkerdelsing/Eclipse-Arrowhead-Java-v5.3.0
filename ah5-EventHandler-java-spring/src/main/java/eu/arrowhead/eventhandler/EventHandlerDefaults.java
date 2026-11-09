/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler;

import eu.arrowhead.common.Defaults;

public final class EventHandlerDefaults extends Defaults {

	public static final String SERVER_PORT_DEFAULT = "8455";
	public static final String PUBLISH_TOLERANCE_SECONDS_DEFAULT = "60";
	public static final String DELIVERY_ATTEMPTS_DEFAULT = "1";
	public static final String EXPIRATION_CLEANUP_INTERVAL_DEFAULT = "60000";
	public static final String SERVICE_AUTHORIZATION_ENABLED_DEFAULT = "false";

	private EventHandlerDefaults() {
		throw new UnsupportedOperationException();
	}
}
