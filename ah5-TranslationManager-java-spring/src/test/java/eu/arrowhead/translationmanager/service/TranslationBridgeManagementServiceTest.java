/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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
package eu.arrowhead.translationmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;

import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.TranslationAbortMgmtResponseDTO;
import eu.arrowhead.dto.TranslationBridgeCandidateDTO;
import eu.arrowhead.dto.TranslationDiscoveryMgmtRequestDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationNegotiationMgmtRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.dto.TranslationQueryListResponseDTO;
import eu.arrowhead.dto.TranslationQueryRequestDTO;
import eu.arrowhead.dto.TranslationQueryResponseDTO;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService;
import eu.arrowhead.translationmanager.service.dto.DTOConverter;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationQueryRequestDTO;
import eu.arrowhead.translationmanager.service.engine.TranslatorBridgeEngine;
import eu.arrowhead.translationmanager.service.validation.TranslationBridgeMgmtValidation;

@ExtendWith(MockitoExtension.class)
public class TranslationBridgeManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslationBridgeManagementService service;

	@Mock
	private TranslationBridgeMgmtValidation validator;

	@Mock
	private TranslationManagerSystemInfo sysInfo;

	@Mock
	private TranslatorBridgeEngine engine;

	@Mock
	private BridgeDbService dbService;

	@Mock
	private DTOConverter converter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDiscoveryOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.discoveryOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDiscoveryOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.discoveryOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDiscoveryOperationOk1() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO("TestProvider|testService|1.0.0", null, null, null, null, null, null, null, null);
		final TranslationDiscoveryMgmtRequestDTO request = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson",
				Map.of("CONSUMER_BLACKLIST_CHECK", true,
						"CANDIDATES_BLACKLIST_CHECK", false));

		final NormalizedServiceInstanceDTO normalizedCandidate = new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of());
		final NormalizedTranslationDiscoveryRequestDTO normalizedRequest = new NormalizedTranslationDiscoveryRequestDTO(
				"Requester",
				List.of(normalizedCandidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> actualFlags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final TranslationBridgeCandidateDTO bridgeCandidate = new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http");
		final TranslationDiscoveryResponseDTO responseDTO = new TranslationDiscoveryResponseDTO("bridgeId", List.of(bridgeCandidate));

		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeDiscoveryMgmtRequest("Requester", request, "origin")).thenReturn(
				Pair.of(
						normalizedRequest,
						Map.of(
								TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, true,
								TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false)));
		when(sysInfo.isDiscoveryFlagsAllowed()).thenReturn(true);
		when(sysInfo.isBlacklistEnabled()).thenReturn(true);
		when(engine.doDiscovery(normalizedRequest, actualFlags, "origin")).thenReturn(responseDTO);

		final TranslationDiscoveryResponseDTO result = service.discoveryOperation("Requester", request, "origin");

		assertNotNull(result);
		assertEquals("bridgeId", result.bridgeId());
		assertNotNull(result.candidates());
		assertEquals(1, result.candidates().size());
		assertEquals("TestProvider|testService|1.0.0", result.candidates().get(0).serviceInstanceId());
		assertEquals("generic_http", result.candidates().get(0).interfaceTemplateName());

		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeDiscoveryMgmtRequest("Requester", request, "origin");
		verify(sysInfo).isDiscoveryFlagsAllowed();
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, never()).isAuthorizationEnabled();
		verify(engine).doDiscovery(normalizedRequest, actualFlags, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDiscoveryOperationOk2() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO("TestProvider|testService|1.0.0", null, null, null, null, null, null, null, null);
		final TranslationDiscoveryMgmtRequestDTO request = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson",
				Map.of("CONSUMER_BLACKLIST_CHECK", false,
						"CANDIDATES_BLACKLIST_CHECK", true,
						"CANDIDATES_AUTH_CHECK", true,
						"TRANSLATORS_BLACKLIST_CHECK", true,
						"TRANSLATORS_AUTH_CHECK", true));

		final NormalizedServiceInstanceDTO normalizedCandidate = new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of());
		final NormalizedTranslationDiscoveryRequestDTO normalizedRequest = new NormalizedTranslationDiscoveryRequestDTO(
				"Requester",
				List.of(normalizedCandidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> actualFlags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true);

		final TranslationBridgeCandidateDTO bridgeCandidate = new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http");
		final TranslationDiscoveryResponseDTO responseDTO = new TranslationDiscoveryResponseDTO("bridgeId", List.of(bridgeCandidate));

		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeDiscoveryMgmtRequest("Requester", request, "origin")).thenReturn(
				Pair.of(
						normalizedRequest,
						Map.of(
								TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
								TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
								TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true,
								TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
								TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true)));
		when(sysInfo.isDiscoveryFlagsAllowed()).thenReturn(true);
		when(sysInfo.isBlacklistEnabled()).thenReturn(true);
		when(sysInfo.isAuthorizationEnabled()).thenReturn(true);
		when(engine.doDiscovery(normalizedRequest, actualFlags, "origin")).thenReturn(responseDTO);

		final TranslationDiscoveryResponseDTO result = service.discoveryOperation("Requester", request, "origin");

		assertNotNull(result);
		assertEquals("bridgeId", result.bridgeId());
		assertNotNull(result.candidates());
		assertEquals(1, result.candidates().size());
		assertEquals("TestProvider|testService|1.0.0", result.candidates().get(0).serviceInstanceId());
		assertEquals("generic_http", result.candidates().get(0).interfaceTemplateName());

		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeDiscoveryMgmtRequest("Requester", request, "origin");
		verify(sysInfo).isDiscoveryFlagsAllowed();
		verify(sysInfo, times(2)).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(engine).doDiscovery(normalizedRequest, actualFlags, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDiscoveryOperationOk3() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO("TestProvider|testService|1.0.0", null, null, null, null, null, null, null, null);
		final TranslationDiscoveryMgmtRequestDTO request = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson",
				Map.of("CONSUMER_BLACKLIST_CHECK", true,
						"CANDIDATES_BLACKLIST_CHECK", true,
						"CANDIDATES_AUTH_CHECK", true,
						"TRANSLATORS_BLACKLIST_CHECK", true,
						"TRANSLATORS_AUTH_CHECK", true));

		final NormalizedServiceInstanceDTO normalizedCandidate = new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of());
		final NormalizedTranslationDiscoveryRequestDTO normalizedRequest = new NormalizedTranslationDiscoveryRequestDTO(
				"Requester",
				List.of(normalizedCandidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> actualFlags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final TranslationBridgeCandidateDTO bridgeCandidate = new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http");
		final TranslationDiscoveryResponseDTO responseDTO = new TranslationDiscoveryResponseDTO("bridgeId", List.of(bridgeCandidate));

		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeDiscoveryMgmtRequest("Requester", request, "origin")).thenReturn(
				Pair.of(
						normalizedRequest,
						Map.of(
								TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, true,
								TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
								TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true,
								TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
								TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true)));
		when(sysInfo.isDiscoveryFlagsAllowed()).thenReturn(true);
		when(sysInfo.isBlacklistEnabled()).thenReturn(false);
		when(sysInfo.isAuthorizationEnabled()).thenReturn(false);
		when(engine.doDiscovery(normalizedRequest, actualFlags, "origin")).thenReturn(responseDTO);

		final TranslationDiscoveryResponseDTO result = service.discoveryOperation("Requester", request, "origin");

		assertNotNull(result);
		assertEquals("bridgeId", result.bridgeId());
		assertNotNull(result.candidates());
		assertEquals(1, result.candidates().size());
		assertEquals("TestProvider|testService|1.0.0", result.candidates().get(0).serviceInstanceId());
		assertEquals("generic_http", result.candidates().get(0).interfaceTemplateName());

		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeDiscoveryMgmtRequest("Requester", request, "origin");
		verify(sysInfo).isDiscoveryFlagsAllowed();
		verify(sysInfo, times(3)).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(engine).doDiscovery(normalizedRequest, actualFlags, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDiscoveryOperationOk4() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO("TestProvider|testService|1.0.0", null, null, null, null, null, null, null, null);
		final TranslationDiscoveryMgmtRequestDTO request = new TranslationDiscoveryMgmtRequestDTO(
				List.of(candidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson",
				Map.of("CONSUMER_BLACKLIST_CHECK", false,
						"CANDIDATES_BLACKLIST_CHECK", false,
						"CANDIDATES_AUTH_CHECK", false,
						"TRANSLATORS_BLACKLIST_CHECK", false,
						"TRANSLATORS_AUTH_CHECK", false));

		final NormalizedServiceInstanceDTO normalizedCandidate = new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of());
		final NormalizedTranslationDiscoveryRequestDTO normalizedRequest = new NormalizedTranslationDiscoveryRequestDTO(
				"Requester",
				List.of(normalizedCandidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> actualFlags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final TranslationBridgeCandidateDTO bridgeCandidate = new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http");
		final TranslationDiscoveryResponseDTO responseDTO = new TranslationDiscoveryResponseDTO("bridgeId", List.of(bridgeCandidate));

		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeDiscoveryMgmtRequest("Requester", request, "origin")).thenReturn(
				Pair.of(
						normalizedRequest,
						Map.of(
								TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
								TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
								TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
								TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
								TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false)));
		when(sysInfo.isDiscoveryFlagsAllowed()).thenReturn(false);
		when(sysInfo.isBlacklistEnabled()).thenReturn(true);
		when(sysInfo.isAuthorizationEnabled()).thenReturn(false);
		when(engine.doDiscovery(normalizedRequest, actualFlags, "origin")).thenReturn(responseDTO);

		final TranslationDiscoveryResponseDTO result = service.discoveryOperation("Requester", request, "origin");

		assertNotNull(result);
		assertEquals("bridgeId", result.bridgeId());
		assertNotNull(result.candidates());
		assertEquals(1, result.candidates().size());
		assertEquals("TestProvider|testService|1.0.0", result.candidates().get(0).serviceInstanceId());
		assertEquals("generic_http", result.candidates().get(0).interfaceTemplateName());

		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeDiscoveryMgmtRequest("Requester", request, "origin");
		verify(sysInfo).isDiscoveryFlagsAllowed();
		verify(sysInfo, times(3)).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(engine).doDiscovery(normalizedRequest, actualFlags, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNegotiationOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.negotiationOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNegotiationOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.negotiationOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNegotiationOperationOk() {
		final TranslationNegotiationMgmtRequestDTO dto = new TranslationNegotiationMgmtRequestDTO("7d026491-b287-47e5-ba97-2f2eaa39aa05", "TestProvider|testService|1.0.0");
		final UUID uuid = UUID.fromString("7d026491-b287-47e5-ba97-2f2eaa39aa05");
		final TranslationNegotiationResponseDTO response = new TranslationNegotiationResponseDTO(
				"7d026491-b287-47e5-ba97-2f2eaa39aa05",
				new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of()),
				null,
				null);

		when(validator.validateAndNormalizeNegotiationMgmtRequest(dto, "origin")).thenReturn(Pair.of(
				uuid,
				"TestProvider|testService|1.0.0"));
		when(engine.doNegotiation(uuid, "TestProvider|testService|1.0.0", "origin")).thenReturn(response);

		final TranslationNegotiationResponseDTO result = service.negotiationOperation(dto, "origin");

		assertEquals(response, result);

		verify(validator).validateAndNormalizeNegotiationMgmtRequest(dto, "origin");
		verify(engine).doNegotiation(uuid, "TestProvider|testService|1.0.0", "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.abortOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.abortOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortOperationOk() {
		final UUID uuid = UUID.fromString("7d026491-b287-47e5-ba97-2f2eaa39aa05");
		final Map<String, Boolean> response = Map.of("7d026491-b287-47e5-ba97-2f2eaa39aa05", true);

		when(validator.validateAndNormalizeAbortMgmtRequest(List.of("7d026491-b287-47e5-ba97-2f2eaa39aa05"), "origin")).thenReturn(List.of(uuid));
		when(engine.doAbort(List.of(uuid), null, "origin")).thenReturn(response);

		final TranslationAbortMgmtResponseDTO result = service.abortOperation(List.of("7d026491-b287-47e5-ba97-2f2eaa39aa05"), "origin");

		assertNotNull(result);
		assertEquals(response, result.results());

		verify(validator).validateAndNormalizeAbortMgmtRequest(List.of("7d026491-b287-47e5-ba97-2f2eaa39aa05"), "origin");
		verify(engine).doAbort(List.of(uuid), null, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerytOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerytOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerytOperationOk() {
		final TranslationQueryRequestDTO dto = new TranslationQueryRequestDTO(
				new PageDTO(0, 10, null, null),
				null,
				List.of("Creator"),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final NormalizedTranslationQueryRequestDTO normalized = new NormalizedTranslationQueryRequestDTO(
				PageRequest.of(0, 10),
				null,
				List.of("Creator"),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString("7d026491-b287-47e5-ba97-2f2eaa39aa05"), "Creator");
		header.setId(1L);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);

		final TranslationQueryResponseDTO element = new TranslationQueryResponseDTO(
				"7d026491-b287-47e5-ba97-2f2eaa39aa05",
				null,
				0,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
		final TranslationQueryListResponseDTO response = new TranslationQueryListResponseDTO(List.of(element), 1);

		when(validator.validateAndNormalizeQueryMgmtRequest(dto, "origin")).thenReturn(normalized);
		when(dbService.getBridgeDetailsPage(normalized)).thenReturn(new PageImpl<>(List.of(details)));
		when(converter.convertBridgeDetailsPage(new PageImpl<>(List.of(details)))).thenReturn(response);

		final TranslationQueryListResponseDTO result = service.queryOperation(dto, "origin");

		assertNotNull(result);
		assertNotNull(result.entries());
		assertEquals(1, result.entries().size());
		assertEquals(details.getHeader().getUuid().toString(), result.entries().get(0).bridgeId());

		verify(validator).validateAndNormalizeQueryMgmtRequest(dto, "origin");
		verify(dbService).getBridgeDetailsPage(normalized);
		verify(converter).convertBridgeDetailsPage(new PageImpl<>(List.of(details)));
	}
}