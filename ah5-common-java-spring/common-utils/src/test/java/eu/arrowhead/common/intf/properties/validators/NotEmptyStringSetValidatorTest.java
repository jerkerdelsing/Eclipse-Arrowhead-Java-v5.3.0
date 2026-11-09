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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameValidator;

@ExtendWith(MockitoExtension.class)
public class NotEmptyStringSetValidatorTest {

	//=================================================================================================
	// members

	@InjectMocks
	private NotEmptyStringSetValidator validator;

	@Mock
	private ServiceOperationNameNormalizer operationNameNormalizer;

	@Mock
	private ServiceOperationNameValidator operationNameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNotACollection() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize("notACollection"));

		assertEquals("Property value should be a set/list of string values", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeEmptyCollection() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(Set.of()));

		assertEquals("Property value should be a non-empty set/list", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNotAStringCollection() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(Set.of(1)));

		assertEquals("Value should be a string", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOperationNameCollectionOk() {
		when(operationNameNormalizer.normalize("operation-name")).thenReturn("operation-name");
		doNothing().when(operationNameValidator).validateServiceOperationName("operation-name");

		final Object resultObj = validator.validateAndNormalize(List.of("operation-name", "operation-name"), "OPERATION");

		verify(operationNameNormalizer, times(2)).normalize("operation-name");
		verify(operationNameValidator, times(2)).validateServiceOperationName("operation-name");

		assertTrue(resultObj instanceof Set);
		@SuppressWarnings("unchecked")
		final Set<String> result = (Set<String>) resultObj;
		assertEquals(1, result.size());
		assertEquals("operation-name", result.iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeEmptyElement() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(Set.of("")));

		assertEquals("Value should be a non-empty string", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGeneralCollectionOk() {
		final Object resultObj = validator.validateAndNormalize(List.of("\tgreen "));

		assertTrue(resultObj instanceof Set);
		@SuppressWarnings("unchecked")
		final Set<String> result = (Set<String>) resultObj;
		assertEquals(1, result.size());
		assertEquals("green", result.iterator().next());
	}
}