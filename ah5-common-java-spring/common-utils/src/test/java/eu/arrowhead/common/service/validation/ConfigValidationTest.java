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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.normalization.ConfigNormalization;

@ExtendWith(MockitoExtension.class)
public class ConfigValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ConfigValidation validator;

	@Mock
	private ConfigNormalization normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateConfigKeyListNullInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateConfigKeyList(null));

		assertEquals("The list of the requested configuration keys is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateConfigKeyListEmptyInput() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateConfigKeyList(List.of()));

		assertEquals("The list of the requested configuration keys is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateConfigKeyListListContainsNull() {
		final List<String> input = new ArrayList<>(1);
		input.add(null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateConfigKeyList(input));

		assertEquals("The list of the requested configuration keys contains a null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateConfigKeyListListContainsEmpty() {
		final List<String> input = new ArrayList<>(1);
		input.add("");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateConfigKeyList(input));

		assertEquals("The list of the requested configuration keys contains a null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateConfigKeyListOk() {
		assertDoesNotThrow(() -> validator.validateConfigKeyList(List.of("key")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeConfigKeyListOk() {
		when(normalizer.normalizeConfigKeyList(anyList())).thenReturn(List.of("key", "key2"));

		assertDoesNotThrow(() -> validator.validateAndNormalizeConfigKeyList(List.of(" key ", "key2\n")));

		verify(normalizer).normalizeConfigKeyList(anyList());
	}
}