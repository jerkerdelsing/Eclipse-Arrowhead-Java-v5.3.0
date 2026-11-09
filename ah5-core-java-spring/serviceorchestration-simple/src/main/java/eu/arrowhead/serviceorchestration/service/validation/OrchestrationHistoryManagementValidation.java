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

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationJobQueryRequest;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationHistoryManagementNormalization;
import eu.arrowhead.serviceorchestration.service.validation.utils.OrchestrationValidation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
	private OrchestrationValidation orchestrationValidator;

	@Autowired
	private OrchestrationHistoryManagementNormalization normalization;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedOrchestrationJobQueryRequest validateAndNormalizeQueryService(final OrchestrationHistoryQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryService started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateQueryService(dto, origin);
		final NormalizedOrchestrationJobQueryRequest normalized = normalization.normalizeOrchestrationHistoryQueryRequestDTO(dto, origin);

		try {
			normalized.getRequesterSystems().forEach(sys -> systemNameValidator.validateSystemName(sys));
			normalized.getTargetSystems().forEach(sys -> systemNameValidator.validateSystemName(sys));
			normalized.getServiceDefinitions().forEach(def -> serviceDefNameValidator.validateServiceDefinitionName(def));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateQueryService(final OrchestrationHistoryQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryService started...");

		if (dto == null) {
			return;
		}

		pageValidator.validatePageParameter(dto.pagination(), OrchestrationJob.SORTABLE_FIELDS_BY, origin);

		// job ids
		if (!Utilities.isEmpty(dto.ids())) {
			dto.ids().forEach(id -> orchestrationValidator.validateUUID(id, origin));
		}

		// statuses
		if (!Utilities.isEmpty(dto.statuses())) {
			if (Utilities.containsNullOrEmpty(dto.statuses())) {
				throw new InvalidParameterException("Status list contains empty element", origin);
			}

			dto.statuses().forEach(status -> {
				if (!Utilities.isEnumValue(status.trim().toUpperCase(), OrchestrationJobStatus.class)) {
					throw new InvalidParameterException("Invalid status: " + status, origin);
				}
			});
		}

		// orchestration type
		if (dto.type() != null && !Utilities.isEnumValue(dto.type().trim().toUpperCase(), OrchestrationType.class)) {
			throw new InvalidParameterException("Invalid type: " + dto.type(), origin);
		}

		// requester systems
		if (!Utilities.isEmpty(dto.requesterSystems()) && Utilities.containsNullOrEmpty(dto.requesterSystems())) {
			throw new InvalidParameterException("Requester system list contains empty element", origin);
		}

		// target systems
		if (!Utilities.isEmpty(dto.targetSystems()) && Utilities.containsNullOrEmpty(dto.targetSystems())) {
			throw new InvalidParameterException("Target system list contains empty element", origin);
		}

		// service definitions
		if (!Utilities.isEmpty(dto.serviceDefinitions()) && Utilities.containsNullOrEmpty(dto.serviceDefinitions())) {
			throw new InvalidParameterException("Service definition list contains empty element", origin);
		}

		// subscription ids
		if (!Utilities.isEmpty(dto.subscriptionIds())) {
			dto.ids().forEach(id -> orchestrationValidator.validateUUID(id, origin));
		}
	}
}
