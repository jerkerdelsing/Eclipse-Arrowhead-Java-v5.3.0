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

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.service.normalization.AuthorizationTokenNormalizer;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Service
public class AuthorizationTokenManagementValidation {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private CloudIdentifierValidator cloudIdentifierValidator;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	@Autowired
	private EventTypeNameValidator eventTypeNameValidator;

	@Autowired
	private AuthorizationScopeValidator scopeValidator;

	@Autowired
	private AuthorizationTokenNormalizer tokenNormalizer;

	@Autowired
	private PageValidator pageValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateTokenReferences(final List<String> tokenReferences, final String origin) {
		logger.debug("validateTokenReferences started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(tokenReferences)) {
			throw new InvalidParameterException("Token references list is empty", origin);
		}

		if (Utilities.containsNullOrEmpty(tokenReferences)) {
			throw new InvalidParameterException("Token references list contains null or empty element", origin);
		}
	}

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
	public AuthorizationTokenGenerationMgmtListRequestDTO validateAndNormalizeGenerateTokenRequests(final AuthorizationTokenGenerationMgmtListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGenerateTokenRequests started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateGenerateTokenRequests(dto, origin);
		final AuthorizationTokenGenerationMgmtListRequestDTO normalized = tokenNormalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(dto);

		for (final AuthorizationTokenGenerationMgmtRequestDTO request : normalized.list()) {
			if (!request.tokenVariant().endsWith(Constants.AUTHORIZATION_TOKEN_VARIANT_SUFFIX)
					|| !ServiceInterfacePolicy.isOfferable(ServiceInterfacePolicy.valueOf(request.tokenVariant()))) {
				throw new InvalidParameterException("Invalid token variant: " + request.tokenVariant(), origin);
			}

			try {
				cloudIdentifierValidator.validateCloudIdentifier(request.consumerCloud());
				systemNameValidator.validateSystemName(request.consumer());
				systemNameValidator.validateSystemName(request.provider());

				if (AuthorizationTargetType.SERVICE_DEF.name().equals(request.targetType())) {
					serviceDefinitionNameValidator.validateServiceDefinitionName(request.target());
				} else {
					eventTypeNameValidator.validateEventTypeName(request.target());
				}

				if (request.scope() != null) {
					scopeValidator.validateScope(request.scope());
				}
			} catch (final InvalidParameterException ex) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenQueryRequestDTO validateAndNormalizeQueryTokensRequest(final AuthorizationTokenQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizedQueryTokensRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateQueryTokensRequest(dto, origin);
		final AuthorizationTokenQueryRequestDTO normalized = tokenNormalizer.normalizeAuthorizationTokenQueryRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.requester())) {
				systemNameValidator.validateSystemName(normalized.requester());
			}
			if (!Utilities.isEmpty(normalized.consumerCloud())) {
				cloudIdentifierValidator.validateCloudIdentifier(normalized.consumerCloud());
			}
			if (!Utilities.isEmpty(normalized.consumer())) {
				systemNameValidator.validateSystemName(normalized.consumer());
			}
			if (!Utilities.isEmpty(normalized.provider())) {
				systemNameValidator.validateSystemName(normalized.provider());
			}
			if (!Utilities.isEmpty(normalized.target())) {
				if (normalized.targetType() == null || AuthorizationTargetType.SERVICE_DEF.name().equals(normalized.targetType())) {
					serviceDefinitionNameValidator.validateServiceDefinitionName(normalized.target());
				} else {
					eventTypeNameValidator.validateEventTypeName(normalized.target());
				}
			}
			if (!Utilities.isEmpty(normalized.tokenType())
					&& !AuthorizationTokenType.isOfferable(AuthorizationTokenType.valueOf(normalized.tokenType()))) {
				throw new InvalidParameterException("Token type is invalid: " + normalized.tokenType());
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO validateAndNormalizeAddEncryptionKeysRequest(final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeAddEncryptionKeysRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateAddEncryptionKeysRequest(dto, origin);
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO normalized = tokenNormalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(dto);

		final Set<String> sysNames = new HashSet<>(normalized.list().size());
		for (final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO request : normalized.list()) {
			try {
				systemNameValidator.validateSystemName(request.systemName());
			} catch (final InvalidParameterException ex) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			if (sysNames.contains(request.systemName())) {
				throw new InvalidParameterException("Duplicate system name: " + request.systemName(), origin);
			}
			sysNames.add(request.systemName());

			if (!(request.algorithm().equalsIgnoreCase(SecretCryptographer.AES_ECB_ALGORITHM) || request.algorithm().equalsIgnoreCase(SecretCryptographer.AES_CBC_ALGORITHM_IV_BASED))) {
				throw new InvalidParameterException("Unsupported algorithm", origin);
			}

			if (request.key().getBytes().length < SecretCryptographer.AES_KEY_MIN_SIZE) {
				throw new InvalidParameterException("Key must be minimum " + SecretCryptographer.AES_KEY_MIN_SIZE + " bytes long for system: " + request.systemName());
			}
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
	private void validateGenerateTokenRequests(final AuthorizationTokenGenerationMgmtListRequestDTO dto, final String origin) {
		logger.debug("validateGenerateTokenRequests started...");

		if (dto == null || Utilities.isEmpty(dto.list())) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.containsNull(dto.list())) {
			throw new InvalidParameterException("Request payload list contains null element", origin);
		}

		for (final AuthorizationTokenGenerationMgmtRequestDTO request : dto.list()) {
			if (Utilities.isEmpty(request.tokenVariant())) {
				throw new InvalidParameterException("Token variant is missing", origin);
			}

			final String tokenVariant = request.tokenVariant().trim().toUpperCase();
			if (!Utilities.isEnumValue(tokenVariant, ServiceInterfacePolicy.class)) {
				throw new InvalidParameterException("Token variant is invalid: " + request.tokenVariant(), origin);
			}

			if (!Utilities.isEmpty(request.targetType())) {
				final String targetTypeName = request.targetType().trim().toUpperCase();
				if (!Utilities.isEnumValue(targetTypeName, AuthorizationTargetType.class)) {
					throw new InvalidParameterException("Target type is invalid: " + request.targetType(), origin);
				}
			}

			if (Utilities.isEmpty(request.consumer())) {
				throw new InvalidParameterException("Consumer system name is missing", origin);
			}

			if (Utilities.isEmpty(request.provider())) {
				throw new InvalidParameterException("Provider system name is missing", origin);
			}

			if (Utilities.isEmpty(request.target())) {
				throw new InvalidParameterException("Target is missing", origin);
			}

			if (!Utilities.isEmpty(request.expiresAt())) {
				ZonedDateTime expiresAt = null;
				try {
					expiresAt = Utilities.parseUTCStringToZonedDateTime(request.expiresAt().trim());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Expiration time has an invalid time format", origin);
				}

				if (Utilities.utcNow().isAfter(expiresAt)) {
					throw new InvalidParameterException("Expiration time is in the past", origin);
				}
			}

			if (request.usageLimit() != null) {
				if (request.usageLimit().intValue() <= 0) {
					throw new InvalidParameterException("Usage limit is invalid", origin);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQueryTokensRequest(final AuthorizationTokenQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryTokensRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		pageValidator.validatePageParameter(dto.pagination(), TokenHeader.SORTABLE_FIELDS_BY, origin);

		if (!Utilities.isEmpty(dto.tokenType())) {
			final String tokenTypeStr = dto.tokenType().toUpperCase().trim();
			if (!Utilities.isEnumValue(tokenTypeStr, AuthorizationTokenType.class)) {
				throw new InvalidParameterException("Invalid token type: " + dto.tokenType(), origin);
			}
		}

		if (!Utilities.isEmpty(dto.targetType())) {
			final String targetTypeStr = dto.targetType().toUpperCase().trim();
			if (!Utilities.isEnumValue(targetTypeStr, AuthorizationTargetType.class)) {
				throw new InvalidParameterException("Invalid target type: " + dto.targetType(), origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateAddEncryptionKeysRequest(final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto, final String origin) {
		logger.debug("validateAddEncryptionKeysRequest started...");

		if (dto == null || Utilities.isEmpty(dto.list())) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.containsNull(dto.list())) {
			throw new InvalidParameterException("Request payload list contains null element", origin);
		}

		for (final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO request : dto.list()) {
			if (Utilities.isEmpty(request.systemName())) {
				throw new InvalidParameterException("System name is missing", origin);
			}
			if (Utilities.isEmpty(request.key())) {
				throw new InvalidParameterException("Key is missing", origin);
			}
		}
	}
}