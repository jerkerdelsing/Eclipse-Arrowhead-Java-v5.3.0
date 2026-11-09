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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedQueryRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyInstanceIdentifierNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyRequestNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationScopeNormalizer;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.DTODefaults;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@Service
public class AuthorizationManagementValidation {

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
	private AuthorizationScopeValidator scopeValidator;

	@Autowired
	private AuthorizationScopeNormalizer scopeNormalizer;

	@Autowired
	private AuthorizationPolicyRequestValidator policyRequestValidator;

	@Autowired
	private AuthorizationPolicyRequestNormalizer policyRequestNormalizer;

	@Autowired
	private AuthorizationPolicyInstanceIdentifierValidator instanceIdValidator;

	@Autowired
	private AuthorizationPolicyInstanceIdentifierNormalizer instanceIdNormalizer;

	@Autowired
	private PageValidator pageValidator;

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
	public List<NormalizedGrantRequest> validateAndNormalizeGrantListRequest(final AuthorizationMgmtGrantListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGrantListRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateGrantListRequest(dto, origin);
		final List<NormalizedGrantRequest> normalized = normalizeGrantListRequest(dto);

		try {
			normalized.forEach(r -> {
				cloudIdentifierValidator.validateCloudIdentifier(r.cloud());
				systemNameValidator.validateSystemName(r.provider());
				if (AuthorizationTargetType.SERVICE_DEF == r.targetType()) {
					serviceDefNameValidator.validateServiceDefinitionName(r.target());
				} else {
					eventTypeNameValidator.validateEventTypeName(r.target());
				}

				r.policies().forEach((k, v) -> {
					scopeValidator.validateScope(k);
					policyRequestValidator.validateNormalizedAuthorizationPolicy(v);
				});
			});
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRevokePoliciesInput(final List<String> instanceIds, final String origin) {
		logger.debug("validateAndNormalizeRevokePoliciesInput started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateRevokePoliciesInput(instanceIds, origin);
		final List<String> normalized = normalizeInstanceIds(instanceIds);

		try {
			normalized.forEach(i -> instanceIdValidator.validateInstanceIdentifier(i));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedQueryRequest validateAndNormalizeQueryRequest(final AuthorizationQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateQueryRequest(dto, origin);
		final NormalizedQueryRequest normalized = normalizeQueryRequest(dto);

		try {
			if (!Utilities.isEmpty(normalized.providers())) {
				normalized.providers().forEach(p -> systemNameValidator.validateSystemName(p));
			}

			if (!Utilities.isEmpty(normalized.instanceIds())) {
				normalized.instanceIds().forEach(i -> instanceIdValidator.validateInstanceIdentifier(i));
			}

			if (!Utilities.isEmpty(normalized.cloudIdentifiers())) {
				normalized.cloudIdentifiers().forEach(c -> cloudIdentifierValidator.validateCloudIdentifier(c));
			}

			if (!Utilities.isEmpty(normalized.targetNames())) {
				if (AuthorizationTargetType.SERVICE_DEF == normalized.targetType()) {
					normalized.targetNames().forEach(t -> serviceDefNameValidator.validateServiceDefinitionName(t));
				} else {
					normalized.targetNames().forEach(t -> eventTypeNameValidator.validateEventTypeName(t));
				}
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedVerifyRequest> validateAndNormalizeVerifyListRequest(final AuthorizationVerifyListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeVerifyListRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateVerifyListRequest(dto, origin);

		final List<NormalizedVerifyRequest> normalized = normalizeVerifyListRequest(dto);

		try {
			normalized.forEach(r -> {
				systemNameValidator.validateSystemName(r.provider());
				systemNameValidator.validateSystemName(r.consumer());
				cloudIdentifierValidator.validateCloudIdentifier(r.cloud());
				if (AuthorizationTargetType.SERVICE_DEF == r.targetType()) {
					serviceDefNameValidator.validateServiceDefinitionName(r.target());
				} else {
					eventTypeNameValidator.validateEventTypeName(r.target());
				}

				if (r.scope() != null) {
					scopeValidator.validateScope(r.scope());
				}
			});
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
	private void validateGrantListRequest(final AuthorizationMgmtGrantListRequestDTO dto, final String origin) {
		logger.debug("validateGrantListRequest started...");

		if (dto == null || Utilities.isEmpty(dto.list())) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.containsNull(dto.list())) {
			throw new InvalidParameterException("Request payload list contains null element", origin);
		}

		for (final AuthorizationMgmtGrantRequestDTO grantDTO : dto.list()) {
			validateGrantRequest(grantDTO, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateGrantRequest(final AuthorizationMgmtGrantRequestDTO dto, final String origin) {
		logger.debug("validateGrantRequest started...");

		// provider
		validateSystemName(dto.provider(), origin);

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
		if (!Utilities.isEmpty(dto.scopedPolicies())) {
			for (final Entry<String, AuthorizationPolicyRequestDTO> entry : dto.scopedPolicies().entrySet()) {
				if (Utilities.isEmpty(entry.getKey())) {
					throw new InvalidParameterException("Scope is missing", origin);
				}

				policyRequestValidator.validateAuthorizationPolicy(entry.getValue(), false, origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRevokePoliciesInput(final List<String> instanceIds, final String origin) {
		logger.debug("validateRevokePoliciesInput started...");

		if (Utilities.isEmpty(instanceIds)) {
			throw new InvalidParameterException("Instance id list is missing", origin);
		}

		if (Utilities.containsNullOrEmpty(instanceIds)) {
			throw new InvalidParameterException("Instance id list contains null or empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQueryRequest(final AuthorizationQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		// level
		if (Utilities.isEmpty(dto.level())) {
			throw new InvalidParameterException("Level is missing", origin);
		}

		final String levelName = dto.level().trim().toUpperCase();
		if (!Utilities.isEnumValue(levelName, AuthorizationLevel.class)) {
			throw new InvalidParameterException("Level is invalid: " + dto.level(), origin);
		}

		// page
		try {
			pageValidator.validatePageParameter(
					dto.pagination(),
					levelName.equals(AuthorizationLevel.MGMT.name()) ? AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY : AuthProviderPolicyHeader.SORTABLE_FIELDS_BY,
					origin);
		} catch (final InvalidParameterException ex) {
			if (Utilities.isEmpty(ex.getOrigin())) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			throw ex;
		}

		// providers
		if (!Utilities.isEmpty(dto.providers()) && Utilities.containsNullOrEmpty(dto.providers())) {
			throw new InvalidParameterException("Provider list contains null or empty element", origin);
		}

		// instance ids
		if (!Utilities.isEmpty(dto.instanceIds()) && Utilities.containsNullOrEmpty(dto.instanceIds())) {
			throw new InvalidParameterException("Instance id list contains null or empty element", origin);
		}

		// target type
		if (!Utilities.isEmpty(dto.targetType())) {
			final String targetTypeName = dto.targetType().trim().toUpperCase();

			if (!Utilities.isEnumValue(targetTypeName, AuthorizationTargetType.class)) {
				throw new InvalidParameterException("Target type is invalid: " + dto.targetType(), origin);
			}
		}

		// target names (in this case target type is mandatory)
		if (!Utilities.isEmpty(dto.targetNames())) {
			if (Utilities.containsNullOrEmpty(dto.targetNames())) {
				throw new InvalidParameterException("Target names list contains null or empty element", origin);
			}

			if (Utilities.isEmpty(dto.targetType())) { // if not empty then it is valid because we have already checked that before
				throw new InvalidParameterException("If target names list is specified then a valid target type is mandatory", origin);
			}
		}

		// cloud identifiers
		if (!Utilities.isEmpty(dto.cloudIdentifiers()) && Utilities.containsNullOrEmpty(dto.cloudIdentifiers())) {
			throw new InvalidParameterException("Cloud identifiers list contains null or empty element", origin);
		}

	}

	//-------------------------------------------------------------------------------------------------
	private void validateVerifyListRequest(final AuthorizationVerifyListRequestDTO dto, final String origin) {
		logger.debug("validateVerifyListRequest started...");

		if (dto == null || Utilities.isEmpty(dto.list())) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.containsNull(dto.list())) {
			throw new InvalidParameterException("Request payload list contains null element", origin);
		}

		for (final AuthorizationVerifyRequestDTO request : dto.list()) {
			validateVerifyRequest(request, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateVerifyRequest(final AuthorizationVerifyRequestDTO dto, final String origin) {
		logger.debug("validateVerifyRequest started...");

		// provider
		if (Utilities.isEmpty(dto.provider())) {
			throw new InvalidParameterException("Provider is missing", origin);
		}

		// consumer
		if (Utilities.isEmpty(dto.consumer())) {
			throw new InvalidParameterException("Consumer is missing", origin);
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
	private List<NormalizedGrantRequest> normalizeGrantListRequest(final AuthorizationMgmtGrantListRequestDTO dto) {
		logger.debug("normalizeGrantListRequest started...");

		return dto
				.list()
				.stream()
				.map(grantDTO -> normalizeGrantRequest(grantDTO))
				.toList();
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedGrantRequest normalizeGrantRequest(final AuthorizationMgmtGrantRequestDTO dto) {
		logger.debug("normalizeGrantRequest started...");

		final NormalizedGrantRequest result = new NormalizedGrantRequest(AuthorizationLevel.MGMT);
		result.setCloud(Utilities.isEmpty(dto.cloud()) ? Defaults.DEFAULT_CLOUD : cloudIdentifierNormalizer.normalize(dto.cloud()));
		result.setProvider(systemNameNormalizer.normalize(dto.provider()));
		result.setTargetType(AuthorizationTargetType.valueOf(dto.targetType().trim().toUpperCase()));
		result.setTarget(AuthorizationTargetType.SERVICE_DEF == result.targetType()
				? serviceDefNameNormalizer.normalize(dto.target())
				: eventTypeNameNormalizer.normalize(dto.target()));
		result.setDescription(dto.description());

		if (!Utilities.isEmpty(dto.scopedPolicies())) {
			for (final Entry<String, AuthorizationPolicyRequestDTO> entry : dto.scopedPolicies().entrySet()) {
				final String scope = scopeNormalizer.normalize(entry.getKey());
				final NormalizedAuthorizationPolicyRequest normalized = policyRequestNormalizer.normalize(entry.getValue());
				result.addPolicy(scope, normalized);
			}
		}

		result.addPolicy(Defaults.DEFAULT_AUTHORIZATION_SCOPE, policyRequestNormalizer.normalize(dto.defaultPolicy()));

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private List<String> normalizeInstanceIds(final List<String> instanceIds) {
		logger.debug("normalizeInstanceIds started...");

		return instanceIds
				.stream()
				.map(i -> instanceIdNormalizer.normalize(i))
				.toList();
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedQueryRequest normalizeQueryRequest(final AuthorizationQueryRequestDTO dto) {
		logger.debug("normalizeQueryRequest started...");

		final NormalizedQueryRequest result = new NormalizedQueryRequest();
		result.setLevel(AuthorizationLevel.valueOf(dto.level().trim().toUpperCase()));

		result.setProviders(Utilities.isEmpty(dto.providers())
				? null
				: dto.providers()
						.stream()
						.map(p -> systemNameNormalizer.normalize(p))
						.toList());

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

		if (Utilities.isEmpty(dto.targetNames())) {
			result.setTargetNames(null);
		} else {
			result.setTargetNames(AuthorizationTargetType.SERVICE_DEF == result.targetType()
					? dto.targetNames().stream().map(t -> serviceDefNameNormalizer.normalize(t)).toList()
					: dto.targetNames().stream().map(t -> eventTypeNameNormalizer.normalize(t)).toList());
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private List<NormalizedVerifyRequest> normalizeVerifyListRequest(final AuthorizationVerifyListRequestDTO request) {
		logger.debug("normalizeVerifyListRequest started...");

		return request
				.list()
				.stream()
				.map(dto -> normalizeVerifyRequest(dto))
				.toList();
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedVerifyRequest normalizeVerifyRequest(final AuthorizationVerifyRequestDTO dto) {
		logger.debug("normalizeVerifyRequest started...");

		final AuthorizationTargetType normalizedTargetType = AuthorizationTargetType.valueOf(dto.targetType().trim().toUpperCase());
		final String normalizedTarget = AuthorizationTargetType.SERVICE_DEF == normalizedTargetType
				? serviceDefNameNormalizer.normalize(dto.target())
				: eventTypeNameNormalizer.normalize(dto.target());

		return new NormalizedVerifyRequest(
				systemNameNormalizer.normalize(dto.provider()),
				systemNameNormalizer.normalize(dto.consumer()),
				Utilities.isEmpty(dto.cloud()) ? DTODefaults.DEFAULT_CLOUD : cloudIdentifierNormalizer.normalize(dto.cloud()),
				normalizedTargetType,
				normalizedTarget,
				Utilities.isEmpty(dto.scope()) ? null : scopeNormalizer.normalize(dto.scope()));
	}
}