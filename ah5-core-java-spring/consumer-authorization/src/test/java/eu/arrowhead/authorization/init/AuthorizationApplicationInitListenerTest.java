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
package eu.arrowhead.authorization.init;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.common.exception.InvalidParameterException;

@ExtendWith(MockitoExtension.class)
public class AuthorizationApplicationInitListenerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationApplicationInitListener listener;

	@Mock
	private AuthorizationSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitKeyNull() {
		when(sysInfo.getSecretCryptographerKey()).thenReturn(null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> listener.customInit(null));

		assertEquals("secret.cryptographer.key property is empty", ex.getMessage());

		verify(sysInfo).getSecretCryptographerKey();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitKeyEmpty() {
		when(sysInfo.getSecretCryptographerKey()).thenReturn("");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> listener.customInit(null));

		assertEquals("secret.cryptographer.key property is empty", ex.getMessage());

		verify(sysInfo).getSecretCryptographerKey();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitKeyShort() {
		when(sysInfo.getSecretCryptographerKey()).thenReturn("short");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> listener.customInit(null));

		assertEquals("secret.cryptographer.key value must be minimum 16 bytes long", ex.getMessage());

		verify(sysInfo, times(2)).getSecretCryptographerKey();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitOk() {
		when(sysInfo.getSecretCryptographerKey()).thenReturn("1234567890123456");

		assertDoesNotThrow(() -> listener.customInit(null));

		verify(sysInfo, times(2)).getSecretCryptographerKey();
	}
}