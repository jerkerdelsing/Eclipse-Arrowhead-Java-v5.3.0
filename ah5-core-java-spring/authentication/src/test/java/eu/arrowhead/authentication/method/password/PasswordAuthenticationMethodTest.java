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
package eu.arrowhead.authentication.method.password;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class PasswordAuthenticationMethodTest {

	//=================================================================================================
	// members

	@InjectMocks
	private PasswordAuthenticationMethod method;

	@Mock
	private PasswordAuthenticationMethodInputValidator validator;

	@Mock
	private PasswordAuthenticationMethodInputNormalizer normalizer;

	@Mock
	private PasswordAuthenticationMethodService service;

	@Mock
	private PasswordAuthenticationMethodDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAll() {
		assertAll(
				() -> assertEquals(validator, method.validator()),
				() -> assertEquals(normalizer, method.normalizer()),
				() -> assertEquals(service, method.service()),
				() -> assertEquals(dbService, method.dbService()),
				() -> assertEquals(AuthenticationMethod.PASSWORD, method.type()));
	}
}