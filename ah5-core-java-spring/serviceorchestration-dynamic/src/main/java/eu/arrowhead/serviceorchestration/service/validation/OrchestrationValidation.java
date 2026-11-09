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

import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationFormValidation;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationSubscriptionValidation;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationServiceNormalization;

@Service
public class OrchestrationValidation {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationServiceNormalization normalization;

	@Autowired
	private OrchestrationFormValidation orchFormValidator;

	@Autowired
	private OrchestrationSubscriptionValidation orchSubsValidator;

	@Autowired
	private SystemNameValidator systemNameValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePullService(final OrchestrationForm form, final String origin) {
		logger.debug("validateAndNormalizePullService started...");

		orchFormValidator.validateAndNormalizeOrchestrationForm(form, false, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePushSubscribeService(final OrchestrationSubscription subscription, final String origin) {
		logger.debug("validateAndNormalizePushSubscribeService started...");

		orchSubsValidator.validateAndNormalizeOrchestrationSubscription(subscription, origin);

		if (!subscription.getOrchestrationForm().getRequesterSystemName().equals(subscription.getOrchestrationForm().getTargetSystemName())) {
			throw new InvalidParameterException("Target system cannot be different than the requester system", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<String, UUID> validateAndNormalizePushUnsubscribeService(final String requesterSystem, final String subscriptionId, final String origin) {
		logger.debug("validateAndNormalizePushUnsubscribeService started...");

		validatePushUnsubscribeService(requesterSystem, subscriptionId, origin);
		final Pair<String, UUID> normalized = normalization.normalizePushUnsubscribe(requesterSystem, subscriptionId);

		try {
			systemNameValidator.validateSystemName(normalized.getLeft());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validatePushUnsubscribeService(final String requesterSystem, final String subscriptionId, final String origin) {
		logger.debug("validatePushUnsubscribeService started...");

		if (Utilities.isEmpty(requesterSystem)) {
			throw new InvalidParameterException("Requester system is missing", origin);
		}

		if (Utilities.isEmpty(subscriptionId)) {
			throw new InvalidParameterException("Subscription id is missing", origin);
		}

		if (!Utilities.isUUID(subscriptionId)) {
			throw new InvalidParameterException("Invalid subscription id", origin);
		}
	}
}