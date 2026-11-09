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

public record TranslationQueryRequestDTO(
		PageDTO pagination,
		List<String> bridgeIds,
		List<String> creators,
		List<String> statuses,
		List<String> consumers,
		List<String> providers,
		List<String> serviceDefinitions,
		List<String> interfaceTranslators,
		List<String> dataModelTranslators,
		String creationFrom,
		String creationTo,
		String alivesFrom,
		String alivesTo,
		Integer minUsage,
		Integer maxUsage) {
}