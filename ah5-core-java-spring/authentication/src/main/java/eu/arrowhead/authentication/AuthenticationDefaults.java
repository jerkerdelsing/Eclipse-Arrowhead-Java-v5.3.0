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
package eu.arrowhead.authentication;

import eu.arrowhead.common.Defaults;

public final class AuthenticationDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String IDENTITY_TOKEN_DURATION_DEFAULT = "600"; // in seconds
	public static final String CLEANER_JOB_INTERVAL_DEFAULT = "30000"; // in ms

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthenticationDefaults() {
		throw new UnsupportedOperationException();
	}
}
