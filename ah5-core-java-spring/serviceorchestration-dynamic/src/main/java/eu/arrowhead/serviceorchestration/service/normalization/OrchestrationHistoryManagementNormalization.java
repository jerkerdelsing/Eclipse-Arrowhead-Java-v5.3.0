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

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;

@Service
public class OrchestrationHistoryManagementNormalization {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationHistoryQueryRequestDTO normalizeOrchestrationHistoryQueryRequestDTO(final OrchestrationHistoryQueryRequestDTO dto) {
		logger.debug("normalizeOrchestrationHistoryQueryRequestDTO started...");

		if (dto == null) {
			return new OrchestrationHistoryQueryRequestDTO(null, new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		}

		return new OrchestrationHistoryQueryRequestDTO(
				dto.pagination(), // no need to normalize, because it will happen in the getPageRequest method
				Utilities.isEmpty(dto.ids()) ? new ArrayList<>() : dto.ids().stream().map(id -> id.trim()).toList(),
				Utilities.isEmpty(dto.statuses()) ? new ArrayList<>() : dto.statuses().stream().map(status -> status.trim().toUpperCase()).toList(),
				Utilities.isEmpty(dto.type()) ? null : dto.type().trim().toUpperCase(),
				Utilities.isEmpty(dto.requesterSystems()) ? new ArrayList<>() : dto.requesterSystems().stream().map(sys -> systemNameNormalizer.normalize(sys)).toList(),
				Utilities.isEmpty(dto.targetSystems()) ? new ArrayList<>() : dto.targetSystems().stream().map(sys -> systemNameNormalizer.normalize(sys)).toList(),
				Utilities.isEmpty(dto.serviceDefinitions()) ? new ArrayList<>() : dto.serviceDefinitions().stream().map(def -> serviceDefNameNormalizer.normalize(def)).toList(),
				Utilities.isEmpty(dto.subscriptionIds()) ? new ArrayList<>() : dto.subscriptionIds().stream().map(id -> id.trim()).toList());
	}
}