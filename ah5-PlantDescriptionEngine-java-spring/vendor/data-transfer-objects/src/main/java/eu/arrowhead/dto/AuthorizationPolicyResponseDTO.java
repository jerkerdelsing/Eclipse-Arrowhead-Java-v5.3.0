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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@JsonInclude(Include.NON_NULL)
public record AuthorizationPolicyResponseDTO(
		String instanceId,
		AuthorizationLevel level,
		String cloud,
		String provider,
		AuthorizationTargetType targetType,
		String target,
		String description,
		AuthorizationPolicyDTO defaultPolicy,
		Map<String, AuthorizationPolicyDTO> scopedPolicies,
		String createdBy,
		String createdAt) {
}
