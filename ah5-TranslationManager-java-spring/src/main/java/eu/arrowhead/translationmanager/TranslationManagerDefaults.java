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
package eu.arrowhead.translationmanager;

import eu.arrowhead.common.Defaults;

public final class TranslationManagerDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String ALLOW_DISCOVERY_FLAGS_DEFAULT = "false";
	public static final String ENABLE_AUTHORIZATION_DEFAULT = "false";
	public static final String TRANSLATOR_SERVICE_MIN_AVAILABILITY_DEFAULT = "5";
	public static final String ENABLE_CUSTOM_CONFIGURATION_DEFAULT = "false";
	public static final String TRANSLATION_DISCOVERY_MAX_AGE_DEFAULT = "8";
	public static final String CLEANER_JOB_INTERVAL_DEFAULT = "60000";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private TranslationManagerDefaults() {
		throw new UnsupportedOperationException();
	}
}