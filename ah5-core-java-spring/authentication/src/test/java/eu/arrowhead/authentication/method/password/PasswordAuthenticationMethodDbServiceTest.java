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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.repository.PasswordAuthenticationRepository;
import eu.arrowhead.authentication.service.dto.IdentityData;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class PasswordAuthenticationMethodDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private PasswordAuthenticationMethodDbService dbService;

	@Mock
	private PasswordEncoder encoder;

	@Mock
	private PasswordAuthenticationRepository paRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkNullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(null));

		assertEquals("Identity list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkListContainsNull() {
		final List<IdentityData> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(list));

		assertEquals("Identity list contains null value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkSystemNull() {
		final IdentityData data = new IdentityData(null, null, false);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("system is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkCredentialsNull() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IdentityData data = new IdentityData(sys, null, false);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("credentials is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkNoPassword() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IdentityData data = new IdentityData(sys, Map.of(), false);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("password field is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkEmptyPassword() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IdentityData data = new IdentityData(sys, Map.of("password", ""), false);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("password field is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkInternalServerError() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final IdentityData data = new IdentityData(sys, Map.of("password", "123456"), false);
		final PasswordAuthentication pa = new PasswordAuthentication(sys, "encoded");

		when(encoder.encode("123456")).thenReturn("encoded");
		when(paRepository.saveAllAndFlush(List.of(pa))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.createIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("Database operation error", ex.getMessage());

		verify(encoder).encode("123456");
		verify(paRepository).saveAllAndFlush(List.of(pa));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final IdentityData data = new IdentityData(sys, Map.of("password", "123456"), false);
		final PasswordAuthentication pa = new PasswordAuthentication(sys, "encoded");

		when(encoder.encode("123456")).thenReturn("encoded");
		when(paRepository.saveAllAndFlush(List.of(pa))).thenReturn(List.of(pa));

		final List<String> result = dbService.createIdentifiableSystemsInBulk(List.of(data));

		assertNull(result);

		verify(encoder).encode("123456");
		verify(paRepository).saveAllAndFlush(List.of(pa));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkNullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(null));

		assertEquals("Identity list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkListContainsNull() {
		final List<IdentityData> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(list));

		assertEquals("Identity list contains null value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkInternalServerError() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final IdentityData data = new IdentityData(sys, Map.of("password", "123456"), false);

		when(paRepository.findAllBySystemIn(List.of(sys))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.updateIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("Database operation error", ex.getMessage());

		verify(paRepository).findAllBySystemIn(List.of(sys));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkSystemNull() {
		final IdentityData data = new IdentityData(null, Map.of("password", "123456"), false);

		when(paRepository.findAllBySystemIn(List.of())).thenReturn(List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("system is null", ex.getMessage());

		verify(paRepository).findAllBySystemIn(List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkCredentialsNull() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final IdentityData data = new IdentityData(sys, null, false);

		when(paRepository.findAllBySystemIn(List.of(sys))).thenReturn(List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("credentials is null", ex.getMessage());

		verify(paRepository).findAllBySystemIn(List.of(sys));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkNoPassword() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final IdentityData data = new IdentityData(sys, Map.of(), false);

		when(paRepository.findAllBySystemIn(List.of(sys))).thenReturn(List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("password field is missing or empty", ex.getMessage());

		verify(paRepository).findAllBySystemIn(List.of(sys));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkPasswordEmpty() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final IdentityData data = new IdentityData(sys, Map.of("password", ""), false);

		when(paRepository.findAllBySystemIn(List.of(sys))).thenReturn(List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("password field is missing or empty", ex.getMessage());

		verify(paRepository).findAllBySystemIn(List.of(sys));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkNoRecordToUpdate() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final IdentityData data = new IdentityData(sys, Map.of("password", "123456"), false);

		when(paRepository.findAllBySystemIn(List.of(sys))).thenReturn(List.of());

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.updateIdentifiableSystemsInBulk(List.of(data)));

		assertEquals("Credentials for system TestSystem are not found", ex.getMessage());

		verify(paRepository).findAllBySystemIn(List.of(sys));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final IdentityData data = new IdentityData(sys, Map.of("password", "123456"), false);
		final PasswordAuthentication pa = new PasswordAuthentication(sys, "oldEncoded");

		when(paRepository.findAllBySystemIn(List.of(sys))).thenReturn(List.of(pa));
		when(encoder.encode("123456")).thenReturn("newEncoded");
		when(paRepository.saveAllAndFlush(List.of(pa))).thenReturn(List.of(pa));

		final List<String> result = dbService.updateIdentifiableSystemsInBulk(List.of(data));

		assertEquals("newEncoded", pa.getPassword());
		assertNull(result);

		verify(paRepository).findAllBySystemIn(List.of(sys));
		verify(encoder).encode("123456");
		verify(paRepository).saveAllAndFlush(List.of(pa));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkNullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.removeIdentifiableSystemsInBulk(null));

		assertEquals("System list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkListContainsNull() {
		final List<System> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.removeIdentifiableSystemsInBulk(list));

		assertEquals("System list contains null value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkInternalServerError() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		doThrow(RuntimeException.class).when(paRepository).deleteAllBySystemIn(List.of(sys));

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.removeIdentifiableSystemsInBulk(List.of(sys)));

		assertEquals("Database operation error", ex.getMessage());

		verify(paRepository).deleteAllBySystemIn(List.of(sys));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		doNothing().when(paRepository).deleteAllBySystemIn(List.of(sys));
		doNothing().when(paRepository).flush();

		assertDoesNotThrow(() -> dbService.removeIdentifiableSystemsInBulk(List.of(sys)));

		verify(paRepository).deleteAllBySystemIn(List.of(sys));
		verify(paRepository).flush();
	}
}