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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class NormalizationUtilsTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSnakeCaseToPascalCase() {
		assertAll("snake_case => PascalCase",
				() -> assertNull(NormalizationUtils.convertSnakeCaseToPascalCase(null)),
				() -> assertNull(NormalizationUtils.convertSnakeCaseToPascalCase("")),
				() -> assertEquals("SnakeCase", NormalizationUtils.convertSnakeCaseToPascalCase("snake_case")),
				() -> assertEquals("SnakeCaseURL", NormalizationUtils.convertSnakeCaseToPascalCase("snake_case_URL")),
				() -> assertEquals("SNAKECASE", NormalizationUtils.convertSnakeCaseToPascalCase("SNAKE_CASE")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSnakeCaseToCamelCase() {
		assertAll("snake_case => camelCase",
				() -> assertNull(NormalizationUtils.convertSnakeCaseToCamelCase(null)),
				() -> assertNull(NormalizationUtils.convertSnakeCaseToCamelCase("")),
				() -> assertEquals("snakeCase", NormalizationUtils.convertSnakeCaseToCamelCase("snake_case")),
				() -> assertEquals("snakeCaseURL", NormalizationUtils.convertSnakeCaseToCamelCase("snake_case_URL")),
				() -> assertEquals("sNAKECASE", NormalizationUtils.convertSnakeCaseToCamelCase("SNAKE_CASE")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSnakeCaseToKebabCase() {
		assertAll("snake_case => kebab-case",
				() -> assertNull(NormalizationUtils.convertSnakeCaseToKebabCase(null)),
				() -> assertNull(NormalizationUtils.convertSnakeCaseToKebabCase("")),
				() -> assertEquals("snake-case", NormalizationUtils.convertSnakeCaseToKebabCase("snake_case")),
				() -> assertEquals("snake-case-url", NormalizationUtils.convertSnakeCaseToKebabCase("snake_case_URL")),
				() -> assertEquals("snake-case", NormalizationUtils.convertSnakeCaseToKebabCase("SNAKE_CASE")));
	}
}
