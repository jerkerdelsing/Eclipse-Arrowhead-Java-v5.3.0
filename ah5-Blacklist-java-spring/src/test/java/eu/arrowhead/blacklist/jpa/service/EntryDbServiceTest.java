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
package eu.arrowhead.blacklist.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.blacklist.jpa.repository.EntryRepository;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.BlacklistCreateRequestDTO;
import eu.arrowhead.dto.enums.Mode;

@ExtendWith(MockitoExtension.class)
public class EntryDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private EntryDbService dbService;

	@Mock
	private EntryRepository entryRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createBulk(null, null));

		assertEquals("candidates list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkException() {
		final BlacklistCreateRequestDTO dto = new BlacklistCreateRequestDTO("TestSystem", null, "reason");
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");

		when(entryRepo.saveAllAndFlush(List.of(entry))).thenThrow(RuntimeException.class);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> dbService.createBulk(List.of(dto), "Requester"));

		assertEquals("Database operation error", ex.getMessage());

		verify(entryRepo).saveAllAndFlush(List.of(entry));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkOk() {
		final BlacklistCreateRequestDTO dto = new BlacklistCreateRequestDTO("TestSystem", null, "reason");
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		final Entry entry2 = new Entry("TestSystem", null, "Requester", "reason");
		entry2.setId(1L);

		when(entryRepo.saveAllAndFlush(List.of(entry))).thenReturn(List.of(entry2));

		final List<Entry> result = dbService.createBulk(List.of(dto), "Requester");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(entry2, result.get(0));

		verify(entryRepo).saveAllAndFlush(List.of(entry));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersPageNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getPageByFilters(
						null,
						null,
						null,
						null,
						null,
						null,
						null));

		assertEquals("page is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersException() {
		final PageRequest page = PageRequest.of(0, 10);

		when(entryRepo.findAll(page)).thenThrow(RuntimeException.class);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getPageByFilters(
						page,
						null,
						null,
						null,
						null,
						null,
						null));

		assertEquals("Database operation error", ex.getMessage());

		verify(entryRepo).findAll(page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersNoFilter() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");

		when(entryRepo.findAll(page)).thenReturn(new PageImpl<>(List.of(entry)));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				null,
				null,
				null,
				null,
				null);

		assertNotNull(result);
		assertNotNull(result.getContent());
		assertEquals(1, result.getContent().size());
		assertEquals(entry, result.getContent().get(0));

		verify(entryRepo).findAll(page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters1() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(false);

		when(entryRepo.findAllBySystemNameIn(List.of("TestSystem"))).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(), page)).thenReturn(new PageImpl<>(List.of()));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				List.of("TestSystem"),
				Mode.ACTIVES,
				null,
				null,
				null,
				null);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo, never()).findAll();
		verify(entryRepo).findAllBySystemNameIn(List.of("TestSystem"));
		verify(entryRepo).findAllByIdIn(List.of(), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters2() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(true);

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(), page)).thenReturn(new PageImpl<>(List.of()));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				Mode.INACTIVES,
				List.of("OtherRequester"),
				null,
				null,
				null);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters3() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(false);

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(), page)).thenReturn(new PageImpl<>(List.of()));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				Mode.INACTIVES,
				List.of("OtherRequester"),
				null,
				null,
				null);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters4() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(false);

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(), page)).thenReturn(new PageImpl<>(List.of()));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				Mode.ALL,
				List.of("Requester"),
				List.of("Revoker"),
				null,
				null);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters5() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(false);
		entry.setRevokedBy("OtherRevoker");

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(), page)).thenReturn(new PageImpl<>(List.of()));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				null,
				List.of("Requester"),
				List.of("Revoker"),
				null,
				null);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters6() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(false);
		entry.setRevokedBy("Revoker");

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(), page)).thenReturn(new PageImpl<>(List.of()));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				null,
				null,
				List.of("Revoker"),
				"Something",
				null);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters7() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)), "Requester", "reason");
		entry.setId(1L);
		entry.setActive(true);
		entry.setRevokedBy("Revoker");

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(), page)).thenReturn(new PageImpl<>(List.of()));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				null,
				null,
				null,
				"Reason",
				ZonedDateTime.of(2026, 3, 20, 18, 0, 0, 0, ZoneId.of(Constants.UTC)));

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters8() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)), "Requester", "reason");
		entry.setId(1L);
		entry.setActive(false);
		entry.setRevokedBy("Revoker");

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(), page)).thenReturn(new PageImpl<>(List.of()));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				null,
				null,
				null,
				null,
				ZonedDateTime.of(2026, 3, 20, 18, 0, 0, 0, ZoneId.of(Constants.UTC)));

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters9() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)), "Requester", "reason");
		entry.setId(1L);
		entry.setActive(true);
		entry.setRevokedBy("Revoker");

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(1L), page)).thenReturn(new PageImpl<>(List.of(entry)));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				null,
				null,
				null,
				null,
				ZonedDateTime.of(2026, 3, 20, 8, 0, 0, 0, ZoneId.of(Constants.UTC)));

		assertNotNull(result);
		assertNotNull(result.getContent());
		assertEquals(1, result.getContent().size());
		assertEquals(entry, result.getContent().get(0));

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(1L), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters10() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(true);
		entry.setRevokedBy("Revoker");

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(1L), page)).thenReturn(new PageImpl<>(List.of(entry)));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				null,
				null,
				null,
				null,
				ZonedDateTime.of(2026, 3, 20, 8, 0, 0, 0, ZoneId.of(Constants.UTC)));

		assertNotNull(result);
		assertNotNull(result.getContent());
		assertEquals(1, result.getContent().size());
		assertEquals(entry, result.getContent().get(0));

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(1L), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithFilters11() {
		final PageRequest page = PageRequest.of(0, 10);
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(true);
		entry.setRevokedBy("Revoker");

		when(entryRepo.findAll()).thenReturn(List.of(entry));
		when(entryRepo.findAllByIdIn(List.of(1L), page)).thenReturn(new PageImpl<>(List.of(entry)));

		final Page<Entry> result = dbService.getPageByFilters(
				page,
				null,
				null,
				List.of("Requester"),
				null,
				null,
				null);

		assertNotNull(result);
		assertNotNull(result.getContent());
		assertEquals(1, result.getContent().size());
		assertEquals(entry, result.getContent().get(0));

		verify(entryRepo, never()).findAll(any(PageRequest.class));
		verify(entryRepo).findAll();
		verify(entryRepo, never()).findAllBySystemNameIn(anyList());
		verify(entryRepo).findAllByIdIn(List.of(1L), page);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInactivateNameListNullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.inactivateNameList(null, null));

		assertEquals("System name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInactivateNameListEmptyList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.inactivateNameList(List.of(), null));

		assertEquals("System name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInactivateNameListNullRevoker() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.inactivateNameList(List.of("TestSystem"), null));

		assertEquals("Revoker name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInactivateNameListEmptyRevoker() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.inactivateNameList(List.of("TestSystem"), ""));

		assertEquals("Revoker name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInactivateNameListDbException() {
		when(entryRepo.findAllBySystemNameIn(List.of("TestSystem"))).thenThrow(RuntimeException.class);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> dbService.inactivateNameList(List.of("TestSystem"), "Revoker"));

		assertEquals("Database operation error", ex.getMessage());

		verify(entryRepo).findAllBySystemNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInactivateNameListOk() {
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(true);

		when(entryRepo.findAllBySystemNameIn(List.of("TestSystem"))).thenReturn(List.of(entry));
		when(entryRepo.saveAllAndFlush(List.of(entry))).thenReturn(List.of(entry));

		assertDoesNotThrow(() -> dbService.inactivateNameList(List.of("TestSystem"), "Revoker"));
		assertEquals("Revoker", entry.getRevokedBy());
		assertFalse(entry.getActive());

		verify(entryRepo).findAllBySystemNameIn(List.of("TestSystem"));
		verify(entryRepo).saveAllAndFlush(List.of(entry));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsActiveEntryForNameNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.isActiveEntryForName(null));

		assertEquals("System name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsActiveEntryForNameEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.isActiveEntryForName(""));

		assertEquals("System name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsActiveEntryForNameDbException() {
		when(entryRepo.findAllBySystemNameAndActive("TestSystem", true)).thenThrow(RuntimeException.class);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> dbService.isActiveEntryForName("TestSystem"));

		assertEquals("Database operation error", ex.getMessage());

		verify(entryRepo).findAllBySystemNameAndActive("TestSystem", true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsActiveEntryForNameTrue1() {
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(true);

		when(entryRepo.findAllBySystemNameAndActive("TestSystem", true)).thenReturn(List.of(entry));

		final boolean result = dbService.isActiveEntryForName("TestSystem");

		assertTrue(result);

		verify(entryRepo).findAllBySystemNameAndActive("TestSystem", true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsActiveEntryForNameTrue2() {
		final Entry entry = new Entry("TestSystem", ZonedDateTime.of(2126, 3, 20, 8, 0, 0, 0, ZoneId.of(Constants.UTC)), "Requester", "reason"); // only works for ~100 years
		entry.setId(1L);
		entry.setActive(true);

		when(entryRepo.findAllBySystemNameAndActive("TestSystem", true)).thenReturn(List.of(entry));

		final boolean result = dbService.isActiveEntryForName("TestSystem");

		assertTrue(result);

		verify(entryRepo).findAllBySystemNameAndActive("TestSystem", true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsActiveEntryForNameFalse() {
		final Entry entry = new Entry("TestSystem", ZonedDateTime.of(2026, 3, 20, 8, 0, 0, 0, ZoneId.of(Constants.UTC)), "Requester", "reason"); // only works for ~100 years
		entry.setId(1L);
		entry.setActive(true);

		when(entryRepo.findAllBySystemNameAndActive("TestSystem", true)).thenReturn(List.of(entry));

		final boolean result = dbService.isActiveEntryForName("TestSystem");

		assertFalse(result);

		verify(entryRepo).findAllBySystemNameAndActive("TestSystem", true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetActiveEntriesForNameNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getActiveEntriesForName(null));

		assertEquals("System name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetActiveEntriesForNameEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getActiveEntriesForName(""));

		assertEquals("System name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetActiveEntriesForNameDbException() {
		when(entryRepo.findAllBySystemNameAndActive("TestSystem", true)).thenThrow(RuntimeException.class);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getActiveEntriesForName("TestSystem"));

		assertEquals("Database operation error", ex.getMessage());

		verify(entryRepo).findAllBySystemNameAndActive("TestSystem", true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetActiveEntriesForNameMatch() {
		final Entry entry = new Entry("TestSystem", null, "Requester", "reason");
		entry.setId(1L);
		entry.setActive(true);

		when(entryRepo.findAllBySystemNameAndActive("TestSystem", true)).thenReturn(List.of(entry));

		final List<Entry> result = dbService.getActiveEntriesForName("TestSystem");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(entry, result.get(0));

		verify(entryRepo).findAllBySystemNameAndActive("TestSystem", true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetActiveEntriesForNameNoMatch() {
		final Entry entry = new Entry("TestSystem", ZonedDateTime.of(2026, 3, 20, 8, 0, 0, 0, ZoneId.of(Constants.UTC)), "Requester", "reason");
		entry.setId(1L);
		entry.setActive(true);

		when(entryRepo.findAllBySystemNameAndActive("TestSystem", true)).thenReturn(List.of(entry));

		final List<Entry> result = dbService.getActiveEntriesForName("TestSystem");

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(entryRepo).findAllBySystemNameAndActive("TestSystem", true);
	}
}