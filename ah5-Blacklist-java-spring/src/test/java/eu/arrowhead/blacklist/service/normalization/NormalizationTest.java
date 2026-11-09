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
package eu.arrowhead.blacklist.service.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.blacklist.service.dto.NormalizedBlacklistQueryRequestDTO;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.BlacklistCreateListRequestDTO;
import eu.arrowhead.dto.BlacklistCreateRequestDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.enums.Mode;

@ExtendWith(MockitoExtension.class)
public class NormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private Normalization normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeBlacklistCreateListRequestDTONullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeBlacklistCreateListRequestDTO(null));

		assertEquals("BlacklistCreateListRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeBlacklistCreateListRequestDTOEntititesListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeBlacklistCreateListRequestDTO(new BlacklistCreateListRequestDTO(null)));

		assertEquals("Entities list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeBlacklistCreateListRequestDTOCandidateNull() {
		final List<BlacklistCreateRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeBlacklistCreateListRequestDTO(new BlacklistCreateListRequestDTO(list)));

		assertEquals("BlacklistCreateRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeBlacklistCreateListRequestDTOOk() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("TestSystem", null, "reason ");

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");

		final BlacklistCreateListRequestDTO result = normalizer.normalizeBlacklistCreateListRequestDTO(new BlacklistCreateListRequestDTO(List.of(request)));

		assertNotNull(result);
		assertNotNull(result.entities());
		assertEquals(1, result.entities().size());
		final BlacklistCreateRequestDTO normalized = result.entities().get(0);
		assertEquals("TestSystem", normalized.systemName());
		assertEquals("", normalized.expiresAt());
		assertEquals("reason", normalized.reason());

		verify(systemNameNormalizer).normalize("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeBlacklistCreateListRequestDTOOk2() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("TestSystem", "2026-03-10T10:00:00Z ", "reason ");

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");

		final BlacklistCreateListRequestDTO result = normalizer.normalizeBlacklistCreateListRequestDTO(new BlacklistCreateListRequestDTO(List.of(request)));

		assertNotNull(result);
		assertNotNull(result.entities());
		assertEquals(1, result.entities().size());
		final BlacklistCreateRequestDTO normalized = result.entities().get(0);
		assertEquals("TestSystem", normalized.systemName());
		assertEquals("2026-03-10T10:00:00Z", normalized.expiresAt());
		assertEquals("reason", normalized.reason());

		verify(systemNameNormalizer).normalize("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeBlacklistQueryRequestDTONullInput() {
		final NormalizedBlacklistQueryRequestDTO result = normalizer.normalizeBlacklistQueryRequestDTO(null);

		assertNotNull(result);
		assertNull(result.pagination());
		assertNull(result.systemNames());
		assertNull(result.mode());
		assertNull(result.issuers());
		assertNull(result.revokers());
		assertNull(result.reason());
		assertNull(result.alivesAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeBlacklistQueryRequestDTOOk1() {
		final PageDTO page = new PageDTO(0, 10, null, null);
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				page,
				List.of(),
				null,
				List.of(),
				List.of(),
				null,
				null);

		final NormalizedBlacklistQueryRequestDTO result = normalizer.normalizeBlacklistQueryRequestDTO(dto);

		assertNotNull(result);
		assertEquals(page, result.pagination());
		assertNull(result.systemNames());
		assertNull(result.mode());
		assertNull(result.issuers());
		assertNull(result.revokers());
		assertNull(result.reason());
		assertEquals("", result.alivesAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeBlacklistQueryRequestDTOOk2() {
		final PageDTO page = new PageDTO(0, 10, null, null);
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				page,
				List.of("TestSystem"),
				"all",
				List.of("Issuer"),
				List.of("Revoker"),
				" reason",
				"2026-03-10T10:00:00Z ");

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");
		when(systemNameNormalizer.normalize("Issuer")).thenReturn("Issuer");
		when(systemNameNormalizer.normalize("Revoker")).thenReturn("Revoker");

		final NormalizedBlacklistQueryRequestDTO result = normalizer.normalizeBlacklistQueryRequestDTO(dto);

		assertNotNull(result);
		assertEquals(page, result.pagination());
		assertEquals(List.of("TestSystem"), result.systemNames());
		assertEquals(Mode.ALL, result.mode());
		assertEquals(List.of("Issuer"), result.issuers());
		assertEquals(List.of("Revoker"), result.revokers());
		assertEquals("reason", result.reason());
		assertEquals("2026-03-10T10:00:00Z", result.alivesAt());

		verify(systemNameNormalizer).normalize("TestSystem");
		verify(systemNameNormalizer).normalize("Issuer");
		verify(systemNameNormalizer).normalize("Revoker");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNamesNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeSystemNames(null));

		assertEquals("names list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNamesOk() {
		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");

		final List<String> result = normalizer.normalizeSystemNames(List.of("TestSystem"));

		assertEquals(List.of("TestSystem"), result);

		verify(systemNameNormalizer).normalize("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNameOk() {
		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");

		final String result = normalizer.normalizeSystemName("TestSystem");

		assertEquals("TestSystem", result);

		verify(systemNameNormalizer).normalize("TestSystem");
	}
}