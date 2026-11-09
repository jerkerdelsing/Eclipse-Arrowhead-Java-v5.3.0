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
package eu.arrowhead.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@JsonInclude(Include.NON_NULL)
public record AuthorizationTokenResponseDTO(
		AuthorizationTokenType tokenType,
		String variant, // the exact token related ServiceInterfacePolicy
		String token, // Raw or encrypted token
		String tokenReference, // Hashed token (as stored in DB)
		String requester,
		String consumerCloud,
		String consumer,
		String provider,
		AuthorizationTargetType targetType,
		String target,
		String scope,
		String createdAt,
		Integer usageLimit,
		Integer usageLeft,
		String expiresAt) {
}