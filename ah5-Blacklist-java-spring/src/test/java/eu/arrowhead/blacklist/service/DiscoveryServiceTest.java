/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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
package eu.arrowhead.blacklist.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.blacklist.jpa.service.EntryDbService;
import eu.arrowhead.blacklist.service.dto.DTOConverter;
import eu.arrowhead.blacklist.service.validation.Validation;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;

@ExtendWith(MockitoExtension.class)
public class DiscoveryServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DiscoveryService service;

	@Mock
	private EntryDbService dbService;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private Validation validator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.check(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.check(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckInternalServerError() {
		when(validator.validateAndNormalizeSystemName("TestSystem", "origin")).thenReturn("TestSystem");
		when(dbService.isActiveEntryForName("TestSystem")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.check("TestSystem", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("TestSystem", "origin");
		verify(dbService).isActiveEntryForName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckOk() {
		when(validator.validateAndNormalizeSystemName("TestSystem", "origin")).thenReturn("TestSystem");
		when(dbService.isActiveEntryForName("TestSystem")).thenReturn(true);

		final boolean result = service.check("TestSystem", "origin");

		assertTrue(result);

		verify(validator).validateAndNormalizeSystemName("TestSystem", "origin");
		verify(dbService).isActiveEntryForName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.lookup(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.lookup(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupInternalServerError() {
		when(validator.validateAndNormalizeSystemName("TestSystem", "origin")).thenReturn("TestSystem");
		when(dbService.getActiveEntriesForName("TestSystem")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.lookup("TestSystem", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("TestSystem", "origin");
		verify(dbService).getActiveEntriesForName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testLookupOk() {
		final Entry entry = new Entry(
				"TestSystem",
				ZonedDateTime.of(2126, 3, 11, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				"Creator",
				"reason");
		entry.setId(1L);
		entry.setCreatedAt(ZonedDateTime.of(2026, 3, 11, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		entry.setUpdatedAt(ZonedDateTime.of(2026, 3, 11, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));

		final BlacklistEntryDTO entryDTO = new BlacklistEntryDTO(
				"TestSystem",
				"Creator",
				null,
				"2026-03-11T10:00:00Z",
				"2026-03-11T10:00:00Z",
				"reason",
				"2126-03-11T10:00:00Z",
				true);

		when(validator.validateAndNormalizeSystemName("TestSystem", "origin")).thenReturn("TestSystem");
		when(dbService.getActiveEntriesForName("TestSystem")).thenReturn(List.of(entry));
		when(dtoConverter.convertEntriesToBlacklistEntryListResponseDTO(List.of(entry), 1)).thenReturn(new BlacklistEntryListResponseDTO(List.of(entryDTO), 1));

		final BlacklistEntryListResponseDTO result = service.lookup("TestSystem", "origin");

		assertNotNull(result);
		assertEquals(1, result.entries().size());
		assertEquals(entryDTO, result.entries().get(0));

		verify(validator).validateAndNormalizeSystemName("TestSystem", "origin");
		verify(dbService).getActiveEntriesForName("TestSystem");
		verify(dtoConverter).convertEntriesToBlacklistEntryListResponseDTO(List.of(entry), 1);
	}
}