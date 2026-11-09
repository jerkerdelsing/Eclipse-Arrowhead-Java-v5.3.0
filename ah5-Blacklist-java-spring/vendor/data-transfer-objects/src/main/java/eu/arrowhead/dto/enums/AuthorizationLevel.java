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
package eu.arrowhead.dto.enums;

public enum AuthorizationLevel {
	MGMT("MGMT"), PROVIDER("PR");

	//=================================================================================================
	// members

	private String prefix;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String getPrefix() {
		return prefix;
	}

	//-------------------------------------------------------------------------------------------------
	public static AuthorizationLevel fromPrefix(final String prefix) {
		return switch (prefix) {
		case "MGMT" -> AuthorizationLevel.MGMT;
		case "PR" -> AuthorizationLevel.PROVIDER;
		case null, default -> null;
		};
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationLevel(final String prefix) {
		this.prefix = prefix;
	}
}