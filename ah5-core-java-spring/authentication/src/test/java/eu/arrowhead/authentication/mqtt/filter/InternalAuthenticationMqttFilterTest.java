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
package eu.arrowhead.authentication.mqtt.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.MqttRequestTemplate;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class InternalAuthenticationMqttFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private InternalAuthenticationMqttFilter filter;

	@Mock
	private IdentityDbService dbService;

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
	public void testDoFilterNoNeedForTokenCheck() {
		final MqttRequestTemplate template = new MqttRequestTemplate(null, null, null, 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/authentication/identity", "identity-login", template);

		assertDoesNotThrow(() -> filter.doFilter("IDENTITY-TOKEN//token", request));

		verify(dbService, never()).getSessionByToken(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterNoAuthInfo() {
		final MqttRequestTemplate template = new MqttRequestTemplate(null, null, null, 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/authentication/identity/management", "identity-mgmt-create", template);

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilter(null, request));

		assertEquals("No authentication info has been provided", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInvalidAuthInfo1() {
		final MqttRequestTemplate template = new MqttRequestTemplate(null, null, null, 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/authentication/identity/management", "identity-mgmt-create", template);

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilter("IDENTITY-TOKEN|token", request));

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInvalidAuthInfo2() {
		final MqttRequestTemplate template = new MqttRequestTemplate(null, null, null, 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/authentication/identity/management", "identity-mgmt-create", template);

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilter("IDENTITYTOKEN//token", request));

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterMissingToken() {
		final MqttRequestTemplate template = new MqttRequestTemplate(null, null, null, 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/authentication/identity/management", "identity-mgmt-create", template);

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilter("IDENTITY-TOKEN// ", request));

		assertEquals("Missing identity token", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalServerError() {
		final MqttRequestTemplate template = new MqttRequestTemplate(null, null, null, 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/authentication/identity/management", "identity-mgmt-create", template);

		when(dbService.getSessionByToken("token")).thenThrow(InternalServerError.class);

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilter("IDENTITY-TOKEN//token", request));

		assertEquals("Session problem", ex.getMessage());

		verify(dbService).getSessionByToken("token");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterSessionNotFound() {
		final MqttRequestTemplate template = new MqttRequestTemplate(null, null, null, 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/authentication/identity/management", "identity-mgmt-create", template);

		when(dbService.getSessionByToken("token")).thenReturn(Optional.empty());

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilter("IDENTITY-TOKEN//token", request));

		assertEquals("Invalid identity token", ex.getMessage());

		verify(dbService).getSessionByToken("token");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterExpiredToken() {
		final MqttRequestTemplate template = new MqttRequestTemplate(null, null, null, 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/authentication/identity/management", "identity-mgmt-create", template);
		final ZonedDateTime now = Utilities.utcNow();
		final ActiveSession session = new ActiveSession(
				new System("TestSystem", AuthenticationMethod.PASSWORD, true, "AdminSystem"),
				"token",
				now.minusHours(2),
				now.minusHours(1));

		when(dbService.getSessionByToken("token")).thenReturn(Optional.of(session));

		final Throwable ex = assertThrows(
				AuthException.class,
				() -> filter.doFilter("IDENTITY-TOKEN//token", request));

		assertEquals("Invalid identity token", ex.getMessage());

		verify(dbService).getSessionByToken("token");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterOk() {
		final MqttRequestTemplate template = new MqttRequestTemplate(null, null, null, 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/authentication/identity/management", "identity-mgmt-create", template);
		final ZonedDateTime now = Utilities.utcNow();
		final ActiveSession session = new ActiveSession(
				new System("TestSystem", AuthenticationMethod.PASSWORD, true, "AdminSystem"),
				"token",
				now.minusHours(1),
				now.plusHours(1));

		when(dbService.getSessionByToken("token")).thenReturn(Optional.of(session));

		assertDoesNotThrow(() -> filter.doFilter("IDENTITY-TOKEN//token", request));

		assertEquals("TestSystem", request.getRequester());
		assertTrue(request.isSysOp());

		verify(dbService).getSessionByToken("token");
	}
}