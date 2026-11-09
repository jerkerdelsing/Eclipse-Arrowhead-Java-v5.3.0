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
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.validation.utils.OrchestrationValidation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrchestrationPushManagementServiceValidation {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationValidation orchValidator;

	@Autowired
	private PageValidator pageValidator;

	@Autowired
	private SystemNameValidator sysNameValidator;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private SystemNameNormalizer sysNameNormalizer;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRequester(final String requesterSystemName, final String origin) {
		logger.debug("validateAndNormalizeRequester started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		return orchValidator.validateAndNormalizeSystemName(requesterSystemName, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public List<SimpleOrchestrationSubscriptionRequest> validateAndNormalizePushSubscribeBulk(final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizePushSubscribeBulk started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		return orchValidator.validateAndNormalizePushSubscribeBulk(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedOrchestrationPushTrigger validateAndNormalizePushTrigger(final OrchestrationPushTriggerDTO dto, final String origin) {
		logger.debug("validateAndNormalizePushTrigger started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		final NormalizedOrchestrationPushTrigger normalized = new NormalizedOrchestrationPushTrigger();

		// target system names
		if (!Utilities.isEmpty(dto.targetSystems())) {
			if (Utilities.containsNullOrEmpty(dto.targetSystems())) {
				throw new InvalidParameterException("Target system list contains null or empty element", origin);
			}

			// validate and normalize
			final List<String> normalizedTargets = dto.targetSystems().stream().map(target -> orchValidator.validateAndNormalizeSystemName(target, origin)).toList();

			// check duplications
			final List<String> checked = new ArrayList<>(normalizedTargets.size());
			for (final String systemName : normalizedTargets) {
				if (checked.contains(systemName)) {
					throw new InvalidParameterException("Duplicated target system name: " + systemName, origin);
				}
				checked.add(systemName);
			}

			normalized.setTargetSystems(normalizedTargets);
		}

		// subscription ids
		if (!Utilities.isEmpty(dto.subscriptionIds())) {
			if (Utilities.containsNullOrEmpty(dto.subscriptionIds())) {
				throw new InvalidParameterException("Subscription id list contains null or empty element", origin);
			}

			// validate and normalize
			final List<UUID> normalizedIds = dto.subscriptionIds().stream().map(id -> orchValidator.validateAndNormalizeUUID(id, origin)).toList();

			// check duplications
			final List<UUID> checked = new ArrayList<>(normalizedIds.size());
			for (final UUID id : normalizedIds) {
				if (checked.contains(id)) {
					throw new InvalidParameterException("Duplicated subscription id: " + id.toString(), origin);
				}
				checked.add(id);
			}

			normalized.setSubscriptionIds(normalizedIds);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionQueryRequestDTO validateAndNormalizeQueryPushSubscriptionsService(final OrchestrationSubscriptionQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryPushSubscriptionsService started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateQueryPushSubscriptionsService(dto, origin);

		final OrchestrationSubscriptionQueryRequestDTO normalized = new OrchestrationSubscriptionQueryRequestDTO(
				dto == null ? null : dto.pagination(),
				dto == null || Utilities.isEmpty(dto.ownerSystems()) ? List.of() : dto.ownerSystems().stream().map(sys -> sysNameNormalizer.normalize(sys)).toList(),
				dto == null || Utilities.isEmpty(dto.targetSystems()) ? List.of() : dto.targetSystems().stream().map(sys -> sysNameNormalizer.normalize(sys)).toList(),
				dto == null || Utilities.isEmpty(dto.serviceDefinitions()) ? List.of() : dto.serviceDefinitions().stream().map(def -> serviceDefNameNormalizer.normalize(def)).toList());

		try {
			if (!Utilities.isEmpty(normalized.ownerSystems())) {
				normalized.ownerSystems().forEach(sys -> sysNameValidator.validateSystemName(sys));
			}

			if (!Utilities.isEmpty(normalized.targetSystems())) {
				normalized.targetSystems().forEach(sys -> sysNameValidator.validateSystemName(sys));
			}

			if (!Utilities.isEmpty(normalized.serviceDefinitions())) {
				normalized.serviceDefinitions().forEach(def -> serviceDefNameValidator.validateServiceDefinitionName(def));
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<UUID> validateAndNormalizePushUnsubscribe(final List<String> ids, final String origin) {
		logger.debug("validateAndNormalizePushUnsubscribe started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validatePushUnsubscribe(ids, origin);
		return ids.stream().map(id -> orchValidator.validateAndNormalizeUUID(id, origin)).toList();
	}

	//=================================================================================================
	// assistant methods

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

	//-------------------------------------------------------------------------------------------------
	public void validatePushUnsubscribe(final List<String> ids, final String origin) {
		logger.debug("validatePushUnsubscribe started...");

		if (Utilities.isEmpty(ids)) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		// null or empty check happens in validateAndNormalizeUUID
	}
}
