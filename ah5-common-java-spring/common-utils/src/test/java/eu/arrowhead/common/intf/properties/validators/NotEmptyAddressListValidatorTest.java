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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.dto.enums.AddressType;

@ExtendWith(MockitoExtension.class)
public class NotEmptyAddressListValidatorTest {

	//=================================================================================================
	// members

	@InjectMocks
	private NotEmptyAddressListValidator validator;

	@Mock
	private AddressNormalizer addressNormalizer;

	@Mock
	private AddressValidator addressValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNotAList() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize("notAList"));

		assertEquals("Property value should be a list of strings", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeEmptyList() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(List.of()));

		assertEquals("Property value should be a non-empty list of strings", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNotAStringList() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(List.of(1)));

		assertEquals("Property value should be a list of strings", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeEmptyListValue() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(List.of("")));

		assertEquals("Property value should be a list of non-blank strings", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeInvalidAddressValue() {
		when(addressNormalizer.normalize("00:1A:2B:3C:4D:5E")).thenReturn("00:1A:2B:3C:4D:5E");
		when(addressValidator.detectType("00:1A:2B:3C:4D:5E")).thenReturn(AddressType.MAC);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(List.of("00:1A:2B:3C:4D:5E")));

		verify(addressNormalizer).normalize("00:1A:2B:3C:4D:5E");
		verify(addressValidator).detectType("00:1A:2B:3C:4D:5E");
		verify(addressValidator, never()).validateNormalizedAddress(AddressType.MAC, "00:1A:2B:3C:4D:5E");

		assertEquals("Unacceptable address type in property value: MAC", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOk() {
		when(addressNormalizer.normalize("192.168.0.1")).thenReturn("192.168.0.1");
		when(addressValidator.detectType("192.168.0.1")).thenReturn(AddressType.IPV4);
		doNothing().when(addressValidator).validateNormalizedAddress(AddressType.IPV4, "192.168.0.1");

		final Object resultObj = validator.validateAndNormalize(List.of("192.168.0.1   "));

		verify(addressNormalizer).normalize("192.168.0.1");
		verify(addressValidator).detectType("192.168.0.1");
		verify(addressValidator).validateNormalizedAddress(AddressType.IPV4, "192.168.0.1");

		assertTrue(resultObj instanceof List);
		@SuppressWarnings("unchecked")
		final List<String> result = (List<String>) resultObj;
		assertEquals(1, result.size());
		assertEquals("192.168.0.1", result.get(0));
	}
}