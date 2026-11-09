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
package eu.arrowhead.authentication.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.repository.ActiveSessionRepository;
import eu.arrowhead.authentication.jpa.repository.PasswordAuthenticationRepository;
import eu.arrowhead.authentication.jpa.repository.SystemRepository;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.method.IAuthenticationMethodDbService;
import eu.arrowhead.authentication.service.dto.IdentityData;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityQueryRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentitySessionQueryRequestDTO;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class IdentityDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private IdentityDbService dbService;

	@Mock
	private SystemRepository systemRepository;

	@Mock
	private PasswordAuthenticationRepository paRepository;

	@Mock
	private ActiveSessionRepository asRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemByNameNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemByName(null));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemByNameEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemByName(""));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemByNameInternalServerError() {
		when(systemRepository.findByName("TestSystem")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getSystemByName("TestSystem"));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findByName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemByNameOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(systemRepository.findByName("TestSystem")).thenReturn(Optional.of(sys));

		final Optional<System> result = dbService.getSystemByName("TestSystem");

		assertEquals(Optional.of(sys), result);

		verify(systemRepository).findByName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemsByNames(null, false));

		assertEquals("names is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemsByNames(List.of(), false));

		assertEquals("names is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemsByNames(list, false));

		assertEquals("names contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesListContainsEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemsByNames(List.of(""), false));

		assertEquals("names contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesInternalServerError() {
		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getSystemsByNames(List.of("TestSystem"), false));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesStrictFalseOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));

		final List<System> result = dbService.getSystemsByNames(List.of("TestSystem"), false);

		assertEquals(List.of(sys), result);

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesStrictTrueOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));

		final List<System> result = dbService.getSystemsByNames(List.of("TestSystem"), true);

		assertEquals(List.of(sys), result);

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesStrictTrueProblem() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(systemRepository.findAllByNameIn(List.of("TestSystem", "TestSystem2"))).thenReturn(List.of(sys));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.getSystemsByNames(List.of("TestSystem", "TestSystem2"), true));

		assertEquals("The following systems are not found: TestSystem2", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem", "TestSystem2"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordSystemNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.changePassword(null, null));

		assertEquals("system is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordNewPasswordNull() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.changePassword(sys, null));

		assertEquals("newPassword is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordNewPasswordEmpty() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.changePassword(sys, ""));

		assertEquals("newPassword is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordInternalServerError() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(paRepository.findBySystem(sys)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.changePassword(sys, "newEncoded"));

		assertEquals("Database operation error", ex.getMessage());

		verify(paRepository).findBySystem(sys);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordRecordNotFound() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(paRepository.findBySystem(sys)).thenReturn(Optional.empty());

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.changePassword(sys, "newEncoded"));

		assertEquals("Entry for system TestSystem is not found", ex.getMessage());

		verify(paRepository).findBySystem(sys);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final PasswordAuthentication pa = new PasswordAuthentication(sys, "oldEncoded");

		when(paRepository.findBySystem(sys)).thenReturn(Optional.of(pa));
		when(paRepository.saveAndFlush(pa)).thenReturn(pa);

		assertDoesNotThrow(() -> dbService.changePassword(sys, "newEncoded"));

		assertEquals("newEncoded", pa.getPassword());

		verify(paRepository).findBySystem(sys);
		verify(paRepository).saveAndFlush(pa);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkRequesterNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(null, null));

		assertEquals("Requester is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkRequesterEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("", null));

		assertEquals("Requester is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkDtoNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", null));

		assertEquals("Payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkAuthenticationMethodNull() {
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkIdentitiesListNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Identities is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkIdentitiesListEmpty() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Identities is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkIdentitiesListContainsNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<NormalizedIdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, list);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Identities contains null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkInternalServerError1() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO identityDto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(identityDto));

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkSystemNameAlreadyUsed() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO identityDto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityMgmtRequestDTO identityDto2 = new NormalizedIdentityMgmtRequestDTO(null, Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(identityDto, identityDto2));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Identifiable systems with names already exist: TestSystem", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkOkNoExtras() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO identityDto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(identityDto));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethodDbService dbServiceMock = Mockito.mock(IAuthenticationMethodDbService.class);
		final List<IdentityData> identityList = List.of(new IdentityData(sys, Map.of("password", "123456"), false));

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of());
		when(systemRepository.saveAllAndFlush(List.of(sys))).thenReturn(List.of(sys));
		when(methodMock.dbService()).thenReturn(dbServiceMock);
		when(dbServiceMock.createIdentifiableSystemsInBulk(identityList)).thenReturn(null);

		final List<System> result = dbService.createIdentifiableSystemsInBulk("AdminSystem", dto);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(sys, result.get(0));

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(systemRepository).saveAllAndFlush(List.of(sys));
		verify(methodMock).dbService();
		verify(dbServiceMock).createIdentifiableSystemsInBulk(identityList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkInternalServerError2() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO identityDto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(identityDto));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethodDbService dbServiceMock = Mockito.mock(IAuthenticationMethodDbService.class);
		final List<IdentityData> identityList = List.of(new IdentityData(sys, Map.of("password", "123456"), false));

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of());
		when(systemRepository.saveAllAndFlush(List.of(sys))).thenReturn(List.of(sys));
		when(methodMock.dbService()).thenReturn(dbServiceMock);
		when(dbServiceMock.createIdentifiableSystemsInBulk(identityList)).thenReturn(List.of("extra1", "extra2"));
		doNothing().when(dbServiceMock).rollbackCreateIdentifiableSystemsInBulk(identityList);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(systemRepository).saveAllAndFlush(List.of(sys));
		verify(methodMock, times(2)).dbService();
		verify(dbServiceMock).createIdentifiableSystemsInBulk(identityList);
		verify(dbServiceMock).rollbackCreateIdentifiableSystemsInBulk(identityList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkInternalServerError3() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO identityDto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(identityDto));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethodDbService dbServiceMock = Mockito.mock(IAuthenticationMethodDbService.class);
		final List<IdentityData> identityList = List.of(new IdentityData(sys, Map.of("password", "123456"), false));

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of());
		when(systemRepository.saveAllAndFlush(List.of(sys))).thenReturn(List.of(sys)).thenThrow(RuntimeException.class);
		when(methodMock.dbService()).thenReturn(dbServiceMock);
		when(dbServiceMock.createIdentifiableSystemsInBulk(identityList)).thenReturn(List.of("extra"));
		doNothing().when(dbServiceMock).rollbackCreateIdentifiableSystemsInBulk(identityList);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(systemRepository, times(2)).saveAllAndFlush(List.of(sys));
		verify(methodMock, times(2)).dbService();
		verify(dbServiceMock).createIdentifiableSystemsInBulk(identityList);
		verify(dbServiceMock).rollbackCreateIdentifiableSystemsInBulk(identityList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkOkWithExtras() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO identityDto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(identityDto));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethodDbService dbServiceMock = Mockito.mock(IAuthenticationMethodDbService.class);
		final List<IdentityData> identityList = List.of(new IdentityData(sys, Map.of("password", "123456"), false));

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of());
		when(systemRepository.saveAllAndFlush(List.of(sys))).thenReturn(List.of(sys));
		when(methodMock.dbService()).thenReturn(dbServiceMock);
		when(dbServiceMock.createIdentifiableSystemsInBulk(identityList)).thenReturn(List.of("extra"));

		final List<System> result = dbService.createIdentifiableSystemsInBulk("AdminSystem", dto);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(sys, result.get(0));
		assertEquals("extra", result.get(0).getExtra());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(systemRepository, times(2)).saveAllAndFlush(List.of(sys));
		verify(methodMock).dbService();
		verify(dbServiceMock).createIdentifiableSystemsInBulk(identityList);
		verify(dbServiceMock, never()).rollbackCreateIdentifiableSystemsInBulk(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindSystemInListNotFound() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final Object result = ReflectionTestUtils.invokeMethod(dbService, "findSystemInList", List.of(sys), "OtherSystem");

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindSystemInListFound() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final Object result = ReflectionTestUtils.invokeMethod(dbService, "findSystemInList", List.of(sys), "TestSystem");

		assertEquals(sys, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkAuthenticationMethodNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(null, null, null));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkRequesterNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(methodMock, null, null));

		assertEquals("Requester is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkRequesterEmpty() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(methodMock, "", null));

		assertEquals("Requester is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkIdentitiesListNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", null));

		assertEquals("Identities is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkIdentitiesListEmpty() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of()));

		assertEquals("Identities is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkIdentitiesListContainsNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<NormalizedIdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", list));

		assertEquals("Identities contains null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkIdentitiesAuthenticationMethodMismatch() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), true);
		final System sys = new System("TestSystem", null, false, "AdminSystem");

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(methodMock.type()).thenReturn(AuthenticationMethod.PASSWORD);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(dto)));

		assertEquals("Bulk updating systems with different authentication methods is not supported", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(methodMock).type();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkIdentitiesNoExtrasInternalServerError() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), true);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethodDbService dbServiceMock = Mockito.mock(IAuthenticationMethodDbService.class);
		final IdentityData identity = new IdentityData(sys, Map.of("password", "123456"), true);

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(methodMock.type()).thenReturn(AuthenticationMethod.PASSWORD);
		when(methodMock.dbService()).thenReturn(dbServiceMock);
		when(dbServiceMock.updateIdentifiableSystemsInBulk(List.of(identity))).thenReturn(null);
		when(systemRepository.saveAllAndFlush(List.of(sys))).thenThrow(RuntimeException.class);
		doNothing().when(dbServiceMock).rollbackUpdateIdentifiableSystemsInBulk(List.of(identity));

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(dto)));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(methodMock).type();
		verify(methodMock, times(2)).dbService();
		verify(dbServiceMock).updateIdentifiableSystemsInBulk(List.of(identity));
		verify(systemRepository).saveAllAndFlush(List.of(sys));
		verify(dbServiceMock).rollbackUpdateIdentifiableSystemsInBulk(List.of(identity));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkIdentitiesWithExtrasInternalServerError() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), true);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethodDbService dbServiceMock = Mockito.mock(IAuthenticationMethodDbService.class);
		final IdentityData identity = new IdentityData(sys, Map.of("password", "123456"), true);

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(methodMock.type()).thenReturn(AuthenticationMethod.PASSWORD);
		when(methodMock.dbService()).thenReturn(dbServiceMock);
		when(dbServiceMock.updateIdentifiableSystemsInBulk(List.of(identity))).thenReturn(List.of("extra1", "extra2"));
		doNothing().when(dbServiceMock).rollbackUpdateIdentifiableSystemsInBulk(List.of(identity));

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(dto)));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(methodMock).type();
		verify(methodMock, times(2)).dbService();
		verify(dbServiceMock).updateIdentifiableSystemsInBulk(List.of(identity));
		verify(dbServiceMock).rollbackUpdateIdentifiableSystemsInBulk(List.of(identity));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentifiableSystemsInBulkIdentitiesWithExtrasOk() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), true);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethodDbService dbServiceMock = Mockito.mock(IAuthenticationMethodDbService.class);
		final IdentityData identity = new IdentityData(sys, Map.of("password", "123456"), true);

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(methodMock.type()).thenReturn(AuthenticationMethod.PASSWORD);
		when(methodMock.dbService()).thenReturn(dbServiceMock);
		when(dbServiceMock.updateIdentifiableSystemsInBulk(List.of(identity))).thenReturn(List.of("extra"));
		when(systemRepository.saveAllAndFlush(List.of(sys))).thenReturn(List.of(sys));
		doNothing().when(dbServiceMock).commitUpdateIdentifiableSystemsInBulk(List.of(identity));

		final List<System> result = dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem2", List.of(dto));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(sys, result.get(0));
		assertEquals("extra", result.get(0).getExtra());
		assertTrue(result.get(0).isSysop());
		assertEquals("AdminSystem2", result.get(0).getUpdatedBy());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(methodMock).type();
		verify(methodMock, times(2)).dbService();
		verify(dbServiceMock).updateIdentifiableSystemsInBulk(List.of(identity));
		verify(systemRepository).saveAllAndFlush(List.of(sys));
		verify(dbServiceMock).commitUpdateIdentifiableSystemsInBulk(List.of(identity));
		verify(dbServiceMock, never()).rollbackUpdateIdentifiableSystemsInBulk(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkAuthenticationMethodNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.removeIdentifiableSystemsInBulk(null, null));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkNamesListNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.removeIdentifiableSystemsInBulk(methodMock, null));

		assertEquals("Names is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkNamesListEmpty() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.removeIdentifiableSystemsInBulk(methodMock, List.of()));

		assertEquals("Names is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkNamesListContainsNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.removeIdentifiableSystemsInBulk(methodMock, list));

		assertEquals("Names contains null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkNamesListContainsEmpty() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.removeIdentifiableSystemsInBulk(methodMock, List.of("")));

		assertEquals("Names contains null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkAuthenticationMethodMismatch() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final System sys = new System("TestSystem", null, false, "AdminSystem");

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(methodMock.type()).thenReturn(AuthenticationMethod.PASSWORD);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.removeIdentifiableSystemsInBulk(methodMock, List.of("TestSystem")));

		assertEquals("Bulk removing systems with different authentication methods is not supported", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(methodMock).type();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkInternalServerError() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethodDbService dbServiceMock = Mockito.mock(IAuthenticationMethodDbService.class);

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(methodMock.type()).thenReturn(AuthenticationMethod.PASSWORD);
		when(methodMock.dbService()).thenReturn(dbServiceMock);
		doNothing().when(dbServiceMock).removeIdentifiableSystemsInBulk(List.of(sys));
		doThrow(RuntimeException.class).when(systemRepository).deleteAllInBatch(List.of(sys));
		doNothing().when(dbServiceMock).rollbackRemoveIdentifiableSystemsInBulk(List.of(sys));

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.removeIdentifiableSystemsInBulk(methodMock, List.of("TestSystem")));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(methodMock).type();
		verify(methodMock, times(2)).dbService();
		verify(dbServiceMock).removeIdentifiableSystemsInBulk(List.of(sys));
		verify(systemRepository).deleteAllInBatch(List.of(sys));
		verify(dbServiceMock).rollbackRemoveIdentifiableSystemsInBulk(List.of(sys));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveIdentifiableSystemsInBulkOk() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethodDbService dbServiceMock = Mockito.mock(IAuthenticationMethodDbService.class);

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));
		when(methodMock.type()).thenReturn(AuthenticationMethod.PASSWORD);
		when(methodMock.dbService()).thenReturn(dbServiceMock);
		doNothing().when(dbServiceMock).removeIdentifiableSystemsInBulk(List.of(sys));
		doNothing().when(systemRepository).deleteAllInBatch(List.of(sys));
		doNothing().when(dbServiceMock).commitRemoveIdentifiableSystemsInBulk(List.of(sys));

		assertDoesNotThrow(() -> dbService.removeIdentifiableSystemsInBulk(methodMock, List.of("TestSystem")));

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
		verify(methodMock).type();
		verify(methodMock, times(2)).dbService();
		verify(dbServiceMock).removeIdentifiableSystemsInBulk(List.of(sys));
		verify(systemRepository).deleteAllInBatch(List.of(sys));
		verify(dbServiceMock).commitRemoveIdentifiableSystemsInBulk(List.of(sys));
		verify(dbServiceMock, never()).rollbackRemoveIdentifiableSystemsInBulk(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentifiableSystemsDTONull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.queryIdentifiableSystems(null));

		assertEquals("Payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentifiableSystemsPageRequestNull() {
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(null, null, null, null, null, null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.queryIdentifiableSystems(dto));

		assertEquals("Page request is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentifiableSystemsInternalServerError() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(pageRequest, null, null, null, null, null, null);

		when(systemRepository.findAll(pageRequest)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.queryIdentifiableSystems(dto));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentifiableSystemsOkNoFilters() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(pageRequest, null, null, null, null, null, null);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(systemRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(sys)));

		assertDoesNotThrow(() -> dbService.queryIdentifiableSystems(dto));

		verify(systemRepository).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentifiableSystemsOkNameFilterOnly() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(
				pageRequest,
				"System",
				null,
				null,
				null,
				null,
				null);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(systemRepository.findAllByNameContainsIgnoreCase("System")).thenReturn(List.of(sys));
		when(systemRepository.findAllByIdIn(pageRequest, List.of(1L))).thenReturn(new PageImpl<>(List.of(sys)));

		assertDoesNotThrow(() -> dbService.queryIdentifiableSystems(dto));

		verify(systemRepository, never()).findAll();
		verify(systemRepository).findAllByNameContainsIgnoreCase("System");
		verify(systemRepository).findAllByIdIn(pageRequest, List.of(1L));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentifiableSystemsOkSysopFilterOnly() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(
				pageRequest,
				null,
				true,
				null,
				null,
				null,
				null);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(systemRepository.findAll()).thenReturn(List.of(sys));
		when(systemRepository.findAllByIdIn(pageRequest, List.of())).thenReturn(new PageImpl<>(List.of()));

		assertDoesNotThrow(() -> dbService.queryIdentifiableSystems(dto));

		verify(systemRepository).findAll();
		verify(systemRepository, never()).findAllByNameContainsIgnoreCase(anyString());
		verify(systemRepository).findAllByIdIn(pageRequest, List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentifiableSystemsOkMultipleFilters1() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(
				pageRequest,
				null,
				false,
				"OtherSystem",
				null,
				null,
				null);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(systemRepository.findAll()).thenReturn(List.of(sys));
		when(systemRepository.findAllByIdIn(pageRequest, List.of())).thenReturn(new PageImpl<>(List.of()));

		assertDoesNotThrow(() -> dbService.queryIdentifiableSystems(dto));

		verify(systemRepository).findAll();
		verify(systemRepository, never()).findAllByNameContainsIgnoreCase(anyString());
		verify(systemRepository).findAllByIdIn(pageRequest, List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryIdentifiableSystemsOkMultipleFilters2() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(
				pageRequest,
				null,
				null,
				"AdminSystem",
				ZonedDateTime.of(2025, 11, 19, 6, 10, 11, 0, ZoneId.of(Constants.UTC)),
				null,
				null);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		sys.setCreatedAt(ZonedDateTime.of(2025, 11, 19, 5, 10, 11, 0, ZoneId.of(Constants.UTC)));

		when(systemRepository.findAll()).thenReturn(List.of(sys));
		when(systemRepository.findAllByIdIn(pageRequest, List.of())).thenReturn(new PageImpl<>(List.of()));

		assertDoesNotThrow(() -> dbService.queryIdentifiableSystems(dto));

		verify(systemRepository).findAll();
		verify(systemRepository, never()).findAllByNameContainsIgnoreCase(anyString());
		verify(systemRepository).findAllByIdIn(pageRequest, List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryIdentifiableSystemsOkMultipleFilters3() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(
				pageRequest,
				null,
				null,
				null,
				ZonedDateTime.of(2025, 11, 19, 6, 10, 11, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2025, 11, 18, 6, 10, 11, 0, ZoneId.of(Constants.UTC)),
				null);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		sys.setCreatedAt(ZonedDateTime.of(2025, 11, 19, 7, 10, 11, 0, ZoneId.of(Constants.UTC)));

		when(systemRepository.findAll()).thenReturn(List.of(sys));
		when(systemRepository.findAllByIdIn(pageRequest, List.of())).thenReturn(new PageImpl<>(List.of()));

		assertDoesNotThrow(() -> dbService.queryIdentifiableSystems(dto));

		verify(systemRepository).findAll();
		verify(systemRepository, never()).findAllByNameContainsIgnoreCase(anyString());
		verify(systemRepository).findAllByIdIn(pageRequest, List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testQueryIdentifiableSystemsOkMultipleFilters4() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(
				pageRequest,
				null,
				null,
				null,
				null,
				ZonedDateTime.of(2025, 11, 20, 6, 10, 11, 0, ZoneId.of(Constants.UTC)),
				true);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		sys.setCreatedAt(ZonedDateTime.of(2025, 11, 19, 7, 10, 11, 0, ZoneId.of(Constants.UTC)));

		when(systemRepository.findAll()).thenReturn(List.of(sys));
		when(asRepository.findBySystem(sys)).thenReturn(Optional.empty());
		when(systemRepository.findAllByIdIn(pageRequest, List.of())).thenReturn(new PageImpl<>(List.of()));

		assertDoesNotThrow(() -> dbService.queryIdentifiableSystems(dto));

		verify(systemRepository).findAll();
		verify(systemRepository, never()).findAllByNameContainsIgnoreCase(anyString());
		verify(asRepository).findBySystem(sys);
		verify(systemRepository).findAllByIdIn(pageRequest, List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentifiableSystemsOkSessionFilterSessionExpired() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(
				pageRequest,
				null,
				null,
				null,
				null,
				null,
				false);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final ActiveSession session = new ActiveSession(sys, "TOKEN", Utilities.utcNow().minusHours(1), Utilities.utcNow().minusMinutes(1));

		when(systemRepository.findAll()).thenReturn(List.of(sys));
		when(asRepository.findBySystem(sys)).thenReturn(Optional.of(session));
		when(systemRepository.findAllByIdIn(pageRequest, List.of(1L))).thenReturn(new PageImpl<>(List.of(sys)));

		assertDoesNotThrow(() -> dbService.queryIdentifiableSystems(dto));

		verify(systemRepository).findAll();
		verify(systemRepository, never()).findAllByNameContainsIgnoreCase(anyString());
		verify(asRepository).findBySystem(sys);
		verify(systemRepository).findAllByIdIn(pageRequest, List.of(1L));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdentifiableSystemsOkSessionFilterSessionNotExpired() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentityQueryRequestDTO dto = new NormalizedIdentityQueryRequestDTO(
				pageRequest,
				null,
				null,
				null,
				null,
				null,
				true);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final ActiveSession session = new ActiveSession(sys, "TOKEN", Utilities.utcNow(), Utilities.utcNow().plusHours(1));

		when(systemRepository.findAll()).thenReturn(List.of(sys));
		when(asRepository.findBySystem(sys)).thenReturn(Optional.of(session));
		when(systemRepository.findAllByIdIn(pageRequest, List.of(1L))).thenReturn(new PageImpl<>(List.of(sys)));

		assertDoesNotThrow(() -> dbService.queryIdentifiableSystems(dto));

		verify(systemRepository).findAll();
		verify(systemRepository, never()).findAllByNameContainsIgnoreCase(anyString());
		verify(asRepository).findBySystem(sys);
		verify(systemRepository).findAllByIdIn(pageRequest, List.of(1L));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSessionByTokenNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSessionByToken(null));

		assertEquals("Token is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSessionByTokenEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSessionByToken(""));

		assertEquals("Token is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSessionByTokenInternalServerError() {
		when(asRepository.findByToken("token")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getSessionByToken("token"));

		assertEquals("Database operation error", ex.getMessage());

		verify(asRepository).findByToken("token");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSessionByTokenOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final ActiveSession session = new ActiveSession(sys, "TOKEN", Utilities.utcNow(), Utilities.utcNow().plusHours(1));

		when(asRepository.findByToken("token")).thenReturn(Optional.of(session));

		assertDoesNotThrow(() -> dbService.getSessionByToken("token"));

		verify(asRepository).findByToken("token");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsDTONull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.querySessions(null));

		assertEquals("Payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsPageRequestNull() {
		final NormalizedIdentitySessionQueryRequestDTO dto = new NormalizedIdentitySessionQueryRequestDTO(null, null, null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.querySessions(dto));

		assertEquals("Page request is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsInternalServerError() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentitySessionQueryRequestDTO dto = new NormalizedIdentitySessionQueryRequestDTO(pageRequest, null, null, null);

		when(asRepository.findAll(pageRequest)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.querySessions(dto));

		assertEquals("Database operation error", ex.getMessage());

		verify(asRepository).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsOkNoFilters() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentitySessionQueryRequestDTO dto = new NormalizedIdentitySessionQueryRequestDTO(pageRequest, null, null, null);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final ActiveSession session = new ActiveSession(sys, "TOKEN", Utilities.utcNow(), Utilities.utcNow().plusHours(1));
		session.setId(1);

		when(asRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(session)));

		assertDoesNotThrow(() -> dbService.querySessions(dto));

		verify(asRepository).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsOkNameFilterOnly() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentitySessionQueryRequestDTO dto = new NormalizedIdentitySessionQueryRequestDTO(
				pageRequest,
				"System",
				null,
				null);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final ActiveSession session = new ActiveSession(sys, "TOKEN", Utilities.utcNow(), Utilities.utcNow().plusHours(1));
		session.setId(1);

		when(asRepository.findAllBySystem_NameContainsIgnoreCase("System")).thenReturn(List.of(session));
		when(asRepository.findAllByIdIn(pageRequest, List.of(1L))).thenReturn(new PageImpl<>(List.of(session)));

		assertDoesNotThrow(() -> dbService.querySessions(dto));

		verify(asRepository).findAllBySystem_NameContainsIgnoreCase("System");
		verify(asRepository, never()).findAll();
		verify(asRepository).findAllByIdIn(pageRequest, List.of(1L));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsOkLoginFromFilterOnly() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentitySessionQueryRequestDTO dto = new NormalizedIdentitySessionQueryRequestDTO(
				pageRequest,
				null,
				ZonedDateTime.of(2025, 11, 19, 7, 10, 11, 0, ZoneId.of(Constants.UTC)),
				null);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final ActiveSession session = new ActiveSession(
				sys,
				"TOKEN",
				ZonedDateTime.of(2025, 11, 19, 6, 10, 11, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2025, 11, 19, 6, 40, 11, 0, ZoneId.of(Constants.UTC)));
		session.setId(1);

		when(asRepository.findAll()).thenReturn(List.of(session));
		when(asRepository.findAllByIdIn(pageRequest, List.of())).thenReturn(new PageImpl<>(List.of()));

		assertDoesNotThrow(() -> dbService.querySessions(dto));

		verify(asRepository).findAll();
		verify(asRepository, never()).findAllBySystem_NameContainsIgnoreCase(anyString());
		verify(asRepository).findAllByIdIn(pageRequest, List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsOkMultipleFilters() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentitySessionQueryRequestDTO dto = new NormalizedIdentitySessionQueryRequestDTO(
				pageRequest,
				null,
				ZonedDateTime.of(2025, 11, 19, 7, 10, 11, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2025, 11, 18, 7, 10, 11, 0, ZoneId.of(Constants.UTC)));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final ActiveSession session = new ActiveSession(
				sys,
				"TOKEN",
				ZonedDateTime.of(2025, 11, 19, 8, 10, 11, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2025, 11, 19, 8, 40, 11, 0, ZoneId.of(Constants.UTC)));
		session.setId(1);

		when(asRepository.findAll()).thenReturn(List.of(session));
		when(asRepository.findAllByIdIn(pageRequest, List.of())).thenReturn(new PageImpl<>(List.of()));

		assertDoesNotThrow(() -> dbService.querySessions(dto));

		verify(asRepository).findAll();
		verify(asRepository, never()).findAllBySystem_NameContainsIgnoreCase(anyString());
		verify(asRepository).findAllByIdIn(pageRequest, List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySessionsOkLoginToFilterOnly() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedIdentitySessionQueryRequestDTO dto = new NormalizedIdentitySessionQueryRequestDTO(
				pageRequest,
				null,
				null,
				ZonedDateTime.of(2025, 11, 20, 7, 10, 11, 0, ZoneId.of(Constants.UTC)));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final ActiveSession session = new ActiveSession(
				sys,
				"TOKEN",
				ZonedDateTime.of(2025, 11, 19, 8, 10, 11, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2025, 11, 19, 8, 40, 11, 0, ZoneId.of(Constants.UTC)));
		session.setId(1);

		when(asRepository.findAll()).thenReturn(List.of(session));
		when(asRepository.findAllByIdIn(pageRequest, List.of(1L))).thenReturn(new PageImpl<>(List.of(session)));

		assertDoesNotThrow(() -> dbService.querySessions(dto));

		verify(asRepository).findAll();
		verify(asRepository, never()).findAllBySystem_NameContainsIgnoreCase(anyString());
		verify(asRepository).findAllByIdIn(pageRequest, List.of(1L));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOrUpdateSessionSystemNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createOrUpdateSession(null, null));

		assertEquals("system is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOrUpdateSessionTokenNull() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createOrUpdateSession(sys, null));

		assertEquals("Token is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOrUpdateSessionTokenEmpty() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createOrUpdateSession(sys, ""));

		assertEquals("Token is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOrUpdateSessionInternalServerError() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(asRepository.findBySystem(sys)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.createOrUpdateSession(sys, "token"));

		assertEquals("Database operation error", ex.getMessage());

		verify(asRepository).findBySystem(sys);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOrUpdateSessionNewSessionWithInfiniteToken() {
		ReflectionTestUtils.setField(dbService, "identityTokenDuration", 0);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final ZonedDateTime now = ZonedDateTime.of(2025, 11, 20, 12, 7, 1, 0, ZoneId.of(Constants.UTC));
		final ActiveSession session = new ActiveSession(sys, "token", now, now.plusYears(100));

		when(asRepository.findBySystem(sys)).thenReturn(Optional.empty());

		try (MockedStatic<Utilities> staticMock = Mockito.mockStatic(Utilities.class)) {
			staticMock.when(() -> Utilities.utcNow()).thenReturn(now);
			when(asRepository.saveAndFlush(session)).thenReturn(session);

			assertDoesNotThrow(() -> dbService.createOrUpdateSession(sys, "token"));

			verify(asRepository).findBySystem(sys);
			staticMock.verify(() -> Utilities.utcNow());
			verify(asRepository).saveAndFlush(session);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateOrUpdateSessionExtendSessionWithFiniteToken() {
		ReflectionTestUtils.setField(dbService, "identityTokenDuration", 300);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final ZonedDateTime now = ZonedDateTime.of(2025, 11, 20, 12, 7, 1, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime oldLogin = ZonedDateTime.of(2025, 11, 20, 12, 4, 1, 0, ZoneId.of(Constants.UTC));
		final ActiveSession session = new ActiveSession(
				sys,
				"oldToken",
				oldLogin,
				ZonedDateTime.of(2025, 11, 20, 12, 9, 1, 0, ZoneId.of(Constants.UTC)));

		when(asRepository.findBySystem(sys)).thenReturn(Optional.of(session));

		try (MockedStatic<Utilities> staticMock = Mockito.mockStatic(Utilities.class)) {
			staticMock.when(() -> Utilities.utcNow()).thenReturn(now);
			when(asRepository.saveAndFlush(session)).thenReturn(session);

			assertDoesNotThrow(() -> dbService.createOrUpdateSession(sys, "token"));

			assertEquals("token", session.getToken());
			assertEquals(oldLogin, session.getLoginTime());
			assertEquals(ZonedDateTime.of(2025, 11, 20, 12, 12, 1, 0, ZoneId.of(Constants.UTC)), session.getExpirationTime());

			verify(asRepository).findBySystem(sys);
			staticMock.verify(() -> Utilities.utcNow());
			verify(asRepository).saveAndFlush(session);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateOrUpdateSessionReplaceSessionWithFiniteToken() {
		ReflectionTestUtils.setField(dbService, "identityTokenDuration", 300);
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final ZonedDateTime now = ZonedDateTime.of(2025, 11, 20, 12, 7, 1, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime oldLogin = ZonedDateTime.of(2025, 11, 20, 12, 1, 1, 0, ZoneId.of(Constants.UTC));
		final ActiveSession session = new ActiveSession(
				sys,
				"oldToken",
				oldLogin,
				ZonedDateTime.of(2025, 11, 20, 12, 6, 1, 0, ZoneId.of(Constants.UTC)));

		when(asRepository.findBySystem(sys)).thenReturn(Optional.of(session));

		try (MockedStatic<Utilities> staticMock = Mockito.mockStatic(Utilities.class)) {
			staticMock.when(() -> Utilities.utcNow()).thenReturn(now);
			when(asRepository.saveAndFlush(session)).thenReturn(session);

			assertDoesNotThrow(() -> dbService.createOrUpdateSession(sys, "token"));

			assertEquals("token", session.getToken());
			assertEquals(now, session.getLoginTime());
			assertEquals(ZonedDateTime.of(2025, 11, 20, 12, 12, 1, 0, ZoneId.of(Constants.UTC)), session.getExpirationTime());

			verify(asRepository).findBySystem(sys);
			staticMock.verify(() -> Utilities.utcNow());
			verify(asRepository).saveAndFlush(session);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSessionSystemNameNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.removeSession(null));

		assertEquals("System name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSessionSystemNameEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.removeSession(""));

		assertEquals("System name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSessionInternalServerError() {
		doThrow(RuntimeException.class).when(asRepository).deleteBySystem_Name("TestSystem");

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.removeSession("TestSystem"));

		assertEquals("Database operation error", ex.getMessage());

		verify(asRepository).deleteBySystem_Name("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSessionOk() {
		doNothing().when(asRepository).deleteBySystem_Name("TestSystem");
		doNothing().when(asRepository).flush();

		assertDoesNotThrow(() -> dbService.removeSession("TestSystem"));

		verify(asRepository).deleteBySystem_Name("TestSystem");
		verify(asRepository).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsInBulkListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.closeSessionsInBulk(null));

		assertEquals("System name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsInBulkListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.closeSessionsInBulk(List.of()));

		assertEquals("System name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsInBulkListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.closeSessionsInBulk(list));

		assertEquals("System name list contains null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsInBulkListContainsEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.closeSessionsInBulk(List.of("")));

		assertEquals("System name list contains null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsInBulkInternalServerError() {
		doThrow(RuntimeException.class).when(asRepository).deleteBySystem_NameIn(List.of("TestSystem"));

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.closeSessionsInBulk(List.of("TestSystem")));

		assertEquals("Database operation error", ex.getMessage());

		verify(asRepository).deleteBySystem_NameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCloseSessionsInBulkOk() {
		doNothing().when(asRepository).deleteBySystem_NameIn(List.of("TestSystem"));
		doNothing().when(asRepository).flush();

		assertDoesNotThrow(() -> dbService.closeSessionsInBulk(List.of("TestSystem")));

		verify(asRepository).deleteBySystem_NameIn(List.of("TestSystem"));
		verify(asRepository).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveExpiredSessionsInternalServerError() {
		final ZonedDateTime now = ZonedDateTime.of(2025, 11, 20, 12, 7, 1, 0, ZoneId.of(Constants.UTC));

		try (MockedStatic<Utilities> staticMock = Mockito.mockStatic(Utilities.class)) {
			staticMock.when(() -> Utilities.utcNow()).thenReturn(now);
			doThrow(RuntimeException.class).when(asRepository).deleteByExpirationTimeLessThan(now);

			final Throwable ex = assertThrows(
					InternalServerError.class,
					() -> dbService.removeExpiredSessions());

			assertEquals("Database operation error", ex.getMessage());

			staticMock.verify(() -> Utilities.utcNow());
			verify(asRepository).deleteByExpirationTimeLessThan(now);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveExpiredSessionsOk() {
		final ZonedDateTime now = ZonedDateTime.of(2025, 11, 20, 12, 7, 1, 0, ZoneId.of(Constants.UTC));

		try (MockedStatic<Utilities> staticMock = Mockito.mockStatic(Utilities.class)) {
			staticMock.when(() -> Utilities.utcNow()).thenReturn(now);
			doNothing().when(asRepository).deleteByExpirationTimeLessThan(now);
			doNothing().when(asRepository).flush();

			assertDoesNotThrow(() -> dbService.removeExpiredSessions());

			staticMock.verify(() -> Utilities.utcNow());
			verify(asRepository).deleteByExpirationTimeLessThan(now);
			verify(asRepository).flush();
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPasswordAuthenticationBySystemNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getPasswordAuthenticationBySystem(null));

		assertEquals("system is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPasswordAuthenticationBySystemInternalServerError() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(paRepository.findBySystem(sys)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getPasswordAuthenticationBySystem(sys));

		assertEquals("Database operation error", ex.getMessage());

		verify(paRepository).findBySystem(sys);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPasswordAuthenticationBySystemOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final PasswordAuthentication pa = new PasswordAuthentication(sys, "encoded");

		when(paRepository.findBySystem(sys)).thenReturn(Optional.of(pa));

		assertDoesNotThrow(() -> dbService.getPasswordAuthenticationBySystem(sys));

		verify(paRepository).findBySystem(sys);
	}
}