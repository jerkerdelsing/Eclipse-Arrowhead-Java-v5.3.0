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
package eu.arrowhead.common.mqtt.filter.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.MqttRequestTemplate;

@ExtendWith(MockitoExtension.class)
public class SelfDeclaredMqttFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SelfDeclaredMqttFilter filter;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testOrder() {
		assertEquals(15, filter.order());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthInfoNull() {
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", null, "response", 0, null, "payload"));

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(null, request));

		assertEquals("No authentication info has been provided", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthInfoEmpty() {
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", "", "response", 0, null, "payload"));

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter("", request));

		assertEquals("No authentication info has been provided", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthInfoTooManyParts() {
		final String authInfo = "SYSTEM//RequesterSystem//other";
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", authInfo, "response", 0, null, "payload"));

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authInfo, request));

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthInfoWrongPrefix() {
		final String authInfo = "SYS//RequesterSystem";
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", authInfo, "response", 0, null, "payload"));

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authInfo, request));

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthInfoOk() {
		final String authInfo = "SYSTEM//RequesterSystem";
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", authInfo, "response", 0, null, "payload"));

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");

		assertDoesNotThrow(() -> filter.doFilter(authInfo, request));

		verify(systemNameNormalizer).normalize("RequesterSystem");
		assertEquals("RequesterSystem", request.getRequester());
		assertFalse(request.isSysOp());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthInfoOkSysop() {
		final String authInfo = "SYSTEM//Sysop";
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", authInfo, "response", 0, null, "payload"));

		when(systemNameNormalizer.normalize("Sysop")).thenReturn("Sysop");

		assertDoesNotThrow(() -> filter.doFilter(authInfo, request));

		verify(systemNameNormalizer).normalize("Sysop");
		assertEquals("Sysop", request.getRequester());
		assertTrue(request.isSysOp());
	}
}