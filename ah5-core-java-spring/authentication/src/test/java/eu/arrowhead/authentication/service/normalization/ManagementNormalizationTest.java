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
package eu.arrowhead.authentication.service.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.method.IAuthenticationMethodInputNormalizer;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityQueryRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentitySessionQueryRequestDTO;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtUpdateRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.IdentityQueryRequestDTO;
import eu.arrowhead.dto.IdentitySessionQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class ManagementNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementNormalization normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private PageService pageService;

	@Mock
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListDTONull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(null));

		assertEquals("Payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListNullList() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListEmptyList() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListListContainsNull() {
		final List<IdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, list);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Identities list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListAuthenticationMethodNull() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, List.of(request));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Authentication method is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListAuthenticationMethodEmpty() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("", List.of(request));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Authentication method is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListAuthenticationMethodInvalid() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("invalid", List.of(request));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Authentication method is invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListAuthenticationMethodUnsupported() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(request));

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Authentication method is unsupported", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListOkWithNotDefinedSysop() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), null);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(request));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodInputNormalizer normalizerMock = Mockito.mock(IAuthenticationMethodInputNormalizer.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(methodMock.normalizer()).thenReturn(normalizerMock);
		when(normalizerMock.normalizeCredentials(Map.of("key", "value"))).thenReturn(Map.of("key", "value"));

		final NormalizedIdentityListMgmtRequestDTO result = normalizer.normalizeCreateIdentityList(dto);

		assertNotNull(result.identities());
		assertEquals(1, result.identities().size());
		final NormalizedIdentityMgmtRequestDTO normalized = result.identities().get(0);
		assertEquals("TestProvider", normalized.systemName());
		assertEquals(Map.of("key", "value"), normalized.credentials());
		assertFalse(normalized.sysop());

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(methodMock).normalizer();
		verify(normalizerMock).normalizeCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListOkWithDefinedSysop() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), true);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(request));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodInputNormalizer normalizerMock = Mockito.mock(IAuthenticationMethodInputNormalizer.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");
		when(methodMock.normalizer()).thenReturn(normalizerMock);
		when(normalizerMock.normalizeCredentials(Map.of("key", "value"))).thenReturn(Map.of("key", "value"));

		final NormalizedIdentityListMgmtRequestDTO result = normalizer.normalizeCreateIdentityList(dto);

		assertNotNull(result.identities());
		assertEquals(1, result.identities().size());
		final NormalizedIdentityMgmtRequestDTO normalized = result.identities().get(0);
		assertEquals("TestProvider", normalized.systemName());
		assertEquals(Map.of("key", "value"), normalized.credentials());
		assertTrue(normalized.sysop());

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(systemNameNormalizer).normalize("TestProvider");
		verify(methodMock).normalizer();
		verify(normalizerMock).normalizeCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeUpdateIdentityListWithoutCredentialsDTONull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeUpdateIdentityListWithoutCredentials(null));

		assertEquals("Payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeUpdateIdentityListWithoutCredentialsNullList() {
		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeUpdateIdentityListWithoutCredentials(dto));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeUpdateIdentityListWithoutCredentialsEmptyList() {
		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeUpdateIdentityListWithoutCredentials(dto));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeUpdateIdentityListWithoutCredentialsListContainsNull() {
		final List<IdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(list);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeUpdateIdentityListWithoutCredentials(dto));

		assertEquals("Identities list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeUpdateIdentityListWithoutCredentialsOkWithNotDefinedSysop() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), null);
		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(List.of(request));

		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");

		final List<NormalizedIdentityMgmtRequestDTO> result = normalizer.normalizeUpdateIdentityListWithoutCredentials(dto);

		assertNotNull(result);
		assertEquals(1, result.size());
		final NormalizedIdentityMgmtRequestDTO normalized = result.get(0);
		assertEquals("TestProvider", normalized.systemName());
		assertEquals(Map.of("key", "value"), normalized.credentials());
		assertFalse(normalized.sysop());

		verify(systemNameNormalizer).normalize("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeUpdateIdentityListWithoutCredentialsOkWithDefinedSysop() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(List.of(request));

		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");

		final List<NormalizedIdentityMgmtRequestDTO> result = normalizer.normalizeUpdateIdentityListWithoutCredentials(dto);

		assertNotNull(result);
		assertEquals(1, result.size());
		final NormalizedIdentityMgmtRequestDTO normalized = result.get(0);
		assertEquals("TestProvider", normalized.systemName());
		assertEquals(Map.of("key", "value"), normalized.credentials());
		assertFalse(normalized.sysop());

		verify(systemNameNormalizer).normalize("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCredentialsAuthenticationMethodNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCredentials(null, null));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCredentialsListNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCredentials(methodMock, null));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCredentialsListEmpty() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCredentials(methodMock, List.of()));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCredentialsListContainsNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<NormalizedIdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCredentials(methodMock, list));

		assertEquals("Identities list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCredentialsOk() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final IAuthenticationMethodInputNormalizer normalizerMock = Mockito.mock(IAuthenticationMethodInputNormalizer.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);

		when(methodMock.normalizer()).thenReturn(normalizerMock);
		when(normalizerMock.normalizeCredentials(Map.of("key", "value"))).thenReturn(Map.of("key", "value"));

		final List<NormalizedIdentityMgmtRequestDTO> result = normalizer.normalizeCredentials(methodMock, List.of(dto));

		assertNotNull(result);
		assertEquals(1, result.size());
		final NormalizedIdentityMgmtRequestDTO normalized = result.get(0);
		assertEquals("TestProvider", normalized.systemName());
		assertEquals(Map.of("key", "value"), normalized.credentials());
		assertFalse(normalized.sysop());

		verify(methodMock).normalizer();
		verify(normalizerMock).normalizeCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeIdentifiableSystemNamesNullNames() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeIdentifiableSystemNames(null));

		assertEquals("name list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeIdentifiableSystemNamesOk() {
		when(systemNameNormalizer.normalize("test-provider")).thenReturn("TestProvider");

		final List<String> result = normalizer.normalizeIdentifiableSystemNames(List.of("test-provider"));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("TestProvider", result.get(0));

		verify(systemNameNormalizer).normalize("test-provider");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeIdentityQueryRequestNullInput() {
		when(pageService.getPageRequest(null, System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(PageRequest.of(0, 10));

		final NormalizedIdentityQueryRequestDTO result = normalizer.normalizeIdentityQueryRequest(null);

		assertNotNull(result);
		assertNotNull(result.pageRequest());
		assertFalse(result.hasFilters());

		verify(pageService).getPageRequest(null, System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, "does not matter");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeIdentityQueryRequestOk1() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentityQueryRequestDTO dto = new IdentityQueryRequestDTO(page, null, false, null, null, null, true);

		when(pageService.getPageRequest(page, System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(PageRequest.of(0, 10));

		final NormalizedIdentityQueryRequestDTO result = normalizer.normalizeIdentityQueryRequest(dto);

		assertNotNull(result);
		assertNotNull(result.pageRequest());
		assertNull(result.namePart());
		assertFalse(result.isSysop());
		assertNull(result.createdBy());
		assertNull(result.creationFrom());
		assertNull(result.creationTo());
		assertTrue(result.hasSession());

		verify(pageService).getPageRequest(page, System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, "does not matter");
		verify(systemNameNormalizer, never()).normalize(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeIdentityQueryRequestOk2() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final String from = "2025-11-05T10:00:01Z";
		final String to = "2025-11-06T10:00:01Z";
		final IdentityQueryRequestDTO dto = new IdentityQueryRequestDTO(page, "Provider ", true, "admin-system", from, to, false);

		when(pageService.getPageRequest(page, System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(PageRequest.of(0, 10));
		when(systemNameNormalizer.normalize("admin-system")).thenReturn("AdminSystem");

		final NormalizedIdentityQueryRequestDTO result = normalizer.normalizeIdentityQueryRequest(dto);

		assertNotNull(result);
		assertNotNull(result.pageRequest());
		assertEquals("Provider", result.namePart());
		assertTrue(result.isSysop());
		assertEquals("AdminSystem", result.createdBy());
		assertEquals(Utilities.parseUTCStringToZonedDateTime(from), result.creationFrom());
		assertEquals(Utilities.parseUTCStringToZonedDateTime(to), result.creationTo());
		assertFalse(result.hasSession());

		verify(pageService).getPageRequest(page, System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, "does not matter");
		verify(systemNameNormalizer).normalize("admin-system");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeSessionQueryRequestNullInput() {
		when(pageService.getPageRequest(null, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(PageRequest.of(0, 10));

		final NormalizedIdentitySessionQueryRequestDTO result = normalizer.normalizeSessionQueryRequest(null);

		assertNotNull(result);
		assertNotNull(result.pageRequest());
		assertFalse(result.hasFilters());

		verify(pageService).getPageRequest(null, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeSessionQueryRequestEverythingNull() {
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(null, null, null, null);

		when(pageService.getPageRequest(null, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(PageRequest.of(0, 10));

		final NormalizedIdentitySessionQueryRequestDTO result = normalizer.normalizeSessionQueryRequest(dto);

		assertNotNull(result);
		assertNotNull(result.pageRequest());
		assertFalse(result.hasFilters());

		verify(pageService).getPageRequest(null, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeSessionQueryRequestOk1() {
		final PageDTO page = new PageDTO(0, 10, "ASC", null);
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(page, "Provider ", null, null);

		when(pageService.getPageRequest(page, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(PageRequest.of(0, 10));

		final NormalizedIdentitySessionQueryRequestDTO result = normalizer.normalizeSessionQueryRequest(dto);

		assertNotNull(result);
		assertNotNull(result.pageRequest());
		assertEquals("Provider", result.namePart());
		assertNull(result.loginFrom());
		assertNull(result.loginTo());

		verify(pageService).getPageRequest(page, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeSessionQueryRequestOk2() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "loginTime");
		final String from = "2025-11-05T10:00:01Z";
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(page, "Provider ", from, null);

		when(pageService.getPageRequest(page, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(PageRequest.of(0, 10));

		final NormalizedIdentitySessionQueryRequestDTO result = normalizer.normalizeSessionQueryRequest(dto);

		assertNotNull(result);
		assertNotNull(result.pageRequest());
		assertEquals("Provider", result.namePart());
		assertEquals(Utilities.parseUTCStringToZonedDateTime(from), result.loginFrom());
		assertNull(result.loginTo());

		verify(pageService).getPageRequest(page, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeSessionQueryRequestOk3() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final PageDTO modifiedPage = new PageDTO(0, 10, "ASC", "system_name");
		final String from = "2025-11-05T10:00:01Z";
		final String to = "2025-11-06T10:00:01Z";
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(page, "Provider ", from, to);

		when(pageService.getPageRequest(modifiedPage, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter")).thenReturn(PageRequest.of(0, 10, Direction.ASC, "system_name"));

		final NormalizedIdentitySessionQueryRequestDTO result = normalizer.normalizeSessionQueryRequest(dto);

		assertNotNull(result);
		assertNotNull(result.pageRequest());
		assertEquals("Provider", result.namePart());
		assertEquals(Utilities.parseUTCStringToZonedDateTime(from), result.loginFrom());
		assertEquals(Utilities.parseUTCStringToZonedDateTime(to), result.loginTo());

		verify(pageService).getPageRequest(modifiedPage, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter");
	}
}