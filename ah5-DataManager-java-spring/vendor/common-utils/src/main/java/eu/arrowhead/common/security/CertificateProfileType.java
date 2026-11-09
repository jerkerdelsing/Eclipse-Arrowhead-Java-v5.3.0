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
package eu.arrowhead.common.security;

public enum CertificateProfileType {
	MASTER("ma"),
	GATE("ga"),
	ORGANIZATION("or"),
	LOCAL_CLOUD("lo"),
	ON_BOARDING("on"),
	DEVICE("de"),
	SYSTEM("sy"),
	OPERATOR("op");

	//=================================================================================================
	// members

	private final String code;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String getCode() {
		return code;
	}

	//-------------------------------------------------------------------------------------------------
	public static CertificateProfileType fromCode(final String code) {
		for (final CertificateProfileType candidate : CertificateProfileType.values()) {
			if (candidate.getCode().equals(code)) {
				return candidate;
			}
		}

		return null;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private CertificateProfileType(final String code) {
		this.code = code;
	}
}