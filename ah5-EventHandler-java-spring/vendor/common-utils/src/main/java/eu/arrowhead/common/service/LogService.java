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
package eu.arrowhead.common.service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.jpa.LogEntity;
import eu.arrowhead.common.jpa.service.LogDbService;
import eu.arrowhead.common.service.validation.LogValidation;
import eu.arrowhead.dto.LogEntryDTO;
import eu.arrowhead.dto.LogEntryListResponseDTO;
import eu.arrowhead.dto.LogRequestDTO;

@Service
public class LogService {

	//=================================================================================================
	// members

	private static final List<LogLevel> logLevelsInOrder = List.of(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL, LogLevel.OFF);

	@Autowired
	private LogValidation validator;

	@Autowired
	private PageService pageService;

	@Autowired
	private LogDbService dbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public LogEntryListResponseDTO getLogEntries(final LogRequestDTO dto, final String origin) {
		logger.debug("getLogEntries started...");

		validator.validateLogRequest(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(dto == null ? null : dto.pagination(), Direction.DESC, LogEntity.SORTABLE_FIELDS_BY, LogEntity.DEFAULT_SORT_FIELD, origin);
		final ZonedDateTime from = Utilities.parseUTCStringToZonedDateTime(dto == null ? null : dto.from());
		final ZonedDateTime to = Utilities.parseUTCStringToZonedDateTime(dto == null ? null : dto.to());
		final List<LogLevel> logLevels = getLogLevels(dto == null ? null : dto.severity());
		final String logger = dto == null || Utilities.isEmpty(dto.logger()) ? null : dto.logger().trim();

		try {
			final Page<LogEntity> page = dbService.getLogEntries(pageRequest, logLevels, from, to, logger);

			return convertPageToResponse(page);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<LogLevel> getLogLevels(final String maxLevel) {
		logger.debug("getLogLevels started...");

		if (Utilities.isEmpty(maxLevel)) {
			return null;
		}

		final LogLevel maxLogLevel = LogLevel.valueOf(maxLevel.toUpperCase().trim());
		final int index = logLevelsInOrder.indexOf(maxLogLevel); // can't be -1 at this point

		return logLevelsInOrder.subList(index, logLevelsInOrder.size());
	}

	//-------------------------------------------------------------------------------------------------
	private LogEntryListResponseDTO convertPageToResponse(final Page<LogEntity> page) {
		logger.debug("convertPageToResponse started...");

		final List<LogEntryDTO> list = page.stream()
				.map(e -> convertLogEntityToDTO(e))
				.collect(Collectors.toList());

		return new LogEntryListResponseDTO(list, page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	private LogEntryDTO convertLogEntityToDTO(final LogEntity entity) {
		logger.debug("convertLogEntityToDTO started...");

		return new LogEntryDTO(entity.getLogId(),
				Utilities.convertZonedDateTimeToUTCString(entity.getEntryDate()),
				entity.getLogger(),
				entity.getLogLevel().name(),
				entity.getMessage(),
				entity.getException());
	}
}