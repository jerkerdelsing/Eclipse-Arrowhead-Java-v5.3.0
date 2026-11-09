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
package eu.arrowhead.serviceorchestration.service.model.validation;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;

@Service
public class OrchestrationPushTriggerValidation {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateOrchestrationPushTrigger(final OrchestrationPushTrigger trigger, final String origin) {
		logger.debug("validateOrchestrationPushTrigger started...");

		if (trigger == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(trigger.getRequesterSystem())) {
			throw new InvalidParameterException("Requester system is missing", origin);
		}

		if (!Utilities.isEmpty(trigger.getTargetSystems()) && Utilities.containsNullOrEmpty(trigger.getTargetSystems())) {
			throw new InvalidParameterException("Target system list contains empty element", origin);
		}

		if (!Utilities.isEmpty(trigger.getSubscriptionIds()) && Utilities.containsNullOrEmpty(trigger.getSubscriptionIds())) {
			throw new InvalidParameterException("Subscription id list contains empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizeOrchestrationPushTrigger(final OrchestrationPushTrigger trigger, final String origin) {
		logger.debug("validateAndNormalizeOrchestrationPushTrigger started...");

		validateOrchestrationPushTrigger(trigger, origin);
		trigger.setRequesterSystem(systemNameNormalizer.normalize(trigger.getRequesterSystem()));

		try {
			systemNameValidator.validateSystemName(trigger.getRequesterSystem());

			if (!Utilities.isEmpty(trigger.getTargetSystems())) {
				trigger.setTargetSystems(trigger.getTargetSystems()
						.stream()
						.map(sys -> {
							final String normalized = systemNameNormalizer.normalize(sys);
							systemNameValidator.validateSystemName(normalized);

							return normalized;
						}).toList());
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		if (!Utilities.isEmpty(trigger.getSubscriptionIds())) {
			trigger.setSubscriptionIds(trigger.getSubscriptionIds().stream().map(id -> {
				try {
					return UUID.fromString(id.trim()).toString();
				} catch (final IllegalArgumentException ex) {
					throw new InvalidParameterException("Invalid subscription id: " + id, origin);
				}
			}).toList());
		}
	}
}