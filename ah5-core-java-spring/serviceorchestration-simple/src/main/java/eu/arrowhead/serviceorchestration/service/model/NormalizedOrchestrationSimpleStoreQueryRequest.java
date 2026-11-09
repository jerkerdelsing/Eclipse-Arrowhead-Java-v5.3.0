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
package eu.arrowhead.serviceorchestration.service.model;

import java.util.List;
import java.util.UUID;

import eu.arrowhead.dto.PageDTO;

public record NormalizedOrchestrationSimpleStoreQueryRequest(
		PageDTO pagination,
		List<UUID> ids,
		List<String> consumerNames,
		List<String> serviceDefinitions,
		List<String> serviceInstanceIds,
		Integer minPriority,
		Integer maxPriority,
		String createdBy) {

}
