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
package eu.arrowhead.translationmanager.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.TranslationReportRequestDTO;
import eu.arrowhead.dto.enums.TranslationBridgeEventState;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationReportRequestDTO;
import eu.arrowhead.translationmanager.service.validation.TranslationReportValidation;

@ExtendWith(MockitoExtension.class)
public class TranslationReportServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslationReportService service;

	@Mock
	private TranslationReportValidation validator;

	@Mock
	private BridgeDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReportOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.reportOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReportOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.reportOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReportOperationInvalidParameterException() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"581fd924-d8b0-4548-8cf8-4334e9f3cba2",
				"2026-02-19T10:00:00Z",
				"USED",
				null);
		final NormalizedTranslationReportRequestDTO normalized = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString("581fd924-d8b0-4548-8cf8-4334e9f3cba2"),
				ZonedDateTime.of(2026, 2, 19, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeReport("Requester", dto, "origin")).thenReturn(normalized);
		doThrow(new InvalidParameterException("test")).when(dbService).handleTranslationReport(normalized);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.reportOperation("Requester", dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeReport("Requester", dto, "origin");
		verify(dbService).handleTranslationReport(normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReportOperationForbiddenException() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"581fd924-d8b0-4548-8cf8-4334e9f3cba2",
				"2026-02-19T10:00:00Z",
				"USED",
				null);
		final NormalizedTranslationReportRequestDTO normalized = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString("581fd924-d8b0-4548-8cf8-4334e9f3cba2"),
				ZonedDateTime.of(2026, 2, 19, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeReport("Requester", dto, "origin")).thenReturn(normalized);
		doThrow(new ForbiddenException("test")).when(dbService).handleTranslationReport(normalized);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> service.reportOperation("Requester", dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeReport("Requester", dto, "origin");
		verify(dbService).handleTranslationReport(normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReportOperationInternalServerError() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"581fd924-d8b0-4548-8cf8-4334e9f3cba2",
				"2026-02-19T10:00:00Z",
				"USED",
				null);
		final NormalizedTranslationReportRequestDTO normalized = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString("581fd924-d8b0-4548-8cf8-4334e9f3cba2"),
				ZonedDateTime.of(2026, 2, 19, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeReport("Requester", dto, "origin")).thenReturn(normalized);
		doThrow(new InternalServerError("test")).when(dbService).handleTranslationReport(normalized);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.reportOperation("Requester", dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeReport("Requester", dto, "origin");
		verify(dbService).handleTranslationReport(normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReportOperationOk() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"581fd924-d8b0-4548-8cf8-4334e9f3cba2",
				"2026-02-19T10:00:00Z",
				"USED",
				null);
		final NormalizedTranslationReportRequestDTO normalized = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString("581fd924-d8b0-4548-8cf8-4334e9f3cba2"),
				ZonedDateTime.of(2026, 2, 19, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeReport("Requester", dto, "origin")).thenReturn(normalized);
		doNothing().when(dbService).handleTranslationReport(normalized);

		assertDoesNotThrow(() -> service.reportOperation("Requester", dto, "origin"));

		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeReport("Requester", dto, "origin");
		verify(dbService).handleTranslationReport(normalized);
	}
}