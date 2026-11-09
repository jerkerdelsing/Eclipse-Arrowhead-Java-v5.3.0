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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.TranslationBridgeCandidateDTO;
import eu.arrowhead.dto.TranslationDiscoveryRequestDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationNegotiationRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationNegotiationRequestDTO;
import eu.arrowhead.translationmanager.service.engine.TranslatorBridgeEngine;
import eu.arrowhead.translationmanager.service.validation.TranslationBridgeValidation;

@ExtendWith(MockitoExtension.class)
public class TranslationBridgeServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslationBridgeService service;

	@Mock
	private TranslationBridgeValidation validator;

	@Mock
	private TranslationManagerSystemInfo sysInfo;

	@Mock
	private TranslatorBridgeEngine engine;

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
	public void testDiscoveryOperationOk() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");
		final NormalizedServiceInstanceDTO normalizedCandidate = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				null,
				null,
				null);
		final NormalizedTranslationDiscoveryRequestDTO normalized = new NormalizedTranslationDiscoveryRequestDTO(
				"TestConsumer",
				List.of(normalizedCandidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");
		final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);
		final TranslationDiscoveryResponseDTO response = new TranslationDiscoveryResponseDTO(
				"bridgeId",
				List.of(new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http")));

		when(validator.validateAndNormalizeRequester("TestConsumer", "origin")).thenReturn("TestConsumer");
		when(validator.validateAndNormalizeDiscoveryRequest("TestConsumer", dto, "origin")).thenReturn(normalized);
		when(sysInfo.isBlacklistEnabled()).thenReturn(false);
		when(sysInfo.isAuthorizationEnabled()).thenReturn(false);
		when(engine.doDiscovery(normalized, discoveryFlags, "origin")).thenReturn(response);

		final TranslationDiscoveryResponseDTO result = service.discoveryOperation("TestConsumer", dto, "origin");

		assertNotNull(result);
		assertEquals("bridgeId", result.bridgeId());
		assertNotNull(result.candidates());
		assertEquals(1, result.candidates().size());
		assertEquals("TestProvider|testService|1.0.0", result.candidates().get(0).serviceInstanceId());
		assertEquals("generic_http", result.candidates().get(0).interfaceTemplateName());

		verify(validator).validateAndNormalizeRequester("TestConsumer", "origin");
		verify(validator).validateAndNormalizeDiscoveryRequest("TestConsumer", dto, "origin");
		verify(sysInfo, times(2)).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(engine).doDiscovery(normalized, discoveryFlags, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNegotiationOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.negotiationOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNegotiationOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.negotiationOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNegotiationOperationWithPreviousDiscoveryOk() {
		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				"fcd92344-e643-4c3f-9dc1-9f5cdff6262a",
				new ServiceInstanceResponseDTO("TestProvider|testService|1.0.0", null, null, null, null, null, null, null, null),
				null,
				null,
				null,
				null);
		final NormalizedServiceInstanceDTO target = new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of());
		final UUID uuid = UUID.fromString("fcd92344-e643-4c3f-9dc1-9f5cdff6262a");
		final NormalizedTranslationNegotiationRequestDTO normalized = new NormalizedTranslationNegotiationRequestDTO(
				uuid,
				target,
				"test-operation",
				"generic_http",
				"testXml",
				"testJson");

		final TranslationNegotiationResponseDTO response = new TranslationNegotiationResponseDTO(
				"fcd92344-e643-4c3f-9dc1-9f5cdff6262a",
				new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of()),
				null,
				null);

		when(validator.validateAndNormalizeNegotiationRequest(dto, "origin")).thenReturn(normalized);
		when(engine.doNegotiation(uuid, "TestProvider|testService|1.0.0", "origin")).thenReturn(response);

		final TranslationNegotiationResponseDTO result = service.negotiationOperation("Requester", dto, "origin");

		assertEquals(response, result);

		verify(validator).validateAndNormalizeNegotiationRequest(dto, "origin");
		verify(validator, never()).validateAndNormalizeDiscoveryRequest(eq("Requester"), any(TranslationDiscoveryRequestDTO.class), eq("origin"));
		verify(engine).doNegotiation(uuid, "TestProvider|testService|1.0.0", "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNegotiationOperationWithoutPreviousDiscoveryOk() {
		final ServiceInstanceResponseDTO target = new ServiceInstanceResponseDTO("TestProvider|testService|1.0.0", null, null, null, null, null, null, null, null);
		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				null,
				target,
				"test-operation",
				"generic_http",
				"testXml",
				"testJson");
		final NormalizedServiceInstanceDTO normalizedTarget = new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of());
		final NormalizedTranslationNegotiationRequestDTO normalized = new NormalizedTranslationNegotiationRequestDTO(
				null,
				normalizedTarget,
				"test-operation",
				"generic_http",
				"testXml",
				"testJson");

		final TranslationDiscoveryRequestDTO discoveryRequest = new TranslationDiscoveryRequestDTO(
				List.of(target),
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");
		final NormalizedServiceInstanceDTO normalizedDRCandidate = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				null,
				null,
				null);
		final NormalizedTranslationDiscoveryRequestDTO normalizedDR = new NormalizedTranslationDiscoveryRequestDTO(
				"TestConsumer",
				List.of(normalizedDRCandidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");
		final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);
		final TranslationDiscoveryResponseDTO discoveryResponse = new TranslationDiscoveryResponseDTO(
				"fcd92344-e643-4c3f-9dc1-9f5cdff6262a",
				List.of(new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http")));
		final TranslationNegotiationResponseDTO response = new TranslationNegotiationResponseDTO(
				"fcd92344-e643-4c3f-9dc1-9f5cdff6262a",
				new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of()),
				null,
				null);

		when(validator.validateAndNormalizeNegotiationRequest(dto, "origin")).thenReturn(normalized);
		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeDiscoveryRequest("Requester", discoveryRequest, "origin")).thenReturn(normalizedDR);
		when(sysInfo.isBlacklistEnabled()).thenReturn(false);
		when(sysInfo.isAuthorizationEnabled()).thenReturn(false);
		when(engine.doDiscovery(normalizedDR, discoveryFlags, "origin")).thenReturn(discoveryResponse);
		when(engine.doNegotiation(UUID.fromString("fcd92344-e643-4c3f-9dc1-9f5cdff6262a"), "TestProvider|testService|1.0.0", "origin")).thenReturn(response);

		final TranslationNegotiationResponseDTO result = service.negotiationOperation("Requester", dto, "origin");

		assertEquals(response, result);

		verify(validator).validateAndNormalizeNegotiationRequest(dto, "origin");
		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeDiscoveryRequest("Requester", discoveryRequest, "origin");
		verify(sysInfo, times(2)).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(engine).doDiscovery(normalizedDR, discoveryFlags, "origin");
		verify(engine).doNegotiation(UUID.fromString("fcd92344-e643-4c3f-9dc1-9f5cdff6262a"), "TestProvider|testService|1.0.0", "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNegotiationOperationWithoutPreviousDiscoveryTranslationNotPossible() {
		final ServiceInstanceResponseDTO target = new ServiceInstanceResponseDTO("TestProvider|testService|1.0.0", null, null, null, null, null, null, null, null);
		final TranslationNegotiationRequestDTO dto = new TranslationNegotiationRequestDTO(
				null,
				target,
				"test-operation",
				"generic_http",
				"testXml",
				"testJson");
		final NormalizedServiceInstanceDTO normalizedTarget = new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of());
		final NormalizedTranslationNegotiationRequestDTO normalized = new NormalizedTranslationNegotiationRequestDTO(
				null,
				normalizedTarget,
				"test-operation",
				"generic_http",
				"testXml",
				"testJson");

		final TranslationDiscoveryRequestDTO discoveryRequest = new TranslationDiscoveryRequestDTO(
				List.of(target),
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");
		final NormalizedServiceInstanceDTO normalizedDRCandidate = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				null,
				null,
				null);
		final NormalizedTranslationDiscoveryRequestDTO normalizedDR = new NormalizedTranslationDiscoveryRequestDTO(
				"TestConsumer",
				List.of(normalizedDRCandidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");
		final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);
		final TranslationDiscoveryResponseDTO discoveryResponse = new TranslationDiscoveryResponseDTO(
				null,
				List.of(new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http")));

		when(validator.validateAndNormalizeNegotiationRequest(dto, "origin")).thenReturn(normalized);
		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeDiscoveryRequest("Requester", discoveryRequest, "origin")).thenReturn(normalizedDR);
		when(sysInfo.isBlacklistEnabled()).thenReturn(false);
		when(sysInfo.isAuthorizationEnabled()).thenReturn(false);
		when(engine.doDiscovery(normalizedDR, discoveryFlags, "origin")).thenReturn(discoveryResponse);

		final TranslationNegotiationResponseDTO result = service.negotiationOperation("Requester", dto, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertNull(result.bridgeInterface());

		verify(validator).validateAndNormalizeNegotiationRequest(dto, "origin");
		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeDiscoveryRequest("Requester", discoveryRequest, "origin");
		verify(sysInfo, times(2)).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(engine).doDiscovery(normalizedDR, discoveryFlags, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.abortOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.abortOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortOperationOk() {
		final UUID uuid = UUID.fromString("7d026491-b287-47e5-ba97-2f2eaa39aa05");
		final Map<String, Boolean> response = Map.of("7d026491-b287-47e5-ba97-2f2eaa39aa05", true);

		when(validator.validateAndNormalizeRequester("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeBridgeId("7d026491-b287-47e5-ba97-2f2eaa39aa05", "origin")).thenReturn(uuid);
		when(engine.doAbort(List.of(uuid), "Requester", "origin")).thenReturn(response);

		final boolean result = service.abortOperation("Requester", "7d026491-b287-47e5-ba97-2f2eaa39aa05", "origin");

		assertTrue(result);

		verify(validator).validateAndNormalizeRequester("Requester", "origin");
		verify(validator).validateAndNormalizeBridgeId("7d026491-b287-47e5-ba97-2f2eaa39aa05", "origin");
		verify(engine).doAbort(List.of(uuid), "Requester", "origin");
	}
}