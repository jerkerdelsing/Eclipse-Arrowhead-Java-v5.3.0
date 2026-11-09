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

import java.util.List;

public record OrchestrationLockQueryRequestDTO(
		PageDTO pagination,
		List<Long> ids,
		List<String> orchestrationJobIds,
		List<String> serviceInstanceIds,
		List<String> owners,
		String expiresBefore,
		String expiresAfter) {

}
