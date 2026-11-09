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
package eu.arrowhead.serviceorchestration;

import eu.arrowhead.common.Defaults;

public final class SimpleStoreServiceOrchestrationDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String PUSH_ORCHESTRATION_MAX_THREAD_DEFAULT = "5";
	public static final String CLEANER_JOB_INTERVAL_DEFAULT = "30000";
	public static final String ORCHESTRATION_HISTORY_MAX_AGE_DEFAULT = "15";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SimpleStoreServiceOrchestrationDefaults() {
		throw new UnsupportedOperationException();
	}
}
