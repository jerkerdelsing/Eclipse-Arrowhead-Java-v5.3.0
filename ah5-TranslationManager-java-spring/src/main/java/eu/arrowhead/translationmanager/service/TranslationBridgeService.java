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
package eu.arrowhead.translationmanager.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.TranslationDiscoveryRequestDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationNegotiationRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationNegotiationRequestDTO;
import eu.arrowhead.translationmanager.service.engine.TranslatorBridgeEngine;
import eu.arrowhead.translationmanager.service.validation.TranslationBridgeValidation;

@Service
public class TranslationBridgeService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private TranslationBridgeValidation validator;

	@Autowired
	private TranslationManagerSystemInfo sysInfo;

	@Autowired
	private TranslatorBridgeEngine engine;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TranslationDiscoveryResponseDTO discoveryOperation(final String requester, final TranslationDiscoveryRequestDTO dto, final String origin) {
		logger.debug("discoveryOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requester, origin);
		final NormalizedTranslationDiscoveryRequestDTO normalized = validator.validateAndNormalizeDiscoveryRequest(normalizedRequester, dto, origin);
		final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false, // already checked in a filter (or blacklist is not used)
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, sysInfo.isBlacklistEnabled(),
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, sysInfo.isAuthorizationEnabled(),
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, sysInfo.isBlacklistEnabled(),
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, sysInfo.isAuthorizationEnabled());

		return engine.doDiscovery(normalized, discoveryFlags, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public TranslationNegotiationResponseDTO negotiationOperation(final String requester, final TranslationNegotiationRequestDTO dto, final String origin) {
		logger.debug("negotiationOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		NormalizedTranslationNegotiationRequestDTO normalized = validator.validateAndNormalizeNegotiationRequest(dto, origin);

		if (normalized.bridgeId() == null) {
			// discovery operation is not completed

			final TranslationDiscoveryResponseDTO discoveryResult = discoveryOperation(
					requester,
					new TranslationDiscoveryRequestDTO(
							List.of(dto.target()),
							normalized.operation(),
							List.of(normalized.interfaceTemplateName()),
							normalized.inputDataModelId(),
							normalized.outputDataModelId()),
					origin);

			if (Utilities.isEmpty(discoveryResult.bridgeId())) {
				// translation is not possible
				return new TranslationNegotiationResponseDTO(null, null, null, null);
			}

			normalized = new NormalizedTranslationNegotiationRequestDTO(
					UUID.fromString(discoveryResult.bridgeId()),
					normalized.target(),
					normalized.operation(),
					normalized.interfaceTemplateName(),
					normalized.inputDataModelId(),
					normalized.outputDataModelId());
		}

		return engine.doNegotiation(normalized.bridgeId(), normalized.target().instanceId(), origin);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean abortOperation(final String requester, final String bridgeId, final String origin) {
		logger.debug("abortOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requester, origin);
		final UUID uuid = validator.validateAndNormalizeBridgeId(bridgeId, origin);

		final Map<String, Boolean> result = engine.doAbort(List.of(uuid), normalizedRequester, origin);

		return result.get(uuid.toString());
	}
}