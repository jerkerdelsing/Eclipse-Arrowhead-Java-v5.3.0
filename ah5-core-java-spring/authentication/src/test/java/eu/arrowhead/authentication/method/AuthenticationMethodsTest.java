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
package eu.arrowhead.authentication.method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.authentication.method.password.PasswordAuthenticationMethod;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class AuthenticationMethodsTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthenticationMethods methods;

	@Mock
	private ApplicationContext appContext;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void test() {
		final PasswordAuthenticationMethod method = new PasswordAuthenticationMethod();

		when(appContext.getBean(PasswordAuthenticationMethod.class)).thenReturn(method);

		ReflectionTestUtils.invokeMethod(methods, "init");
		final IAuthenticationMethod result = methods.method(AuthenticationMethod.PASSWORD);

		assertEquals(method, result);

		verify(appContext).getBean(PasswordAuthenticationMethod.class);
	}
}