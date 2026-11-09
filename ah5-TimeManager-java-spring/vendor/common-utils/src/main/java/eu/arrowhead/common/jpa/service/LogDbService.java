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
package eu.arrowhead.common.jpa.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.jpa.LogEntity;
import eu.arrowhead.common.jpa.LogEntityRepository;

@Service
public class LogDbService {

	//=================================================================================================
	// members

	private static final List<LogLevel> ALL_LOG_LEVELS = Arrays.asList(LogLevel.values());
	private static final ZonedDateTime START_OF_TIMES = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 1, ZoneId.systemDefault());

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private LogEntityRepository<? extends LogEntity> logRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Page<LogEntity> getLogEntries(final PageRequest pageRequest, final List<LogLevel> levels, final ZonedDateTime from, final ZonedDateTime to, final String loggerStr) {
		logger.debug("getLogEntries started...");

		final List<LogLevel> _levels = Utilities.isEmpty(levels) ? ALL_LOG_LEVELS : levels;
		final ZonedDateTime _from = from == null ? START_OF_TIMES : from;
		final ZonedDateTime _to = to == null ? Utilities.utcNow() : to;

		try {
			if (Utilities.isEmpty(loggerStr)) {
				return logRepository.findAllByLogLevelInAndEntryDateBetween(_levels, _from, _to, pageRequest);
			} else {
				return logRepository.findAllByLogLevelInAndEntryDateBetweenAndLoggerContainsIgnoreCase(_levels, _from, _to, loggerStr, pageRequest);
			}
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}