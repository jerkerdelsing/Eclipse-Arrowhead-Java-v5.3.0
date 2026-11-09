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
package eu.arrowhead.common.service.validation.cloud;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;

@ExtendWith(MockitoExtension.class)
public class CloudIdentifierValidatorTest {

	//=================================================================================================
	// members

	private static final String MESSAGE_PREFIX = "The specified cloud identifier does not match the naming convention: ";

	@InjectMocks
	private CloudIdentifierValidator validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCloudIdentifierNull() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCloudIdentifier(null));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCloudIdentifierEmpty() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCloudIdentifier(" "));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCloudIdentifierDefaultCloud() {
		assertDoesNotThrow(() -> validator.validateCloudIdentifier("LOCAL"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCloudIdentifierWrongFormat() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCloudIdentifier("CloudName"));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCloudIdentifierWrongFormat2() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCloudIdentifier("CloudName|OrganizationName|Other"));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCloudIdentifierOk() {
		doNothing().when(systemNameValidator).validateSystemName("CloudName");
		doNothing().when(systemNameValidator).validateSystemName("OrganizationName");

		assertDoesNotThrow(() -> validator.validateCloudIdentifier("CloudName|OrganizationName"));

		verify(systemNameValidator, times(2)).validateSystemName(anyString());
	}
}