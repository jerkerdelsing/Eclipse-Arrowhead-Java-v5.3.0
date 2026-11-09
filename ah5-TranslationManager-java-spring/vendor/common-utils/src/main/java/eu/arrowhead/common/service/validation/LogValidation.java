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
package eu.arrowhead.common.service.validation;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.jpa.LogEntity;
import eu.arrowhead.dto.LogRequestDTO;

@Service
public class LogValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private PageValidator pageValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateLogRequest(final LogRequestDTO dto, final String origin) {
		logger.debug("validateLogRequest started...");

		if (dto != null) {
			pageValidator.validatePageParameter(dto.pagination(), LogEntity.SORTABLE_FIELDS_BY, origin);
			final ZonedDateTime from = validateAndParseDateTime(dto.from(), "from", origin);
			final ZonedDateTime to = validateAndParseDateTime(dto.to(), "to", origin);

			if (from != null && to != null && to.isBefore(from)) {
				throw new InvalidParameterException("Invalid time interval", origin);
			}

			validateSeverity(dto.severity(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ZonedDateTime validateAndParseDateTime(final String dateTime, final String fieldName, final String origin) {
		logger.debug("validateAndParseDateTime started...");

		try {
			return Utilities.parseUTCStringToZonedDateTime(dateTime);
		} catch (final DateTimeParseException ex) {
			throw new InvalidParameterException("Invalid date time in field: " + fieldName + ". Please use a UTC date time in ISO-8601 format", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateSeverity(final String severity, final String origin) {
		logger.debug("validateSeverity started...");

		if (!Utilities.isEmpty(severity)) {
			try {
				LogLevel.valueOf(severity.trim().toUpperCase());
			} catch (final IllegalArgumentException ex) {
				throw new InvalidParameterException("Invalid severity is specified. Allowed values are: " + LogLevel.values(), origin);
			}
		}
	}
}