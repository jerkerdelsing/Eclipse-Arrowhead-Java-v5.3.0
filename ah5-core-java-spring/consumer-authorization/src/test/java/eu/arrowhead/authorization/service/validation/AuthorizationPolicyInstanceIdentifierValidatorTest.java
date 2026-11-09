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
package eu.arrowhead.authorization.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;

@ExtendWith(MockitoExtension.class)
public class AuthorizationPolicyInstanceIdentifierValidatorTest {

	//=================================================================================================
	// members

	private static final String MESSAGE_PREFIX = "The specified instance identifier does not match the naming convention: ";

	@InjectMocks
	private AuthorizationPolicyInstanceIdentifierValidator validator;

	@Mock
	private CloudIdentifierValidator cloudIdentifierValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private EventTypeNameValidator eventTypeNameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInstanceIdentifierNullInput() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateInstanceIdentifier(null));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInstanceIdentifierTooLongInput() {
		final String hugeId = "ididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididid"
				+ "ididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididid"
				+ "ididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididid"
				+ "ididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididid"
				+ "ididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididid"
				+ "ididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididididid"
				+ "idididididididididididididididididididididid";

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateInstanceIdentifier(hugeId));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInstanceIdentifierTooShort() {
		final String id = "PR|TestCloud|Company|TemperatureProvider2|SERVICE_DEF";

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateInstanceIdentifier(id));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInstanceIdentifierTooLong() {
		final String id = "PR|TestCloud|Company|TemperatureProvider2|SERVICE_DEF|kelvinInfo|something";

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateInstanceIdentifier(id));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInstanceIdentifierInvalidLevel() {
		final String id = "WTF|TestCloud|Company|TemperatureProvider2|SERVICE_DEF|kelvinInfo";

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateInstanceIdentifier(id));

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInstanceIdentifierInvalidTargetType() {
		final String id = "PR|LOCAL|TemperatureProvider2|SOMETHING|kelvinInfo";

		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("TemperatureProvider2");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateInstanceIdentifier(id));

		verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		verify(systemNameValidator).validateSystemName("TemperatureProvider2");

		assertTrue(ex.getMessage().startsWith(MESSAGE_PREFIX));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInstanceIdentifierLocalOk() {
		final String id = "MGMT|LOCAL|TemperatureProvider2|SERVICE_DEF|kelvinInfo";

		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		doNothing().when(systemNameValidator).validateSystemName("TemperatureProvider2");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("kelvinInfo");

		assertDoesNotThrow(() -> validator.validateInstanceIdentifier(id));

		verify(cloudIdentifierValidator).validateCloudIdentifier("LOCAL");
		verify(systemNameValidator).validateSystemName("TemperatureProvider2");
		verify(serviceDefNameValidator).validateServiceDefinitionName("kelvinInfo");
		verify(eventTypeNameValidator, never()).validateEventTypeName(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateInstanceIdentifierInterOk() {
		final String id = "MGMT|TestCloud|Company|TemperatureProvider2|EVENT_TYPE|testEvent";

		doNothing().when(cloudIdentifierValidator).validateCloudIdentifier("TestCloud|Company");
		doNothing().when(systemNameValidator).validateSystemName("TemperatureProvider2");
		doNothing().when(eventTypeNameValidator).validateEventTypeName("testEvent");

		assertDoesNotThrow(() -> validator.validateInstanceIdentifier(id));

		verify(cloudIdentifierValidator).validateCloudIdentifier("TestCloud|Company");
		verify(systemNameValidator).validateSystemName("TemperatureProvider2");
		verify(serviceDefNameValidator, never()).validateServiceDefinitionName(anyString());
		verify(eventTypeNameValidator).validateEventTypeName("testEvent");
	}
}