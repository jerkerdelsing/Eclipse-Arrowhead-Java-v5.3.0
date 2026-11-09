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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.jpa.LogEntity;
import eu.arrowhead.common.jpa.LogEntityRepository;
import jakarta.persistence.QueryTimeoutException;

@ExtendWith(MockitoExtension.class)
public class LogDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private LogDbService dbService;

	@Mock
	private LogEntityRepository<? extends LogEntity> logRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetLogEntriesNoLoggerStrException() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final List<LogLevel> allLevels = Arrays.asList(LogLevel.values());

		when(logRepository.findAllByLogLevelInAndEntryDateBetween(eq(allLevels), any(ZonedDateTime.class), any(ZonedDateTime.class), eq(pageRequest))).thenThrow(QueryTimeoutException.class);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> dbService.getLogEntries(pageRequest, null, null, null, null));

		verify(logRepository).findAllByLogLevelInAndEntryDateBetween(eq(allLevels), any(ZonedDateTime.class), any(ZonedDateTime.class), eq(pageRequest));

		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetLogEntriesNoLoggerStrOk() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final List<LogLevel> allLevels = Arrays.asList(LogLevel.values());
		final List<LogEntity> results = List.of(new LogEntity(
				"id",
				Utilities.parseUTCStringToZonedDateTime("2025-07-21T12:41:12Z"),
				"loggername",
				LogLevel.INFO,
				"something important",
				null));

		when(logRepository.findAllByLogLevelInAndEntryDateBetween(eq(allLevels), any(ZonedDateTime.class), any(ZonedDateTime.class), eq(pageRequest)))
				.thenReturn(new PageImpl<>(results, pageRequest, 1));

		Page<LogEntity> resultPage = dbService.getLogEntries(pageRequest, null, null, null, null);

		verify(logRepository).findAllByLogLevelInAndEntryDateBetween(eq(allLevels), any(ZonedDateTime.class), any(ZonedDateTime.class), eq(pageRequest));

		assertEquals(1L, resultPage.getTotalElements());
		assertEquals(results.get(0), resultPage.getContent().get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetLogEntriesWithLoggerStrException() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final List<LogLevel> levels = List.of(LogLevel.INFO);
		final ZonedDateTime from = Utilities.parseUTCStringToZonedDateTime("2025-07-21T10:00:00Z");
		final ZonedDateTime to = Utilities.parseUTCStringToZonedDateTime("2025-07-21T14:00:00Z");

		when(logRepository.findAllByLogLevelInAndEntryDateBetweenAndLoggerContainsIgnoreCase(levels, from, to, "logger", pageRequest)).thenThrow(QueryTimeoutException.class);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> dbService.getLogEntries(pageRequest, levels, from, to, "logger"));

		verify(logRepository).findAllByLogLevelInAndEntryDateBetweenAndLoggerContainsIgnoreCase(levels, from, to, "logger", pageRequest);

		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetLogEntriesWithLoggerStrOk() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final List<LogLevel> levels = List.of(LogLevel.INFO);
		final ZonedDateTime from = Utilities.parseUTCStringToZonedDateTime("2025-07-21T10:00:00Z");
		final ZonedDateTime to = Utilities.parseUTCStringToZonedDateTime("2025-07-21T14:00:00Z");
		final List<LogEntity> results = List.of(new LogEntity(
				"id",
				Utilities.parseUTCStringToZonedDateTime("2025-07-21T12:41:12Z"),
				"loggername",
				LogLevel.INFO,
				"something important",
				null));

		when(logRepository.findAllByLogLevelInAndEntryDateBetweenAndLoggerContainsIgnoreCase(levels, from, to, "logger", pageRequest)).thenReturn(new PageImpl<>(results, pageRequest, 1));

		Page<LogEntity> resultPage = dbService.getLogEntries(pageRequest, levels, from, to, "logger");

		verify(logRepository).findAllByLogLevelInAndEntryDateBetweenAndLoggerContainsIgnoreCase(levels, from, to, "logger", pageRequest);

		assertEquals(1L, resultPage.getTotalElements());
		assertEquals(results.get(0), resultPage.getContent().get(0));
	}
}