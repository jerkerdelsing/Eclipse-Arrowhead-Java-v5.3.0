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

public class ServiceOperationNameValidatorTest {

	//=================================================================================================
	// members

	private static final String MESSAGE_PREFIX = "The specified service operation name does not match the naming convention: ";

	private final ServiceOperationNameValidator validator = new ServiceOperationNameValidator();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceOperationNameNullInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceOperationName(null));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceOperationNameEmptyInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceOperationName(""));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceOperationNameTooLong() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceOperationName("very-very-very-very-very-very-very-very-very-very-very-long-service-operation-name"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceOperationNameUppercase() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceOperationName("MY-OP"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceOperationNameInvalidCharacter() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceOperationName("my-op-is-$uper"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceOperationNameStartsWithNumber() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceOperationName("1st-op"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceOperationNameStartsWithHyphen() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceOperationName("-my-op"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceOperationNameEndsWithHyphen() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceOperationName("my-op-"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceOperationNameOk() {
		assertAll("valid service operation names",
				() -> assertDoesNotThrow(() -> validator.validateServiceOperationName("my-op")),
				() -> assertDoesNotThrow(() -> validator.validateServiceOperationName("my-op-9")),
				() -> assertDoesNotThrow(() -> validator.validateServiceOperationName("myop9")));
	}
}