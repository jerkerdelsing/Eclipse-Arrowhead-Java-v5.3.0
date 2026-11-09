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
package eu.arrowhead.authorization.service.dto;

import eu.arrowhead.dto.enums.AuthorizationTargetType;

public record SelfContainedTokenPayload(
		String provider,
		String consumer,
		String cloud,
		AuthorizationTargetType targetType,
		String target,
		String scope) {
}