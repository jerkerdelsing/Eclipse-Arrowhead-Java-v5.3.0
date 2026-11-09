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

public enum AuthorizationTokenType {
	TIME_LIMITED_TOKEN, USAGE_LIMITED_TOKEN, SELF_CONTAINED_TOKEN, TRANSLATION_BRIDGE_TOKEN;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static AuthorizationTokenType fromServiceInterfacePolicy(final ServiceInterfacePolicy policy) {
		if (policy == null) {
			return null;
		}

		switch (policy) {
		case NONE:
		case CERT_AUTH:
			return null;
		case BASE64_SELF_CONTAINED_TOKEN_AUTH:
		case RSA_SHA256_JSON_WEB_TOKEN_AUTH:
		case RSA_SHA512_JSON_WEB_TOKEN_AUTH:
			return SELF_CONTAINED_TOKEN;
		case TIME_LIMITED_TOKEN_AUTH:
			return TIME_LIMITED_TOKEN;
		case USAGE_LIMITED_TOKEN_AUTH:
			return USAGE_LIMITED_TOKEN;
		case TRANSLATION_BRIDGE_TOKEN_AUTH:
			return TRANSLATION_BRIDGE_TOKEN;
		default:
			throw new IllegalArgumentException("Unknown service interface policy: " + policy.name());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isOfferable(final AuthorizationTokenType type) {
		return type != TRANSLATION_BRIDGE_TOKEN;
	}
}