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
package eu.arrowhead.translationmanager.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.TranslationReportRequestDTO;
import eu.arrowhead.dto.enums.TranslationBridgeEventState;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationReportRequestDTO;

@ExtendWith(MockitoExtension.class)
public class TranslationReportValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslationReportValidation validator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private SystemNameValidator systemNameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester("Consumer", null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester("Consumer", ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester(null, "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterEmpty() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("", "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterInvalid() {
		when(systemNameNormalizer.normalize("inv@lid")).thenReturn("Inv@lid");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("Inv@lid");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("inv@lid", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("inv@lid");
		verify(systemNameValidator).validateSystemName("Inv@lid");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterOk() {
		when(systemNameNormalizer.normalize("consumer")).thenReturn("Consumer");
		doNothing().when(systemNameValidator).validateSystemName("Consumer");

		final String result = validator.validateAndNormalizeRequester("consumer", "origin");

		assertEquals("Consumer", result);

		verify(systemNameNormalizer).normalize("consumer");
		verify(systemNameValidator).validateSystemName("Consumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportRequestNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", null, "origin"));

		assertEquals("Request is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportBridgeIdNull() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin"));

		assertEquals("Bridge identifier is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportBridgeIdEmpty() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"",
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin"));

		assertEquals("Bridge identifier is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportBridgeIdInvalid() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"invalid",
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin"));

		assertEquals("Bridge identifier is invalid: invalid", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportTimestampNull() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"37afcc60-e8c6-45ea-9fce-17f281e67a56",
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin"));

		assertEquals("Timestamp is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportTimestampEmpty() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"37afcc60-e8c6-45ea-9fce-17f281e67a56",
				"",
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin"));

		assertEquals("Timestamp is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportTimestampInvalid() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"37afcc60-e8c6-45ea-9fce-17f281e67a56",
				"invalid",
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin"));

		assertEquals("Timestamp is invalid: invalid", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportStateNull() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"37afcc60-e8c6-45ea-9fce-17f281e67a56",
				"2026-01-15T10:31:00Z",
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin"));

		assertEquals("State is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportStateEmpty() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"37afcc60-e8c6-45ea-9fce-17f281e67a56",
				"2026-01-15T10:31:00Z",
				"",
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin"));

		assertEquals("State is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportStateInvalid() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"37afcc60-e8c6-45ea-9fce-17f281e67a56",
				"2026-01-15T10:31:00Z",
				"INVALID",
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin"));

		assertEquals("State is invalid: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportOk1() {
		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"37afcc60-e8c6-45ea-9fce-17f281e67a56",
				"2026-01-14T10:31:00Z",
				"USED",
				null);

		final NormalizedTranslationReportRequestDTO normalized = validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin");

		assertEquals("InterfaceTranslator", normalized.requester());
		assertEquals(UUID.fromString("37afcc60-e8c6-45ea-9fce-17f281e67a56"), normalized.bridgeId());
		assertEquals(Utilities.parseUTCStringToZonedDateTime("2026-01-14T10:31:00Z"), normalized.timestamp());
		assertEquals(TranslationBridgeEventState.USED, normalized.state());
		assertNull(normalized.errorMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeReportOk2() {
		final ZonedDateTime outOfSyncTimestamp = Utilities.utcNow().plusHours(2);

		final TranslationReportRequestDTO dto = new TranslationReportRequestDTO(
				"37afcc60-e8c6-45ea-9fce-17f281e67a56",
				Utilities.convertZonedDateTimeToUTCString(outOfSyncTimestamp),
				"INTERNAL_ERROR",
				"error message ");

		final NormalizedTranslationReportRequestDTO normalized = validator.validateAndNormalizeReport("InterfaceTranslator", dto, "origin");

		assertEquals("InterfaceTranslator", normalized.requester());
		assertEquals(UUID.fromString("37afcc60-e8c6-45ea-9fce-17f281e67a56"), normalized.bridgeId());
		assertTrue(outOfSyncTimestamp.isAfter(normalized.timestamp()));
		assertEquals(TranslationBridgeEventState.INTERNAL_ERROR, normalized.state());
		assertEquals("error message", normalized.errorMessage());
	}
}