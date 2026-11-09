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
package eu.arrowhead.authorization.service.normalization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;
import eu.arrowhead.dto.DTODefaults;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@Service
public class AuthorizationTokenNormalizer {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Autowired
	private EventTypeNameNormalizer eventTypeNameNormalizer;

	@Autowired
	private AuthorizationScopeNormalizer scopeNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String normalizeSystemName(final String name) {
		logger.debug("normalizeSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(name), "System name is empty");

		return systemNameNormalizer.normalize(name);
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeToken(final String token) {
		logger.debug("normalizeToken started...");
		Assert.isTrue(!Utilities.isEmpty(token), "Token is empty");

		return token.trim();
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationRequestDTO normalizeAuthorizationTokenGenerationRequestDTO(final AuthorizationTokenGenerationRequestDTO dto) {
		logger.debug("normalizeAuthorizationTokenGenerationRequestDTO started...");
		Assert.notNull(dto, "AuthorizationTokenGenerationRequestDTO is null");

		final String normalizedTargetType = Utilities.isEmpty(dto.targetType()) ? AuthorizationTargetType.SERVICE_DEF.name() : dto.targetType().toUpperCase().trim();
		final String normalizedTarget = AuthorizationTargetType.SERVICE_DEF.name().equals(normalizedTargetType)
				? serviceDefNameNormalizer.normalize(dto.target())
				: eventTypeNameNormalizer.normalize(dto.target());

		return new AuthorizationTokenGenerationRequestDTO(
				dto.tokenVariant().toUpperCase().trim(),
				systemNameNormalizer.normalize(dto.provider()),
				normalizedTargetType,
				normalizedTarget,
				Utilities.isEmpty(dto.scope()) ? null : scopeNormalizer.normalize(dto.scope()));
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationEncryptionKeyRegistrationRequestDTO normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(final AuthorizationEncryptionKeyRegistrationRequestDTO dto) {
		logger.debug("normalizeAuthorizationEncryptionKeyRegistrationRequestDTO started...");
		Assert.notNull(dto, "AuthorizationEncryptionKeyRegistrationRequestDTO is null");

		return new AuthorizationEncryptionKeyRegistrationRequestDTO(
				dto.key(),
				Utilities.isEmpty(dto.algorithm()) ? SecretCryptographer.DEFAULT_ENCRYPTION_ALGORITHM : dto.algorithm().trim());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationMgmtListRequestDTO normalizeAuthorizationTokenGenerationMgmtListRequestDTO(final AuthorizationTokenGenerationMgmtListRequestDTO dto) {
		logger.debug("normalizeAuthorizationTokenGenerationMgmtListRequestDTO started...");
		Assert.notNull(dto, "AuthorizationTokenGenerationMgmtListRequestDTO is null");
		Assert.notNull(dto.list(), "AuthorizationTokenGenerationMgmtListRequestDTO.list is null");

		return new AuthorizationTokenGenerationMgmtListRequestDTO(
				dto.list()
						.stream()
						.map((item) -> {
							final String normalizedTargetType = Utilities.isEmpty(item.targetType()) ? AuthorizationTargetType.SERVICE_DEF.name() : item.targetType().toUpperCase().trim();
							final String normalizedTarget = AuthorizationTargetType.SERVICE_DEF.name().equals(normalizedTargetType)
									? serviceDefNameNormalizer.normalize(item.target())
									: eventTypeNameNormalizer.normalize(item.target());

							return new AuthorizationTokenGenerationMgmtRequestDTO(
									item.tokenVariant().trim().toUpperCase(),
									normalizedTargetType,
									Utilities.isEmpty(item.consumerCloud()) ? DTODefaults.DEFAULT_CLOUD : cloudIdentifierNormalizer.normalize(item.consumerCloud()),
									systemNameNormalizer.normalize(item.consumer()),
									systemNameNormalizer.normalize(item.provider()),
									normalizedTarget,
									Utilities.isEmpty(item.scope()) ? null : scopeNormalizer.normalize(item.scope()),
									Utilities.isEmpty(item.expiresAt()) ? null : item.expiresAt().trim(),
									item.usageLimit());
						})
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenQueryRequestDTO normalizeAuthorizationTokenQueryRequestDTO(final AuthorizationTokenQueryRequestDTO dto) {
		logger.debug("normalizeAuthorizationTokenQueryRequestDTO started...");
		Assert.notNull(dto, "AuthorizationTokenQueryRequestDTO is null");

		final String normalizedTargetTypeStr = Utilities.isEmpty(dto.targetType()) ? null : dto.targetType().trim().toUpperCase();
		String normalizedTarget = null;
		if (!Utilities.isEmpty(dto.target())) {
			normalizedTarget = normalizedTargetTypeStr == null || AuthorizationTargetType.SERVICE_DEF.name().equals(normalizedTargetTypeStr)
					? serviceDefNameNormalizer.normalize(dto.target())
					: eventTypeNameNormalizer.normalize(dto.target());
		}

		return new AuthorizationTokenQueryRequestDTO(
				dto.pagination(),
				Utilities.isEmpty(dto.requester()) ? null : systemNameNormalizer.normalize(dto.requester()),
				Utilities.isEmpty(dto.tokenType()) ? null : dto.tokenType().trim().toUpperCase(),
				Utilities.isEmpty(dto.consumerCloud()) ? null : cloudIdentifierNormalizer.normalize(dto.consumerCloud()),
				Utilities.isEmpty(dto.consumer()) ? null : systemNameNormalizer.normalize(dto.consumer()),
				Utilities.isEmpty(dto.provider()) ? null : systemNameNormalizer.normalize(dto.provider()),
				normalizedTargetTypeStr,
				normalizedTarget);
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto) {
		logger.debug("normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO started...");
		Assert.notNull(dto, "AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO is null");
		Assert.notNull(dto.list(), "AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO.list is null");

		return new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(
				dto.list()
						.stream()
						.map((item) -> new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO(
								systemNameNormalizer.normalize(item.systemName()),
								item.key(),
								Utilities.isEmpty(item.algorithm()) ? SecretCryptographer.DEFAULT_ENCRYPTION_ALGORITHM : item.algorithm().trim()))
						.toList());
	}
}