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

public enum ServiceInterfacePolicy {
	NONE, CERT_AUTH, TIME_LIMITED_TOKEN_AUTH, USAGE_LIMITED_TOKEN_AUTH, BASE64_SELF_CONTAINED_TOKEN_AUTH, RSA_SHA256_JSON_WEB_TOKEN_AUTH, RSA_SHA512_JSON_WEB_TOKEN_AUTH, TRANSLATION_BRIDGE_TOKEN_AUTH;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static boolean isOfferable(final ServiceInterfacePolicy policy) {
		return policy != TRANSLATION_BRIDGE_TOKEN_AUTH;
	}
}