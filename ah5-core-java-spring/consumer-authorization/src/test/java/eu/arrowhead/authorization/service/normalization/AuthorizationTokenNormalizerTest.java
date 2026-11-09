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
package eu.arrowhead.authorization.service.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;

@ExtendWith(MockitoExtension.class)
public class AuthorizationTokenNormalizerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationTokenNormalizer normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private EventTypeNameNormalizer eventTypeNameNormalizer;

	@Mock
	private AuthorizationScopeNormalizer scopeNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNameNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeSystemName(null));

		assertEquals("System name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNameOk() {
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");

		final String result = normalizer.normalizeSystemName("TestProvider");

		verify(systemNameNormalizer).normalize("TestProvider");

		assertEquals("TestProvider", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeTokenEmptyInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeToken(""));

		assertEquals("Token is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeTokenOk() {
		assertEquals("aToken", normalizer.normalizeToken("\taToken   "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenGenerationRequestDTONullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeAuthorizationTokenGenerationRequestDTO(null));

		assertEquals("AuthorizationTokenGenerationRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenGenerationRequestDTOOk1() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"time_limited_token_auth",
				"ProviderName",
				null,
				"testService",
				"op");

		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(scopeNormalizer.normalize("op")).thenReturn("op");

		final AuthorizationTokenGenerationRequestDTO result = normalizer.normalizeAuthorizationTokenGenerationRequestDTO(dto);

		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(scopeNormalizer).normalize("op");

		assertEquals("TIME_LIMITED_TOKEN_AUTH", result.tokenVariant());
		assertEquals("ProviderName", result.provider());
		assertEquals("SERVICE_DEF", result.targetType());
		assertEquals("testService", result.target());
		assertEquals("op", result.scope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenGenerationRequestDTOOk2() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"time_limited_token_auth",
				"ProviderName",
				"event_type",
				"testEvent",
				null);

		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");

		final AuthorizationTokenGenerationRequestDTO result = normalizer.normalizeAuthorizationTokenGenerationRequestDTO(dto);

		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		verify(eventTypeNameNormalizer).normalize("testEvent");
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(scopeNormalizer, never()).normalize(anyString());

		assertEquals("TIME_LIMITED_TOKEN_AUTH", result.tokenVariant());
		assertEquals("ProviderName", result.provider());
		assertEquals("EVENT_TYPE", result.targetType());
		assertEquals("testEvent", result.target());
		assertNull(result.scope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationEncryptionKeyRegistrationRequestDTONullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(null));

		assertEquals("AuthorizationEncryptionKeyRegistrationRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationEncryptionKeyRegistrationRequestDTOOk1() {
		final AuthorizationEncryptionKeyRegistrationRequestDTO dto = new AuthorizationEncryptionKeyRegistrationRequestDTO("key", null);

		final AuthorizationEncryptionKeyRegistrationRequestDTO result = normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(dto);

		assertEquals("key", result.key());
		assertEquals("AES/ECB/PKCS5Padding", result.algorithm());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationEncryptionKeyRegistrationRequestDTOOk2() {
		final AuthorizationEncryptionKeyRegistrationRequestDTO dto = new AuthorizationEncryptionKeyRegistrationRequestDTO("key", " AES/CBC/PKCS5Padding ");

		final AuthorizationEncryptionKeyRegistrationRequestDTO result = normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(dto);

		assertEquals("key", result.key());
		assertEquals("AES/CBC/PKCS5Padding", result.algorithm());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenGenerationMgmtListRequestDTONullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(null));

		assertEquals("AuthorizationTokenGenerationMgmtListRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenGenerationMgmtListRequestDTONullList() {
		final AuthorizationTokenGenerationMgmtListRequestDTO request = new AuthorizationTokenGenerationMgmtListRequestDTO(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(request));

		assertEquals("AuthorizationTokenGenerationMgmtListRequestDTO.list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenGenerationMgmtListRequestDTOOk1() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"time_limited_token_auth",
						null,
						"LOCAL",
						"ConsumerName",
						"ProviderName",
						"testService",
						"op",
						"2095-08-06T08:00:00Z ",
						null));
		final AuthorizationTokenGenerationMgmtListRequestDTO dto = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		when(systemNameNormalizer.normalize("ConsumerName")).thenReturn("ConsumerName");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(scopeNormalizer.normalize("op")).thenReturn("op");

		final AuthorizationTokenGenerationMgmtListRequestDTO result = normalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(dto);

		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(cloudIdentifierNormalizer).normalize("LOCAL");
		verify(systemNameNormalizer).normalize("ConsumerName");
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(scopeNormalizer).normalize("op");

		assertEquals(1, result.list().size());
		final AuthorizationTokenGenerationMgmtRequestDTO req = result.list().get(0);
		assertEquals("TIME_LIMITED_TOKEN_AUTH", req.tokenVariant());
		assertEquals("SERVICE_DEF", req.targetType());
		assertEquals("LOCAL", req.consumerCloud());
		assertEquals("ConsumerName", req.consumer());
		assertEquals("ProviderName", req.provider());
		assertEquals("testService", req.target());
		assertEquals("op", req.scope());
		assertEquals("2095-08-06T08:00:00Z", req.expiresAt());
		assertNull(req.usageLimit());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeAuthorizationTokenGenerationMgmtListRequestDTOOk2() {
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						"usage_limited_token_auth",
						"EVENT_TYPE",
						"",
						"ConsumerName",
						"ProviderName",
						"testEvent",
						null,
						"",
						12));
		final AuthorizationTokenGenerationMgmtListRequestDTO dto = new AuthorizationTokenGenerationMgmtListRequestDTO(list);

		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");
		when(systemNameNormalizer.normalize("ConsumerName")).thenReturn("ConsumerName");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");

		final AuthorizationTokenGenerationMgmtListRequestDTO result = normalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(dto);

		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		verify(eventTypeNameNormalizer).normalize("testEvent");
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());
		verify(systemNameNormalizer).normalize("ConsumerName");
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(scopeNormalizer, never()).normalize(anyString());

		assertEquals(1, result.list().size());
		final AuthorizationTokenGenerationMgmtRequestDTO req = result.list().get(0);
		assertEquals("USAGE_LIMITED_TOKEN_AUTH", req.tokenVariant());
		assertEquals("EVENT_TYPE", req.targetType());
		assertEquals("LOCAL", req.consumerCloud());
		assertEquals("ConsumerName", req.consumer());
		assertEquals("ProviderName", req.provider());
		assertEquals("testEvent", req.target());
		assertNull(req.scope());
		assertNull(req.expiresAt());
		assertEquals(12, req.usageLimit());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenQueryRequestDTONullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeAuthorizationTokenQueryRequestDTO(null));

		assertEquals("AuthorizationTokenQueryRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenQueryRequestDTOOk1() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO dto = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				"Requester",
				"time_limited_token",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				null,
				"testService");

		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(systemNameNormalizer.normalize("Requester")).thenReturn("Requester");
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		when(systemNameNormalizer.normalize("ConsumerName")).thenReturn("ConsumerName");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");

		final AuthorizationTokenQueryRequestDTO result = normalizer.normalizeAuthorizationTokenQueryRequestDTO(dto);

		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameNormalizer).normalize("Requester");
		verify(cloudIdentifierNormalizer).normalize("LOCAL");
		verify(systemNameNormalizer).normalize("ConsumerName");
		verify(systemNameNormalizer).normalize("ProviderName");

		assertEquals(pageDTO, result.pagination());
		assertEquals("Requester", result.requester());
		assertEquals("TIME_LIMITED_TOKEN", result.tokenType());
		assertEquals("LOCAL", result.consumerCloud());
		assertEquals("ConsumerName", result.consumer());
		assertEquals("ProviderName", result.provider());
		assertNull(result.targetType());
		assertEquals("testService", result.target());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenQueryRequestDTOOk2() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO dto = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				"",
				null,
				"",
				null,
				"",
				"service_def",
				"testService");

		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");

		final AuthorizationTokenQueryRequestDTO result = normalizer.normalizeAuthorizationTokenQueryRequestDTO(dto);

		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());

		assertEquals(pageDTO, result.pagination());
		assertNull(result.requester());
		assertNull(result.tokenType());
		assertNull(result.consumerCloud());
		assertNull(result.consumer());
		assertNull(result.provider());
		assertEquals("SERVICE_DEF", result.targetType());
		assertEquals("testService", result.target());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenQueryRequestDTOOk3() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO dto = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				"",
				null,
				"",
				null,
				"",
				"event_type",
				"testEvent");

		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");

		final AuthorizationTokenQueryRequestDTO result = normalizer.normalizeAuthorizationTokenQueryRequestDTO(dto);

		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		verify(eventTypeNameNormalizer).normalize("testEvent");
		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());

		assertEquals(pageDTO, result.pagination());
		assertNull(result.requester());
		assertNull(result.tokenType());
		assertNull(result.consumerCloud());
		assertNull(result.consumer());
		assertNull(result.provider());
		assertEquals("EVENT_TYPE", result.targetType());
		assertEquals("testEvent", result.target());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenQueryRequestDTOOk4() {
		final PageDTO pageDTO = new PageDTO(0, 5, "ASC", "id");
		final AuthorizationTokenQueryRequestDTO dto = new AuthorizationTokenQueryRequestDTO(
				pageDTO,
				"",
				null,
				"",
				null,
				"",
				"",
				null);

		final AuthorizationTokenQueryRequestDTO result = normalizer.normalizeAuthorizationTokenQueryRequestDTO(dto);

		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(cloudIdentifierNormalizer, never()).normalize(anyString());

		assertEquals(pageDTO, result.pagination());
		assertNull(result.requester());
		assertNull(result.tokenType());
		assertNull(result.consumerCloud());
		assertNull(result.consumer());
		assertNull(result.provider());
		assertNull(result.targetType());
		assertNull(result.target());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTONullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(null));

		assertEquals("AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTONullList() {
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(dto));

		assertEquals("AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO.list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTOOk1() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = List.of(
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO(
						"ProviderName",
						"aKey",
						null));
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO result = normalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(dto);

		verify(systemNameNormalizer).normalize("ProviderName");

		assertEquals(1, result.list().size());
		final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO req = result.list().get(0);
		assertEquals("ProviderName", req.systemName());
		assertEquals("aKey", req.key());
		assertEquals("AES/ECB/PKCS5Padding", req.algorithm());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTOOk2() {
		final List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO> list = List.of(
				new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO(
						"ProviderName",
						"aKey",
						"AES/CBC/PKCS5Padding"));
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(list);

		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO result = normalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(dto);

		verify(systemNameNormalizer).normalize("ProviderName");

		assertEquals(1, result.list().size());
		final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO req = result.list().get(0);
		assertEquals("ProviderName", req.systemName());
		assertEquals("aKey", req.key());
		assertEquals("AES/CBC/PKCS5Padding", req.algorithm());
	}
}