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

public class ServiceDefinitionValidatorTest {

	//=================================================================================================
	// members

	private static final String MESSAGE_PREFIX = "The specified service definition name does not match the naming convention: ";

	private final ServiceDefinitionNameValidator validator = new ServiceDefinitionNameValidator();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceDefinitionNameNullInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceDefinitionName(null));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceDefinitionNameEmptyInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceDefinitionName(""));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceDefinitionNameTooLong() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceDefinitionName("veryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongServiceDefinitionName"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceDefinitionInvalidCharacter() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceDefinitionName("myServiceDefinitionIs$uper"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceDefinitionNameWithUnderscore() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceDefinitionName("service_def"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceDefinitionNameWithHyphen() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceDefinitionName("service-def"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateEvenTypeNameStartsWithUppercase() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceDefinitionName("ServiceDefinition"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceDefinitionNameStartsWithNumber() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceDefinitionName("2ndServiceDefinition"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceDefinitionNameOk() {
		assertAll("valid service definition names",
				() -> assertDoesNotThrow(() -> validator.validateServiceDefinitionName("serviceDefinition")),
				() -> assertDoesNotThrow(() -> validator.validateServiceDefinitionName("serviceDefinition9")),
				() -> assertDoesNotThrow(() -> validator.validateServiceDefinitionName("serviceDefinitionURLUpdate")));
	}
}