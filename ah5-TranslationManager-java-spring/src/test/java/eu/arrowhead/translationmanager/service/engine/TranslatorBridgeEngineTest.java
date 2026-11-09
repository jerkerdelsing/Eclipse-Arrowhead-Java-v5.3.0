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
package eu.arrowhead.translationmanager.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierValidator;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.TranslationBridgeCandidateDTO;
import eu.arrowhead.dto.TranslationDataModelTranslatorInitializationResponseDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService.AbortResult;
import eu.arrowhead.translationmanager.service.dto.DTOConverter;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;
import eu.arrowhead.translationmanager.service.matchmaking.DataModelTranslatorMatchmaker;
import eu.arrowhead.translationmanager.service.matchmaking.InterfaceTranslatorMatchmaker;

@ExtendWith(MockitoExtension.class)
public class TranslatorBridgeEngineTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslatorBridgeEngine engine;

	@Mock
	private CoreSystemsDriver csDriver;

	@Mock
	private InterfaceTranslatorDriver itDriver;

	@Mock
	private DataModelTranslatorFactoryDriver dmfDriver;

	@Mock
	private DataModelIdentifierNormalizer dataModelIdentifierNormalizer;

	@Mock
	private DataModelIdentifierValidator dataModelIdentifierValidator;

	@Mock
	private InterfaceTranslatorMatchmaker interfaceTranslatorMatchmaker;

	@Mock
	private DataModelTranslatorMatchmaker dataModelTranslatorMatchmaker;

	@Mock
	private BridgeDbService dbService;

	@Mock
	private DTOConverter converter;

	@Mock
	private TranslationManagerSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryDTONull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doDiscovery(null, null, null));

		assertEquals("dto is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryFlagsNull() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doDiscovery(dto, null, null));

		assertEquals("discoveryFlags is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryOriginNull() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doDiscovery(dto, Map.of(), null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryOriginEmpty() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doDiscovery(dto, Map.of(), ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryConsumerBlacklisted() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, true);

		when(csDriver.isBlacklisted("TestConsumer")).thenReturn(true);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("TestConsumer system is blacklisted", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver).isBlacklisted("TestConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryNoNeedForTranslation() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, true);

		when(csDriver.isBlacklisted("TestConsumer")).thenReturn(false);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver).isBlacklisted("TestConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryTranslationNotPossible1() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", "string"));

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testJson",
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryTranslationNotPossible2() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of()));

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryInvalidDataModelId() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "test+ml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		when(dataModelIdentifierNormalizer.normalize("test+ml")).thenReturn("test+ml");
		doThrow(InvalidParameterException.class).when(dataModelIdentifierValidator).validateDataModelIdentifier("test+ml");

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("test+ml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("test+ml");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryTranslationNotPossible3() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryTranslationNotPossible4() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", null);
		opMap.put("output", "testXml");
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryBlacklistedCandidate() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", "string"), "a", "b"));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true);

		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemAuthException() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new AuthException("test"));

		final ArrowheadException ex = assertThrows(
				AuthException.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemForbiddenException() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new ForbiddenException("test"));

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemInvalidParameterException() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemExternalServerError() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new ExternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemRuntimeException() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new RuntimeException("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryUnauthorizedCandidate() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryNoAppropriateInterfaceTranslator() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", "testXml");
		opMap.put("output", null);
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenReturn(List.of("TestProvider"));
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryBlacklistedInterfaceTranslator() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("InterfaceTranslator"))).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryUnauthorizedInterfaceTranslator() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("InterfaceTranslator"))).thenReturn(List.of("InterfaceTranslator"));
		when(sysInfo.getSystemName()).thenReturn("TranslationManager");
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null)).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(sysInfo).getSystemName();
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryIncompatibleInterfaceTranslatorForTarget() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http")),
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(sysInfo.getSystemName()).thenReturn("TranslationManager");
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null)).thenReturn(List.of("InterfaceTranslator"));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(sysInfo).getSystemName();
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryEmptyModels() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http")),
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(null);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryEmptyModels2() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("some_problem"))),
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoDiscoveryOkNoDMTranslators() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", null);
		opMap.put("output", null);
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				null,
				null);
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));

		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final TranslationDiscoveryResponseDTO expected = new TranslationDiscoveryResponseDTO(
				bridgeId.toString(),
				List.of(new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http")));

		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);

		try (MockedStatic<UUID> mockedUUID = Mockito.mockStatic(UUID.class)) {
			mockedUUID.when(() -> UUID.randomUUID()).thenReturn(bridgeId);
			when(dbService.storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model))).thenReturn(null);
			when(converter.convertDiscoveryModels(bridgeId, List.of(model))).thenReturn(expected);

			final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

			assertNotNull(result);
			assertEquals(expected, result);

			verify(csDriver, never()).isBlacklisted("TestConsumer");
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
			verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
			verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
			verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
			verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
			mockedUUID.verify(() -> UUID.randomUUID());
			verify(dbService).storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model));
			verify(converter).convertDiscoveryModels(bridgeId, List.of(model));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoDiscoveryDMTranslatorNotFound() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", "testXml");
		opMap.put("output", null);
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				null);
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetInputDataModelId("testXml");

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
		verify(csDriver).collectDataModelTranslatorCandidates(List.of(model));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoDiscoveryDMTranslatorBlacklisted() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", null);
		opMap.put("output", "testXml");
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				null,
				"testJson");
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetOutputDataModelId("testXml");

		final ServiceInstanceInterfaceResponseDTO dmTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12347));

		final ServiceInstanceResponseDTO dataModelTranslator = new ServiceInstanceResponseDTO(
				"DataModelTranslator|dataModelTranslation|1.0.0",
				new SystemResponseDTO("DataModelTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("dataModelTranslation", null, null),
				"1.0.0",
				null,
				Map.of("dataModelIds", List.of(List.of("testXml", "testJson"))),
				List.of(dmTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("InterfaceTranslator"))).thenReturn(List.of("InterfaceTranslator"));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of(dataModelTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("DataModelTranslator"))).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
		verify(csDriver).collectDataModelTranslatorCandidates(List.of(model));
		verify(csDriver).filterOutBlacklistedSystems(List.of("DataModelTranslator"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoDiscoveryDMTranslatorUnauthorizedToUse() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", null);
		opMap.put("output", "testXml");
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				null,
				"testJson");
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetOutputDataModelId("testXml");

		final ServiceInstanceInterfaceResponseDTO dmTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12347));

		final ServiceInstanceResponseDTO dataModelTranslator = new ServiceInstanceResponseDTO(
				"DataModelTranslator|dataModelTranslation|1.0.0",
				new SystemResponseDTO("DataModelTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("dataModelTranslation", null, null),
				"1.0.0",
				null,
				Map.of("dataModelIds", List.of(List.of("testXml", "testJson"))),
				List.of(dmTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("InterfaceTranslator"))).thenReturn(List.of("InterfaceTranslator"));
		when(sysInfo.getSystemName()).thenReturn("TranslationManager");
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null)).thenReturn(List.of("InterfaceTranslator"));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of(dataModelTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("DataModelTranslator"))).thenReturn(List.of("DataModelTranslator"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("DataModelTranslator"), "InterfaceTranslator", "dataModelTranslation", null)).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(sysInfo).getSystemName();
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
		verify(csDriver).collectDataModelTranslatorCandidates(List.of(model));
		verify(csDriver).filterOutBlacklistedSystems(List.of("DataModelTranslator"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("DataModelTranslator"), "InterfaceTranslator", "dataModelTranslation", null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoDiscoveryTranslationNotSupportedByFactory() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", null);
		opMap.put("output", "testXml");
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				null,
				"testJson");
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetOutputDataModelId("testXml");

		final ServiceInstanceInterfaceResponseDTO dmTranslatorFactoryIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12347));

		final ServiceInstanceResponseDTO dataModelTranslatorFactory = new ServiceInstanceResponseDTO(
				"DataModelTranslatorFactory|dataModelTranslatorFactoryControl|1.0.0",
				new SystemResponseDTO("DataModelTranslatorFactory", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("dataModelTranslatorFactoryControl", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(dmTranslatorFactoryIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of());
		when(csDriver.collectDataModelTranslatorFactoryCandidates()).thenReturn(List.of(dataModelTranslatorFactory));
		when(dmfDriver.isFactorySupportsTranslation("DataModelTranslatorFactory", dmTranslatorFactoryIntf.properties(), "testXml", "testJson")).thenReturn(false);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
		verify(csDriver).collectDataModelTranslatorCandidates(List.of(model));
		verify(csDriver).collectDataModelTranslatorFactoryCandidates();
		verify(dmfDriver).isFactorySupportsTranslation("DataModelTranslatorFactory", dmTranslatorFactoryIntf.properties(), "testXml", "testJson");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:MagicNumber", "checkstyle:MethodLength" })
	@Test
	public void testDoDiscoveryOk1() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", "testXml");
		opMap.put("output", "testXml");
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final ServiceInstanceInterfaceResponseDTO dmTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12347));

		final ServiceInstanceResponseDTO dataModelTranslator = new ServiceInstanceResponseDTO(
				"DataModelTranslator|dataModelTranslation|1.0.0",
				new SystemResponseDTO("DataModelTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("dataModelTranslation", null, null),
				"1.0.0",
				null,
				Map.of("dataModelIds", List.of(List.of("testXml", "testJson"), List.of("testJson", "testXml"))),
				List.of(dmTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				"testJson");
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetInputDataModelId("testXml");
		model.setTargetOutputDataModelId("testXml");

		final TranslationDiscoveryModel model2 = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				"testJson");
		model2.setFromInterfaceTemplate("generic_mqtt");
		model2.setToInterfaceTemplate("generic_http");
		model2.setInterfaceTranslator("InterfaceTranslator");
		model2.setInterfaceTranslatorPolicy("NONE");
		model2.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model2.setTargetPolicy("NONE");
		model2.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model2.setTargetInputDataModelId("testXml");
		model2.setTargetOutputDataModelId("testXml");
		model2.setInputDataModelTranslator("DataModelTranslator");
		model2.setInputDataModelTranslatorProperties(dmTranslatorIntf.properties());
		model2.setInputDataModelTranslatorFactory(false);
		model2.setOutputDataModelTranslator("DataModelTranslator");
		model2.setOutputDataModelTranslatorProperties(dmTranslatorIntf.properties());
		model2.setOutputDataModelTranslatorFactory(false);

		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final TranslationDiscoveryResponseDTO expected = new TranslationDiscoveryResponseDTO(
				bridgeId.toString(),
				List.of(new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http")));

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(sysInfo.getSystemName()).thenReturn("TranslationManager");
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null)).thenReturn(List.of("InterfaceTranslator"));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of(dataModelTranslator));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("DataModelTranslator"), "InterfaceTranslator", "dataModelTranslation", null)).thenReturn(List.of("DataModelTranslator"));
		when(dataModelTranslatorMatchmaker.doMatchmaking(List.of(dataModelTranslator), Map.of())).thenReturn(dataModelTranslator);

		try (MockedStatic<UUID> mockedUUID = Mockito.mockStatic(UUID.class)) {
			mockedUUID.when(() -> UUID.randomUUID()).thenReturn(bridgeId);
			when(dbService.storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model2))).thenReturn(null);
			when(converter.convertDiscoveryModels(bridgeId, List.of(model2))).thenReturn(expected);

			final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

			assertNotNull(result);
			assertEquals(expected, result);

			verify(csDriver, never()).isBlacklisted("TestConsumer");
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
			verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
			verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
			verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
			verify(sysInfo).getSystemName();
			verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
			verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
			verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
			verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
			verify(csDriver).collectDataModelTranslatorCandidates(List.of(model2));
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("DataModelTranslator"));
			verify(csDriver, times(2)).filterOutProvidersBecauseOfUnauthorization(List.of("DataModelTranslator"), "InterfaceTranslator", "dataModelTranslation", null);
			verify(dataModelTranslatorMatchmaker, times(2)).doMatchmaking(List.of(dataModelTranslator), Map.of());
			mockedUUID.verify(() -> UUID.randomUUID());
			verify(dbService).storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model2));
			verify(converter).convertDiscoveryModels(bridgeId, List.of(model2));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:MagicNumber", "checkstyle:MethodLength" })
	@Test
	public void testDoDiscoveryOk2() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", "testXml");
		opMap.put("output", null);
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final ServiceInstanceInterfaceResponseDTO dmTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12347));

		final ServiceInstanceResponseDTO dataModelTranslator = new ServiceInstanceResponseDTO(
				"DataModelTranslator|dataModelTranslation|1.0.0",
				new SystemResponseDTO("DataModelTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("dataModelTranslation", null, null),
				"1.0.0",
				null,
				Map.of("dataModelIds", List.of(List.of("testXml", "testJson"), List.of("testJson", "testXml"))),
				List.of(dmTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				null);
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetInputDataModelId("testXml");

		final TranslationDiscoveryModel model2 = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				null);
		model2.setFromInterfaceTemplate("generic_mqtt");
		model2.setToInterfaceTemplate("generic_http");
		model2.setInterfaceTranslator("InterfaceTranslator");
		model2.setInterfaceTranslatorPolicy("NONE");
		model2.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model2.setTargetPolicy("NONE");
		model2.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model2.setTargetInputDataModelId("testXml");
		model2.setInputDataModelTranslator("DataModelTranslator");
		model2.setInputDataModelTranslatorProperties(dmTranslatorIntf.properties());
		model2.setInputDataModelTranslatorFactory(false);

		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final TranslationDiscoveryResponseDTO expected = new TranslationDiscoveryResponseDTO(
				bridgeId.toString(),
				List.of(new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http")));

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of(dataModelTranslator));
		when(dataModelTranslatorMatchmaker.doMatchmaking(List.of(dataModelTranslator), Map.of())).thenReturn(dataModelTranslator);

		try (MockedStatic<UUID> mockedUUID = Mockito.mockStatic(UUID.class)) {
			mockedUUID.when(() -> UUID.randomUUID()).thenReturn(bridgeId);
			when(dbService.storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model2))).thenReturn(null);
			when(converter.convertDiscoveryModels(bridgeId, List.of(model2))).thenReturn(expected);

			final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

			assertNotNull(result);
			assertEquals(expected, result);

			verify(csDriver, never()).isBlacklisted("TestConsumer");
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
			verify(dataModelIdentifierNormalizer).normalize("testXml");
			verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
			verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
			verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
			verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
			verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
			verify(csDriver).collectDataModelTranslatorCandidates(List.of(model2));
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("DataModelTranslator"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("DataModelTranslator"), "InterfaceTranslator", "dataModelTranslation", null);
			verify(dataModelTranslatorMatchmaker).doMatchmaking(List.of(dataModelTranslator), Map.of());
			mockedUUID.verify(() -> UUID.randomUUID());
			verify(dbService).storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model2));
			verify(converter).convertDiscoveryModels(bridgeId, List.of(model2));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:MagicNumber", "checkstyle:MethodLength" })
	@Test
	public void testDoDiscoveryOk3() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", "testXml");
		opMap.put("output", null);
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final ServiceInstanceInterfaceResponseDTO dmTranslatorFactoryIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12347));

		final ServiceInstanceResponseDTO dataModelTranslatorFactory = new ServiceInstanceResponseDTO(
				"DataModelTranslatorFactory|dataModelTranslatorFactoryControl|1.0.0",
				new SystemResponseDTO("DataModelTranslatorFactory", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("dataModelTranslatorFactoryControl", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(dmTranslatorFactoryIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				null);
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetInputDataModelId("testXml");

		final TranslationDiscoveryModel model2 = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				null);
		model2.setFromInterfaceTemplate("generic_mqtt");
		model2.setToInterfaceTemplate("generic_http");
		model2.setInterfaceTranslator("InterfaceTranslator");
		model2.setInterfaceTranslatorPolicy("NONE");
		model2.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model2.setTargetPolicy("NONE");
		model2.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model2.setTargetInputDataModelId("testXml");
		model2.setInputDataModelTranslator("DataModelTranslatorFactory");
		model2.setInputDataModelTranslatorProperties(dmTranslatorFactoryIntf.properties());
		model2.setInputDataModelTranslatorFactory(true);

		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final TranslationDiscoveryResponseDTO expected = new TranslationDiscoveryResponseDTO(
				bridgeId.toString(),
				List.of(new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http")));

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of());
		when(csDriver.collectDataModelTranslatorFactoryCandidates()).thenReturn(List.of(dataModelTranslatorFactory));
		when(dmfDriver.isFactorySupportsTranslation("DataModelTranslatorFactory", dmTranslatorFactoryIntf.properties(), "testJson", "testXml")).thenReturn(true);

		try (MockedStatic<UUID> mockedUUID = Mockito.mockStatic(UUID.class)) {
			mockedUUID.when(() -> UUID.randomUUID()).thenReturn(bridgeId);
			when(dbService.storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model2))).thenReturn(null);
			when(converter.convertDiscoveryModels(bridgeId, List.of(model2))).thenReturn(expected);

			final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

			assertNotNull(result);
			assertEquals(expected, result);

			verify(csDriver, never()).isBlacklisted("TestConsumer");
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
			verify(dataModelIdentifierNormalizer).normalize("testXml");
			verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
			verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
			verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
			verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
			verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
			verify(csDriver).collectDataModelTranslatorCandidates(List.of(model2));
			verify(csDriver).collectDataModelTranslatorFactoryCandidates();
			verify(dmfDriver).isFactorySupportsTranslation("DataModelTranslatorFactory", dmTranslatorFactoryIntf.properties(), "testJson", "testXml");
			mockedUUID.verify(() -> UUID.randomUUID());
			verify(dbService).storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model2));
			verify(converter).convertDiscoveryModels(bridgeId, List.of(model2));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doNegotiation(null, null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationTargetInstanceIdNull() {
		final UUID bridgeId = UUID.randomUUID();

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doNegotiation(bridgeId, null, null));

		assertEquals("targetInstanceId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationTargetInstanceIdEmpty() {
		final UUID bridgeId = UUID.randomUUID();

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doNegotiation(bridgeId, "", null));

		assertEquals("targetInstanceId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationOriginNull() {
		final UUID bridgeId = UUID.randomUUID();

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationOriginEmpty() {
		final UUID bridgeId = UUID.randomUUID();

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationInvalidParameterExceptionNotStored() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(dbService, never()).storeBridgeProblem(eq(bridgeId), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationInternalServerErrorNotStored() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(dbService, never()).storeBridgeProblem(eq(bridgeId), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationInvalidParameterExceptionStored() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setInterfaceTranslator("InterfaceTranslator");
		final BridgeDetails details = new BridgeDetails();

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenReturn(Pair.of(model, details));
		when(sysInfo.isCustomConfigurationEnabled()).thenReturn(true);
		when(csDriver.getConfigurationForSystem("InterfaceTranslator")).thenThrow(new InvalidParameterException("test"));
		doNothing().when(dbService).storeBridgeProblem(bridgeId, "test");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(sysInfo).isCustomConfigurationEnabled();
		verify(csDriver).getConfigurationForSystem("InterfaceTranslator");
		verify(dbService).storeBridgeProblem(bridgeId, "test");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationDataNotFoundExceptionStored() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setInterfaceTranslator("InterfaceTranslator");
		final BridgeDetails details = new BridgeDetails();

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenReturn(Pair.of(model, details));
		when(sysInfo.isCustomConfigurationEnabled()).thenReturn(true);
		when(csDriver.getConfigurationForSystem("InterfaceTranslator")).thenThrow(new DataNotFoundException("test"));
		doNothing().when(dbService).storeBridgeProblem(bridgeId, "test");

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(sysInfo).isCustomConfigurationEnabled();
		verify(csDriver).getConfigurationForSystem("InterfaceTranslator");
		verify(dbService).storeBridgeProblem(bridgeId, "test");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoNegotiationExternalServerErrorStored() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setInterfaceTranslator("InterfaceTranslator");
		final BridgeDetails details = new BridgeDetails();

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenReturn(Pair.of(model, details));
		when(sysInfo.isCustomConfigurationEnabled()).thenReturn(true);
		when(csDriver.getConfigurationForSystem("InterfaceTranslator")).thenThrow(new ExternalServerError("test"));
		doNothing().when(dbService).storeBridgeProblem(bridgeId, "test");

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(sysInfo).isCustomConfigurationEnabled();
		verify(csDriver).getConfigurationForSystem("InterfaceTranslator");
		verify(dbService).storeBridgeProblem(bridgeId, "test");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoNegotiationBridgeIsInEndState() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenReturn(Pair.of(model, details));
		when(sysInfo.isCustomConfigurationEnabled()).thenReturn(false);
		when(dbService.updateDetailsRecord(details)).thenReturn(true);
		doNothing().when(dbService).storeBridgeProblem(bridgeId, "Bridge is already in an end state");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin"));

		assertEquals("Bridge is already in an end state", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(sysInfo).isCustomConfigurationEnabled();
		verify(csDriver, never()).getConfigurationForSystem("InterfaceTranslator");
		verify(dbService).updateDetailsRecord(details);
		verify(dbService).storeBridgeProblem(bridgeId, "Bridge is already in an end state");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoNegotiationErrorInBridgeInitialization() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setInputDataModelIdRequirement("testJson");
		model.setOutputDataModelIdRequirement("testJson2");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model.setInputDataModelTranslatorFactory(false);
		model.setInputDataModelTranslator("DataModelTranslator");
		model.setTargetInputDataModelId("testXml");
		model.setInputDataModelTranslatorProperties(Map.of("accessPort", 33333));
		model.setOutputDataModelTranslatorFactory(true);
		model.setOutputDataModelTranslatorProperties(Map.of("accessPort", 44444));
		model.setTargetOutputDataModelId("testXml2");
		model.setTargetPolicy("NONE");

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);

		final TranslationDiscoveryModel model2 = new TranslationDiscoveryModel();
		model2.setInputDataModelIdRequirement("testJson");
		model2.setOutputDataModelIdRequirement("testJson2");
		model2.setInterfaceTranslator("InterfaceTranslator");
		model2.setFromInterfaceTemplate("generic_http");
		model2.setToInterfaceTemplate("generic_mqtt");
		model2.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model2.setInputDataModelTranslatorFactory(false);
		model2.setInputDataModelTranslator("DataModelTranslator");
		model2.setTargetInputDataModelId("testXml");
		model2.setInputDataModelTranslatorProperties(Map.of("accessPort", 33333));
		model2.setOutputDataModelTranslator("GeneratedDataModelTranslator");
		model2.setOutputDataModelTranslatorFactory(false);
		model2.setOutputDataModelTranslatorProperties(Map.of("accessPort", 55555));
		model2.setTargetOutputDataModelId("testXml2");
		model2.setTargetPolicy("NONE");

		final TranslationDataModelTranslatorInitializationResponseDTO initTranslatorResponse = new TranslationDataModelTranslatorInitializationResponseDTO(
				"GeneratedDataModelTranslator",
				Map.of("accessPort", 55555));

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenReturn(Pair.of(model, details));
		when(sysInfo.isCustomConfigurationEnabled()).thenReturn(true);
		when(csDriver.getConfigurationForSystem("InterfaceTranslator")).thenReturn(null);
		when(csDriver.getConfigurationForSystem("DataModelTranslator")).thenReturn(Map.of("a", "b"));
		when(dmfDriver.initializeDataModelTranslator(null, Map.of("accessPort", 44444), "testXml2", "testJson2")).thenReturn(initTranslatorResponse);
		when(dbService.updateDetailsRecord(details)).thenReturn(false);
		when(itDriver.initializeBridge(bridgeId, model2, null, null, null, Map.of("a", "b"), null)).thenReturn(Pair.of(Optional.empty(), Optional.of(new ExternalServerError("test"))));
		doNothing().when(dbService).storeBridgeProblem(bridgeId, "test");

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(sysInfo, times(2)).isCustomConfigurationEnabled();
		verify(csDriver).getConfigurationForSystem("InterfaceTranslator");
		verify(csDriver).getConfigurationForSystem("DataModelTranslator");
		verify(dmfDriver).initializeDataModelTranslator(null, Map.of("accessPort", 44444), "testXml2", "testJson2");
		verify(dbService).updateDetailsRecord(details);
		verify(itDriver).initializeBridge(bridgeId, model2, null, null, null, Map.of("a", "b"), null);
		verify(dbService).storeBridgeProblem(bridgeId, "test");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoNegotiationWrongPolicyInInterfaceTranslator() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		model.setServiceDefinition("testService");
		model.setOperation("test-operation");
		model.setInputDataModelIdRequirement("testJson");
		model.setOutputDataModelIdRequirement("testJson2");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model.setInputDataModelTranslatorFactory(true);
		model.setTargetInputDataModelId("testXml");
		model.setInputDataModelTranslatorProperties(Map.of("accessPort", 33333));
		model.setOutputDataModelTranslatorFactory(false);
		model.setOutputDataModelTranslator("DataModelTranslator");
		model.setTargetOutputDataModelId("testXml2");
		model.setOutputDataModelTranslatorProperties(Map.of("accessPort", 44444));
		model.setTargetPolicy("USAGE_LIMITED_TOKEN_AUTH");

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);

		final TranslationDiscoveryModel model2 = new TranslationDiscoveryModel();
		model2.setTargetInstanceId("TestProvider|testService|1.0.0");
		model2.setServiceDefinition("testService");
		model2.setOperation("test-operation");
		model2.setInputDataModelIdRequirement("testJson");
		model2.setOutputDataModelIdRequirement("testJson2");
		model2.setInterfaceTranslator("InterfaceTranslator");
		model2.setFromInterfaceTemplate("generic_http");
		model2.setToInterfaceTemplate("generic_mqtt");
		model2.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model2.setInputDataModelTranslatorFactory(false);
		model2.setTargetInputDataModelId("testXml");
		model2.setInputDataModelTranslator("GeneratedDataModelTranslator");
		model2.setInputDataModelTranslatorProperties(Map.of("accessPort", 55555));
		model2.setOutputDataModelTranslatorFactory(false);
		model2.setOutputDataModelTranslator("DataModelTranslator");
		model2.setTargetOutputDataModelId("testXml2");
		model2.setOutputDataModelTranslatorProperties(Map.of("accessPort", 44444));
		model2.setTargetPolicy("USAGE_LIMITED_TOKEN_AUTH");

		final TranslationDataModelTranslatorInitializationResponseDTO initTranslatorResponse = new TranslationDataModelTranslatorInitializationResponseDTO(
				"GeneratedDataModelTranslator",
				Map.of("accessPort", 55555));

		final ServiceInstanceInterfaceResponseDTO bridgeResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of("accessPort", 22223));

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenReturn(Pair.of(model, details));
		when(sysInfo.isCustomConfigurationEnabled()).thenReturn(true);
		when(csDriver.getConfigurationForSystem("InterfaceTranslator")).thenReturn(null);
		when(dmfDriver.initializeDataModelTranslator(null, Map.of("accessPort", 33333), "testJson", "testXml")).thenReturn(initTranslatorResponse);
		when(csDriver.getConfigurationForSystem("DataModelTranslator")).thenReturn(Map.of("a", "b"));
		when(csDriver.generateTokenForInterfaceTranslatorToTargetOperation(
				"USAGE_LIMITED_TOKEN_AUTH",
				"InterfaceTranslator",
				"TestProvider",
				"testService",
				"test-operation")).thenReturn(new AuthorizationTokenResponseDTO(null, null, "token", null, null, null, null, null, null, null, null, null, null, null, null));
		when(dbService.updateDetailsRecord(details)).thenReturn(false);
		when(itDriver.initializeBridge(bridgeId, model2, "token", null, null, null, Map.of("a", "b"))).thenReturn(Pair.of(Optional.of(bridgeResponse), Optional.empty()));
		doNothing().when(dbService).storeBridgeProblem(bridgeId, "Interface provider returns with invalid data");
		doNothing().when(itDriver).abortBridge(bridgeId, Map.of("accessPort", 22222), null);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin"));

		assertEquals("Interface provider returns with invalid data", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(sysInfo, times(2)).isCustomConfigurationEnabled();
		verify(csDriver).getConfigurationForSystem("InterfaceTranslator");
		verify(dmfDriver).initializeDataModelTranslator(null, Map.of("accessPort", 33333), "testJson", "testXml");
		verify(csDriver).getConfigurationForSystem("DataModelTranslator");
		verify(csDriver).generateTokenForInterfaceTranslatorToTargetOperation(
				"USAGE_LIMITED_TOKEN_AUTH",
				"InterfaceTranslator",
				"TestProvider",
				"testService",
				"test-operation");
		verify(dbService).updateDetailsRecord(details);
		verify(itDriver).initializeBridge(bridgeId, model2, "token", null, null, null, Map.of("a", "b"));
		verify(dbService).storeBridgeProblem(bridgeId, "Interface provider returns with invalid data");
		verify(itDriver).abortBridge(bridgeId, Map.of("accessPort", 22222), null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoNegotiationIsInEndState2() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		model.setServiceDefinition("testService");
		model.setOperation("test-operation");
		model.setInputDataModelIdRequirement("testJson");
		model.setOutputDataModelIdRequirement("testJson2");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model.setInputDataModelTranslatorFactory(true);
		model.setTargetInputDataModelId("testXml");
		model.setInputDataModelTranslatorProperties(Map.of("accessPort", 33333));
		model.setOutputDataModelTranslatorFactory(false);
		model.setOutputDataModelTranslator("DataModelTranslator");
		model.setTargetOutputDataModelId("testXml2");
		model.setOutputDataModelTranslatorProperties(Map.of("accessPort", 44444));
		model.setTargetPolicy("USAGE_LIMITED_TOKEN_AUTH");

		final BridgeHeader header = new BridgeHeader(bridgeId, "TestCreator");
		header.setId(1L);
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);

		final TranslationDiscoveryModel model2 = new TranslationDiscoveryModel();
		model2.setTargetInstanceId("TestProvider|testService|1.0.0");
		model2.setServiceDefinition("testService");
		model2.setOperation("test-operation");
		model2.setInputDataModelIdRequirement("testJson");
		model2.setOutputDataModelIdRequirement("testJson2");
		model2.setInterfaceTranslator("InterfaceTranslator");
		model2.setFromInterfaceTemplate("generic_http");
		model2.setToInterfaceTemplate("generic_mqtt");
		model2.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model2.setInputDataModelTranslatorFactory(false);
		model2.setTargetInputDataModelId("testXml");
		model2.setInputDataModelTranslator("GeneratedDataModelTranslator");
		model2.setInputDataModelTranslatorProperties(Map.of("accessPort", 55555));
		model2.setOutputDataModelTranslatorFactory(false);
		model2.setOutputDataModelTranslator("DataModelTranslator");
		model2.setTargetOutputDataModelId("testXml2");
		model2.setOutputDataModelTranslatorProperties(Map.of("accessPort", 44444));
		model2.setTargetPolicy("USAGE_LIMITED_TOKEN_AUTH");

		final TranslationDataModelTranslatorInitializationResponseDTO initTranslatorResponse = new TranslationDataModelTranslatorInitializationResponseDTO(
				"GeneratedDataModelTranslator",
				Map.of("accessPort", 55555));

		final ServiceInstanceInterfaceResponseDTO bridgeResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TRANSLATION_BRIDGE_TOKEN_AUTH", Map.of("accessPort", 22223));

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenReturn(Pair.of(model, details));
		when(sysInfo.isCustomConfigurationEnabled()).thenReturn(true);
		when(csDriver.getConfigurationForSystem("InterfaceTranslator")).thenReturn(null);
		when(dmfDriver.initializeDataModelTranslator(null, Map.of("accessPort", 33333), "testJson", "testXml")).thenReturn(initTranslatorResponse);
		when(csDriver.getConfigurationForSystem("DataModelTranslator")).thenReturn(Map.of("a", "b"));
		when(csDriver.generateTokenForInterfaceTranslatorToTargetOperation(
				"USAGE_LIMITED_TOKEN_AUTH",
				"InterfaceTranslator",
				"TestProvider",
				"testService",
				"test-operation")).thenReturn(new AuthorizationTokenResponseDTO(null, null, "token", null, null, null, null, null, null, null, null, null, null, null, null));
		when(dbService.updateDetailsRecord(details)).thenReturn(false);
		when(itDriver.initializeBridge(bridgeId, model2, "token", null, null, null, Map.of("a", "b"))).thenReturn(Pair.of(Optional.of(bridgeResponse), Optional.empty()));
		when(dbService.bridgeInitialized(header)).thenReturn(true);
		doNothing().when(itDriver).abortBridge(bridgeId, Map.of("accessPort", 22222), null);
		doNothing().when(dbService).storeBridgeProblem(bridgeId, "Bridge is already in an end state");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin"));

		assertEquals("Bridge is already in an end state", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(sysInfo, times(2)).isCustomConfigurationEnabled();
		verify(csDriver).getConfigurationForSystem("InterfaceTranslator");
		verify(dmfDriver).initializeDataModelTranslator(null, Map.of("accessPort", 33333), "testJson", "testXml");
		verify(csDriver).getConfigurationForSystem("DataModelTranslator");
		verify(csDriver).generateTokenForInterfaceTranslatorToTargetOperation(
				"USAGE_LIMITED_TOKEN_AUTH",
				"InterfaceTranslator",
				"TestProvider",
				"testService",
				"test-operation");
		verify(dbService).updateDetailsRecord(details);
		verify(itDriver).initializeBridge(bridgeId, model2, "token", null, null, null, Map.of("a", "b"));
		verify(dbService).bridgeInitialized(header);
		verify(itDriver).abortBridge(bridgeId, Map.of("accessPort", 22222), null);
		verify(dbService).storeBridgeProblem(bridgeId, "Bridge is already in an end state");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoNegotiationOk1() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		model.setServiceDefinition("testService");
		model.setOperation("test-operation");
		model.setInputDataModelIdRequirement("testJson");
		model.setOutputDataModelIdRequirement("testJson2");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model.setInputDataModelTranslatorFactory(true);
		model.setTargetInputDataModelId("testXml");
		model.setInputDataModelTranslatorProperties(Map.of("accessPort", 33333));
		model.setOutputDataModelTranslatorFactory(false);
		model.setOutputDataModelTranslator("DataModelTranslator");
		model.setTargetOutputDataModelId("testXml2");
		model.setOutputDataModelTranslatorProperties(Map.of("accessPort", 44444));
		model.setTargetPolicy("USAGE_LIMITED_TOKEN_AUTH");

		final BridgeHeader header = new BridgeHeader(bridgeId, "TestCreator");
		header.setId(1L);
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);

		final TranslationDiscoveryModel model2 = new TranslationDiscoveryModel();
		model2.setTargetInstanceId("TestProvider|testService|1.0.0");
		model2.setServiceDefinition("testService");
		model2.setOperation("test-operation");
		model2.setInputDataModelIdRequirement("testJson");
		model2.setOutputDataModelIdRequirement("testJson2");
		model2.setInterfaceTranslator("InterfaceTranslator");
		model2.setFromInterfaceTemplate("generic_http");
		model2.setToInterfaceTemplate("generic_mqtt");
		model2.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model2.setInputDataModelTranslatorFactory(false);
		model2.setTargetInputDataModelId("testXml");
		model2.setInputDataModelTranslator("GeneratedDataModelTranslator");
		model2.setInputDataModelTranslatorProperties(Map.of("accessPort", 55555));
		model2.setOutputDataModelTranslatorFactory(false);
		model2.setOutputDataModelTranslator("DataModelTranslator");
		model2.setTargetOutputDataModelId("testXml2");
		model2.setOutputDataModelTranslatorProperties(Map.of("accessPort", 44444));
		model2.setTargetPolicy("USAGE_LIMITED_TOKEN_AUTH");

		final TranslationDataModelTranslatorInitializationResponseDTO initTranslatorResponse = new TranslationDataModelTranslatorInitializationResponseDTO(
				"GeneratedDataModelTranslator",
				Map.of("accessPort", 55555));

		final ServiceInstanceInterfaceResponseDTO bridgeResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TRANSLATION_BRIDGE_TOKEN_AUTH", Map.of("accessPort", 22223));

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenReturn(Pair.of(model, details));
		when(sysInfo.isCustomConfigurationEnabled()).thenReturn(true);
		when(csDriver.getConfigurationForSystem("InterfaceTranslator")).thenReturn(null);
		when(dmfDriver.initializeDataModelTranslator(null, Map.of("accessPort", 33333), "testJson", "testXml")).thenReturn(initTranslatorResponse);
		when(csDriver.getConfigurationForSystem("DataModelTranslator")).thenReturn(Map.of("a", "b"));
		when(csDriver.generateTokenForInterfaceTranslatorToTargetOperation(
				"USAGE_LIMITED_TOKEN_AUTH",
				"InterfaceTranslator",
				"TestProvider",
				"testService",
				"test-operation")).thenReturn(new AuthorizationTokenResponseDTO(null, null, "token", null, null, null, null, null, null, null, null, null, 10, null, "aDate"));
		when(dbService.updateDetailsRecord(details)).thenReturn(false);
		when(itDriver.initializeBridge(bridgeId, model2, "token", null, null, null, Map.of("a", "b"))).thenReturn(Pair.of(Optional.of(bridgeResponse), Optional.empty()));
		when(dbService.bridgeInitialized(header)).thenReturn(false);

		final TranslationNegotiationResponseDTO result = engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin");

		assertNotNull(result);
		assertEquals(bridgeId.toString(), result.bridgeId());
		assertEquals(bridgeResponse, result.bridgeInterface());
		assertEquals("aDate", result.tokenExpiresAt());
		assertEquals(10, result.tokenUsageLimit());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(sysInfo, times(2)).isCustomConfigurationEnabled();
		verify(csDriver).getConfigurationForSystem("InterfaceTranslator");
		verify(dmfDriver).initializeDataModelTranslator(null, Map.of("accessPort", 33333), "testJson", "testXml");
		verify(csDriver).getConfigurationForSystem("DataModelTranslator");
		verify(csDriver).generateTokenForInterfaceTranslatorToTargetOperation(
				"USAGE_LIMITED_TOKEN_AUTH",
				"InterfaceTranslator",
				"TestProvider",
				"testService",
				"test-operation");
		verify(dbService).updateDetailsRecord(details);
		verify(itDriver).initializeBridge(bridgeId, model2, "token", null, null, null, Map.of("a", "b"));
		verify(dbService).bridgeInitialized(header);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoNegotiationOk2() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		model.setServiceDefinition("testService");
		model.setOperation("test-operation");
		model.setInputDataModelIdRequirement("testJson");
		model.setOutputDataModelIdRequirement("testJson2");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model.setInputDataModelTranslatorFactory(true);
		model.setTargetInputDataModelId("testXml");
		model.setInputDataModelTranslatorProperties(Map.of("accessPort", 33333));
		model.setOutputDataModelTranslatorFactory(false);
		model.setOutputDataModelTranslator("DataModelTranslator");
		model.setTargetOutputDataModelId("testXml2");
		model.setOutputDataModelTranslatorProperties(Map.of("accessPort", 44444));
		model.setTargetPolicy("NONE");

		final BridgeHeader header = new BridgeHeader(bridgeId, "TestCreator");
		header.setId(1L);
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);

		final TranslationDiscoveryModel model2 = new TranslationDiscoveryModel();
		model2.setTargetInstanceId("TestProvider|testService|1.0.0");
		model2.setServiceDefinition("testService");
		model2.setOperation("test-operation");
		model2.setInputDataModelIdRequirement("testJson");
		model2.setOutputDataModelIdRequirement("testJson2");
		model2.setInterfaceTranslator("InterfaceTranslator");
		model2.setFromInterfaceTemplate("generic_http");
		model2.setToInterfaceTemplate("generic_mqtt");
		model2.setInterfaceTranslatorProperties(Map.of("accessPort", 22222));
		model2.setInputDataModelTranslatorFactory(false);
		model2.setTargetInputDataModelId("testXml");
		model2.setInputDataModelTranslator("GeneratedDataModelTranslator");
		model2.setInputDataModelTranslatorProperties(Map.of("accessPort", 55555));
		model2.setOutputDataModelTranslatorFactory(false);
		model2.setOutputDataModelTranslator("DataModelTranslator");
		model2.setTargetOutputDataModelId("testXml2");
		model2.setOutputDataModelTranslatorProperties(Map.of("accessPort", 44444));
		model2.setTargetPolicy("NONE");

		final TranslationDataModelTranslatorInitializationResponseDTO initTranslatorResponse = new TranslationDataModelTranslatorInitializationResponseDTO(
				"GeneratedDataModelTranslator",
				Map.of("accessPort", 55555));

		final ServiceInstanceInterfaceResponseDTO bridgeResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TRANSLATION_BRIDGE_TOKEN_AUTH", Map.of("accessPort", 22223));

		when(dbService.selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0")).thenReturn(Pair.of(model, details));
		when(sysInfo.isCustomConfigurationEnabled()).thenReturn(true);
		when(csDriver.getConfigurationForSystem("InterfaceTranslator")).thenReturn(null);
		when(dmfDriver.initializeDataModelTranslator(null, Map.of("accessPort", 33333), "testJson", "testXml")).thenReturn(initTranslatorResponse);
		when(csDriver.getConfigurationForSystem("DataModelTranslator")).thenReturn(Map.of("a", "b"));
		when(dbService.updateDetailsRecord(details)).thenReturn(false);
		when(itDriver.initializeBridge(bridgeId, model2, null, null, null, null, Map.of("a", "b"))).thenReturn(Pair.of(Optional.of(bridgeResponse), Optional.empty()));
		when(dbService.bridgeInitialized(header)).thenReturn(false);

		final TranslationNegotiationResponseDTO result = engine.doNegotiation(bridgeId, "TestProvider|testService|1.0.0", "origin");

		assertNotNull(result);
		assertEquals(bridgeId.toString(), result.bridgeId());
		assertEquals(bridgeResponse, result.bridgeInterface());
		assertNull(result.tokenExpiresAt());
		assertNull(result.tokenUsageLimit());

		verify(dbService).selectBridgeFromDiscoveries(bridgeId, "TestProvider|testService|1.0.0");
		verify(sysInfo, times(2)).isCustomConfigurationEnabled();
		verify(csDriver).getConfigurationForSystem("InterfaceTranslator");
		verify(dmfDriver).initializeDataModelTranslator(null, Map.of("accessPort", 33333), "testJson", "testXml");
		verify(csDriver).getConfigurationForSystem("DataModelTranslator");
		verify(dbService).updateDetailsRecord(details);
		verify(itDriver).initializeBridge(bridgeId, model2, null, null, null, null, Map.of("a", "b"));
		verify(dbService).bridgeInitialized(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoAbortBridgeIdsListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doAbort(null, null, null));

		assertEquals("bridge identifier list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoAbortBridgeIdsListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doAbort(List.of(), null, null));

		assertEquals("bridge identifier list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoAbortBridgeIdsListContainsNull() {
		final List<UUID> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doAbort(list, null, null));

		assertEquals("bridge identifier list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoAbortOriginNull() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doAbort(List.of(bridgeId), null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoAbortOriginEmpty() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doAbort(List.of(bridgeId), null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoAbortForbiddenException() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");

		when(dbService.abortBridge(bridgeId, "ANobody")).thenThrow(new ForbiddenException("test"));

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> engine.doAbort(List.of(bridgeId), "ANobody", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).abortBridge(bridgeId, "ANobody");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoAbortInternalServerError() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");

		when(dbService.abortBridge(bridgeId, "ANobody")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> engine.doAbort(List.of(bridgeId), "ANobody", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(dbService).abortBridge(bridgeId, "ANobody");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoAbortNonActiveBridge() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final AbortResult abortResult = new AbortResult(true, TranslationBridgeStatus.NEW, null);

		when(dbService.abortBridge(bridgeId, "ANobody")).thenReturn(abortResult);

		final Map<String, Boolean> result = engine.doAbort(List.of(bridgeId), "ANobody", "origin");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.get(bridgeId.toString()));

		verify(dbService).abortBridge(bridgeId, "ANobody");
		verify(itDriver, never()).abortBridge(eq(bridgeId), anyMap(), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoAbortActiveBridge() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setInterfaceTranslatorData(Utilities.toJson(Map.of(
				"fromInterfaceTemplate", "generic_http",
				"toInterfaceTemplate", "generic_mqtt",
				"token", "itToken",
				"interfaceProperties", Map.of("accessPort", 12345))));

		final AbortResult abortResult = new AbortResult(true, TranslationBridgeStatus.INITIALIZED, details);

		when(dbService.abortBridge(bridgeId, "ANobody")).thenReturn(abortResult);
		doNothing().when(itDriver).abortBridge(bridgeId, Map.of("accessPort", 12345), "itToken");

		final Map<String, Boolean> result = engine.doAbort(List.of(bridgeId), "ANobody", "origin");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.get(bridgeId.toString()));

		verify(dbService).abortBridge(bridgeId, "ANobody");
		verify(itDriver).abortBridge(bridgeId, Map.of("accessPort", 12345), "itToken");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoAbortBridgeUnknown() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final AbortResult abortResult = new AbortResult(false, null, null);

		when(dbService.abortBridge(bridgeId, "ANobody")).thenReturn(abortResult);

		final Map<String, Boolean> result = engine.doAbort(List.of(bridgeId), "ANobody", "origin");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertFalse(result.get(bridgeId.toString()));

		verify(dbService).abortBridge(bridgeId, "ANobody");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoAbortBridgeInEndState() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final AbortResult abortResult = new AbortResult(false, TranslationBridgeStatus.CLOSED, null);

		when(dbService.abortBridge(bridgeId, "ANobody")).thenReturn(abortResult);

		final Map<String, Boolean> result = engine.doAbort(List.of(bridgeId), "ANobody", "origin");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.get(bridgeId.toString()));

		verify(dbService).abortBridge(bridgeId, "ANobody");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoAbortBridgeNotAborted() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final AbortResult abortResult = new AbortResult(false, TranslationBridgeStatus.USED, null);

		when(dbService.abortBridge(bridgeId, "ANobody")).thenReturn(abortResult);

		final Map<String, Boolean> result = engine.doAbort(List.of(bridgeId), "ANobody", "origin");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertFalse(result.get(bridgeId.toString()));

		verify(dbService).abortBridge(bridgeId, "ANobody");
	}
}