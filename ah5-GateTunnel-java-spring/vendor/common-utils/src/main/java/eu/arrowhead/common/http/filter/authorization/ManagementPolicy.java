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
package eu.arrowhead.common.http.filter.authorization;

public enum ManagementPolicy {
	SYSOP_ONLY, WHITELIST, AUTHORIZATION;

	//=================================================================================================
	// members

	public static final String SYSOP_ONLY_VALUE = "SYSOP_ONLY"; // right side must be a constant expression
}