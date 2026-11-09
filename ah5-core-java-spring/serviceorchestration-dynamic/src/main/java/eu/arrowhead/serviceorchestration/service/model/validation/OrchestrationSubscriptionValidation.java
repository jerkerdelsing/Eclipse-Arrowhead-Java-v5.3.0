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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.dto.enums.NotifyProtocol;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import io.swagger.v3.oas.models.PathItem.HttpMethod;

@Service
public class OrchestrationSubscriptionValidation {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationFormValidation orchFormValidator;

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateOrchestrationSubscription(final OrchestrationSubscription subscription, final String origin) {
		logger.debug("validateOrchestrationSubscription started...");

		if (subscription == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (subscription.getDuration() != null && subscription.getDuration().longValue() <= 0) {
			throw new InvalidParameterException("Subscription duration must be greater than 0", origin);
		}

		if (Utilities.isEmpty(subscription.getNotifyProtocol())) {
			throw new InvalidParameterException("Notify protocol is missing", origin);
		}

		if (Utilities.isEmpty(subscription.getNotifyProperties())) {
			throw new InvalidParameterException("Notify properties are missing", origin);
		}

		subscription.getNotifyProperties().forEach((k, v) -> {
			if (Utilities.isEmpty(k)) {
				throw new InvalidParameterException("Notify properties contains empty key", origin);
			}
			if (Utilities.isEmpty(v)) {
				throw new InvalidParameterException("Notify properties contains empty value", origin);
			}
		});

		orchFormValidator.validateOrchestrationForm(subscription.getOrchestrationForm(), origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizeOrchestrationSubscription(final OrchestrationSubscription subscription, final String origin) {
		logger.debug("validateAndNormalizeOrchestrationSubscription started...");

		validateOrchestrationSubscription(subscription, origin);
		orchFormValidator.validateAndNormalizeOrchestrationForm(subscription.getOrchestrationForm(), true, origin); // we skip form pre-validation, because previous method already done that

		subscription.setNotifyProtocol(subscription.getNotifyProtocol().trim().toUpperCase());
		if (!Utilities.isEnumValue(subscription.getNotifyProtocol(), NotifyProtocol.class)) {
			throw new InvalidParameterException("Unsupported notify protocol: " + subscription.getNotifyProtocol(), origin);
		}

		final Map<String, String> normalizedNotifyProps = new HashMap<>();
		subscription.getNotifyProperties().forEach((k, v) -> {
			normalizedNotifyProps.put(k.trim().toLowerCase(), v.trim());
		});
		subscription.setNotifyProperties(normalizedNotifyProps);

		if (subscription.getNotifyProtocol().equals(NotifyProtocol.HTTP.name())
				|| subscription.getNotifyProtocol().equals(NotifyProtocol.HTTPS.name())) {
			validateNormalizedNotifyPropertiesForHTTP(subscription.getNotifyProperties(), origin);
		} else if (subscription.getNotifyProtocol().equals(NotifyProtocol.MQTT.name())
				|| subscription.getNotifyProtocol().equals(NotifyProtocol.MQTTS.name())) {
			if (!sysInfo.isMqttApiEnabled()) {
				throw new InvalidParameterException("MQTT notify protocol required, but MQTT is not enabled", origin);
			}
			validateNormalizedNotifyPropertiesForMQTT(subscription.getNotifyProperties(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedNotifyPropertiesForHTTP(final Map<String, String> props, final String origin) {
		logger.debug("validateNormalizedNotifyPropertiesForHTTP started...");

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS + " property", origin);
		}

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT + " property", origin);
		}

		try {
			final int port = Integer.parseInt(props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT));
			if (port < Constants.MIN_PORT || port > Constants.MAX_PORT) {
				throw new InvalidParameterException("Notify port is out of the valid range", origin);
			}
		} catch (final NumberFormatException ex) {
			throw new InvalidParameterException("Notify port is not a number", origin);
		}

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD + " property", origin);
		}

		if (!(props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(HttpMethod.POST.name())
				|| props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(HttpMethod.PUT.name())
				|| props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(HttpMethod.PATCH.name()))) {
			throw new InvalidParameterException("Unsupported notify HTTP method: " + props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD), origin);
		}

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PATH)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_PATH + " property", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedNotifyPropertiesForMQTT(final Map<String, String> props, final String origin) {
		logger.debug("validateNormalizedNotifyPropertiesForMQTT...");

		// Sending MQTT notification is supported only via the main broker. Orchestrator does not connect to unknown brokers to send the orchestration results, so no address and port is required.

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_TOPIC)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_TOPIC + " member", origin);
		}
	}
}