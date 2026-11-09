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
package eu.arrowhead.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.security.SecurityUtilities;

@ExtendWith(MockitoExtension.class)
public class AuthenticationSystemInfoTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthenticationSystemInfo sysInfo = new AuthenticationSystemInfo();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitSecretKeyNull() {
		ReflectionTestUtils.setField(sysInfo, "authenticationSecretKey", null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> sysInfo.customInit());

		assertEquals("'authenticationSecretKey' is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitSecretKeyEmpty() {
		ReflectionTestUtils.setField(sysInfo, "authenticationSecretKey", "");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> sysInfo.customInit());

		assertEquals("'authenticationSecretKey' is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitAuthenticationPolicyMismatch() {
		ReflectionTestUtils.setField(sysInfo, "authenticationSecretKey", "secretKey");
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.OUTSOURCED);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> sysInfo.customInit());

		assertEquals("'authenticationPolicy' is invalid: must be internal", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitInvalidKey() {
		ReflectionTestUtils.setField(sysInfo, "authenticationSecretKey", "invalidSecretKey");
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.INTERNAL);

		try (MockedStatic<SecurityUtilities> staticMock = Mockito.mockStatic(SecurityUtilities.class)) {
			staticMock.when(() -> SecurityUtilities.hashWithSecretKey("Authentication", "invalidSecretKey")).thenThrow(new InvalidKeyException("test"));

			final Throwable ex = assertThrows(
					InternalServerError.class,
					() -> sysInfo.customInit());

			assertEquals("test", ex.getMessage());

			staticMock.verify(() -> SecurityUtilities.hashWithSecretKey("Authentication", "invalidSecretKey"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitNoSuchAlgorithm() {
		ReflectionTestUtils.setField(sysInfo, "authenticationSecretKey", "secretKey");
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.INTERNAL);

		try (MockedStatic<SecurityUtilities> staticMock = Mockito.mockStatic(SecurityUtilities.class)) {
			staticMock.when(() -> SecurityUtilities.hashWithSecretKey("Authentication", "secretKey")).thenThrow(new NoSuchAlgorithmException("test"));

			final Throwable ex = assertThrows(
					InternalServerError.class,
					() -> sysInfo.customInit());

			assertEquals("test", ex.getMessage());

			staticMock.verify(() -> SecurityUtilities.hashWithSecretKey("Authentication", "secretKey"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitOk() {
		ReflectionTestUtils.setField(sysInfo, "authenticationSecretKey", "secretKey");
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.INTERNAL);
		ReflectionTestUtils.setField(sysInfo, "specIdentityToken", null);

		try (MockedStatic<SecurityUtilities> staticMock = Mockito.mockStatic(SecurityUtilities.class)) {
			staticMock.when(() -> SecurityUtilities.hashWithSecretKey("Authentication", "secretKey")).thenReturn("specToken");

			assertDoesNotThrow(() -> sysInfo.customInit());

			assertEquals("specToken", ReflectionTestUtils.getField(sysInfo, "specIdentityToken"));

			staticMock.verify(() -> SecurityUtilities.hashWithSecretKey("Authentication", "secretKey"));
		}
	}
}