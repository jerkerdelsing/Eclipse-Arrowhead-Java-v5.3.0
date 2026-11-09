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
package eu.arrowhead.deviceqosevaluator;

import eu.arrowhead.common.Defaults;

public final class DeviceQoSEvaluatorDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String MEASUREMENT_ORGANIZER_JOB_INTERVAL_DEFAULT = "30"; // sec
	public static final String RTT_MEASUEREMENT_JOB_INTERVAL_DEFAULT = "15"; // sec
	public static final String RTT_MEASUEREMENT_TIMEOUT_DEFAULT = "5000"; // ms
	public static final String AUGMENTED_MEASUEREMENT_JOB_INTERVAL_DEFAULT = "15"; // sec
	public static final String EVALUATION_TIME_WINDOW_DEFAULT = "60"; // sec
	public static final String CLEANING_JOB_INTERVAL_DEFAULT = "60"; // sec
	public static final String RAW_MEASUREMENT_DATA_MAX_AGE_DEFAULT = "15"; // min
	public static final String INACTIVE_DEVICE_MAX_AGE_DEFAULT = "30"; // min

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DeviceQoSEvaluatorDefaults() {
		throw new UnsupportedOperationException();
	}
}
