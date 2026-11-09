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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.blacklist.jpa.service.EntryDbService;
import eu.arrowhead.blacklist.service.dto.DTOConverter;
import eu.arrowhead.blacklist.service.dto.NormalizedBlacklistQueryRequestDTO;
import eu.arrowhead.blacklist.service.validation.Validation;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.BlacklistCreateListRequestDTO;
import eu.arrowhead.dto.BlacklistCreateRequestDTO;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;

@ExtendWith(MockitoExtension.class)
public class ManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementService service;

	@Mock
	private Validation validator;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private EntryDbService dbService;

	@Mock
	private PageService pageService;

	@Mock
	private WhitelistService whitelistService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.query(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.query(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryInternalServerError() {
		final BlacklistQueryRequestDTO request = new BlacklistQueryRequestDTO(null, null, null, null, null, null, null);
		final NormalizedBlacklistQueryRequestDTO normalized = new NormalizedBlacklistQueryRequestDTO(null, null, null, null, null, null, null);
		final PageRequest pageRequest = PageRequest.of(0, 1000, Direction.DESC, "systemName");

		when(validator.validateAndNormalizeBlacklistQueryRequestDTO(request, "origin")).thenReturn(normalized);
		when(pageService.getPageRequest(null, Direction.DESC, Entry.SORTABLE_FIELDS_BY, Entry.DEFAULT_SORT_FIELD, "origin")).thenReturn(pageRequest);
		when(dbService.getPageByFilters(pageRequest, null, null, null, null, null, null)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.query(request, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeBlacklistQueryRequestDTO(request, "origin");
		verify(pageService).getPageRequest(null, Direction.DESC, Entry.SORTABLE_FIELDS_BY, Entry.DEFAULT_SORT_FIELD, "origin");
		verify(dbService).getPageByFilters(pageRequest, null, null, null, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOk() {
		final BlacklistQueryRequestDTO request = new BlacklistQueryRequestDTO(null, null, null, null, null, null, null);
		final NormalizedBlacklistQueryRequestDTO normalized = new NormalizedBlacklistQueryRequestDTO(null, null, null, null, null, null, null);
		final PageRequest pageRequest = PageRequest.of(0, 1000, Direction.DESC, "systemName");
		final Entry entry = new Entry("TestSystem", null, "Creator", "reason");
		final BlacklistEntryDTO blEntry = new BlacklistEntryDTO("TestSystem", "Creator", null, "2026-03-18T10:00:00Z", "2026-03-18T10:00:00Z", "reason", null, true);

		when(validator.validateAndNormalizeBlacklistQueryRequestDTO(request, "origin")).thenReturn(normalized);
		when(pageService.getPageRequest(null, Direction.DESC, Entry.SORTABLE_FIELDS_BY, Entry.DEFAULT_SORT_FIELD, "origin")).thenReturn(pageRequest);
		when(dbService.getPageByFilters(pageRequest, null, null, null, null, null, null)).thenReturn(new PageImpl<>(List.of(entry)));
		when(dtoConverter.convertEntriesToBlacklistEntryListResponseDTO(List.of(entry), 1)).thenReturn(new BlacklistEntryListResponseDTO(List.of(blEntry), 1));

		final BlacklistEntryListResponseDTO result = service.query(request, "origin");

		assertNotNull(result);
		assertNotNull(result.entries());
		assertEquals(1, result.entries().size());
		final BlacklistEntryDTO resultEntry = result.entries().get(0);
		assertEquals(blEntry, resultEntry);

		verify(validator).validateAndNormalizeBlacklistQueryRequestDTO(request, "origin");
		verify(pageService).getPageRequest(null, Direction.DESC, Entry.SORTABLE_FIELDS_BY, Entry.DEFAULT_SORT_FIELD, "origin");
		verify(dbService).getPageByFilters(pageRequest, null, null, null, null, null, null);
		verify(dtoConverter).convertEntriesToBlacklistEntryListResponseDTO(List.of(entry), 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.create(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.create(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSelfBlacklisting() {
		final BlacklistCreateRequestDTO dto = new BlacklistCreateRequestDTO("TestSystem", null, "reason");
		final BlacklistCreateListRequestDTO request = new BlacklistCreateListRequestDTO(List.of(dto));

		when(validator.validateAndNormalizeBlacklistCreateListRequestDTO(request, "origin")).thenReturn(request);
		when(validator.validateAndNormalizeSystemName("TestSystem", "origin")).thenReturn("TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.create(request, "TestSystem", "origin"));

		assertEquals("It is not allowed to add yourself to the blacklist", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeBlacklistCreateListRequestDTO(request, "origin");
		verify(validator).validateAndNormalizeSystemName("TestSystem", "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInternalServerError() {
		final BlacklistCreateRequestDTO dto = new BlacklistCreateRequestDTO("TestSystem", null, "reason");
		final BlacklistCreateListRequestDTO request = new BlacklistCreateListRequestDTO(List.of(dto));

		when(validator.validateAndNormalizeBlacklistCreateListRequestDTO(request, "origin")).thenReturn(request);
		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		doNothing().when(whitelistService).checkWhitelist(List.of("TestSystem"), "origin");
		when(dbService.createBulk(List.of(dto), "Requester")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.create(request, "Requester", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeBlacklistCreateListRequestDTO(request, "origin");
		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(whitelistService).checkWhitelist(List.of("TestSystem"), "origin");
		verify(dbService).createBulk(List.of(dto), "Requester");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOk() {
		final BlacklistCreateRequestDTO dto = new BlacklistCreateRequestDTO("TestSystem", null, "reason");
		final BlacklistCreateListRequestDTO request = new BlacklistCreateListRequestDTO(List.of(dto));
		final Entry entry = new Entry("TestSystem", null, "Creator", "reason");
		final BlacklistEntryDTO blEntry = new BlacklistEntryDTO("TestSystem", "Creator", null, "2026-03-18T10:00:00Z", "2026-03-18T10:00:00Z", "reason", null, true);

		when(validator.validateAndNormalizeBlacklistCreateListRequestDTO(request, "origin")).thenReturn(request);
		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		doNothing().when(whitelistService).checkWhitelist(List.of("TestSystem"), "origin");
		when(dbService.createBulk(List.of(dto), "Requester")).thenReturn(List.of(entry));
		when(dtoConverter.convertEntriesToBlacklistEntryListResponseDTO(List.of(entry), 1)).thenReturn(new BlacklistEntryListResponseDTO(List.of(blEntry), 1));

		final BlacklistEntryListResponseDTO result = service.create(request, "Requester", "origin");

		assertNotNull(result);
		assertNotNull(result.entries());
		assertEquals(1, result.entries().size());
		final BlacklistEntryDTO resultEntry = result.entries().get(0);
		assertEquals(blEntry, resultEntry);

		verify(validator).validateAndNormalizeBlacklistCreateListRequestDTO(request, "origin");
		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(whitelistService).checkWhitelist(List.of("TestSystem"), "origin");
		verify(dbService).createBulk(List.of(dto), "Requester");
		verify(dtoConverter).convertEntriesToBlacklistEntryListResponseDTO(List.of(entry), 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.remove(null, false, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.remove(null, false, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSelfRemovingFalse() {
		when(validator.validateAndNormalizeSystemNameList(List.of("TestSystem"), "origin")).thenReturn(List.of("TestSystem"));
		when(validator.validateAndNormalizeSystemName("TestSystem", "origin")).thenReturn("TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.remove(List.of("TestSystem"), false, "TestSystem", "origin"));

		assertEquals("Only sysop can remove itself from the blacklist", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemNameList(List.of("TestSystem"), "origin");
		verify(validator).validateAndNormalizeSystemName("TestSystem", "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveInternalServerError() {
		when(validator.validateAndNormalizeSystemNameList(List.of("TestSystem"), "origin")).thenReturn(List.of("TestSystem"));
		when(validator.validateAndNormalizeSystemName("TestSystem", "origin")).thenReturn("TestSystem");
		doThrow(new InternalServerError("test")).when(dbService).inactivateNameList(List.of("TestSystem"), "TestSystem");

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.remove(List.of("TestSystem"), true, "TestSystem", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemNameList(List.of("TestSystem"), "origin");
		verify(validator).validateAndNormalizeSystemName("TestSystem", "origin");
		verify(dbService).inactivateNameList(List.of("TestSystem"), "TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveOk() {
		when(validator.validateAndNormalizeSystemNameList(List.of("TestSystem"), "origin")).thenReturn(List.of("TestSystem"));
		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		doNothing().when(dbService).inactivateNameList(List.of("TestSystem"), "Requester");

		assertDoesNotThrow(() -> service.remove(List.of("TestSystem"), false, "Requester", "origin"));

		verify(validator).validateAndNormalizeSystemNameList(List.of("TestSystem"), "origin");
		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(dbService).inactivateNameList(List.of("TestSystem"), "Requester");
	}
}