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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.TranslationBridgeInitializationRequestDTO;
import eu.arrowhead.dto.TranslationCheckTargetsRequestDTO;
import eu.arrowhead.dto.TranslationCheckTargetsResponseDTO;
import eu.arrowhead.dto.TranslationDataModelTranslationDataDescriptorDTO;
import eu.arrowhead.dto.TranslationTargetDTO;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;

@ExtendWith(MockitoExtension.class)
public class InterfaceTranslatorDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private InterfaceTranslatorDriver driver;

	@Mock
	private TranslationManagerSystemInfo sysInfo;

	@Mock
	private HttpService httpService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorTranslatorNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.filterOutNotAppropriateTargetsForInterfaceTranslator(null, null, null, null));

		assertEquals("interfaceTranslator is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorOperationNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(intf),
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, null, null, null));

		assertEquals("targetOperation is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorOperationEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(intf),
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, "", null, null));

		assertEquals("targetOperation is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorTargetsNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(intf),
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, null, "target-operation", null));

		assertEquals("targets list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorTargetsEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(intf),
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, null, "target-operation", List.of()));

		assertEquals("targets list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorTargetsContainsNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(intf),
				null,
				null);

		final List<NormalizedServiceInstanceDTO> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, null, "target-operation", list));

		assertEquals("targets list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorTargetsExceptionDuringWSCall() {
		final ServiceInstanceInterfaceResponseDTO translatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test"));

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(translatorIntf),
				null,
				null);

		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedServiceInstanceDTO target = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				List.of(targetIntf));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/check-targets");
		final TranslationCheckTargetsRequestDTO payload = new TranslationCheckTargetsRequestDTO(
				"target-operation",
				List.of(new TranslationTargetDTO("TestProvider|testService|1.0.0", List.of(targetIntf))));

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.POST,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of())).thenThrow(ArrowheadException.class);

		final List<NormalizedServiceInstanceDTO> result = driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, null, "target-operation", List.of(target));

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.POST,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorTargetsResponseNull() {
		final ServiceInstanceInterfaceResponseDTO translatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_https",
				"https",
				"NONE",
				Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", "string"));

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(translatorIntf),
				null,
				null);

		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedServiceInstanceDTO target = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				List.of(targetIntf));

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test/check-targets");
		final TranslationCheckTargetsRequestDTO payload = new TranslationCheckTargetsRequestDTO(
				"target-operation",
				List.of(new TranslationTargetDTO("TestProvider|testService|1.0.0", List.of(targetIntf))));

		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(httpService.sendRequest(
				uri,
				HttpMethod.POST,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of("Authorization", "Bearer token"))).thenReturn(null);

		final List<NormalizedServiceInstanceDTO> result = driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, "token", "target-operation", List.of(target));

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.POST,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of("Authorization", "Bearer token"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorTargetsResponseTargetsNull() {
		final ServiceInstanceInterfaceResponseDTO translatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", Map.of()));

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(translatorIntf),
				null,
				null);

		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedServiceInstanceDTO target = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				List.of(targetIntf));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/check-targets");
		final TranslationCheckTargetsRequestDTO payload = new TranslationCheckTargetsRequestDTO(
				"target-operation",
				List.of(new TranslationTargetDTO("TestProvider|testService|1.0.0", List.of(targetIntf))));

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.POST,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of())).thenReturn(new TranslationCheckTargetsResponseDTO(null));

		final List<NormalizedServiceInstanceDTO> result = driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, null, "target-operation", List.of(target));

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.POST,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorTargetsResponseTargetsEmpty() {
		final ServiceInstanceInterfaceResponseDTO translatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", Map.of("check-targets", "notAModel")));

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(translatorIntf),
				null,
				null);

		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedServiceInstanceDTO target = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				List.of(targetIntf));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/check-targets");
		final TranslationCheckTargetsRequestDTO payload = new TranslationCheckTargetsRequestDTO(
				"target-operation",
				List.of(new TranslationTargetDTO("TestProvider|testService|1.0.0", List.of(targetIntf))));

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.POST,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of())).thenReturn(new TranslationCheckTargetsResponseDTO(List.of()));

		final List<NormalizedServiceInstanceDTO> result = driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, null, "target-operation", List.of(target));

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.POST,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutNotAppropriateTargetsForInterfaceTranslatorTargetsOk() {
		final HttpOperationModel opModel = new HttpOperationModel("/check-targets2", "GET"); // different from default

		final ServiceInstanceInterfaceResponseDTO translatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", Map.of("check-targets", opModel)));

		final ServiceInstanceResponseDTO translator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(translatorIntf),
				null,
				null);

		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedServiceInstanceDTO target = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				List.of(targetIntf));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/check-targets2");

		final TranslationTargetDTO targetDTO = new TranslationTargetDTO("TestProvider|testService|1.0.0", List.of(targetIntf));

		final TranslationCheckTargetsRequestDTO payload = new TranslationCheckTargetsRequestDTO(
				"target-operation",
				List.of(targetDTO));

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.GET,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of())).thenReturn(new TranslationCheckTargetsResponseDTO(List.of(targetDTO)));

		final List<NormalizedServiceInstanceDTO> result = driver.filterOutNotAppropriateTargetsForInterfaceTranslator(translator, null, "target-operation", List.of(target));

		assertNotNull(result);
		assertEquals(1, result.size());
		final NormalizedServiceInstanceDTO resultDTO = result.get(0);
		assertEquals("TestProvider|testService|1.0.0", resultDTO.instanceId());
		assertEquals("TestProvider", resultDTO.provider());
		assertEquals("testService", resultDTO.serviceDefinition());
		assertEquals(List.of(targetIntf), resultDTO.interfaces());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.GET,
				TranslationCheckTargetsResponseDTO.class,
				payload,
				null,
				Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeBridge(null, null, null, null, null, null, null));

		assertEquals("bridgeId is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeBridgeModelNull() {
		final UUID bridgeId = UUID.fromString("7bbdef4a-f06f-4329-b82b-a3a0ff16589b");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeBridge(bridgeId, null, null, null, null, null, null));

		assertEquals("model is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeBridgeExceptionDuringWSCall() {
		final UUID bridgeId = UUID.fromString("7bbdef4a-f06f-4329-b82b-a3a0ff16589b");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				null,
				null);
		model.setInterfaceTranslatorProperties(Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test"));
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setTargetProperties(Map.of("a", "b"));

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test/initialize-bridge");

		final TranslationBridgeInitializationRequestDTO payload = new TranslationBridgeInitializationRequestDTO(
				"7bbdef4a-f06f-4329-b82b-a3a0ff16589b",
				"generic_http",
				null,
				null,
				null,
				null,
				"generic_mqtt",
				Map.of("a", "b"),
				"test-operation",
				"targetToken",
				Map.of("c", "d"));

		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(httpService.sendRequest(
				uri,
				HttpMethod.POST,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of("Authorization", "Bearer interfaceTranslatorToken"))).thenThrow(new ArrowheadException("test"));

		final Pair<Optional<ServiceInstanceInterfaceResponseDTO>, Optional<ArrowheadException>> result = driver.initializeBridge(
				bridgeId,
				model,
				"targetToken",
				Map.of("c", "d"),
				"interfaceTranslatorToken",
				null,
				null);

		assertNotNull(result);
		assertTrue(result.getFirst().isEmpty());
		assertTrue(result.getSecond().isPresent());
		assertEquals("test", result.getSecond().get().getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.POST,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of("Authorization", "Bearer interfaceTranslatorToken"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeBridgeOk1() {
		final UUID bridgeId = UUID.fromString("7bbdef4a-f06f-4329-b82b-a3a0ff16589b");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				"testJson");
		model.setInterfaceTranslatorProperties(Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", "text"));
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setTargetProperties(Map.of("a", "b"));
		model.setTargetInputDataModelId("testXml");
		model.setInputDataModelTranslatorProperties(Map.of("input", "data"));
		model.setTargetOutputDataModelId("testXml");
		model.setOutputDataModelTranslatorProperties(Map.of("result", "data"));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-bridge");

		final TranslationBridgeInitializationRequestDTO payload = new TranslationBridgeInitializationRequestDTO(
				"7bbdef4a-f06f-4329-b82b-a3a0ff16589b",
				"generic_http",
				new TranslationDataModelTranslationDataDescriptorDTO("testJson", "testXml", Map.of("input", "data"), Map.of("input", "settings")),
				new TranslationDataModelTranslationDataDescriptorDTO("testXml", "testJson", Map.of("result", "data"), Map.of("result", "settings")),
				"testJson",
				"testJson",
				"generic_mqtt",
				Map.of("a", "b"),
				"test-operation",
				"targetToken",
				Map.of("c", "d"));

		final ServiceInstanceInterfaceResponseDTO response = new ServiceInstanceInterfaceResponseDTO("generic_mqtt", "tcp", "NONE", Map.of("custom", "access"));

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.POST,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of())).thenReturn(response);

		final Pair<Optional<ServiceInstanceInterfaceResponseDTO>, Optional<ArrowheadException>> result = driver.initializeBridge(
				bridgeId,
				model,
				"targetToken",
				Map.of("c", "d"),
				null,
				Map.of("input", "settings"),
				Map.of("result", "settings"));

		assertNotNull(result);
		assertTrue(result.getFirst().isPresent());
		assertTrue(result.getSecond().isEmpty());
		assertEquals(response, result.getFirst().get());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.POST,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeBridgeOk2() {
		final UUID bridgeId = UUID.fromString("7bbdef4a-f06f-4329-b82b-a3a0ff16589b");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				"testJson");
		model.setInterfaceTranslatorProperties(Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", Map.of()));
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setTargetProperties(Map.of("a", "b"));
		model.setTargetInputDataModelId("testXml");
		model.setInputDataModelTranslatorProperties(Map.of("input", "data"));
		model.setTargetOutputDataModelId("testXml");
		model.setOutputDataModelTranslatorProperties(Map.of("result", "data"));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-bridge");

		final TranslationBridgeInitializationRequestDTO payload = new TranslationBridgeInitializationRequestDTO(
				"7bbdef4a-f06f-4329-b82b-a3a0ff16589b",
				"generic_http",
				new TranslationDataModelTranslationDataDescriptorDTO("testJson", "testXml", Map.of("input", "data"), Map.of("input", "settings")),
				new TranslationDataModelTranslationDataDescriptorDTO("testXml", "testJson", Map.of("result", "data"), Map.of("result", "settings")),
				"testJson",
				"testJson",
				"generic_mqtt",
				Map.of("a", "b"),
				"test-operation",
				"targetToken",
				Map.of("c", "d"));

		final ServiceInstanceInterfaceResponseDTO response = new ServiceInstanceInterfaceResponseDTO("generic_mqtt", "tcp", "NONE", Map.of("custom", "access"));

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.POST,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of())).thenReturn(response);

		final Pair<Optional<ServiceInstanceInterfaceResponseDTO>, Optional<ArrowheadException>> result = driver.initializeBridge(
				bridgeId,
				model,
				"targetToken",
				Map.of("c", "d"),
				null,
				Map.of("input", "settings"),
				Map.of("result", "settings"));

		assertNotNull(result);
		assertTrue(result.getFirst().isPresent());
		assertTrue(result.getSecond().isEmpty());
		assertEquals(response, result.getFirst().get());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.POST,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeBridgeOk3() {
		final UUID bridgeId = UUID.fromString("7bbdef4a-f06f-4329-b82b-a3a0ff16589b");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				"testJson");
		model.setInterfaceTranslatorProperties(Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", Map.of("initialize-bridge", "notAModel")));
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setTargetProperties(Map.of("a", "b"));
		model.setTargetInputDataModelId("testXml");
		model.setInputDataModelTranslatorProperties(Map.of("input", "data"));
		model.setTargetOutputDataModelId("testXml");
		model.setOutputDataModelTranslatorProperties(Map.of("result", "data"));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-bridge");

		final TranslationBridgeInitializationRequestDTO payload = new TranslationBridgeInitializationRequestDTO(
				"7bbdef4a-f06f-4329-b82b-a3a0ff16589b",
				"generic_http",
				new TranslationDataModelTranslationDataDescriptorDTO("testJson", "testXml", Map.of("input", "data"), Map.of("input", "settings")),
				new TranslationDataModelTranslationDataDescriptorDTO("testXml", "testJson", Map.of("result", "data"), Map.of("result", "settings")),
				"testJson",
				"testJson",
				"generic_mqtt",
				Map.of("a", "b"),
				"test-operation",
				"targetToken",
				Map.of("c", "d"));

		final ServiceInstanceInterfaceResponseDTO response = new ServiceInstanceInterfaceResponseDTO("generic_mqtt", "tcp", "NONE", Map.of("custom", "access"));

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.POST,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of())).thenReturn(response);

		final Pair<Optional<ServiceInstanceInterfaceResponseDTO>, Optional<ArrowheadException>> result = driver.initializeBridge(
				bridgeId,
				model,
				"targetToken",
				Map.of("c", "d"),
				null,
				Map.of("input", "settings"),
				Map.of("result", "settings"));

		assertNotNull(result);
		assertTrue(result.getFirst().isPresent());
		assertTrue(result.getSecond().isEmpty());
		assertEquals(response, result.getFirst().get());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.POST,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeBridgeOk4() {
		final UUID bridgeId = UUID.fromString("7bbdef4a-f06f-4329-b82b-a3a0ff16589b");
		final HttpOperationModel opModel = new HttpOperationModel("/init", "GET");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				"testJson");
		model.setInterfaceTranslatorProperties(Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", Map.of("initialize-bridge", opModel)));
		model.setFromInterfaceTemplate("generic_http");
		model.setToInterfaceTemplate("generic_mqtt");
		model.setTargetProperties(Map.of("a", "b"));
		model.setTargetInputDataModelId("testXml");
		model.setInputDataModelTranslatorProperties(Map.of("input", "data"));
		model.setTargetOutputDataModelId("testXml");
		model.setOutputDataModelTranslatorProperties(Map.of("result", "data"));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/init");

		final TranslationBridgeInitializationRequestDTO payload = new TranslationBridgeInitializationRequestDTO(
				"7bbdef4a-f06f-4329-b82b-a3a0ff16589b",
				"generic_http",
				new TranslationDataModelTranslationDataDescriptorDTO("testJson", "testXml", Map.of("input", "data"), Map.of("input", "settings")),
				new TranslationDataModelTranslationDataDescriptorDTO("testXml", "testJson", Map.of("result", "data"), Map.of("result", "settings")),
				"testJson",
				"testJson",
				"generic_mqtt",
				Map.of("a", "b"),
				"test-operation",
				"targetToken",
				Map.of("c", "d"));

		final ServiceInstanceInterfaceResponseDTO response = new ServiceInstanceInterfaceResponseDTO("generic_mqtt", "tcp", "NONE", Map.of("custom", "access"));

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.GET,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of())).thenReturn(response);

		final Pair<Optional<ServiceInstanceInterfaceResponseDTO>, Optional<ArrowheadException>> result = driver.initializeBridge(
				bridgeId,
				model,
				"targetToken",
				Map.of("c", "d"),
				null,
				Map.of("input", "settings"),
				Map.of("result", "settings"));

		assertNotNull(result);
		assertTrue(result.getFirst().isPresent());
		assertTrue(result.getSecond().isEmpty());
		assertEquals(response, result.getFirst().get());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.GET,
				ServiceInstanceInterfaceResponseDTO.class,
				payload,
				null,
				Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.abortBridge(null, null, null));

		assertEquals("bridgeId is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgePropertiesNull() {
		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.abortBridge(bridgeId, null, null));

		assertEquals("interfaceProperties is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgePropertiesEmpty() {
		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.abortBridge(bridgeId, Map.of(), null));

		assertEquals("interfaceProperties is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testAbortBridgeExceptionDuringWSCall() {
		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Map<String, Object> properties = Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test");

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test/abort-bridge" + "/" + bridgeId.toString());

		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(httpService.sendRequest(
				uri,
				HttpMethod.DELETE,
				Map.of("Authorization", "Bearer token"),
				Void.TYPE)).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> driver.abortBridge(bridgeId, properties, "token"));

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.DELETE,
				Map.of("Authorization", "Bearer token"),
				Void.TYPE);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testAbortBridgeOk1() {
		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Map<String, Object> properties = Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", "string");

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/abort-bridge" + "/" + bridgeId.toString());

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.DELETE,
				Map.of(),
				Void.TYPE)).thenReturn(null);

		assertDoesNotThrow(() -> driver.abortBridge(bridgeId, properties, null));

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.DELETE,
				Map.of(),
				Void.TYPE);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testAbortBridgeOk2() {
		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Map<String, Object> properties = Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", Map.of());

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/abort-bridge" + "/" + bridgeId.toString());

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.DELETE,
				Map.of(),
				Void.TYPE)).thenReturn(null);

		assertDoesNotThrow(() -> driver.abortBridge(bridgeId, properties, null));

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.DELETE,
				Map.of(),
				Void.TYPE);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testAbortBridgeOk3() {
		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final Map<String, Object> properties = Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", Map.of("abort-bridge", "notAModel"));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/abort-bridge" + "/" + bridgeId.toString());

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.DELETE,
				Map.of(),
				Void.TYPE)).thenReturn(null);

		assertDoesNotThrow(() -> driver.abortBridge(bridgeId, properties, null));

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.DELETE,
				Map.of(),
				Void.TYPE);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testAbortBridgeOk4() {
		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final HttpOperationModel opModel = new HttpOperationModel("/abort", "GET");
		final Map<String, Object> properties = Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/test", "operations", Map.of("abort-bridge", opModel));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/abort" + "/" + bridgeId.toString());

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(
				uri,
				HttpMethod.GET,
				Map.of(),
				Void.TYPE)).thenReturn(null);

		assertDoesNotThrow(() -> driver.abortBridge(bridgeId, properties, null));

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(
				uri,
				HttpMethod.GET,
				Map.of(),
				Void.TYPE);
	}
}