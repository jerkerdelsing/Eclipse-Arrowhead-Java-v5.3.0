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
package eu.arrowhead.blacklist.service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;

public class DTOConverterTest {

	//=================================================================================================
	// members

	private DTOConverter converter = new DTOConverter();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertEntriesToBlacklistEntryListResponseDTONullList() {
		final BlacklistEntryListResponseDTO result = converter.convertEntriesToBlacklistEntryListResponseDTO(null, 0);

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertEntriesToBlacklistEntryListResponseDTOOk() {
		final Entry entry = new Entry(
				"TestSystem",
				ZonedDateTime.of(2126, 3, 11, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				"Creator",
				"reason");
		entry.setId(1L);
		entry.setCreatedAt(ZonedDateTime.of(2026, 3, 11, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		entry.setUpdatedAt(ZonedDateTime.of(2026, 3, 11, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));

		final BlacklistEntryListResponseDTO result = converter.convertEntriesToBlacklistEntryListResponseDTO(List.of(entry), 1);

		assertNotNull(result);
		assertNotNull(result.entries());
		assertEquals(1, result.entries().size());
		final BlacklistEntryDTO entryDTO = result.entries().get(0);
		assertEquals("TestSystem", entryDTO.systemName());
		assertEquals("Creator", entryDTO.createdBy());
		assertNull(entryDTO.revokedBy());
		assertEquals("2026-03-11T10:00:00Z", entryDTO.createdAt());
		assertEquals("2026-03-11T10:00:00Z", entryDTO.updatedAt());
		assertEquals("reason", entryDTO.reason());
		assertEquals("2126-03-11T10:00:00Z", entryDTO.expiresAt());
		assertTrue(entryDTO.active());
	}
}