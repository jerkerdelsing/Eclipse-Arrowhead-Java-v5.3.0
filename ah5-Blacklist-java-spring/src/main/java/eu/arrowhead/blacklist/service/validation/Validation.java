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
package eu.arrowhead.blacklist.service.validation;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.blacklist.BlacklistConstants;
import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.blacklist.service.dto.NormalizedBlacklistQueryRequestDTO;
import eu.arrowhead.blacklist.service.normalization.Normalization;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.BlacklistCreateListRequestDTO;
import eu.arrowhead.dto.BlacklistCreateRequestDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;
import eu.arrowhead.dto.enums.Mode;

@Service
public class Validation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SystemNameNormalizer systemNameNormalizer; // for checking duplications

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private PageValidator pageValidator;

	@Autowired
	private Normalization normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public BlacklistCreateListRequestDTO validateAndNormalizeBlacklistCreateListRequestDTO(final BlacklistCreateListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeBlacklistCreateListRequestDTO started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateBlacklistCreateListRequestDTO(dto, origin);
		final BlacklistCreateListRequestDTO normalized = normalizer.normalizeBlacklistCreateListRequestDTO(dto);

		try {
			normalized.entities().forEach(e -> systemNameValidator.validateSystemName(e.systemName()));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedBlacklistQueryRequestDTO validateAndNormalizeBlacklistQueryRequestDTO(final BlacklistQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeBlacklistQueryRequestDTO started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateBlacklistQueryRequestDTO(dto, origin);
		final NormalizedBlacklistQueryRequestDTO normalized = normalizer.normalizeBlacklistQueryRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.systemNames())) {
				normalized.systemNames().forEach(n -> systemNameValidator.validateSystemName(n));
			}

			if (!Utilities.isEmpty(normalized.issuers())) {
				normalized.issuers().forEach(n -> systemNameValidator.validateSystemName(n));
			}

			if (!Utilities.isEmpty(normalized.revokers())) {
				normalized.revokers().forEach(n -> systemNameValidator.validateSystemName(n));
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeSystemNameList(final List<String> names, final String origin) {
		logger.debug("validateAndNormalizeSystemNameList started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateSystemNameList(names, origin);
		final List<String> normalizedNames = normalizer.normalizeSystemNames(names);

		try {
			normalizedNames.forEach(n -> systemNameValidator.validateSystemName(n));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalizedNames;
	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeSystemName(final String name, final String origin) {
		logger.debug("validateAndNormalizeSystemName");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedName = normalizer.normalizeSystemName(name);

		try {
			systemNameValidator.validateSystemName(normalizedName);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalizedName;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateBlacklistCreateListRequestDTO(final BlacklistCreateListRequestDTO dto, final String origin) {
		logger.debug("validateBlacklistCreateListRequestDTO started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.entities())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> names = new HashSet<>();
		for (final BlacklistCreateRequestDTO entity : dto.entities()) {
			if (entity == null) {
				throw new InvalidParameterException("Entity list contains null element", origin);
			}

			// system name
			if (Utilities.isEmpty(entity.systemName())) {
				throw new InvalidParameterException("System name is empty", origin);
			}

			final String normalizedSystemName = systemNameNormalizer.normalize(entity.systemName());
			if (names.contains(normalizedSystemName)) {
				throw new InvalidParameterException("Duplicated system name: " + normalizedSystemName, origin);
			}

			names.add(normalizedSystemName);

			// expires at
			if (!Utilities.isEmpty(entity.expiresAt())) {
				try {
					final ZonedDateTime expiresAt = Utilities.parseUTCStringToZonedDateTime(entity.expiresAt());
					if (Utilities.utcNow().isAfter(expiresAt)) {
						throw new InvalidParameterException("Expiration time is in the past", origin);
					}
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Expiration time has an invalid time format, UTC string expected (example: 2024-10-11T14:30:00Z)", origin);
				}
			}

			// reason
			if (Utilities.isEmpty(entity.reason())) {
				throw new InvalidParameterException("You cannot blacklist a system without specifying the reason", origin);
			}

			if (entity.reason().trim().length() > BlacklistConstants.REASON_LENGTH) {
				throw new InvalidParameterException("Reason is too long", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateBlacklistQueryRequestDTO(final BlacklistQueryRequestDTO dto, final String origin) {
		logger.debug("validateBlacklistQueryRequestDTO started...");

		if (dto != null) {
			// pagination
			pageValidator.validatePageParameter(dto.pagination(), Entry.SORTABLE_FIELDS_BY, origin);

			// system names
			if (!Utilities.isEmpty(dto.systemNames()) && Utilities.containsNullOrEmpty(dto.systemNames())) {
				throw new InvalidParameterException("System name list contains null or empty element", origin);
			}

			// mode
			if (dto.mode() != null) {
				try {
					Mode.valueOf(dto.mode().trim().toUpperCase());
				} catch (final IllegalArgumentException ex) {
					// throwing exception containing the possible values of mode
					final List<String> possibleValues = Stream
							.of(Mode.values())
							.map(m -> m.name())
							.toList();

					throw new InvalidParameterException("Mode is invalid. Possible values: " + String.join(", ", possibleValues), origin);
				}
			}

			// issuers
			if (!Utilities.isEmpty(dto.issuers()) && Utilities.containsNullOrEmpty(dto.issuers())) {
				throw new InvalidParameterException("Issuer name list contains null or empty element", origin);
			}

			// revokers
			if (!Utilities.isEmpty(dto.revokers()) && Utilities.containsNullOrEmpty(dto.revokers())) {
				throw new InvalidParameterException("Revoker name list contains null or empty element", origin);
			}

			// reason -> no need to validate

			// alivesAt
			if (!Utilities.isEmpty(dto.alivesAt())) {
				try {
					Utilities.parseUTCStringToZonedDateTime(dto.alivesAt());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Alives at date has an invalid time format, UTC string expected (example: 2024-10-11T14:30:00Z)", origin);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateSystemNameList(final List<String> names, final String origin) {
		logger.debug("validateSystemNameList started...");

		if (Utilities.isEmpty(names)) {
			throw new InvalidParameterException("System name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(names)) {
			throw new InvalidParameterException("System name list contains null or empty element", origin);

		}
	}
}