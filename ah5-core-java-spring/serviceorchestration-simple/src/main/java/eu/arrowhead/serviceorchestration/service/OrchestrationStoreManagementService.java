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
package eu.arrowhead.serviceorchestration.service;


import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationSimpleStoreListRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.dto.PriorityRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.service.SimpleStoreDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationSimpleStoreQueryRequest;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationStoreManagementServiceValidation;

@Service
public class OrchestrationStoreManagementService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private OrchestrationStoreManagementServiceValidation validator;

	@Autowired
	private SimpleStoreDbService dbService;

	@Autowired
	private PageService pageService;

	@Autowired
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO createSimpleStoreEntries(final OrchestrationSimpleStoreListRequestDTO dto, final String requesterName, final String origin) {
		logger.debug("createSimpleStoreEntries started...");

		final List<OrchestrationSimpleStoreRequestDTO> normalized = validator.validateAndNormalizeCreateBulk(dto, origin);

		try {
			final List<OrchestrationStore> created = dbService.createBulk(normalized, requesterName);
			return dtoConverter.convertStoreEntityListToResponseListDTO(created);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO querySimpleStoreEntries(final OrchestrationSimpleStoreQueryRequestDTO dto, final String origin) {
		logger.debug("querySimpleStoreEntries started...");

		final NormalizedOrchestrationSimpleStoreQueryRequest normalized = validator.validateAndNormalizeQuery(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, OrchestrationStore.SORTABLE_FIELDS_BY, OrchestrationStore.DEFAULT_SORT_FIELD, origin);

		try {
			final Page<OrchestrationStore> results = dbService.getPageByFilters(
					pageRequest,
					normalized.ids(),
					normalized.consumerNames(),
					normalized.serviceDefinitions(),
					normalized.serviceInstanceIds(),
					normalized.minPriority(),
					normalized.maxPriority(),
					normalized.createdBy());

			return dtoConverter.convertStoreEntityPageToResponseListTO(results);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO modifyPriorities(final PriorityRequestDTO dto, final String requesterName, final String origin) {
		logger.debug("modifyPriorities started...");

		final Map<UUID, Integer> normalized = validator.validateAndNormalizePriorityRequestDTO(dto, origin);
		try {
			final List<OrchestrationStore> modified = dbService.setPriorities(normalized, requesterName);
			return dtoConverter.convertStoreEntityListToResponseListDTO(modified);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void removeSimpleStoreEntries(final List<String> uuids, final String origin) {
		logger.debug("removeSimpleStoreEntries started...");

		final List<UUID> normalized = validator.validateAndNormalizeRemove(uuids, origin);

		try {
			dbService.deleteBulk(normalized);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}
