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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.service.normalization.AuthorizationTokenNormalizer;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Service
public class AuthorizationTokenValidation {

	//-------------------------------------------------------------------------------------------------
	// members

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	@Autowired
	private EventTypeNameValidator eventTypeNameValidator;

	@Autowired
	private AuthorizationScopeValidator scopeValidator;

	@Autowired
	private AuthorizationTokenNormalizer normalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeSystemName(final String systemName, final String origin) {
		logger.debug("validateAndNormalizeSystemName started...");

		validateSystemName(systemName, origin);
		final String normalized = normalizer.normalizeSystemName(systemName);

		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeToken(final String token, final String origin) {
		logger.debug("validateAndNormalizeToken started...");

		validateToken(token, origin);

		return normalizer.normalizeToken(token);
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationRequestDTO validateAndNormalizeGenerateRequest(final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGenerateRequest started...");

		validateGenerateRequest(dto, origin);
		final AuthorizationTokenGenerationRequestDTO normalized = normalizer.normalizeAuthorizationTokenGenerationRequestDTO(dto);

		try {
			if (!normalized.tokenVariant().endsWith(Constants.AUTHORIZATION_TOKEN_VARIANT_SUFFIX)
					|| !ServiceInterfacePolicy.isOfferable(ServiceInterfacePolicy.valueOf(normalized.tokenVariant()))) {
				throw new InvalidParameterException("Token variant is invalid", origin);
			}

			systemNameValidator.validateSystemName(normalized.provider());

			if (AuthorizationTargetType.SERVICE_DEF.name().equals(normalized.targetType())) {
				serviceDefinitionNameValidator.validateServiceDefinitionName(normalized.target());
			} else {
				eventTypeNameValidator.validateEventTypeName(normalized.target());
			}

			if (!Utilities.isEmpty(normalized.scope())) {
				scopeValidator.validateScope(normalized.scope());
			}
		} catch (final InvalidParameterException ex) {
			if (Utilities.isEmpty(ex.getOrigin())) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			throw ex;
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationEncryptionKeyRegistrationRequestDTO validateAndNormalizeRegisterEncryptionKeyRequest(final AuthorizationEncryptionKeyRegistrationRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeRegisterEncryptionKeyRequest started...");

		validateRegisterEncryptionKeyRequest(dto, origin);
		final AuthorizationEncryptionKeyRegistrationRequestDTO normalized = normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(dto);

		if (!(normalized.algorithm().equalsIgnoreCase(SecretCryptographer.AES_ECB_ALGORITHM) || normalized.algorithm().equalsIgnoreCase(SecretCryptographer.AES_CBC_ALGORITHM_IV_BASED))) {
			throw new InvalidParameterException("Unsupported algorithm", origin);
		}

		if (normalized.key().getBytes().length < SecretCryptographer.AES_KEY_MIN_SIZE) {
			throw new InvalidParameterException("Key must be minimum " + SecretCryptographer.AES_KEY_MIN_SIZE + " bytes long");
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
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(systemName)) {
			throw new InvalidParameterException("System name is empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateGenerateRequest(final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		logger.debug("validateGenerateRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.tokenVariant())) {
			throw new InvalidParameterException("Token variant is missing", origin);
		}

		final String tokenVariant = dto.tokenVariant().trim().toUpperCase();
		if (!Utilities.isEnumValue(tokenVariant, ServiceInterfacePolicy.class)) {
			throw new InvalidParameterException("Token variant is invalid: " + dto.tokenVariant(), origin);
		}

		if (Utilities.isEmpty(dto.provider())) {
			throw new InvalidParameterException("Provider system is missing", origin);
		}

		if (!Utilities.isEmpty(dto.targetType())) {
			final String targetTypeName = dto.targetType().trim().toUpperCase();
			if (!Utilities.isEnumValue(targetTypeName, AuthorizationTargetType.class)) {
				throw new InvalidParameterException("Target type is invalid: " + dto.targetType(), origin);
			}
		}

		if (Utilities.isEmpty(dto.target())) {
			throw new InvalidParameterException("Target is missing", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateToken(final String token, final String origin) {
		logger.debug("validateToken started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(token)) {
			throw new InvalidParameterException("Token is empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRegisterEncryptionKeyRequest(final AuthorizationEncryptionKeyRegistrationRequestDTO dto, final String origin) {
		logger.debug("validateRegisterEncryptionKey started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.key())) {
			throw new InvalidParameterException("Key is empty", origin);
		}
	}
}