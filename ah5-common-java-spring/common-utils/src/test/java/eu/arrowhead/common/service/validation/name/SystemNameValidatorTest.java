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

public class SystemNameValidatorTest {

	//=================================================================================================
	// members

	private static final String MESSAGE_PREFIX = "The specified system name does not match the naming convention: ";

	private final SystemNameValidator validator = new SystemNameValidator();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateSystemNameNullInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateSystemName(null));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateSystemNameEmptyInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateSystemName(""));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateSystemNameTooLong() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateSystemName("VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongSystemName"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateSystemInvalidCharacter() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateSystemName("MySystemIs$uper"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateSystemNameWithUnderscore() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateSystemName("system_name"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateSystemNameWithHyphen() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateSystemName("system-name"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEvenTypeNameStartsWithLowercase() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateSystemName("systemName"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateSystemNameStartsWithNumber() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateSystemName("2ndSystem"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateSystemNameOk() {
		assertAll("valid system names",
				() -> assertDoesNotThrow(() -> validator.validateSystemName("SystemName")),
				() -> assertDoesNotThrow(() -> validator.validateSystemName("SystemName9")),
				() -> assertDoesNotThrow(() -> validator.validateSystemName("SystemWithURL")));
	}
}