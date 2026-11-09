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
package eu.arrowhead.authorization.service.validation;

import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedLookupRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyInstanceIdentifierNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyRequestNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationScopeNormalizer;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationLookupRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.DTODefaults;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@Service
public class AuthorizationValidation {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private CloudIdentifierValidator cloudIdentifierValidator;

	@Autowired
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Autowired
	private EventTypeNameValidator eventTypeNameValidator;

	@Autowired
	private EventTypeNameNormalizer eventTypeNameNormalizer;

	@Autowired
	private AuthorizationPolicyRequestValidator policyRequestValidator;

	@Autowired
	private AuthorizationPolicyRequestNormalizer policyRequestNormalizer;

	@Autowired
	private AuthorizationScopeValidator scopeValidator;

	@Autowired
	private AuthorizationScopeNormalizer scopeNormalizer;

	@Autowired
	private AuthorizationPolicyInstanceIdentifierValidator instanceIdValidator;

	@Autowired
	private AuthorizationPolicyInstanceIdentifierNormalizer instanceIdNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeSystemName(final String systemName, final String origin) {
		logger.debug("validateAndNormalizeSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateSystemName(systemName, origin);
		final String normalized = systemNameNormalizer.normalize(systemName);

		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedGrantRequest validateAndNormalizeGrantRequest(final String normalizedProvider, final AuthorizationGrantRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGrantRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateGrantRequest(dto, origin);
		final NormalizedGrantRequest result = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		result.setProvider(normalizedProvider);
		normalizeGrantRequest(dto, result);

		try {
			cloudIdentifierValidator.validateCloudIdentifier(result.cloud());
			systemNameValidator.validateSystemName(result.provider());
			if (AuthorizationTargetType.SERVICE_DEF == result.targetType()) {
				serviceDefNameValidator.validateServiceDefinitionName(result.target());
			} else {
				eventTypeNameValidator.validateEventTypeName(result.target());
			}

			result.policies().forEach((k, v) -> {
				scopeValidator.validateScope(k);
				policyRequestValidator.validateNormalizedAuthorizationPolicy(v);
			});
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<String, String> validateAndNormalizeRevokeInput(final String systemName, final String instanceId, final String origin) {
		logger.debug("validateAndNormalizeRevokeInput started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateRevokeInput(systemName, instanceId, origin);

		final String normalizedSystemName = systemNameNormalizer.normalize(systemName);
		final String normalizedInstanceId = instanceIdNormalizer.normalize(instanceId);

		try {
			systemNameValidator.validateSystemName(normalizedSystemName);
			instanceIdValidator.validateInstanceIdentifier(normalizedInstanceId);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return Pair.of(normalizedSystemName, normalizedInstanceId);
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedLookupRequest validateAndNormalizeLookupRequest(final String normalizedProvider, final AuthorizationLookupRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLookupRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateLookupRequest(dto, origin);
		final NormalizedLookupRequest result = new NormalizedLookupRequest();
		result.setProviders(List.of(normalizedProvider));
		normalizeLookupRequest(dto, result);

		try {
			systemNameValidator.validateSystemName(normalizedProvider);

			if (!Utilities.isEmpty(result.instanceIds())) {
				result.instanceIds().forEach(i -> instanceIdValidator.validateInstanceIdentifier(i));
			}

			if (!Utilities.isEmpty(result.cloudIdentifiers())) {
				result.cloudIdentifiers().forEach(c -> cloudIdentifierValidator.validateCloudIdentifier(c));
			}

			if (!Utilities.isEmpty(result.targetNames())) {
				if (AuthorizationTargetType.SERVICE_DEF == result.targetType()) {
					result.targetNames().forEach(t -> serviceDefNameValidator.validateServiceDefinitionName(t));
				} else {
					result.targetNames().forEach(t -> eventTypeNameValidator.validateEventTypeName(t));
				}
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedVerifyRequest validateAndNormalizeVerifyRequest(final String normalizedRequester, final AuthorizationVerifyRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeVerifyRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateVerifyRequest(dto, origin);
		final NormalizedVerifyRequest normalized = normalizeVerifyRequest(normalizedRequester, dto, origin);

		try {
			systemNameValidator.validateSystemName(normalized.provider());
			systemNameValidator.validateSystemName(normalized.consumer());
			cloudIdentifierValidator.validateCloudIdentifier(normalized.cloud());

			if (AuthorizationTargetType.SERVICE_DEF == normalized.targetType()) {
				serviceDefNameValidator.validateServiceDefinitionName(normalized.target());
			} else {
				eventTypeNameValidator.validateEventTypeName(normalized.target());
			}

			if (!Utilities.isEmpty(normalized.scope())) {
				scopeValidator.validateScope(normalized.scope());
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateSystemName(final String systemName, final String origin) {
		logger.debug("validateSystemName started...");

		if (Utilities.isEmpty(systemName)) {
			throw new InvalidParameterException("System name is empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateGrantRequest(final AuthorizationGrantRequestDTO dto, final String origin) {
		logger.debug("validateGrantRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		// target type
		if (Utilities.isEmpty(dto.targetType())) {
			throw new InvalidParameterException("Target type is missing", origin);
		}

		final String targetTypeName = dto.targetType().trim().toUpperCase();

		if (!Utilities.isEnumValue(targetTypeName, AuthorizationTargetType.class)) {
			throw new InvalidParameterException("Target type is invalid: " + dto.targetType(), origin);
		}

		// target
		if (Utilities.isEmpty(dto.target())) {
			throw new InvalidParameterException("Target is missing", origin);
		}

		// default policy
		policyRequestValidator.validateAuthorizationPolicy(dto.defaultPolicy(), true, origin);

		// scoped policies
		if (dto.scopedPolicies() != null) {
			dto.scopedPolicies().entrySet().forEach(e -> {
				policyRequestValidator.validateAuthorizationPolicy(e.getValue(), false, origin);
			});
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRevokeInput(final String systemName, final String instanceId, final String origin) {
		logger.debug("validateRevokeInput started...");

		validateSystemName(systemName, origin);

		if (Utilities.isEmpty(instanceId)) {
			throw new InvalidParameterException("Instance id is missing", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateLookupRequest(final AuthorizationLookupRequestDTO dto, final String origin) {
		logger.debug("validateLookupRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		// one of the filters is mandatory
		if (Utilities.isEmpty(dto.instanceIds()) && Utilities.isEmpty(dto.targetNames()) && Utilities.isEmpty(dto.cloudIdentifiers())) {
			throw new InvalidParameterException("One of the following filters must be used: 'instanceIds', 'targetNames', 'cloudIdentifiers'", origin);
		}

		// instance ids
		if (!Utilities.isEmpty(dto.instanceIds()) && Utilities.containsNullOrEmpty(dto.instanceIds())) {
			throw new InvalidParameterException("Instance id list contains null or empty element", origin);
		}

		// target names
		if (!Utilities.isEmpty(dto.targetNames()) && Utilities.containsNullOrEmpty(dto.targetNames())) {
			throw new InvalidParameterException("Target names list contains null or empty element", origin);
		}

		// if target names are specified then target type is mandatory
		if (!Utilities.isEmpty(dto.targetNames()) && Utilities.isEmpty(dto.targetType())) {
			throw new InvalidParameterException("If target names list is specified then a valid target type is mandatory", origin);
		}

		// cloud identifiers
		if (!Utilities.isEmpty(dto.cloudIdentifiers()) && Utilities.containsNullOrEmpty(dto.cloudIdentifiers())) {
			throw new InvalidParameterException("Cloud identifiers list contains null or empty element", origin);
		}

		// target type
		if (!Utilities.isEmpty(dto.targetType())) {
			final String targetTypeName = dto.targetType().trim().toUpperCase();

			if (!Utilities.isEnumValue(targetTypeName, AuthorizationTargetType.class)) {
				throw new InvalidParameterException("Target type is invalid: " + dto.targetType(), origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateVerifyRequest(final AuthorizationVerifyRequestDTO dto, final String origin) {
		logger.debug("validateVerifyRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		// provider or consumer must be not null
		if (Utilities.isEmpty(dto.provider()) && Utilities.isEmpty(dto.consumer())) {
			throw new InvalidParameterException("Either provider or consumer must be specified", origin);
		}

		// target type
		if (Utilities.isEmpty(dto.targetType())) {
			throw new InvalidParameterException("Target type is missing", origin);
		}

		final String targetTypeName = dto.targetType().trim().toUpperCase();

		if (!Utilities.isEnumValue(targetTypeName, AuthorizationTargetType.class)) {
			throw new InvalidParameterException("Target type is invalid: " + dto.targetType(), origin);
		}

		// target
		if (Utilities.isEmpty(dto.target())) {
			throw new InvalidParameterException("Target is missing", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	private void normalizeGrantRequest(final AuthorizationGrantRequestDTO dto, final NormalizedGrantRequest result) {
		logger.debug("normalizeGrantRequest started...");

		result.setCloud(Utilities.isEmpty(dto.cloud()) ? Defaults.DEFAULT_CLOUD : cloudIdentifierNormalizer.normalize(dto.cloud()));
		result.setTargetType(AuthorizationTargetType.valueOf(dto.targetType().trim().toUpperCase()));
		if (AuthorizationTargetType.SERVICE_DEF == result.targetType()) {
			result.setTarget(serviceDefNameNormalizer.normalize(dto.target()));
		} else {
			result.setTarget(eventTypeNameNormalizer.normalize(dto.target()));
		}
		result.setDescription(dto.description());

		if (!Utilities.isEmpty(dto.scopedPolicies())) {
			for (final Entry<String, AuthorizationPolicyRequestDTO> entry : dto.scopedPolicies().entrySet()) {
				final String scope = scopeNormalizer.normalize(entry.getKey());
				final NormalizedAuthorizationPolicyRequest normalized = policyRequestNormalizer.normalize(entry.getValue());
				result.addPolicy(scope, normalized);
			}
		}

		result.addPolicy(Defaults.DEFAULT_AUTHORIZATION_SCOPE, policyRequestNormalizer.normalize(dto.defaultPolicy()));
	}

	//-------------------------------------------------------------------------------------------------
	private void normalizeLookupRequest(final AuthorizationLookupRequestDTO dto, final NormalizedLookupRequest result) {
		logger.debug("normalizeLookupRequest started...");

		result.setInstanceIds(Utilities.isEmpty(dto.instanceIds())
				? null
				: dto.instanceIds()
						.stream()
						.map(i -> instanceIdNormalizer.normalize(i))
						.toList());

		result.setCloudIdentifiers(Utilities.isEmpty(dto.cloudIdentifiers())
				? null
				: dto.cloudIdentifiers()
						.stream()
						.map(cId -> cloudIdentifierNormalizer.normalize(cId))
						.toList());

		result.setTargetType(Utilities.isEmpty(dto.targetType()) ? null : AuthorizationTargetType.valueOf(dto.targetType().trim().toUpperCase()));

		result.setTargetNames(Utilities.isEmpty(dto.targetNames())
				? null
				: dto.targetNames()
						.stream()
						.map(target -> {
							if (AuthorizationTargetType.SERVICE_DEF == result.targetType()) {
								return serviceDefNameNormalizer.normalize(target);
							} else {
								return eventTypeNameNormalizer.normalize(target);
							}
						})
						.toList());

	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedVerifyRequest normalizeVerifyRequest(final String normalizedRequester, final AuthorizationVerifyRequestDTO dto, final String origin) {
		logger.debug("normalizeVerifyRequest started...");

		final String normalizedProvider = Utilities.isEmpty(dto.provider()) ? normalizedRequester : systemNameNormalizer.normalize(dto.provider());
		final String normalizedConsumer = Utilities.isEmpty(dto.consumer()) ? normalizedRequester : systemNameNormalizer.normalize(dto.consumer());

		if (!normalizedProvider.equals(normalizedRequester) && !normalizedConsumer.equals(normalizedRequester)) {
			throw new ForbiddenException("Only the related provider or consumer can use this operation", origin);
		}

		final AuthorizationTargetType normalizedTargetType = AuthorizationTargetType.valueOf(dto.targetType().trim().toUpperCase());
		final String normalizedTarget = AuthorizationTargetType.SERVICE_DEF == normalizedTargetType
				? serviceDefNameNormalizer.normalize(dto.target())
				: eventTypeNameNormalizer.normalize(dto.target());

		return new NormalizedVerifyRequest(
				normalizedProvider,
				normalizedConsumer,
				Utilities.isEmpty(dto.cloud()) ? DTODefaults.DEFAULT_CLOUD : cloudIdentifierNormalizer.normalize(dto.cloud()),
				normalizedTargetType,
				normalizedTarget,
				Utilities.isEmpty(dto.scope()) ? null : scopeNormalizer.normalize(dto.scope()));
	}
}