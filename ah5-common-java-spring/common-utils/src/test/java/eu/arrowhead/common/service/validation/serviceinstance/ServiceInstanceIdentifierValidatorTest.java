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
package eu.arrowhead.common.service.validation.serviceinstance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.version.VersionValidator;

@ExtendWith(MockitoExtension.class)
public class ServiceInstanceIdentifierValidatorTest {

	//=================================================================================================
	// members

	private static final String MESSAGE_PREFIX = "The specified service instance identifier does not match the naming convention: ";

	@InjectMocks
	private ServiceInstanceIdentifierValidator validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private VersionValidator versionValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceInstanceIdentifierNull() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceInstanceIdentifier(null));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceInstanceIdentifierEmpty() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceInstanceIdentifier(""));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceInstanceIdentifierWrongFormat() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateServiceInstanceIdentifier("SystemName|serviceDef"));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateServiceInstanceIdentifierOk() {
		doNothing().when(systemNameValidator).validateSystemName("SystemName");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("serviceDef");
		doNothing().when(versionValidator).validateNormalizedVersion("1.2.0");

		assertDoesNotThrow(() -> validator.validateServiceInstanceIdentifier("SystemName|serviceDef|1.2.0"));

		verify(systemNameValidator).validateSystemName(anyString());
		verify(serviceDefNameValidator).validateServiceDefinitionName(anyString());
		verify(versionValidator).validateNormalizedVersion(anyString());
	}
}