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
package eu.arrowhead.common.service.validation.name;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.exception.InvalidParameterException;

public class EventTypeValidatorTest {

	//=================================================================================================
	// members

	private static final String MESSAGE_PREFIX = "The specified event type name does not match the naming convention: ";

	private final EventTypeNameValidator validator = new EventTypeNameValidator();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEventTypeNameNullInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateEventTypeName(null));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEventTypeNameEmptyInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateEventTypeName(""));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEventTypeNameTooLong() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateEventTypeName("veryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongEventTypeName"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEventTypeInvalidCharacter() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateEventTypeName("myEventTypeIs$uper"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEventTypeNameWithUnderscore() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateEventTypeName("event_type"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEventTypeNameWithHyphen() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateEventTypeName("event-type"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEvenTypeNameStartsWithUppercase() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateEventTypeName("EventType"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEventTypeNameStartsWithNumber() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateEventTypeName("2ndEventType"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEventTypeNameOk() {
		assertAll("valid event type names",
				() -> assertDoesNotThrow(() -> validator.validateEventTypeName("eventType")),
				() -> assertDoesNotThrow(() -> validator.validateEventTypeName("eventType9")),
				() -> assertDoesNotThrow(() -> validator.validateEventTypeName("eventTypeURLChanged")));
	}
}