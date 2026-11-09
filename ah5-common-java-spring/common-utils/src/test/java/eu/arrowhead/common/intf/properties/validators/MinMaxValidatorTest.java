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
package eu.arrowhead.common.intf.properties.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.exception.InvalidParameterException;

@SuppressWarnings("checkstyle:MagicNumber")
public class MinMaxValidatorTest {

	//=================================================================================================
	// members

	private MinMaxValidator validator = new MinMaxValidator();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNotNumber() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize("notANumber", "10", "20"));

		assertEquals("Property value should be a number", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNoArgs() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(12, (String[]) null));

		assertEquals("Missing minimum and/or maximum values", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeTooFewArgs() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(12, "10"));

		assertEquals("Missing minimum and/or maximum values", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRealValueInvalidThreshold() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(12., "ten", "20"));

		assertEquals("Invalid minimum and/or maximum values", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRealValueLessThanMin() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(8.2f, "10", "20"));

		assertEquals("Invalid property value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRealValueGreaterThanMax() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(28., "10", "20"));

		assertEquals("Invalid property value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRealValueOk() {
		final double epsilon = 0.000001d;
		final Object result = validator.validateAndNormalize(18., "10", "20");

		assertEquals(18., ((Number) result).doubleValue(), epsilon);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIntegerValueInvalidThreshold() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(12, "ten", "20"));

		assertEquals("Invalid minimum and/or maximum values", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIntegerValueLessThanMin() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(8, "10", "20"));

		assertEquals("Invalid property value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIntegerValueGreaterThanMax() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(28, "10", "20"));

		assertEquals("Invalid property value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIntegerValueOk() {
		final Object result = validator.validateAndNormalize(18, "10", "20");

		assertEquals(18, ((Number) result).longValue());
	}
}