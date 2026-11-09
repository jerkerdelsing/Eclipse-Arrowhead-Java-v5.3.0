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

public class InterfaceTemplateNameValidatorTest {

	//=================================================================================================
	// members

	private static final String MESSAGE_PREFIX = "The specified interface template name does not match the naming convention: ";

	private final InterfaceTemplateNameValidator validator = new InterfaceTemplateNameValidator();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInterfaceTemplateNameNullInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateInterfaceTemplateName(null));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInterfaceTemplateNameEmptyInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateInterfaceTemplateName(""));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInterfaceTemplateNameTooLong() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateInterfaceTemplateName("very_very_very_very_very_very_very_very_very_very_very_long_interface_template_name"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInterfaceTemplateNameUppercase() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateInterfaceTemplateName("MY_INTERFACE_TEMPLATE"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInterfaceTemplateNameInvalidCharacter() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateInterfaceTemplateName("my_interface_template_is_$uper"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInterfaceTemplateNameStartsWithNumber() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateInterfaceTemplateName("1st_interface_template"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInterfaceTemplateNameStartsWithUnderscore() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateInterfaceTemplateName("_my_interface_template"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInterfaceTemplateNameEndsWithUnderscore() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateInterfaceTemplateName("my_interface_template_"));
		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInterfaceTemplateNameOk() {
		assertAll("valid interface template names",
				() -> assertDoesNotThrow(() -> validator.validateInterfaceTemplateName("my_interface_template")),
				() -> assertDoesNotThrow(() -> validator.validateInterfaceTemplateName("my_interface_template_9")),
				() -> assertDoesNotThrow(() -> validator.validateInterfaceTemplateName("myinterfacetemplate9")));
	}
}
