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
package eu.arrowhead.blacklist.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.blacklist.service.dto.NormalizedBlacklistQueryRequestDTO;
import eu.arrowhead.blacklist.service.normalization.Normalization;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.BlacklistCreateListRequestDTO;
import eu.arrowhead.dto.BlacklistCreateRequestDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;

@ExtendWith(MockitoExtension.class)
public class ValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private Validation validator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer; // for checking duplications

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private Normalization normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTONullDTO() {
		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(null, "origin"));

		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOEntitiesNull() {
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("Request payload is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOEntitiesEmpty() {
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of());

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("Request payload is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOEntityNull() {
		final List<BlacklistCreateRequestDTO> list = new ArrayList<>(1);
		list.add(null);
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(list);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("Entity list contains null element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOSystemNameNull() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO(null, null, null);
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("System name is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOSystemNameEmpty() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("", null, null);
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("System name is empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOWrongExpiration() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("TestSystem", "expiration", null);
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request));

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("Expiration time has an invalid time format, UTC string expected (example: 2024-10-11T14:30:00Z)", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOPastExpiration() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("TestSystem", "2025-12-10T10:00:00Z", null);
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request));

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("Expiration time is in the past", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTONullReason() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("TestSystem", "2125-12-10T10:00:00Z", null); // will fail in ~100 years
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request));

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("You cannot blacklist a system without specifying the reason", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOEmptyReason() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("TestSystem", "", "");
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request));

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("You cannot blacklist a system without specifying the reason", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOTooLongReason() {
		final String reason = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("TestSystem", null, reason);
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request));

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("Reason is too long", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTODuplicateName() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("TestSystem", null, "reason");
		final BlacklistCreateRequestDTO request2 = new BlacklistCreateRequestDTO("testSystem", null, "reason2");
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request, request2));

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");
		when(systemNameNormalizer.normalize("testSystem")).thenReturn("TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("Duplicated system name: TestSystem", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("TestSystem");
		verify(systemNameNormalizer).normalize("testSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOInvalidSystemName() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("1TestSystem", null, "reason");
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request));

		when(systemNameNormalizer.normalize("1TestSystem")).thenReturn("1TestSystem");
		when(normalizer.normalizeBlacklistCreateListRequestDTO(dto)).thenReturn(dto);
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("1TestSystem");
		verify(normalizer).normalizeBlacklistCreateListRequestDTO(dto);
		verify(systemNameValidator).validateSystemName("1TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistCreateListRequestDTOOk() {
		final BlacklistCreateRequestDTO request = new BlacklistCreateRequestDTO("TestSystem", null, "reason");
		final BlacklistCreateListRequestDTO dto = new BlacklistCreateListRequestDTO(List.of(request));

		when(systemNameNormalizer.normalize("TestSystem")).thenReturn("TestSystem");
		when(normalizer.normalizeBlacklistCreateListRequestDTO(dto)).thenReturn(dto);
		doNothing().when(systemNameValidator).validateSystemName("TestSystem");

		final BlacklistCreateListRequestDTO result = validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, "origin");

		assertEquals(dto, result);

		verify(systemNameNormalizer).normalize("TestSystem");
		verify(normalizer).normalizeBlacklistCreateListRequestDTO(dto);
		verify(systemNameValidator).validateSystemName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTOOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTOOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTONullDTO() {
		final NormalizedBlacklistQueryRequestDTO normalized = new NormalizedBlacklistQueryRequestDTO(
				null,
				List.of(),
				null,
				List.of(),
				List.of(),
				null,
				null);

		when(normalizer.normalizeBlacklistQueryRequestDTO(null)).thenReturn(normalized);

		final NormalizedBlacklistQueryRequestDTO result = validator.validateAndNormalizeBlacklistQueryRequestDTO(null, "origin");

		assertEquals(normalized, result);

		verify(normalizer).normalizeBlacklistQueryRequestDTO(null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTONullSystemName() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				list,
				null,
				List.of(),
				List.of(),
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin"));

		assertEquals("System name list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTOEmptySystemName() {
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				List.of(""),
				null,
				List.of(),
				List.of(),
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin"));

		assertEquals("System name list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTOInvalidMode() {
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				List.of("TestSystem"),
				"invalid",
				List.of(),
				List.of(),
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin"));

		assertEquals("Mode is invalid. Possible values: ALL, ACTIVES, INACTIVES", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTONullIssuer() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				null,
				"all",
				list,
				List.of(),
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin"));

		assertEquals("Issuer name list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTOEmptyIssuer() {
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				null,
				null,
				List.of(""),
				List.of(),
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin"));

		assertEquals("Issuer name list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTONullRevoker() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				null,
				null,
				List.of("Issuer"),
				list,
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin"));

		assertEquals("Revoker name list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTOEmptyRevoker() {
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				null,
				null,
				null,
				List.of(""),
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin"));

		assertEquals("Revoker name list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTOInvalidAlivesAt() {
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				null,
				null,
				null,
				List.of("Revoker"),
				null,
				"invalid");

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin"));

		assertEquals("Alives at date has an invalid time format, UTC string expected (example: 2024-10-11T14:30:00Z)", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTOInvalidParameterException() {
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				List.of("1TestSystem"),
				null,
				null,
				null,
				null,
				"2025-12-10T10:00:00Z");

		final NormalizedBlacklistQueryRequestDTO normalized = new NormalizedBlacklistQueryRequestDTO(
				null,
				List.of("1TestSystem"),
				null,
				null,
				null,
				null,
				"2025-12-10T10:00:00Z");

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeBlacklistQueryRequestDTO(dto)).thenReturn(normalized);
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeBlacklistQueryRequestDTO(dto);
		verify(systemNameValidator).validateSystemName("1TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeBlacklistQueryRequestDTOOk() {
		final BlacklistQueryRequestDTO dto = new BlacklistQueryRequestDTO(
				null,
				List.of("TestSystem"),
				null,
				List.of("Issuer"),
				List.of("Revoker"),
				null,
				null);

		final NormalizedBlacklistQueryRequestDTO normalized = new NormalizedBlacklistQueryRequestDTO(
				null,
				List.of("TestSystem"),
				null,
				List.of("Issuer"),
				List.of("Revoker"),
				null,
				null);

		doNothing().when(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeBlacklistQueryRequestDTO(dto)).thenReturn(normalized);
		doNothing().when(systemNameValidator).validateSystemName("TestSystem");
		doNothing().when(systemNameValidator).validateSystemName("Issuer");
		doNothing().when(systemNameValidator).validateSystemName("Revoker");

		final NormalizedBlacklistQueryRequestDTO result = validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, "origin");

		assertEquals(normalized, result);

		verify(pageValidator).validatePageParameter(null, Entry.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeBlacklistQueryRequestDTO(dto);
		verify(systemNameValidator).validateSystemName("TestSystem");
		verify(systemNameValidator).validateSystemName("Issuer");
		verify(systemNameValidator).validateSystemName("Revoker");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameListNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeSystemNameList(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameListEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeSystemNameList(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameListNullList() {
		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemNameList(null, "origin"));

		assertEquals("System name list is missing or empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameListEmptyList() {
		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemNameList(List.of(), "origin"));

		assertEquals("System name list is missing or empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemNameList(list, "origin"));

		assertEquals("System name list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameListContainsEmptyElement() {
		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemNameList(List.of(""), "origin"));

		assertEquals("System name list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameListInvalidParameterException() {
		when(normalizer.normalizeSystemNames(List.of("1TestSystem"))).thenReturn(List.of("1TestSystem"));
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemNameList(List.of("1TestSystem"), "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(normalizer).normalizeSystemNames(List.of("1TestSystem"));
		verify(systemNameValidator).validateSystemName("1TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameListOk() {
		when(normalizer.normalizeSystemNames(List.of("TestSystem"))).thenReturn(List.of("TestSystem"));
		doNothing().when(systemNameValidator).validateSystemName("TestSystem");

		final List<String> result = validator.validateAndNormalizeSystemNameList(List.of("TestSystem"), "origin");

		assertEquals(List.of("TestSystem"), result);

		verify(normalizer).normalizeSystemNames(List.of("TestSystem"));
		verify(systemNameValidator).validateSystemName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeSystemName(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeSystemName(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameInvalidParameterException() {
		when(normalizer.normalizeSystemName("1TestSystem")).thenReturn("1TestSystem");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1TestSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemName("1TestSystem", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(normalizer).normalizeSystemName("1TestSystem");
		verify(systemNameValidator).validateSystemName("1TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameOk() {
		when(normalizer.normalizeSystemName("TestSystem")).thenReturn("TestSystem");
		doNothing().when(systemNameValidator).validateSystemName("TestSystem");

		final String result = validator.validateAndNormalizeSystemName("TestSystem", "origin");

		assertEquals("TestSystem", result);

		verify(normalizer).normalizeSystemName("TestSystem");
		verify(systemNameValidator).validateSystemName("TestSystem");
	}
}