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
package eu.arrowhead.translationmanager.service.validation;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.TranslationReportRequestDTO;
import eu.arrowhead.dto.enums.TranslationBridgeEventState;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationReportRequestDTO;

@Service
public class TranslationReportValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private SystemNameValidator systemNameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRequester(final String requester, final String origin) {
		logger.debug("validateAndNormalizeRequester started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateRequester(requester, origin);
		final String normalized = systemNameNormalizer.normalize(requester);

		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedTranslationReportRequestDTO validateAndNormalizeReport(final String requester, final TranslationReportRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeReport started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateReport(dto, origin);
		return normalizeReport(requester, dto);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateRequester(final String requester, final String origin) {
		logger.debug("validateRequester started...");

		if (Utilities.isEmpty(requester)) {
			throw new InvalidParameterException("Requester name is missing or empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateReport(final TranslationReportRequestDTO dto, final String origin) {
		logger.debug("validateReport started...");

		if (dto == null) {
			throw new InvalidParameterException("Request is missing", origin);
		}

		if (Utilities.isEmpty(dto.bridgeId())) {
			throw new InvalidParameterException("Bridge identifier is missing", origin);
		}

		if (!Utilities.isUUID(dto.bridgeId().trim())) {
			throw new InvalidParameterException("Bridge identifier is invalid: " + dto.bridgeId(), origin);
		}

		if (Utilities.isEmpty(dto.timestamp())) {
			throw new InvalidParameterException("Timestamp is missing", origin);
		}

		try {
			Utilities.parseUTCStringToZonedDateTime(dto.timestamp().trim());
		} catch (final DateTimeParseException __) {
			throw new InvalidParameterException("Timestamp is invalid: " + dto.timestamp(), origin);
		}

		if (Utilities.isEmpty(dto.state())) {
			throw new InvalidParameterException("State is missing", origin);
		}

		if (!Utilities.isEnumValue(dto.state().trim().toUpperCase(), TranslationBridgeEventState.class)) {
			throw new InvalidParameterException("State is invalid: " + dto.state(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	private NormalizedTranslationReportRequestDTO normalizeReport(final String requester, final TranslationReportRequestDTO dto) {
		logger.debug("normalizeReport started...");

		final ZonedDateTime now = Utilities.utcNow();
		ZonedDateTime timestamp = Utilities.parseUTCStringToZonedDateTime(dto.timestamp().trim());
		if (timestamp.isAfter(now)) {
			// maybe clocks out of sync, so we use 'now' as timestamp
			timestamp = now;
		}
		return new NormalizedTranslationReportRequestDTO(
				requester,
				UUID.fromString(dto.bridgeId().trim()),
				timestamp,
				TranslationBridgeEventState.valueOf(dto.state().trim().toUpperCase()),
				Utilities.isEmpty(dto.errorMessage()) ? null : dto.errorMessage().trim());
	}
}