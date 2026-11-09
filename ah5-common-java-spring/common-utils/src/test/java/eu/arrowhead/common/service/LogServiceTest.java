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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
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
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.jpa.LogEntity;
import eu.arrowhead.common.jpa.service.LogDbService;
import eu.arrowhead.common.service.validation.LogValidation;
import eu.arrowhead.dto.LogEntryDTO;
import eu.arrowhead.dto.LogEntryListResponseDTO;
import eu.arrowhead.dto.LogRequestDTO;
import eu.arrowhead.dto.PageDTO;
import jakarta.persistence.Entity;

@ExtendWith(MockitoExtension.class)
public class LogServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private LogService service;

	@Mock
	private LogValidation validator;

	@Mock
	private PageService pageService;

	@Mock
	private LogDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:MagicNumber", "unchecked" })
	@Test
	public void testGetLogEntriesExceptionForwarding() {
		when(pageService.getPageRequest(null, Direction.DESC, LogEntity.SORTABLE_FIELDS_BY, LogEntity.DEFAULT_SORT_FIELD, "test"))
				.thenReturn(PageRequest.of(0, 10, Direction.DESC, LogEntity.DEFAULT_SORT_FIELD));
		when(dbService.getLogEntries(any(PageRequest.class), nullable(List.class), nullable(ZonedDateTime.class), nullable(ZonedDateTime.class), nullable(String.class)))
				.thenThrow(new InternalServerError("Database operation error"));

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> service.getLogEntries(null, "test"));

		assertEquals("Database operation error", ex.getMessage());
		assertEquals("test", ((InternalServerError) ex).getOrigin());

		verify(pageService).getPageRequest(nullable(PageDTO.class), any(Direction.class), anyList(), anyString(), anyString());
		verify(dbService).getLogEntries(any(PageRequest.class), nullable(List.class), nullable(ZonedDateTime.class), nullable(ZonedDateTime.class), nullable(String.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetLogEntries() {
		final PageDTO page = new PageDTO(0, 10, "DESC", "entryDate");
		final LogRequestDTO input = new LogRequestDTO(
				page,
				"2025-06-27T10:00:00Z",
				"2025-06-27T12:00:00Z",
				"WARN",
				"testLogger");

		final PageRequest pageRequest = PageRequest.of(0, 10, Direction.DESC, "entryDate");
		final Page<LogEntity> dbResult = new PageImpl<>(
				List.of(new Log(
						"id1",
						Utilities.parseUTCStringToZonedDateTime("2025-06-27T10:02:01Z"),
						"testLogger",
						LogLevel.WARN,
						"test message",
						null)),
				pageRequest,
				1);

		final LogEntryDTO convertedResult = new LogEntryDTO(
				"id1",
				"2025-06-27T10:02:01Z",
				"testLogger",
				"WARN",
				"test message",
				null);

		when(pageService.getPageRequest(page, Direction.DESC, LogEntity.SORTABLE_FIELDS_BY, LogEntity.DEFAULT_SORT_FIELD, "test"))
				.thenReturn(pageRequest);
		when(dbService.getLogEntries(any(PageRequest.class), anyList(), any(ZonedDateTime.class), any(ZonedDateTime.class), anyString()))
				.thenReturn(dbResult);

		final LogEntryListResponseDTO result = service.getLogEntries(input, "test");

		verify(pageService).getPageRequest(any(PageDTO.class), any(Direction.class), anyList(), anyString(), anyString());
		verify(dbService).getLogEntries(any(PageRequest.class), anyList(), any(ZonedDateTime.class), any(ZonedDateTime.class), anyString());

		assertEquals(1, result.entries().size());
		assertEquals(convertedResult, result.entries().get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetLogEntries2() {
		final PageDTO page = new PageDTO(0, 10, "DESC", "entryDate");
		final LogRequestDTO input = new LogRequestDTO(
				page,
				"2025-06-27T10:00:00Z",
				"2025-06-27T12:00:00Z",
				"WARN",
				"");

		final PageRequest pageRequest = PageRequest.of(0, 10, Direction.DESC, "entryDate");
		final Page<LogEntity> dbResult = new PageImpl<>(
				List.of(new Log(
						"id1",
						Utilities.parseUTCStringToZonedDateTime("2025-06-27T10:02:01Z"),
						"testLogger",
						LogLevel.WARN,
						"test message",
						null)),
				pageRequest,
				1);

		final LogEntryDTO convertedResult = new LogEntryDTO(
				"id1",
				"2025-06-27T10:02:01Z",
				"testLogger",
				"WARN",
				"test message",
				null);

		when(pageService.getPageRequest(page, Direction.DESC, LogEntity.SORTABLE_FIELDS_BY, LogEntity.DEFAULT_SORT_FIELD, "test"))
				.thenReturn(pageRequest);
		when(dbService.getLogEntries(any(PageRequest.class), anyList(), any(ZonedDateTime.class), any(ZonedDateTime.class), nullable(String.class)))
				.thenReturn(dbResult);

		final LogEntryListResponseDTO result = service.getLogEntries(input, "test");

		verify(pageService).getPageRequest(any(PageDTO.class), any(Direction.class), anyList(), anyString(), anyString());
		verify(dbService).getLogEntries(any(PageRequest.class), anyList(), any(ZonedDateTime.class), any(ZonedDateTime.class), nullable(String.class));

		assertEquals(1, result.entries().size());
		assertEquals(convertedResult, result.entries().get(0));
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	@Entity
	public static class Log extends LogEntity {

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public Log(final String logId, final ZonedDateTime entryDate, final String logger, final LogLevel logLevel, final String message, final String exception) {
			super(logId, entryDate, logger, logLevel, message, exception);
		}
	}
}