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
package eu.arrowhead.authentication.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.DTOConverter;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityQueryRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentitySessionQueryRequestDTO;
import eu.arrowhead.authentication.service.validation.ManagementValidation;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtResponseDTO;
import eu.arrowhead.dto.IdentityListMgmtUpdateRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.IdentityMgmtResponseDTO;
import eu.arrowhead.dto.IdentitySessionListMgmtResponseDTO;
import eu.arrowhead.dto.IdentitySessionResponseDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class ManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementService service;

	@Mock
	private ManagementValidation validator;

	@Mock
	private IdentityDbService dbService;

	@Mock
	private DTOConverter converter;

	@Mock
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createIdentitiesOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createIdentitiesOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationInvalidParameterException() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtCreateRequestDTO list = new IdentityListMgmtCreateRequestDTO("PASSWORD", List.of(dto));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO normalizedList = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(normalized));

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeCreateIdentityList(list, "origin")).thenReturn(normalizedList);
		when(dbService.createIdentifiableSystemsInBulk("AdminSystem", normalizedList)).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.createIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeCreateIdentityList(list, "origin");
		verify(dbService).createIdentifiableSystemsInBulk("AdminSystem", normalizedList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationInternalServerError() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtCreateRequestDTO list = new IdentityListMgmtCreateRequestDTO("PASSWORD", List.of(dto));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO normalizedList = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(normalized));

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeCreateIdentityList(list, "origin")).thenReturn(normalizedList);
		when(dbService.createIdentifiableSystemsInBulk("AdminSystem", normalizedList)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.createIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeCreateIdentityList(list, "origin");
		verify(dbService).createIdentifiableSystemsInBulk("AdminSystem", normalizedList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationExternalServerError() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtCreateRequestDTO list = new IdentityListMgmtCreateRequestDTO("PASSWORD", List.of(dto));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO normalizedList = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(normalized));

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeCreateIdentityList(list, "origin")).thenReturn(normalizedList);
		when(dbService.createIdentifiableSystemsInBulk("AdminSystem", normalizedList)).thenThrow(new ExternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> service.createIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeCreateIdentityList(list, "origin");
		verify(dbService).createIdentifiableSystemsInBulk("AdminSystem", normalizedList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationOk() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtCreateRequestDTO list = new IdentityListMgmtCreateRequestDTO("PASSWORD", List.of(dto));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO normalizedList = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(normalized));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		system.setId(1);
		final ZonedDateTime time = ZonedDateTime.of(2025, 11, 14, 15, 7, 12, 0, ZoneId.of(Constants.UTC));
		system.setCreatedAt(time);
		system.setUpdatedAt(time);
		final IdentityMgmtResponseDTO responseDto = new IdentityMgmtResponseDTO("TestSystem", "PASSWORD", false, "AdminSystem", "2025-11-14T15:07:12Z", "AdminSystem", "2025-11-14T15:07:12Z");
		final IdentityListMgmtResponseDTO response = new IdentityListMgmtResponseDTO(List.of(responseDto), 1);

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeCreateIdentityList(list, "origin")).thenReturn(normalizedList);
		when(dbService.createIdentifiableSystemsInBulk("AdminSystem", normalizedList)).thenReturn(List.of(system));
		when(converter.convertIdentifiableSystemListToDTO(List.of(system))).thenReturn(response);

		assertDoesNotThrow(() -> service.createIdentitiesOperation("AdminSystem", list, "origin"));

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeCreateIdentityList(list, "origin");
		verify(dbService).createIdentifiableSystemsInBulk("AdminSystem", normalizedList);
		verify(converter).convertIdentifiableSystemListToDTO(List.of(system));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateIdentitiesOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateIdentitiesOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationInternalServerError1() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationDifferentAuthenticationMethods() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final System system2 = new System("TestSystem2", null, false, "AdminSystem");

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenReturn(List.of(system, system2));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("Bulk updating systems with different authentication method is not supported", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationAuthenticationMethodUnsupported() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenReturn(List.of(system));
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("Authentication method is unsupported: PASSWORD", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationInvalidParameterException() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenReturn(List.of(system));
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin")).thenReturn(List.of(normalized));
		when(dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized))).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(validator).validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin");
		verify(dbService).updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationInternalServerError() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenReturn(List.of(system));
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin")).thenReturn(List.of(normalized));
		when(dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized))).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(validator).validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin");
		verify(dbService).updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationExternalServerError() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenReturn(List.of(system));
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin")).thenReturn(List.of(normalized));
		when(dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized))).thenThrow(new ExternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(validator).validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin");
		verify(dbService).updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testUpdateIdentitiesOperationOk() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		system.setId(1);
		system.setCreatedAt(ZonedDateTime.of(2025, 11, 17, 10, 10, 5, 0, ZoneId.of(Constants.UTC)));
		system.setUpdatedBy("AdminSystem");
		system.setUpdatedAt(ZonedDateTime.of(2025, 11, 17, 12, 12, 5, 0, ZoneId.of(Constants.UTC)));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IdentityMgmtResponseDTO responseDTO = new IdentityMgmtResponseDTO(
				"TestSystem",
				"PASSWORD",
				false,
				"AdminSystem",
				"2025-11-17T10:10:05Z",
				"AdminSystem",
				"2025-11-17T12:12:05Z");
		final IdentityListMgmtResponseDTO response = new IdentityListMgmtResponseDTO(List.of(responseDTO), 1);

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenReturn(List.of(system));
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin")).thenReturn(List.of(normalized));
		when(dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized))).thenReturn(List.of(system));
		when(converter.convertIdentifiableSystemListToDTO(List.of(system))).thenReturn(response);

		assertDoesNotThrow(() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(validator).validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin");
		verify(dbService).updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized));
		verify(converter).convertIdentifiableSystemListToDTO(List.of(system));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentitiesOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeIdentitiesOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentitiesOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeIdentitiesOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentitiesOperationInternalServerError() {
		when(validator.validateAndNormalizeRemoveIdentities(List.of("TestSystem"), "origin")).thenReturn(List.of("TestSystem"));
		when(dbService.getSystemsByNames(List.of("TestSystem"), false)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.removeIdentitiesOperation(List.of("TestSystem"), "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRemoveIdentities(List.of("TestSystem"), "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), false);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentitiesOperationUnknownSystems() {
		when(validator.validateAndNormalizeRemoveIdentities(List.of("TestSystem"), "origin")).thenReturn(List.of("TestSystem"));
		when(dbService.getSystemsByNames(List.of("TestSystem"), false)).thenReturn(List.of());

		assertDoesNotThrow(() -> service.removeIdentitiesOperation(List.of("TestSystem"), "origin"));

		verify(validator).validateAndNormalizeRemoveIdentities(List.of("TestSystem"), "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), false);
		verify(methods, never()).method(any(AuthenticationMethod.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentitiesOperationDifferentAuthenticationMethods() {
		final System s1 = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final System s2 = new System("TestSystem2", null, false, "AdminSystem");

		when(validator.validateAndNormalizeRemoveIdentities(List.of("TestSystem", "TestSystem2"), "origin")).thenReturn(List.of("TestSystem", "TestSystem2"));
		when(dbService.getSystemsByNames(List.of("TestSystem", "TestSystem2"), false)).thenReturn(List.of(s1, s2));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.removeIdentitiesOperation(List.of("TestSystem", "TestSystem2"), "origin"));

		assertEquals("Bulk removing systems with different authentication method is not supported", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRemoveIdentities(List.of("TestSystem", "TestSystem2"), "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem", "TestSystem2"), false);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentitiesOperationExternalServerError() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(validator.validateAndNormalizeRemoveIdentities(List.of("TestSystem"), "origin")).thenReturn(List.of("TestSystem"));
		when(dbService.getSystemsByNames(List.of("TestSystem"), false)).thenReturn(List.of(sys));
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		doThrow(new ExternalServerError("test")).when(dbService).removeIdentifiableSystemsInBulk(methodMock, List.of("TestSystem"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> service.removeIdentitiesOperation(List.of("TestSystem"), "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRemoveIdentities(List.of("TestSystem"), "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), false);
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(dbService).removeIdentifiableSystemsInBulk(methodMock, List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentitiesOperationOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(validator.validateAndNormalizeRemoveIdentities(List.of("TestSystem"), "origin")).thenReturn(List.of("TestSystem"));
		when(dbService.getSystemsByNames(List.of("TestSystem"), false)).thenReturn(List.of(sys));
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		doNothing().when(dbService).removeIdentifiableSystemsInBulk(methodMock, List.of("TestSystem"));

		assertDoesNotThrow(() -> service.removeIdentitiesOperation(List.of("TestSystem"), "origin"));

		verify(validator).validateAndNormalizeRemoveIdentities(List.of("TestSystem"), "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), false);
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(dbService).removeIdentifiableSystemsInBulk(methodMock, List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentitiesOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryIdentitiesOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentitiesOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryIdentitiesOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentitiesOperationInternalServerError() {
		final NormalizedIdentityQueryRequestDTO normalized = new NormalizedIdentityQueryRequestDTO(
				PageRequest.of(0, 1000, Direction.ASC, System.DEFAULT_SORT_FIELD),
				null,
				null,
				null,
				null,
				null,
				null);

		when(validator.validateAndNormalizeIdentityQueryRequest(null, "origin")).thenReturn(normalized);
		when(dbService.queryIdentifiableSystems(normalized)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.queryIdentitiesOperation(null, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeIdentityQueryRequest(null, "origin");
		verify(dbService).queryIdentifiableSystems(normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryIdentitiesOperationOk() {
		final NormalizedIdentityQueryRequestDTO normalized = new NormalizedIdentityQueryRequestDTO(
				PageRequest.of(0, 1000, Direction.ASC, System.DEFAULT_SORT_FIELD),
				null,
				null,
				null,
				null,
				null,
				null);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		system.setId(1);
		system.setCreatedAt(ZonedDateTime.of(2025, 11, 17, 10, 10, 5, 0, ZoneId.of(Constants.UTC)));
		system.setUpdatedBy("AdminSystem");
		system.setUpdatedAt(ZonedDateTime.of(2025, 11, 17, 12, 12, 5, 0, ZoneId.of(Constants.UTC)));
		final IdentityMgmtResponseDTO responseDTO = new IdentityMgmtResponseDTO(
				"TestSystem",
				"PASSWORD",
				false,
				"AdminSystem",
				"2025-11-17T10:10:05Z",
				"AdminSystem",
				"2025-11-17T12:12:05Z");
		final IdentityListMgmtResponseDTO response = new IdentityListMgmtResponseDTO(List.of(responseDTO), 1);

		when(validator.validateAndNormalizeIdentityQueryRequest(null, "origin")).thenReturn(normalized);
		when(dbService.queryIdentifiableSystems(normalized)).thenReturn(new PageImpl<>(List.of(system)));
		when(converter.convertIdentifiableSystemListToDTO(new PageImpl<>(List.of(system)))).thenReturn(response);

		assertDoesNotThrow(() -> service.queryIdentitiesOperation(null, "origin"));

		verify(validator).validateAndNormalizeIdentityQueryRequest(null, "origin");
		verify(dbService).queryIdentifiableSystems(normalized);
		verify(converter).convertIdentifiableSystemListToDTO(new PageImpl<>(List.of(system)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.closeSessionsOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.closeSessionsOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsOperationInternalServerError() {
		when(validator.validateAndNormalizeCloseSessions(List.of("TestSystem"), "origin")).thenReturn(List.of("TestSystem"));
		doThrow(new InternalServerError("test")).when(dbService).closeSessionsInBulk(List.of("TestSystem"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.closeSessionsOperation(List.of("TestSystem"), "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeCloseSessions(List.of("TestSystem"), "origin");
		verify(dbService).closeSessionsInBulk(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsOperationOk() {
		when(validator.validateAndNormalizeCloseSessions(List.of("TestSystem"), "origin")).thenReturn(List.of("TestSystem"));
		doNothing().when(dbService).closeSessionsInBulk(List.of("TestSystem"));

		assertDoesNotThrow(() -> service.closeSessionsOperation(List.of("TestSystem"), "origin"));

		verify(validator).validateAndNormalizeCloseSessions(List.of("TestSystem"), "origin");
		verify(dbService).closeSessionsInBulk(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.querySessionsOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.querySessionsOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsOperationInternalServerError() {
		final NormalizedIdentitySessionQueryRequestDTO normalized = new NormalizedIdentitySessionQueryRequestDTO(
				PageRequest.of(0, 1000, Direction.ASC, ActiveSession.DEFAULT_SORT_FIELD),
				null,
				null,
				null);

		when(validator.validateAndNormalizeSessionQueryRequest(null, "origin")).thenReturn(normalized);
		when(dbService.querySessions(normalized)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.querySessionsOperation(null, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSessionQueryRequest(null, "origin");
		verify(dbService).querySessions(normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsOperationOk() {
		final NormalizedIdentitySessionQueryRequestDTO normalized = new NormalizedIdentitySessionQueryRequestDTO(
				PageRequest.of(0, 1000, Direction.ASC, ActiveSession.DEFAULT_SORT_FIELD),
				null,
				null,
				null);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		system.setId(1);
		final ActiveSession session = new ActiveSession(
				system,
				"token",
				ZonedDateTime.of(2025, 11, 18, 10, 10, 5, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2025, 11, 18, 10, 10, 10, 0, ZoneId.of(Constants.UTC)));
		final IdentitySessionResponseDTO responseDTO = new IdentitySessionResponseDTO("TestSystem", "2025-11-18T10:10:05Z", "2025-11-18T10:10:05Z");
		final IdentitySessionListMgmtResponseDTO response = new IdentitySessionListMgmtResponseDTO(List.of(responseDTO), 1);

		when(validator.validateAndNormalizeSessionQueryRequest(null, "origin")).thenReturn(normalized);
		when(dbService.querySessions(normalized)).thenReturn(new PageImpl<>(List.of(session)));
		when(converter.convertSessionListToDTO(new PageImpl<>(List.of(session)))).thenReturn(response);

		assertDoesNotThrow(() -> service.querySessionsOperation(null, "origin"));

		verify(validator).validateAndNormalizeSessionQueryRequest(null, "origin");
		verify(dbService).querySessions(normalized);
		verify(converter).convertSessionListToDTO(new PageImpl<>(List.of(session)));
	}
}