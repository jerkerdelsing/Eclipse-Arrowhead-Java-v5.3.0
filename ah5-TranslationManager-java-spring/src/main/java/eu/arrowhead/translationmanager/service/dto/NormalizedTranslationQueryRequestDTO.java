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
package eu.arrowhead.translationmanager.service.dto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;

public record NormalizedTranslationQueryRequestDTO(
		PageRequest pageRequest,
		List<UUID> bridgeIds,
		List<String> creators,
		List<TranslationBridgeStatus> statuses,
		List<String> consumers,
		List<String> providers,
		List<String> serviceDefinitions,
		List<String> interfaceTranslators,
		List<String> dataModelTranslators,
		ZonedDateTime creationFrom,
		ZonedDateTime creationTo,
		ZonedDateTime alivesFrom,
		ZonedDateTime alivesTo,
		Integer minUsage,
		Integer maxUsage) {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean hasAnyFilter() {
		return !Utilities.isEmpty(bridgeIds)
				|| !Utilities.isEmpty(creators)
				|| !Utilities.isEmpty(statuses)
				|| !Utilities.isEmpty(consumers)
				|| !Utilities.isEmpty(providers)
				|| !Utilities.isEmpty(serviceDefinitions)
				|| !Utilities.isEmpty(interfaceTranslators)
				|| !Utilities.isEmpty(dataModelTranslators)
				|| creationFrom != null
				|| creationTo != null
				|| alivesFrom != null
				|| alivesTo != null
				|| minUsage != null
				|| maxUsage != null;
	}
}