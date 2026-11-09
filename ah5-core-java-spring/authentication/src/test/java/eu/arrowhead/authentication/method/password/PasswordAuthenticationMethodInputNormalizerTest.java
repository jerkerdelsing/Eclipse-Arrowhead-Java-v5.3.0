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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class PasswordAuthenticationMethodInputNormalizerTest {

	//=================================================================================================
	// members

	private PasswordAuthenticationMethodInputNormalizer normalizer = new PasswordAuthenticationMethodInputNormalizer();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCredentialsWrongInputs() {
		assertAll(
				() -> assertNull(normalizer.normalizeCredentials(null)),
				() -> assertEquals(Map.of(), normalizer.normalizeCredentials(Map.of())),
				() -> assertEquals(Map.of("password", ""), normalizer.normalizeCredentials(Map.of("password", ""))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCredentialsOk() {
		assertEquals(Map.of("password", "123456"), normalizer.normalizeCredentials(Map.of("password", "123456 ")));
	}
}