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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationPushTriggerValidation;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationSubscriptionValidation;

@Service
public class OrchestrationPushManagementValidation {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationSubscriptionValidation orchSubsValidator;

	@Autowired
	private OrchestrationPushTriggerValidation orchPushTriggerValidator;

	@Autowired
	private PageValidator pageValidator;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePushSubscribeService(final List<OrchestrationSubscription> subscription, final String origin) {
		logger.debug("validateAndNormalizePushSubscribeService started");

		if (Utilities.isEmpty(subscription)) {
			throw new InvalidParameterException("Subscription request list is empty", origin);
		}

		for (final OrchestrationSubscription subs : subscription) {
			orchSubsValidator.validateAndNormalizeOrchestrationSubscription(subs, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePushTriggerService(final OrchestrationPushTrigger trigger, final String origin) {
		logger.debug("validateAndNormalizePushTriggerService started");

		orchPushTriggerValidator.validateAndNormalizeOrchestrationPushTrigger(trigger, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRequesterSystem(final String requesterSystem, final String origin) {
		logger.debug("validateAndNormalizeRequesterSystem started");

		final String normalized = systemNameNormalizer.normalize(requesterSystem);

		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizePublishUnsubscribeService(final List<String> ids, final String origin) {
		logger.debug("validateAndNormalizePublishUnsubscribeService started...");

		validatePublishUnsubscribeService(ids, origin);

		final List<String> normalizedList = new ArrayList<>();
		ids.forEach(id -> {
			final String normalized = id.trim();
			if (!Utilities.isUUID(normalized)) {
				throw new InvalidParameterException("Invalid subscription ID: " + id, origin);
			}
			normalizedList.add(normalized);
		});

		return normalizedList;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionQueryRequestDTO validateAndNormalizeQueryPushSubscriptionsService(final OrchestrationSubscriptionQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryPushSubscriptionsService started...");

		validateQueryPushSubscriptionsService(dto, origin);

		final OrchestrationSubscriptionQueryRequestDTO normalized = new OrchestrationSubscriptionQueryRequestDTO(
				dto == null ? null : dto.pagination(),
				dto == null || Utilities.isEmpty(dto.ownerSystems()) ? List.of() : dto.ownerSystems().stream().map(sys -> systemNameNormalizer.normalize(sys)).toList(),
				dto == null || Utilities.isEmpty(dto.targetSystems()) ? List.of() : dto.targetSystems().stream().map(sys -> systemNameNormalizer.normalize(sys)).toList(),
				dto == null || Utilities.isEmpty(dto.serviceDefinitions()) ? List.of() : dto.serviceDefinitions().stream().map(def -> serviceDefNameNormalizer.normalize(def)).toList());

		try {
			if (!Utilities.isEmpty(normalized.ownerSystems())) {
				normalized.ownerSystems().forEach(sys -> systemNameValidator.validateSystemName(sys));
			}

			if (!Utilities.isEmpty(normalized.targetSystems())) {
				normalized.targetSystems().forEach(sys -> systemNameValidator.validateSystemName(sys));
			}

			if (!Utilities.isEmpty(normalized.serviceDefinitions())) {
				normalized.serviceDefinitions().forEach(def -> serviceDefNameValidator.validateServiceDefinitionName(def));
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validatePublishUnsubscribeService(final List<String> ids, final String origin) {
		logger.debug("validatePublishUnsubscribeService started...");

		if (Utilities.isEmpty(ids)) {
			throw new InvalidParameterException("ID list is empty", origin);
		}

		if (Utilities.containsNullOrEmpty(ids)) {
			throw new InvalidParameterException("ID list contains empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQueryPushSubscriptionsService(final OrchestrationSubscriptionQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryPushSubscriptionsService started...");

		if (dto == null) {
			return;
		}

		pageValidator.validatePageParameter(dto.pagination(), Subscription.SORTABLE_FIELDS_BY, origin);

		if (!Utilities.isEmpty(dto.ownerSystems()) && Utilities.containsNullOrEmpty(dto.ownerSystems())) {
			throw new InvalidParameterException("Owner system list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.targetSystems()) && Utilities.containsNullOrEmpty(dto.targetSystems())) {
			throw new InvalidParameterException("Target system list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.serviceDefinitions()) && Utilities.containsNullOrEmpty(dto.serviceDefinitions())) {
			throw new InvalidParameterException("Service definition list contains empty element", origin);
		}
	}
}