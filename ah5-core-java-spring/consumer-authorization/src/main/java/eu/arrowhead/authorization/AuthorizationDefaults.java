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
package eu.arrowhead.authorization;

import eu.arrowhead.common.Defaults;

public final class AuthorizationDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final int DEFAULT_TOKEN_MAX_AGE = 60; // minutes
	public static final int DEFAULT_TOKEN_TIME_LIMIT = 30;  // seconds
	public static final int DEFAULT_SIMPLE_TOKEN_BYTE_SIZE = 32;
	public static final int DEFAULT_SIMPLE_TOKEN_USAGE_LIMIT = 10;
	public static final String DEFAULT_UNBOUNDED_TOKEN_GENERATION_WHITELIST = "\"\"";
	public static final String DEFAULT_UNBOUND_VALUE = "false";
	public static final String CLEANER_JOB_INTERVAL_DEFAULT = "30000"; // milliseconds

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationDefaults() {
		throw new UnsupportedOperationException();
	}
}