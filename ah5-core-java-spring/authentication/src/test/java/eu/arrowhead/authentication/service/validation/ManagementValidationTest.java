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
package eu.arrowhead.authentication.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.method.IAuthenticationMethodInputValidator;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityQueryRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentitySessionQueryRequestDTO;
import eu.arrowhead.authentication.service.normalization.ManagementNormalization;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtUpdateRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.IdentityQueryRequestDTO;
import eu.arrowhead.dto.IdentitySessionQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class ManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementValidation validator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ManagementNormalization normalizer;

	@Mock
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterNullRequester() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester(null, "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterEmptyRequester() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("", "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterInvalidParameterException() {
		when(systemNameNormalizer.normalize("1Requester")).thenReturn("1Requester");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1Requester");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("1Requester", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("1Requester");
		verify(systemNameValidator).validateSystemName("1Requester");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOk() {
		when(systemNameNormalizer.normalize("Requester")).thenReturn("Requester");
		doNothing().when(systemNameValidator).validateSystemName("Requester");

		final String result = validator.validateAndNormalizeRequester("Requester", "origin");

		assertEquals("Requester", result);

		verify(systemNameNormalizer).normalize("Requester");
		verify(systemNameValidator).validateSystemName("Requester");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListDTONull() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(null, "origin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListAuthenticationMethodNull() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListAuthenticationMethodEmpty() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(" ", null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListAuthenticationMethodInvalid() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("Invalid", null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Authentication method is invalid: Invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListAuthenticationMethodUnsupported() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", null);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Authentication method is unsupported: password", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListIdentityListNull() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", null);

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Identity list is missing or empty", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListIdentityListEmpty() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of());

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Identity list is missing or empty", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListIdentityListConstainsNullElement() {
		final List<IdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", list);

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Identity list contains null element", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListSystemNameNull() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO(null, null, null);

		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(identity));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("System name is missing or empty", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListSystemNameEmpty() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO(" ", null, null);

		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(identity));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("System name is missing or empty", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListDuplicates() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO("TestProvider", Map.of(), false);
		final IdentityMgmtRequestDTO identity2 = new IdentityMgmtRequestDTO("TestProvider", Map.of(), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(identity, identity2));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<NormalizedIdentityMgmtRequestDTO> normalizedList = List.of(
				new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of(), false),
				new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of(), false));
		final NormalizedIdentityListMgmtRequestDTO normalized = new NormalizedIdentityListMgmtRequestDTO(methodMock, normalizedList);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(normalizer.normalizeCreateIdentityList(dto)).thenReturn(normalized);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Duplicated system name: TestProvider", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(normalizer).normalizeCreateIdentityList(dto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListInvalidParameterException() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO("1TestProvider", Map.of(), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(identity));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<NormalizedIdentityMgmtRequestDTO> normalizedList = List.of(
				new NormalizedIdentityMgmtRequestDTO("1TestProvider", Map.of(), false));
		final NormalizedIdentityListMgmtRequestDTO normalized = new NormalizedIdentityListMgmtRequestDTO(methodMock, normalizedList);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(normalizer.normalizeCreateIdentityList(dto)).thenReturn(normalized);
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1TestProvider");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(normalizer).normalizeCreateIdentityList(dto);
		verify(systemNameValidator).validateSystemName("1TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListInternalServerError() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(identity));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<NormalizedIdentityMgmtRequestDTO> normalizedList = List.of(
				new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false));
		final NormalizedIdentityListMgmtRequestDTO normalized = new NormalizedIdentityListMgmtRequestDTO(methodMock, normalizedList);
		final IAuthenticationMethodInputValidator validatorMock = Mockito.mock(IAuthenticationMethodInputValidator.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(normalizer.normalizeCreateIdentityList(dto)).thenReturn(normalized);
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		when(methodMock.validator()).thenReturn(validatorMock);
		doThrow(new InternalServerError("test")).when(validatorMock).validateCredentials(Map.of("key", "value"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(normalizer).normalizeCreateIdentityList(dto);
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(methodMock).validator();
		verify(validatorMock).validateCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListOk() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(identity));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<NormalizedIdentityMgmtRequestDTO> normalizedList = List.of(
				new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false));
		final NormalizedIdentityListMgmtRequestDTO normalized = new NormalizedIdentityListMgmtRequestDTO(methodMock, normalizedList);
		final IAuthenticationMethodInputValidator validatorMock = Mockito.mock(IAuthenticationMethodInputValidator.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(normalizer.normalizeCreateIdentityList(dto)).thenReturn(normalized);
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");
		when(methodMock.validator()).thenReturn(validatorMock);
		doNothing().when(validatorMock).validateCredentials(Map.of("key", "value"));

		final NormalizedIdentityListMgmtRequestDTO result = validator.validateAndNormalizeCreateIdentityList(dto, "origin");

		assertEquals(normalized, result);

		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(normalizer).normalizeCreateIdentityList(dto);
		verify(systemNameValidator).validateSystemName("TestProvider");
		verify(methodMock).validator();
		verify(validatorMock).validateCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1OriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1OriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1DTONull() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(null, "origin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1IdentityListNull() {
		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(dto, "origin"));

		assertEquals("Identity list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1IdentityListEmpty() {
		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(List.of());

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(dto, "origin"));

		assertEquals("Identity list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1IdentityListConstainsNullElement() {
		final List<IdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(list);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(dto, "origin"));

		assertEquals("Identity list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1SystemNameNull() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO(null, null, null);

		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(List.of(identity));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(dto, "origin"));

		assertEquals("System name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1SystemNameEmpty() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO(" ", null, null);

		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(List.of(identity));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(dto, "origin"));

		assertEquals("System name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1Duplicates() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO("TestProvider", Map.of(), false);
		final IdentityMgmtRequestDTO identity2 = new IdentityMgmtRequestDTO("TestProvider", Map.of(), false);
		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(List.of(identity, identity2));
		final List<NormalizedIdentityMgmtRequestDTO> normalizedList = List.of(
				new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of(), false),
				new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of(), false));

		when(normalizer.normalizeUpdateIdentityListWithoutCredentials(dto)).thenReturn(normalizedList);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(dto, "origin"));

		assertEquals("Duplicated system name: TestProvider", ex.getMessage());

		verify(normalizer).normalizeUpdateIdentityListWithoutCredentials(dto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1InvalidParameterException() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO("1TestProvider", Map.of(), false);
		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(List.of(identity));
		final List<NormalizedIdentityMgmtRequestDTO> normalizedList = List.of(
				new NormalizedIdentityMgmtRequestDTO("1TestProvider", Map.of(), false));

		when(normalizer.normalizeUpdateIdentityListWithoutCredentials(dto)).thenReturn(normalizedList);
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1TestProvider");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase1(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(normalizer).normalizeUpdateIdentityListWithoutCredentials(dto);
		verify(systemNameValidator).validateSystemName("1TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase1Ok() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO("TestProvider", Map.of(), false);
		final IdentityListMgmtUpdateRequestDTO dto = new IdentityListMgmtUpdateRequestDTO(List.of(identity));
		final List<NormalizedIdentityMgmtRequestDTO> normalizedList = List.of(
				new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of(), false));

		when(normalizer.normalizeUpdateIdentityListWithoutCredentials(dto)).thenReturn(normalizedList);
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");

		final List<NormalizedIdentityMgmtRequestDTO> resultList = validator.validateAndNormalizeUpdateIdentityListPhase1(dto, "origin");

		assertEquals(normalizedList, resultList);

		verify(normalizer).normalizeUpdateIdentityListWithoutCredentials(dto);
		verify(systemNameValidator).validateSystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase2AuthenticationMethodNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase2(null, null, null));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase2ListNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, null, null));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase2ListEmpty() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(), null));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase2ListContainsNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<NormalizedIdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, list, null));

		assertEquals("Identities list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase2OriginNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO(null, null, false);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(dto), null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase2OriginEmpty() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO(null, null, false);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(dto), ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase2InvalidParameterException() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IAuthenticationMethodInputValidator validatorMock = Mockito.mock(IAuthenticationMethodInputValidator.class);

		when(normalizer.normalizeCredentials(methodMock, List.of(dto))).thenReturn(List.of(dto));
		when(methodMock.validator()).thenReturn(validatorMock);
		doThrow(new InvalidParameterException("test")).when(validatorMock).validateCredentials(Map.of("key", "value"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(dto), "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(normalizer).normalizeCredentials(methodMock, List.of(dto));
		verify(methodMock).validator();
		verify(validatorMock).validateCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase2InternalServerError() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IAuthenticationMethodInputValidator validatorMock = Mockito.mock(IAuthenticationMethodInputValidator.class);

		when(normalizer.normalizeCredentials(methodMock, List.of(dto))).thenReturn(List.of(dto));
		when(methodMock.validator()).thenReturn(validatorMock);
		doThrow(new InternalServerError("test")).when(validatorMock).validateCredentials(Map.of("key", "value"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(dto), "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(normalizer).normalizeCredentials(methodMock, List.of(dto));
		verify(methodMock).validator();
		verify(validatorMock).validateCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateIdentityListPhase2Ok() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO dto = new NormalizedIdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IAuthenticationMethodInputValidator validatorMock = Mockito.mock(IAuthenticationMethodInputValidator.class);

		when(normalizer.normalizeCredentials(methodMock, List.of(dto))).thenReturn(List.of(dto));
		when(methodMock.validator()).thenReturn(validatorMock);
		doNothing().when(validatorMock).validateCredentials(Map.of("key", "value"));

		final List<NormalizedIdentityMgmtRequestDTO> result = validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(dto), "origin");

		assertEquals(List.of(dto), result);

		verify(normalizer).normalizeCredentials(methodMock, List.of(dto));
		verify(methodMock).validator();
		verify(validatorMock).validateCredentials(Map.of("key", "value"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveIdentitiesOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRemoveIdentities(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveIdentitiesOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRemoveIdentities(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveIdentitiesListNull() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRemoveIdentities(null, "origin"));

		assertEquals("Identifiable system name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveIdentitiesListEmpty() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRemoveIdentities(List.of(), "origin"));

		assertEquals("Identifiable system name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveIdentitiesListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRemoveIdentities(list, "origin"));

		assertEquals("Identifiable system name list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveIdentitiesListContainsEmptyElement() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRemoveIdentities(List.of(" "), "origin"));

		assertEquals("Identifiable system name list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveIdentitiesInvalidParameterException() {
		when(normalizer.normalizeIdentifiableSystemNames(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("TestProvider");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRemoveIdentities(List.of("TestProvider"), "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(normalizer).normalizeIdentifiableSystemNames(List.of("TestProvider"));
		verify(systemNameValidator).validateSystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveIdentitiesOk() {
		when(normalizer.normalizeIdentifiableSystemNames(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");

		final List<String> result = validator.validateAndNormalizeRemoveIdentities(List.of("TestProvider"), "origin");

		assertEquals(List.of("TestProvider"), result);

		verify(normalizer).normalizeIdentifiableSystemNames(List.of("TestProvider"));
		verify(systemNameValidator).validateSystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityQueryRequestOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeIdentityQueryRequest(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityQueryRequestOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeIdentityQueryRequest(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityQueryRequestDTONull() {
		final NormalizedIdentityQueryRequestDTO normalized = new NormalizedIdentityQueryRequestDTO(PageRequest.of(0, 10), null, null, null, null, null, null);

		when(normalizer.normalizeIdentityQueryRequest(null)).thenReturn(normalized);

		final NormalizedIdentityQueryRequestDTO result = validator.validateAndNormalizeIdentityQueryRequest(null, "origin");

		assertEquals(normalized, result);

		verify(pageValidator, never()).validatePageParameter(any(PageDTO.class), anyList(), eq("origin"));
		verify(normalizer).normalizeIdentityQueryRequest(null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityQueryRequestCreationFromInvalid() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentityQueryRequestDTO dto = new IdentityQueryRequestDTO(page, "Provider", true, "AdminSystem", "invalid", "2025-11-05T10:00:02Z", true);

		doNothing().when(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeIdentityQueryRequest(dto, "origin"));

		assertEquals("Minimum creation time has invalid time format", ex.getMessage());

		verify(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityQueryRequestCreationToInvalid() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentityQueryRequestDTO dto = new IdentityQueryRequestDTO(page, "Provider", true, "AdminSystem", null, "invalid", true);

		doNothing().when(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeIdentityQueryRequest(dto, "origin"));

		assertEquals("Maximum creation time has invalid time format", ex.getMessage());

		verify(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityQueryRequestCreationIntervalInvalid() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentityQueryRequestDTO dto = new IdentityQueryRequestDTO(page, "Provider", true, "AdminSystem", "2025-11-05T10:00:02Z", "2025-11-05T08:00:02Z", true);

		doNothing().when(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeIdentityQueryRequest(dto, "origin"));

		assertEquals("Empty creation time interval", ex.getMessage());

		verify(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityQueryRequestInvalidParameterException() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentityQueryRequestDTO dto = new IdentityQueryRequestDTO(page, "Provider", true, "1AdminSystem", "2025-11-05T10:00:02Z", "2025-11-05T12:00:02Z", true);
		final NormalizedIdentityQueryRequestDTO normalized = new NormalizedIdentityQueryRequestDTO(
				PageRequest.of(0, 10),
				"Provider",
				true,
				"1AdminSystem",
				Utilities.parseUTCStringToZonedDateTime("2025-11-05T10:00:02Z"),
				Utilities.parseUTCStringToZonedDateTime("2025-11-05T12:00:02Z"),
				true);

		doNothing().when(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeIdentityQueryRequest(dto)).thenReturn(normalized);
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1AdminSystem");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeIdentityQueryRequest(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeIdentityQueryRequest(dto);
		verify(systemNameValidator).validateSystemName("1AdminSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityQueryRequestOk1() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentityQueryRequestDTO dto = new IdentityQueryRequestDTO(page, "Provider", true, "AdminSystem", null, "2025-11-05T12:00:02Z", true);
		final NormalizedIdentityQueryRequestDTO normalized = new NormalizedIdentityQueryRequestDTO(
				PageRequest.of(0, 10),
				"Provider",
				true,
				"AdminSystem",
				null,
				Utilities.parseUTCStringToZonedDateTime("2025-11-05T12:00:02Z"),
				true);

		doNothing().when(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeIdentityQueryRequest(dto)).thenReturn(normalized);
		doNothing().when(systemNameValidator).validateSystemName("AdminSystem");

		final NormalizedIdentityQueryRequestDTO result = validator.validateAndNormalizeIdentityQueryRequest(dto, "origin");

		assertEquals(normalized, result);

		verify(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeIdentityQueryRequest(dto);
		verify(systemNameValidator).validateSystemName("AdminSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeIdentityQueryRequestOk2() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentityQueryRequestDTO dto = new IdentityQueryRequestDTO(page, "Provider", true, null, "2025-11-05T10:00:02Z", null, true);
		final NormalizedIdentityQueryRequestDTO normalized = new NormalizedIdentityQueryRequestDTO(
				PageRequest.of(0, 10),
				"Provider",
				true,
				null,
				Utilities.parseUTCStringToZonedDateTime("2025-11-05T10:00:02Z"),
				null,
				true);

		doNothing().when(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");
		when(normalizer.normalizeIdentityQueryRequest(dto)).thenReturn(normalized);

		final NormalizedIdentityQueryRequestDTO result = validator.validateAndNormalizeIdentityQueryRequest(dto, "origin");

		assertEquals(normalized, result);

		verify(pageValidator).validatePageParameter(page, System.SORTABLE_FIELDS_BY, "origin");
		verify(normalizer).normalizeIdentityQueryRequest(dto);
		verify(systemNameValidator, never()).validateSystemName(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCloseSessionsOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeCloseSessions(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCloseSessionsOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeCloseSessions(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCloseSessionsListNull() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCloseSessions(null, "origin"));

		assertEquals("Identifiable system name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCloseSessionsListEmpty() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCloseSessions(List.of(), "origin"));

		assertEquals("Identifiable system name list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCloseSessionsListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCloseSessions(list, "origin"));

		assertEquals("Identifiable system name list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCloseSessionsListContainsEmptyElement() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCloseSessions(List.of(" "), "origin"));

		assertEquals("Identifiable system name list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCloseSessionsInvalidParameterException() {
		when(normalizer.normalizeIdentifiableSystemNames(List.of("1TestProvider"))).thenReturn(List.of("1TestProvider"));
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1TestProvider");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCloseSessions(List.of("1TestProvider"), "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(normalizer).normalizeIdentifiableSystemNames(List.of("1TestProvider"));
		verify(systemNameValidator).validateSystemName("1TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCloseSessionsOk() {
		when(normalizer.normalizeIdentifiableSystemNames(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		doNothing().when(systemNameValidator).validateSystemName("TestProvider");

		final List<String> result = validator.validateAndNormalizeCloseSessions(List.of("TestProvider"), "origin");

		assertEquals(List.of("TestProvider"), result);

		verify(normalizer).normalizeIdentifiableSystemNames(List.of("TestProvider"));
		verify(systemNameValidator).validateSystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSessionQueryRequesttOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeSessionQueryRequest(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSessionQueryRequestOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeSessionQueryRequest(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSessionQueryRequestDTONull() {
		final NormalizedIdentitySessionQueryRequestDTO normalized = new NormalizedIdentitySessionQueryRequestDTO(PageRequest.of(0, 10), null, null, null);

		when(normalizer.normalizeSessionQueryRequest(null)).thenReturn(normalized);

		final NormalizedIdentitySessionQueryRequestDTO result = validator.validateAndNormalizeSessionQueryRequest(null, "origin");

		assertEquals(normalized, result);

		verify(pageValidator, never()).validatePageParameter(any(PageDTO.class), anyList(), eq("origin"));
		verify(normalizer).normalizeSessionQueryRequest(null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSessionQueryRequestLoginFromInvalid() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(page, "Provider", "invalid", "2025-11-05T10:00:02Z");

		doNothing().when(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeSessionQueryRequest(dto, "origin"));

		assertEquals("Minimum login time has invalid time format", ex.getMessage());

		verify(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSessionQueryRequestLoginToInvalid() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(page, "Provider", null, "invalid");

		doNothing().when(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeSessionQueryRequest(dto, "origin"));

		assertEquals("Maximum login time has invalid time format", ex.getMessage());

		verify(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSessionQueryRequestLoginIntervalInvalid() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(page, "Provider", "2025-11-05T10:00:02Z", "2025-11-05T08:00:02Z");

		doNothing().when(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeSessionQueryRequest(dto, "origin"));

		assertEquals("Empty login time interval", ex.getMessage());

		verify(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSessionQueryRequestOk1() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(page, "Provider", "2025-11-05T10:00:02Z", "2025-11-05T12:00:02Z");
		final NormalizedIdentitySessionQueryRequestDTO normalized = new NormalizedIdentitySessionQueryRequestDTO(
				PageRequest.of(0, 10),
				"Provider",
				Utilities.parseUTCStringToZonedDateTime("2025-11-05T10:00:02Z"),
				Utilities.parseUTCStringToZonedDateTime("2025-11-05T12:00:02Z"));

		doNothing().when(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");
		when(normalizer.normalizeSessionQueryRequest(dto)).thenReturn(normalized);

		final NormalizedIdentitySessionQueryRequestDTO result = validator.validateAndNormalizeSessionQueryRequest(dto, "origin");

		assertEquals(normalized, result);

		verify(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");
		verify(normalizer).normalizeSessionQueryRequest(dto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSessionQueryRequestOk2() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(page, "Provider", null, "2025-11-05T12:00:02Z");
		final NormalizedIdentitySessionQueryRequestDTO normalized = new NormalizedIdentitySessionQueryRequestDTO(
				PageRequest.of(0, 10),
				"Provider",
				null,
				Utilities.parseUTCStringToZonedDateTime("2025-11-05T12:00:02Z"));

		doNothing().when(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");
		when(normalizer.normalizeSessionQueryRequest(dto)).thenReturn(normalized);

		final NormalizedIdentitySessionQueryRequestDTO result = validator.validateAndNormalizeSessionQueryRequest(dto, "origin");

		assertEquals(normalized, result);

		verify(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");
		verify(normalizer).normalizeSessionQueryRequest(dto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSessionQueryRequestOk3() {
		final PageDTO page = new PageDTO(0, 10, "ASC", "name");
		final IdentitySessionQueryRequestDTO dto = new IdentitySessionQueryRequestDTO(page, "Provider", "2025-11-05T12:00:02Z", null);
		final NormalizedIdentitySessionQueryRequestDTO normalized = new NormalizedIdentitySessionQueryRequestDTO(
				PageRequest.of(0, 10),
				"Provider",
				Utilities.parseUTCStringToZonedDateTime("2025-11-05T12:00:02Z"),
				null);

		doNothing().when(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");
		when(normalizer.normalizeSessionQueryRequest(dto)).thenReturn(normalized);

		final NormalizedIdentitySessionQueryRequestDTO result = validator.validateAndNormalizeSessionQueryRequest(dto, "origin");

		assertEquals(normalized, result);

		verify(pageValidator).validatePageParameter(page, ActiveSession.ACCEPTABLE_SORT_FIELDS, "origin");
		verify(normalizer).normalizeSessionQueryRequest(dto);
	}
}