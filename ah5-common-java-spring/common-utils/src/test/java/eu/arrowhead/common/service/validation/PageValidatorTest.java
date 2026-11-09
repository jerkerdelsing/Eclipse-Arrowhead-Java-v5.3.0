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
package eu.arrowhead.common.service.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.PageDTO;

@SuppressWarnings("checkstyle:MagicNumber")
public class PageValidatorTest {

	//=================================================================================================
	// members

	private final PageValidator validator = new PageValidator();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidatePageParameterSizeDefinedPageNot() {
		final PageDTO pageDto = new PageDTO(null, 10, "ASC", "id");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validatePageParameter(pageDto, List.of(), "test"));

		assertEquals("If size parameter is defined then page parameter cannot be undefined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidatePageParameterOverMaxPageSize() {
		ReflectionTestUtils.setField(validator, "maxPageSize", 5);
		final PageDTO pageDto = new PageDTO(0, 10, "ASC", "id");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validatePageParameter(pageDto, List.of(), "test"));

		assertEquals("The page size cannot be larger than 5", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidatePageParameterPageDefinedSizeNot() {
		final PageDTO pageDto = new PageDTO(0, null, "ASC", "id");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validatePageParameter(pageDto, List.of(), "test"));

		assertEquals("If page parameter is defined then size parameter cannot be undefined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidatePageParameterInvalidDirection() {
		ReflectionTestUtils.setField(validator, "maxPageSize", 10);
		final PageDTO pageDto = new PageDTO(0, 5, "left", "id");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validatePageParameter(pageDto, List.of(), "test"));

		assertEquals("Direction is invalid. Only ASC or DESC are allowed", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidatePageParameterInvalidSortField() {
		ReflectionTestUtils.setField(validator, "maxPageSize", 10);
		final PageDTO pageDto = new PageDTO(0, 5, "ASC", "something");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validatePageParameter(pageDto, List.of("id"), "test"));

		assertEquals("Sort field is invalid. Only the following are allowed: [id]", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidatePageParameterOk() {
		ReflectionTestUtils.setField(validator, "maxPageSize", 10);
		final PageDTO pageDto = new PageDTO(0, 5, null, null);
		final PageDTO pageDto2 = new PageDTO(0, 5, "ASC", "id");
		final PageDTO pageDto3 = new PageDTO(null, null, "ASC", "id");

		assertAll("valid page parameters",
				() -> assertDoesNotThrow(() -> validator.validatePageParameter(null, null, null)),
				() -> assertDoesNotThrow(() -> validator.validatePageParameter(pageDto, null, null)),
				() -> assertDoesNotThrow(() -> validator.validatePageParameter(pageDto2, List.of("id"), "test")),
				() -> assertDoesNotThrow(() -> validator.validatePageParameter(pageDto3, List.of("id"), "test")));
	}
}
