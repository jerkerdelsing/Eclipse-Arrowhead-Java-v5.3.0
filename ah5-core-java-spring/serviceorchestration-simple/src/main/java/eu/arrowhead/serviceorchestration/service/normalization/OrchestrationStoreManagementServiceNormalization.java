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
package eu.arrowhead.serviceorchestration.service.normalization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.dto.PriorityRequestDTO;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationSimpleStoreQueryRequest;

@Service
public class OrchestrationStoreManagementServiceNormalization {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdNormalizer;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreRequestDTO normalizeCreate(final OrchestrationSimpleStoreRequestDTO dto) {
		logger.debug("normalizeCreate started...");
		Assert.notNull(dto, "DTO is null");

		return new OrchestrationSimpleStoreRequestDTO(
				systemNameNormalizer.normalize(dto.consumer()),
				serviceInstanceIdNormalizer.normalize(dto.serviceInstanceId()),
				dto.priority());
	}

	//-------------------------------------------------------------------------------------------------
	public Map<UUID, Integer> normalizePriorityRequestDTO(final PriorityRequestDTO dto) {
		logger.debug("normalizePriorityRequestDTO started...");

		final Map<UUID, Integer> normalized = new HashMap<>(dto.size());
		dto.forEach((idString, priority) -> normalized.put(UUID.fromString(idString.trim()), priority));
		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedOrchestrationSimpleStoreQueryRequest normalizeQuery(final OrchestrationSimpleStoreQueryRequestDTO dto) {
		logger.debug("normalizeQuery started...");

		return new NormalizedOrchestrationSimpleStoreQueryRequest(
				// no need to normalize, because it will happen in the getPageRequest method
				dto.pagination(),

				Utilities.isEmpty(dto.ids()) ? null
						: dto.ids().stream().map(id -> UUID.fromString(id.trim())).collect(Collectors.toList()),

				Utilities.isEmpty(dto.consumerNames()) ? null
						: dto.consumerNames().stream().map(c -> systemNameNormalizer.normalize(c)).collect(Collectors.toList()),

				Utilities.isEmpty(dto.serviceDefinitions()) ? null
						: dto.serviceDefinitions().stream().map(s -> serviceDefNameNormalizer.normalize(s)).collect(Collectors.toList()),

				Utilities.isEmpty(dto.serviceInstanceIds()) ? null
						: dto.serviceInstanceIds().stream().map(s -> serviceInstanceIdNormalizer.normalize(s)).collect(Collectors.toList()),

				dto.minPriority(),
				dto.maxPriority(),

				Utilities.isEmpty(dto.createdBy()) ? null
						: systemNameNormalizer.normalize(dto.createdBy()));
	}

	//-------------------------------------------------------------------------------------------------
	public List<UUID> normalizeRemove(final List<String> uuids) {
		logger.debug("normalizeRemove started...");

		return uuids.stream().map(id -> UUID.fromString(id.trim())).collect(Collectors.toList());
	}
}
