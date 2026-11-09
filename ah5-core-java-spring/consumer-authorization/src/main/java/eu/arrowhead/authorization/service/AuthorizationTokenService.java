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
package eu.arrowhead.authorization.service;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.authorization.service.engine.TokenEngine;
import eu.arrowhead.authorization.service.model.EncryptionKeyModel;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.validation.AuthorizationTokenValidation;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenVerifyResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import jakarta.annotation.Resource;

@Service
public class AuthorizationTokenService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationSystemInfo sysInfo;

	@Autowired
	private AuthorizationPolicyEngine policyEngine;

	@Autowired
	private TokenEngine tokenEngine;

	@Autowired
	private SecretCryptographer secretCryptographer;

	@Autowired
	private EncryptionKeyDbService encryptionKeyDbService;

	@Autowired
	private AuthorizationTokenValidation validator;

	@Autowired
	private DTOConverter dtoConverter;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationResponseDTO generate(final String requesterSystem, final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		logger.debug("generate started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requesterSystem, origin);
		final AuthorizationTokenGenerationRequestDTO normalizedDTO = validator.validateAndNormalizeGenerateRequest(dto, origin);

		// Check permission
		final boolean isAuthorized = policyEngine.isAccessGranted(new NormalizedVerifyRequest(
				normalizedDTO.provider(),
				normalizedRequester,
				Defaults.DEFAULT_CLOUD,
				AuthorizationTargetType.valueOf(normalizedDTO.targetType()),
				normalizedDTO.target(),
				normalizedDTO.scope()));

		if (!isAuthorized) {
			throw new ForbiddenException("Requester has no permisson to the service instance and/or operation", origin);
		}

		// Generate token
		final ServiceInterfacePolicy tokenType = ServiceInterfacePolicy.valueOf(normalizedDTO.tokenVariant());
		final TokenModel tokenResult = tokenEngine.produce(
				normalizedRequester,
				normalizedRequester,
				tokenType,
				normalizedDTO.provider(),
				AuthorizationTargetType.valueOf(normalizedDTO.targetType()),
				normalizedDTO.target(),
				Utilities.isEmpty(normalizedDTO.scope()) ? null : normalizedDTO.scope(),
				origin);

		// Encrypt token if required
		final AuthorizationTokenType authorizationTokenType = AuthorizationTokenType.fromServiceInterfacePolicy(ServiceInterfacePolicy.valueOf(normalizedDTO.tokenVariant()));
		if (authorizationTokenType == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
			tokenEngine.encryptTokenIfNeeded(tokenResult, origin);
		}

		return dtoConverter.convertTokenModelToResponse(tokenResult);
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenVerifyResponseDTO verify(final String requesterSystem, final String token, final String origin) {
		logger.debug("verify started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requesterSystem, origin);
		final String normalizedToken = validator.validateAndNormalizeToken(token, origin);
		final Pair<Boolean, Optional<TokenModel>> result = tokenEngine.verify(normalizedRequester, normalizedToken, origin);

		return dtoConverter.convertTokenVerificationResultToResponse(result);
	}

	//-------------------------------------------------------------------------------------------------
	public String getPublicKey(final String origin) {
		logger.debug("registerEncryptionKey started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final Optional<Object> pubKeyOpt = Optional.ofNullable(arrowheadContext.get(Constants.SERVER_PUBLIC_KEY));
		if (pubKeyOpt.isEmpty()) {
			throw new DataNotFoundException("Public key is not available", origin);
		}

		final PublicKey pubKey = (PublicKey) pubKeyOpt.get();

		return Base64.getEncoder().encodeToString(pubKey.getEncoded());
	}

	//-------------------------------------------------------------------------------------------------
	public String registerEncryptionKey(final String requesterSystem, final AuthorizationEncryptionKeyRegistrationRequestDTO dto, final String origin) {
		logger.debug("registerEncryptionKey started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requesterSystem, origin);
		final AuthorizationEncryptionKeyRegistrationRequestDTO normalizedDTO = validator.validateAndNormalizeRegisterEncryptionKeyRequest(dto, origin);

		String externalAuxiliary = "";
		if (normalizedDTO.algorithm().equalsIgnoreCase(SecretCryptographer.AES_CBC_ALGORITHM_IV_BASED)) {
			externalAuxiliary = secretCryptographer.generateInitializationVectorBase64();
		}

		Pair<String, String> encryptedToSave = null; // encrypted key and internal IV in that order
		try {
			encryptedToSave = secretCryptographer.encrypt_AES_CBC_PKCS5P_IV(normalizedDTO.key(), sysInfo.getSecretCryptographerKey());
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Secret encryption failed", origin);
		}

		try {
			encryptionKeyDbService.save(new EncryptionKeyModel(
					normalizedRequester,
					normalizedDTO.key(),
					encryptedToSave.getFirst(), // encrypted key
					normalizedDTO.algorithm(),
					encryptedToSave.getSecond(), // internal IV
					externalAuxiliary));
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}

		return externalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean unregisterEncryptionKey(final String requesterSystem, final String origin) {
		logger.debug("unregisterEncryptionKey started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requesterSystem, origin);

		return encryptionKeyDbService.delete(normalizedRequester);
	}
}