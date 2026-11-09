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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.exception.InvalidParameterException;

public class PasswordAuthenticationMethodInputValidatorTest {

	//=================================================================================================
	// members

	private PasswordAuthenticationMethodInputValidator validator = new PasswordAuthenticationMethodInputValidator();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCredentialsInputNull() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCredentials(null));

		assertEquals("Missing credentials", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCredentialsInputEmpty() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCredentials(Map.of()));

		assertEquals("Missing credentials", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCredentialsNoPassword() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCredentials(Map.of("abc", "def")));

		assertEquals("Missing credentials", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCredentialsNullPassword() {
		final Map<String, String> map = new HashMap<>(1);
		map.put("password", null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCredentials(map));

		assertEquals("Missing or empty password", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCredentialsEmptyPassword() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCredentials(Map.of("password", "")));

		assertEquals("Missing or empty password", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCredentialsTooLongPassword() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateCredentials(Map.of("password", "longlonglonglonglonglonglonglonglonglonglonglonglonglonglongpassword")));

		assertEquals("Password is too long, maximum length is 63 characters", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCredentialsOk() {
		assertDoesNotThrow(() -> validator.validateCredentials(Map.of("password", "123456")));
	}
}