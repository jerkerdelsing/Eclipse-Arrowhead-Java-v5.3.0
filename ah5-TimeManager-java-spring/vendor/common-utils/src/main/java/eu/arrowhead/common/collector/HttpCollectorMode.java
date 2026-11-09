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
package eu.arrowhead.common.collector;

public enum HttpCollectorMode {
	// SR_ONLY: every service is looked up in the ServiceRegistry
	// SR_AND_ORCH: ServiceRegistry lookup first, orchestration when the lookup finds nothing
	// ORCHESTRATION: the bootstrap services (http.collector.bootstrap.services) are looked up in the ServiceRegistry,
	//                every other service is resolved by an orchestration request on behalf of this system
	SR_ONLY, SR_AND_ORCH, ORCHESTRATION;

	//=================================================================================================
	// members

	public static final String SR_AND_ORCH_VALUE = "SR_AND_ORCH"; // right side must be a constant expression
	public static final String ORCHESTRATION_VALUE = "ORCHESTRATION";
}