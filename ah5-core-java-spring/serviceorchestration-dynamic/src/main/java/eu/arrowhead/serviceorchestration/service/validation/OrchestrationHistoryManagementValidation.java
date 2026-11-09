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
package eu.arrowhead.serviceorchestration.service.validation;

import eu.arrowhead.dto.enums.OrchestrationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationHistoryManagementNormalization;

@Service
public class OrchestrationHistoryManagementValidation {

	//=================================================================================================
	// members

	@Autowired
	private PageValidator pageValidator;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private OrchestrationHistoryManagementNormalization normalization;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION & NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public OrchestrationHistoryQueryRequestDTO validateAndNormalizeQueryService(final OrchestrationHistoryQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryService started...");

		validateQueryService(dto, origin);
		final OrchestrationHistoryQueryRequestDTO normalized = normalization.normalizeOrchestrationHistoryQueryRequestDTO(dto);

		normalized.ids().forEach(id -> {
			if (!Utilities.isUUID(id)) {
				throw new InvalidParameterException("Invalid id: " + id, origin);
			}
		});

		normalized.statuses().forEach(status -> {
			if (!Utilities.isEnumValue(status, OrchestrationJobStatus.class)) {
				throw new InvalidParameterException("Invalid status: " + status, origin);
			}
		});

		if (!Utilities.isEmpty(normalized.type()) && !Utilities.isEnumValue(normalized.type(), OrchestrationType.class)) {
			throw new InvalidParameterException("Invalid type: " + normalized.type(), origin);
		}

		try {
			normalized.requesterSystems().forEach(sys -> systemNameValidator.validateSystemName(sys));
			normalized.targetSystems().forEach(sys -> systemNameValidator.validateSystemName(sys));
			normalized.serviceDefinitions().forEach(def -> serviceDefNameValidator.validateServiceDefinitionName(def));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		normalized.subscriptionIds().forEach(id -> {
			if (!Utilities.isUUID(id)) {
				throw new InvalidParameterException("Invalid id: " + id, origin);
			}
		});

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateQueryService(final OrchestrationHistoryQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryService started...");

		if (dto == null) {
			return;
		}

		pageValidator.validatePageParameter(dto.pagination(), OrchestrationJob.SORTABLE_FIELDS_BY, origin);

		if (!Utilities.isEmpty(dto.ids()) && Utilities.containsNullOrEmpty(dto.ids())) {
			throw new InvalidParameterException("ID list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.statuses()) && Utilities.containsNullOrEmpty(dto.statuses())) {
			throw new InvalidParameterException("Status list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.requesterSystems()) && Utilities.containsNullOrEmpty(dto.requesterSystems())) {
			throw new InvalidParameterException("Requester system list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.targetSystems()) && Utilities.containsNullOrEmpty(dto.targetSystems())) {
			throw new InvalidParameterException("Target system list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.serviceDefinitions()) && Utilities.containsNullOrEmpty(dto.serviceDefinitions())) {
			throw new InvalidParameterException("Service definition list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.subscriptionIds()) && Utilities.containsNullOrEmpty(dto.subscriptionIds())) {
			throw new InvalidParameterException("Subscription ID list contains empty element", origin);
		}
	}
}