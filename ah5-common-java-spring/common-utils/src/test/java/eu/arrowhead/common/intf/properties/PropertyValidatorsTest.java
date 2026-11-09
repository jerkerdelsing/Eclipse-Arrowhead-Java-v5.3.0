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
package eu.arrowhead.common.intf.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.intf.properties.validators.HttpOperationsValidator;
import eu.arrowhead.common.intf.properties.validators.MinMaxValidator;
import eu.arrowhead.common.intf.properties.validators.NotEmptyAddressListValidator;
import eu.arrowhead.common.intf.properties.validators.NotEmptyStringSetValidator;
import eu.arrowhead.common.intf.properties.validators.PortValidator;

@ExtendWith(MockitoExtension.class)
public class PropertyValidatorsTest {

	//=================================================================================================
	// members

	@InjectMocks
	private PropertyValidators validators;

	@Mock
	private ApplicationContext appContext;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testGetValidator() {
		final Map<PropertyValidatorType, IPropertyValidator> map = (Map<PropertyValidatorType, IPropertyValidator>) ReflectionTestUtils.getField(validators, "validators");
		map.put(PropertyValidatorType.PORT, new PortValidator());

		final IPropertyValidator validator1 = validators.getValidator(PropertyValidatorType.MINMAX);
		assertNull(validator1);

		final IPropertyValidator validator2 = validators.getValidator(PropertyValidatorType.PORT);
		assertTrue(validator2 instanceof PortValidator);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:MagicNumber", "unchecked" })
	@Test
	public void testInit() {
		final Map<PropertyValidatorType, IPropertyValidator> map = (Map<PropertyValidatorType, IPropertyValidator>) ReflectionTestUtils.getField(validators, "validators");

		assertTrue(map.isEmpty());

		when(appContext.getBean(MinMaxValidator.class)).thenReturn(new MinMaxValidator());
		when(appContext.getBean(PortValidator.class)).thenReturn(new PortValidator());
		when(appContext.getBean(NotEmptyAddressListValidator.class)).thenReturn(new NotEmptyAddressListValidator());
		when(appContext.getBean(NotEmptyStringSetValidator.class)).thenReturn(new NotEmptyStringSetValidator());
		when(appContext.getBean(HttpOperationsValidator.class)).thenReturn(new HttpOperationsValidator());

		ReflectionTestUtils.invokeMethod(validators, "init");

		verify(appContext).getBean(MinMaxValidator.class);
		verify(appContext).getBean(PortValidator.class);
		verify(appContext).getBean(NotEmptyAddressListValidator.class);
		verify(appContext).getBean(NotEmptyStringSetValidator.class);
		verify(appContext).getBean(HttpOperationsValidator.class);

		assertEquals(5, map.size());
		assertTrue(map.containsKey(PropertyValidatorType.MINMAX));
		assertTrue(map.get(PropertyValidatorType.MINMAX) instanceof MinMaxValidator);
		assertTrue(map.containsKey(PropertyValidatorType.PORT));
		assertTrue(map.get(PropertyValidatorType.PORT) instanceof PortValidator);
		assertTrue(map.containsKey(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST));
		assertTrue(map.get(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST) instanceof NotEmptyAddressListValidator);
		assertTrue(map.containsKey(PropertyValidatorType.NOT_EMPTY_STRING_SET));
		assertTrue(map.get(PropertyValidatorType.NOT_EMPTY_STRING_SET) instanceof NotEmptyStringSetValidator);
		assertTrue(map.containsKey(PropertyValidatorType.HTTP_OPERATIONS));
		assertTrue(map.get(PropertyValidatorType.HTTP_OPERATIONS) instanceof HttpOperationsValidator);
	}
}