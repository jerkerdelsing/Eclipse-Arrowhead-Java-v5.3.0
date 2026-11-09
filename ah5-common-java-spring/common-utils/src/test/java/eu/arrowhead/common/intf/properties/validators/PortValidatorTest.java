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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class PortValidatorTest {

	//=================================================================================================
	// members

	@InjectMocks
	private PortValidator validator;

	@Mock
	private MinMaxValidator minMaxValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNotNumberValue() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize("notANumber"));

		assertEquals("Property value should be an integer", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRealValue() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(10.2));

		assertEquals("Property value should be an integer", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRealValue2() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(10.2f));

		assertEquals("Property value should be an integer", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIntegerValueOk() {
		when(minMaxValidator.validateAndNormalize(42, "1", "65535")).thenReturn(42);

		final Object resultObj = validator.validateAndNormalize(42);

		verify(minMaxValidator).validateAndNormalize(42, "1", "65535");

		assertTrue(resultObj instanceof Integer);
		assertEquals(42, ((Integer) resultObj).intValue());
	}
}