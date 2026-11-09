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
package eu.arrowhead.deviceqosevaluator.enums;

public enum OidGroup {

	//=================================================================================================
	// members

	RTT("0.0", 100d), CPU_TOTAL_LOAD("1.4", 100d), MEMORY_USED("2.1", 100d), NETWORK_EGRESS_LOAD("3.1", 100d), NETWORK_INGRESS_LOAD("3.2", 100d);

	private final String value;
	private final double worstStat;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	private OidGroup(final String value, final double worstStat) {
		this.value = value;
		this.worstStat = worstStat;
	}

	//-------------------------------------------------------------------------------------------------
	public String getValue() {
		return value;
	}

	//-------------------------------------------------------------------------------------------------
	public double getWorstStat() {
		return worstStat;
	}
}
