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

package eu.arrowhead.serviceorchestration.service.validation.utils;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.enums.NotifyProtocol;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.normalization.utils.OrchestrationNormalization;
import io.swagger.v3.oas.models.PathItem;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrchestrationValidation {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationNormalization normalizer;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private SimpleStoreServiceOrchestrationSystemInfo sysInfo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeSystemName(final String systemName, final String origin) {
		logger.debug("validateAndNormalizeSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalized = systemNameNormalizer.normalize(systemName);
		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public UUID validateAndNormalizeUUID(final String uuid, final String origin) {
		logger.debug("validateAndNormalizeUUID started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateUUID(uuid, origin);
		return normalizer.normalizeUUID(uuid);
	}

	//-------------------------------------------------------------------------------------------------
	public SimpleOrchestrationRequest validateAndNormalizeOrchestrationRequest(final OrchestrationRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeOrchestrationRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final SimpleOrchestrationRequest request = validateOrchestrationRequest(dto, origin);
		normalizer.normalizePull(request);

		try {
			if (!Utilities.isEmpty(request.getServiceDefinition())) {
				serviceDefNameValidator.validateServiceDefinitionName(request.getServiceDefinition());
			}

			if (!Utilities.isEmpty(request.getPreferredProviders())) {
				request.getPreferredProviders().forEach(p -> systemNameValidator.validateSystemName(p));
			}

			filterOutNotSupportedFlags(request);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		if (!Utilities.isEmpty(request.getOrchestrationFlags()) && request.getOrchestrationFlags().getOrDefault(OrchestrationFlag.ONLY_PREFERRED.toString(), false) && Utilities.isEmpty(request.getPreferredProviders())) {
			throw new InvalidParameterException(OrchestrationFlag.ONLY_PREFERRED.toString() + " flag is present, but no preferred provider is defined", origin);
		}


		return request;
	}

	//-------------------------------------------------------------------------------------------------
	public SimpleOrchestrationSubscriptionRequest validateAndNormalizePushSubscribe(final OrchestrationSubscriptionRequestDTO dto, final String requesterSystemName, final String origin) {
		logger.debug("validateAndNormalizePushSubscribe started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final SimpleOrchestrationSubscriptionRequest subscriptionRequest = validatePushSubscribe(dto, origin);
		normalizer.normalizeSubscribe(subscriptionRequest);

		// target system name
		final String normalizedRequesterSystemName = validateAndNormalizeSystemName(requesterSystemName, origin);

		if (Utilities.isEmpty(subscriptionRequest.getTargetSystemName())) {
			subscriptionRequest.setTargetSystemName(normalizedRequesterSystemName);
		} else if (!subscriptionRequest.getTargetSystemName().equals(normalizedRequesterSystemName)) {
			throw new InvalidParameterException("Target system cannot be different than the requester system", origin);
		}
		systemNameValidator.validateSystemName(subscriptionRequest.getTargetSystemName());

		// orchestration request
		validateSimpleOrchestrationRequest(subscriptionRequest.getOrchestrationRequest(), origin);

		// notify interface
		validateNotifyInterface(subscriptionRequest.getNotifyInterface(), origin);

		return subscriptionRequest;
	}

	//-------------------------------------------------------------------------------------------------
	public List<SimpleOrchestrationSubscriptionRequest> validateAndNormalizePushSubscribeBulk(final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizePushSubscribeBulk started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		final List<SimpleOrchestrationSubscriptionRequest> normalized = new ArrayList<>(dto.subscriptions().size());

		for (final OrchestrationSubscriptionRequestDTO subscriptionRequestDTO : dto.subscriptions()) {
			final SimpleOrchestrationSubscriptionRequest subscriptionRequest = validatePushSubscribeBulk(subscriptionRequestDTO, origin);
			normalizer.normalizeSubscribe(subscriptionRequest);
			systemNameValidator.validateSystemName(subscriptionRequest.getTargetSystemName());
			validateSimpleOrchestrationRequest(subscriptionRequest.getOrchestrationRequest(), origin);
			validateNotifyInterface(subscriptionRequest.getNotifyInterface(), origin);
			normalized.add(subscriptionRequest);
		}

		// check for duplications
		final List<Pair<String, String>> existing = new ArrayList<>(dto.subscriptions().size());
		dto.subscriptions().forEach(sub -> {
			final Pair<String, String> current = (Pair.of(sub.targetSystemName(), sub.orchestrationRequest().serviceRequirement().serviceDefinition()));
			if (existing.contains(current)) {
				throw new InvalidParameterException("Duplicated subscription request for system " + current.getLeft() + " and service definition " + current.getRight());
			}
			existing.add(current);
		});

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public void validateUUID(final String uuid, final String origin) {
		logger.debug("validateUUID started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(uuid.trim())) {
			throw new InvalidParameterException("UUID is missing", origin);
		}

		if (!Utilities.isUUID(uuid.trim())) {
			throw new InvalidParameterException("Invalid UUID id", origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-----------------------------------------------------------------------------------------------
	private SimpleOrchestrationRequest validateOrchestrationRequest(final OrchestrationRequestDTO dto, final String origin) {

		SimpleOrchestrationRequest simpleOrchestrationRequest = new SimpleOrchestrationRequest(null, null, null, null);

		// ignored fields will be added as warnings
		final List<String> ignoredFields = new ArrayList<>();

		if (dto == null) {
			return simpleOrchestrationRequest;
		}

		if (dto.serviceRequirement() != null) {

			if (!Utilities.isEmpty(dto.serviceRequirement().serviceDefinition())) {
				simpleOrchestrationRequest.setServiceDefinition(dto.serviceRequirement().serviceDefinition());
			}

			if (!Utilities.isEmpty(dto.serviceRequirement().operations())) {
				ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_OPERATIONS);
			}

			if (!Utilities.isEmpty(dto.serviceRequirement().versions())) {
				ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_VERSIONS);
			}

			if (!Utilities.isEmpty(dto.serviceRequirement().alivesAt())) {
				ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_ALIVES_AT);
			}

			if (!Utilities.isEmpty(dto.serviceRequirement().metadataRequirements())) {
				ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_METADATA_REQ);
			}

			if (!Utilities.isEmpty(dto.serviceRequirement().interfaceTemplateNames())) {
				ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_INTF_TEMPLATE_NAMES);
			}

			if (!Utilities.isEmpty(dto.serviceRequirement().interfaceAddressTypes())) {
				ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_INTF_ADDRESS_TYPES);
			}

			if (!Utilities.isEmpty(dto.serviceRequirement().interfacePropertyRequirements())) {
				ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_INTF_PROP_REQ);
			}

			if (!Utilities.isEmpty(dto.serviceRequirement().securityPolicies())) {
				ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_SECURITY_POLICIES);
			}

			if (!Utilities.isEmpty(dto.serviceRequirement().preferredProviders())) {
				if (Utilities.containsNullOrEmpty(dto.serviceRequirement().preferredProviders())) {
					throw new InvalidParameterException("Preferred providers contains null or empty element", origin);
				}

				simpleOrchestrationRequest.setPreferredProviders(dto.serviceRequirement().preferredProviders());
			}
		}

		if (!Utilities.isEmpty(dto.orchestrationFlags())) {
			dto.orchestrationFlags().values().forEach(value -> {
				if (value == null) {
					throw new InvalidParameterException("Orchestration flag map contains null value", origin);
				}
			});

			// ignored flags will be collected after normalization

			simpleOrchestrationRequest.setOrchestrationFlags(dto.orchestrationFlags());
		}

		if (!Utilities.isEmpty(dto.qualityRequirements())) {
			ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_QUALITY_REQ);
		}

		if (dto.exclusivityDuration() != null) {
			ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_EXCLUSIVITY_DURATION);
		}

		if (!Utilities.isEmpty(ignoredFields)) {
			simpleOrchestrationRequest.addWarning(ignoredFieldsToString(SimpleStoreServiceOrchestrationConstants.ORCH_WARN_IGNORED_FIELDS_KEY, ignoredFields));
		}

		return simpleOrchestrationRequest;
	}

	//-----------------------------------------------------------------------------------------------
	private SimpleOrchestrationSubscriptionRequest validatePushSubscribe(final OrchestrationSubscriptionRequestDTO dto, final String origin) {

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing");
		}

		// orchestration request
		final SimpleOrchestrationRequest validatedOrchestrationRequest = validateOrchestrationRequest(dto.orchestrationRequest(), origin);

		// notify interface
		if (Utilities.isEmpty(dto.notifyInterface().protocol())) {
			throw new InvalidParameterException("Notify protocol is missing", origin);
		}

		if (Utilities.isEmpty(dto.notifyInterface().properties())) {
			throw new InvalidParameterException("Notify properties are missing", origin);
		}

		dto.notifyInterface().properties().forEach((k, v) -> {
			if (Utilities.isEmpty(k)) {
				throw new InvalidParameterException("Notify properties contains empty key", origin);
			}
			if (Utilities.isEmpty(v)) {
				throw new InvalidParameterException("Notify properties contains empty value", origin);
			}
		});

		// duration
		if (dto.duration() != null && dto.duration() <= 0) {
			throw new InvalidParameterException("Subscription duration must be greater than 0", origin);
		}

		return new SimpleOrchestrationSubscriptionRequest(dto.targetSystemName(), validatedOrchestrationRequest, dto.notifyInterface(), dto.duration());
	}

	//-----------------------------------------------------------------------------------------------
	private SimpleOrchestrationSubscriptionRequest validatePushSubscribeBulk(final OrchestrationSubscriptionRequestDTO dto, final String origin) {
		final SimpleOrchestrationSubscriptionRequest validated = validatePushSubscribe(dto, origin);
		if (Utilities.isEmpty(dto.targetSystemName())) {
			throw new InvalidParameterException("Target system name is missing");
		}
		return validated;
	}


	//-----------------------------------------------------------------------------------------------
	private void validateSimpleOrchestrationRequest(final SimpleOrchestrationRequest request, final String origin) {
		try {
			if (!Utilities.isEmpty(request.getServiceDefinition())) {
				serviceDefNameValidator.validateServiceDefinitionName(request.getServiceDefinition());
			}

			if (!Utilities.isEmpty(request.getPreferredProviders())) {
				request.getPreferredProviders().forEach(p -> systemNameValidator.validateSystemName(p));
			}

			filterOutNotSupportedFlags(request);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		if (!Utilities.isEmpty(request.getOrchestrationFlags()) && request.getOrchestrationFlags().getOrDefault(OrchestrationFlag.ONLY_PREFERRED.toString(), false) && Utilities.isEmpty(request.getPreferredProviders())) {
			throw new InvalidParameterException(OrchestrationFlag.ONLY_PREFERRED.toString() + " flag is present, but no preferred provider is defined", origin);
		}
	}

	//-----------------------------------------------------------------------------------------------
	private void validateNotifyInterface(final OrchestrationNotifyInterfaceDTO intf, final String origin) {

		if (!Utilities.isEnumValue(intf.protocol(), NotifyProtocol.class)) {
			throw new InvalidParameterException("Unsupported notify protocol: " + intf.protocol(), origin);
		}

		if (intf.protocol().equals(NotifyProtocol.HTTP.name())
				|| intf.protocol().equals(NotifyProtocol.HTTPS.name())) {
			validateNormalizedNotifyPropertiesForHTTP(intf.properties(), origin);
		} else if (intf.protocol().equals(NotifyProtocol.MQTT.name())
				|| intf.protocol().equals(NotifyProtocol.MQTTS.name())) {
			if (!sysInfo.isMqttApiEnabled()) {
				throw new InvalidParameterException("MQTT notify protocol required, but MQTT is not enabled", origin);
			}
			validateNormalizedNotifyPropertiesForMQTT(intf.properties(), origin);
		}
	}

	//-----------------------------------------------------------------------------------------------
	private void filterOutNotSupportedFlags(final SimpleOrchestrationRequest request) {
		if (!Utilities.isEmpty(request.getOrchestrationFlags())) {
			final List<String> ignoredFlags = new ArrayList<>();
			final Map<String, Boolean> acceptedFlags = new HashMap<>();
			request.getOrchestrationFlags().forEach((key, value) -> {
				if (!Utilities.isEnumValue(key, OrchestrationFlag.class)) {
					throw new InvalidParameterException("Invalid orchestration flag: " + key);
				}

				if (SimpleStoreServiceOrchestrationConstants.SUPPORTED_FLAGS.contains(key)) {
					acceptedFlags.put(key, value);
				} else {
					ignoredFlags.add(key);
				}
			});

			request.setOrchestrationFlags(acceptedFlags);

			// add warnings, if any
			if (!Utilities.isEmpty(ignoredFlags)) {
				request.addWarning(ignoredFieldsToString(SimpleStoreServiceOrchestrationConstants.ORCH_WARN_IGNORED_FLAGS_KEY, ignoredFlags));
			}
		}
	}

	//-----------------------------------------------------------------------------------------------
	private String ignoredFieldsToString(final String fieldKey, final List<String> ignoredFields) {
		return fieldKey + ": [" + String.join(", ", ignoredFields) + "]";
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedNotifyPropertiesForHTTP(final Map<String, String> props, final String origin) {

		if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS)) {
			throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS + " property", origin);
		}

		if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PORT)) {
			throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PORT + " property", origin);
		}

		try {
			final int port = Integer.parseInt(props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PORT));
			if (port < Constants.MIN_PORT || port > Constants.MAX_PORT) {
				throw new InvalidParameterException("Notify port is out of the valid range", origin);
			}
		} catch (final NumberFormatException ex) {
			throw new InvalidParameterException("Notify port is not a number", origin);
		}

		if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD)) {
			throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD + " property", origin);
		}

		if (!(props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(PathItem.HttpMethod.POST.name())
				|| props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(PathItem.HttpMethod.PUT.name())
				|| props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(PathItem.HttpMethod.PATCH.name()))) {
			throw new InvalidParameterException("Unsupported notify HTTP method: " + props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD), origin);
		}

		if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PATH)) {
			throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PATH + " property", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedNotifyPropertiesForMQTT(final Map<String, String> props, final String origin) {

		// Sending MQTT notification is supported only via the main broker. Orchestrator does not connect to unknown brokers to send the orchestration results, so no address and port is required.

		if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_TOPIC)) {
			throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_TOPIC + " member", origin);
		}
	}
}
