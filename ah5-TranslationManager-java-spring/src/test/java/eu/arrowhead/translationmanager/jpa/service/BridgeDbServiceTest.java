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
package eu.arrowhead.translationmanager.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.TranslationBridgeEventState;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDiscovery;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDetailsRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDiscoveryRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeHeaderRepository;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService.AbortResult;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationQueryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationReportRequestDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;

@ExtendWith(MockitoExtension.class)
public class BridgeDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private BridgeDbService dbService;

	@Mock
	private BridgeHeaderRepository headerRepository;

	@Mock
	private BridgeDetailsRepository detailsRepository;

	@Mock
	private BridgeDiscoveryRepository discoveryRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.handleTranslationReport(null));

		assertEquals("dto is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportBridgeIdNull() {
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO("Requester", null, null, null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportTimestampNull() {
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString("42ab0775-26cc-49aa-87a4-2313f9d9975b"),
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("timestamp is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportNoBridge1() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.empty());

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Invalid bridge id: 42ab0775-26cc-49aa-87a4-2313f9d9975b", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportNoBridge2() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.empty());

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Invalid bridge id: 42ab0775-26cc-49aa-87a4-2313f9d9975b", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportWrongRequester() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Requester has no permission to report about the specified translation bridge", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportNotActiveBridge() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"InterfaceTranslator",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.CLOSED);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Invalid reporting case", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportInvalidTransition() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"InterfaceTranslator",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				null,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.USED);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Invalid reporting case", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"InterfaceTranslator",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.INTERNAL_ERROR,
				"Error");

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.USED);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));
		when(headerRepository.saveAndFlush(header)).thenThrow(RuntimeException.class);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportOk() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"InterfaceTranslator",
				UUID.fromString(bridgeId),
				timestamp,
				TranslationBridgeEventState.USED,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.INITIALIZED);
		header.setUsageReportCount(0);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));
		when(headerRepository.saveAndFlush(header)).thenReturn(header);

		assertDoesNotThrow(() -> dbService.handleTranslationReport(dto));

		assertEquals(TranslationBridgeStatus.USED, header.getStatus());
		assertEquals(timestamp, header.getAlivesAt());
		assertEquals(1, header.getUsageReportCount());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(null, null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesCreatedByNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), null, null));

		assertEquals("createdBy is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesCreatedByEmpty() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "", null));

		assertEquals("createdBy is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesModelsNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "Creator", null));

		assertEquals("models list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesModelsEmpty() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "Creator", List.of()));

		assertEquals("models list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesModelsContainsNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final List<TranslationDiscoveryModel> list = new ArrayList<>(1);
		list.add(null);
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "Creator", list));

		assertEquals("models list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");

		when(headerRepository.saveAndFlush(any(BridgeHeader.class))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "Creator", List.of(model)));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).saveAndFlush(any(BridgeHeader.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesOk() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		final String modelJson = Utilities.toJson(model);
		final BridgeDiscovery discovery = new BridgeDiscovery(header, modelJson);

		when(headerRepository.saveAndFlush(header)).thenReturn(header);
		when(discoveryRepository.saveAllAndFlush(List.of(discovery))).thenReturn(List.of(discovery));

		final Pair<BridgeHeader, List<BridgeDiscovery>> result = dbService.storeBridgeDiscoveries(uuid, "Creator", List.of(model));

		assertEquals(header, result.getFirst());
		assertEquals(TranslationBridgeStatus.DISCOVERED, result.getFirst().getStatus());
		assertEquals(1, result.getSecond().size());
		assertEquals(discovery, result.getSecond().get(0));

		verify(headerRepository, times(2)).saveAndFlush(header);
		verify(discoveryRepository).saveAllAndFlush(List.of(discovery));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.selectBridgeFromDiscoveries(null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesInstanceIdNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, null));

		assertEquals("instanceId is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesInstanceIdEmpty() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, ""));

		assertEquals("instanceId is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesBridgeNotFound() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.empty());

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0"));

		assertEquals("Invalid bridge identifier: " + bridgeId, ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesNotAppropriateStatus() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.NEW);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0"));

		assertEquals("Invalid bridge identifier: " + bridgeId, ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesNotAppropriateModel() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.DISCOVERED);
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("OtherProvider|testService|1.0.0");
		final String modelJson = Utilities.toJson(model);
		final BridgeDiscovery discovery = new BridgeDiscovery(header, modelJson);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(discoveryRepository.findByHeader(header)).thenReturn(List.of(discovery));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0"));

		assertEquals("Invalid bridge identifier: " + bridgeId, ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(discoveryRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.DISCOVERED);
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		final String modelJson = Utilities.toJson(model);
		final BridgeDiscovery discovery = new BridgeDiscovery(header, modelJson);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(discoveryRepository.findByHeader(header)).thenReturn(List.of(discovery));
		when(headerRepository.saveAndFlush(header)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0"));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(discoveryRepository).findByHeader(header);
		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesOk() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.DISCOVERED);
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		model.setConsumer("TestConsumer");
		model.setProvider("TestProvider");
		model.setServiceDefinition("testService");
		model.setOperation("test-operation");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInputDataModelTranslator("DataModelTranslator1");
		model.setOutputDataModelTranslator("DataModelTranslator2");
		final String modelJson = Utilities.toJson(model);
		final BridgeDiscovery discovery = new BridgeDiscovery(header, modelJson);
		final BridgeDetails details = new BridgeDetails(
				header,
				model.getConsumer(),
				model.getProvider(),
				model.getServiceDefinition(),
				model.getOperation(),
				model.getInterfaceTranslator(),
				"", // will be set later
				model.getInputDataModelTranslator(),
				null, // will be set later if necessary
				model.getOutputDataModelTranslator(),
				null); // will be set later if necessary

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(discoveryRepository.findByHeader(header)).thenReturn(List.of(discovery));
		when(headerRepository.saveAndFlush(header)).thenReturn(header);
		doNothing().when(discoveryRepository).deleteByHeader(header);
		doNothing().when(discoveryRepository).flush();
		when(detailsRepository.saveAndFlush(details)).thenReturn(details);

		final Pair<TranslationDiscoveryModel, BridgeDetails> result = dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0");

		assertEquals(model, result.getFirst());
		assertEquals(details, result.getSecond());

		verify(headerRepository).findByUuid(bridgeId);
		verify(discoveryRepository).findByHeader(header);
		verify(headerRepository).saveAndFlush(header);
		verify(discoveryRepository).deleteByHeader(header);
		verify(discoveryRepository).flush();
		verify(detailsRepository).saveAndFlush(details);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateDetailsRecord(null));

		assertEquals("record is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordDbException() {
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);

		when(detailsRepository.findById(1L)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.updateDetailsRecord(details));

		assertEquals("Database operation error", ex.getMessage());

		verify(detailsRepository).findById(1L);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordNotFound() {
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);

		when(detailsRepository.findById(1L)).thenReturn(Optional.empty());

		final boolean result = dbService.updateDetailsRecord(details);

		assertFalse(result);

		verify(detailsRepository).findById(1L);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordEndState() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.CLOSED);
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);

		when(detailsRepository.findById(1L)).thenReturn(Optional.of(details));

		final boolean result = dbService.updateDetailsRecord(details);

		assertTrue(result);

		verify(detailsRepository).findById(1L);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordOk() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.DISCOVERED);
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);

		when(detailsRepository.findById(1L)).thenReturn(Optional.of(details));
		when(detailsRepository.saveAndFlush(details)).thenReturn(details);

		final boolean result = dbService.updateDetailsRecord(details);

		assertFalse(result);

		verify(detailsRepository).findById(1L);
		verify(detailsRepository).saveAndFlush(details);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.bridgeInitialized(null));

		assertEquals("header is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedDbException() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.PENDING);

		when(headerRepository.saveAndFlush(header)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.bridgeInitialized(header));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedOk() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.PENDING);

		when(headerRepository.saveAndFlush(header)).thenReturn(header);

		final boolean result = dbService.bridgeInitialized(header);

		assertFalse(result);
		assertEquals(TranslationBridgeStatus.INITIALIZED, header.getStatus());

		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedEndState() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.ABORTED);

		final boolean result = dbService.bridgeInitialized(header);

		assertTrue(result);

		verify(headerRepository, never()).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedOtherState() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.INITIALIZED);

		final boolean result = dbService.bridgeInitialized(header);

		assertFalse(result);

		verify(headerRepository, never()).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeProblem(null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorMessageNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeProblem(uuid, null));

		assertEquals("errorMessage is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorMessageEmpty() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeProblem(uuid, ""));

		assertEquals("errorMessage is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenThrow(RuntimeException.class);

		assertDoesNotThrow(() -> dbService.storeBridgeProblem(uuid, "error"));

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorBridgeNotFound() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> dbService.storeBridgeProblem(uuid, "error"));

		verify(headerRepository).findByUuid(bridgeId);
		verify(headerRepository, never()).saveAndFlush(any(BridgeHeader.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorEndStateBridge() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.CLOSED);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));

		assertDoesNotThrow(() -> dbService.storeBridgeProblem(uuid, "error"));

		verify(headerRepository).findByUuid(bridgeId);
		verify(headerRepository, never()).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorOk() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.INITIALIZED);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(headerRepository.saveAndFlush(header)).thenReturn(header);

		assertDoesNotThrow(() -> dbService.storeBridgeProblem(uuid, "error"));
		assertEquals(TranslationBridgeStatus.ERROR, header.getStatus());
		assertEquals("error", header.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.abortBridge(null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.abortBridge(uuid, null));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeNotFound() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.empty());

		final AbortResult result = dbService.abortBridge(uuid, null);

		assertNotNull(result);
		assertFalse(result.abortHappened());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeNoPermission() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));

		final Throwable ex = assertThrows(
				ForbiddenException.class,
				() -> dbService.abortBridge(uuid, "Other"));

		assertEquals("No permission to abort bridge: " + bridgeId, ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeEndStateBridge() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.CLOSED);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));

		final AbortResult result = dbService.abortBridge(uuid, "Creator");

		assertNotNull(result);
		assertFalse(result.abortHappened());
		assertEquals(TranslationBridgeStatus.CLOSED, result.fromStatus());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeNoDetails() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.INITIALIZED);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		doNothing().when(discoveryRepository).deleteByHeader(header);
		when(headerRepository.saveAndFlush(header)).thenReturn(header);
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.empty());

		final AbortResult result = dbService.abortBridge(uuid, null);

		assertNotNull(result);
		assertTrue(result.abortHappened());
		assertNull(result.detailsRecord());
		assertEquals(TranslationBridgeStatus.INITIALIZED, result.fromStatus());
		assertEquals(TranslationBridgeStatus.ABORTED, header.getStatus());

		verify(headerRepository).findByUuid(bridgeId);
		verify(discoveryRepository).deleteByHeader(header);
		verify(headerRepository).saveAndFlush(header);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeWithDetails() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.INITIALIZED);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		doNothing().when(discoveryRepository).deleteByHeader(header);
		when(headerRepository.saveAndFlush(header)).thenReturn(header);
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));

		final AbortResult result = dbService.abortBridge(uuid, null);

		assertNotNull(result);
		assertTrue(result.abortHappened());
		assertEquals(details, result.detailsRecord());
		assertEquals(TranslationBridgeStatus.INITIALIZED, result.fromStatus());
		assertEquals(TranslationBridgeStatus.ABORTED, header.getStatus());

		verify(headerRepository).findByUuid(bridgeId);
		verify(discoveryRepository).deleteByHeader(header);
		verify(headerRepository).saveAndFlush(header);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetBridgeDetailsPageInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getBridgeDetailsPage(null));

		assertEquals("dto is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageDbException() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
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

		when(detailsRepository.findAll(pageRequest)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getBridgeDetailsPage(dto));

		assertEquals("Database operation error", ex.getMessage());

		verify(detailsRepository).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageNoFilters() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
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

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);

		when(detailsRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(details)));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		assertEquals(details, result.getContent().get(0));

		verify(detailsRepository).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters1() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				List.of(uuid),
				null,
				null,
				List.of("OtherConsumer"),
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

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");

		when(detailsRepository.findByHeader_UuidIn(List.of(bridgeId))).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findByHeader_UuidIn(List.of(bridgeId));
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters2() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				List.of(uuid),
				null,
				null,
				List.of("TestConsumer"),
				List.of("OtherProvider"),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");

		when(detailsRepository.findByHeader_UuidIn(List.of(bridgeId))).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findByHeader_UuidIn(List.of(bridgeId));
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters3() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				List.of(uuid),
				null,
				null,
				null,
				List.of("TestProvider"),
				List.of("otherService"),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");

		when(detailsRepository.findByHeader_UuidIn(List.of(bridgeId))).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findByHeader_UuidIn(List.of(bridgeId));
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters4() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				null,
				null,
				null,
				List.of("TestConsumer"),
				null,
				List.of("testService"),
				List.of("OtherInterfaceTranslator"),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");

		when(detailsRepository.findByConsumerIn(List.of("TestConsumer"))).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findByConsumerIn(List.of("TestConsumer"));
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters5() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				null,
				null,
				List.of(TranslationBridgeStatus.INITIALIZED),
				null,
				List.of("TestProvider"),
				null,
				List.of("InterfaceTranslator"),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");

		when(detailsRepository.findByProviderIn(List.of("TestProvider"))).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findByProviderIn(List.of("TestProvider"));
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters6() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				null,
				List.of("Other"),
				List.of(TranslationBridgeStatus.USED),
				null,
				null,
				List.of("testService"),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");

		when(detailsRepository.findByServiceDefinitionIn(List.of("testService"))).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findByServiceDefinitionIn(List.of("testService"));
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters7() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				null,
				List.of("Creator"),
				null,
				null,
				null,
				null,
				List.of("InterfaceTranslator"),
				List.of("OtherDataModelTranslator"),
				null,
				null,
				null,
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findByInterfaceTranslatorIn(List.of("InterfaceTranslator"))).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findByInterfaceTranslatorIn(List.of("InterfaceTranslator"));
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters8() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				null,
				null,
				List.of(TranslationBridgeStatus.USED),
				null,
				null,
				null,
				null,
				List.of("ResultDMTranslator"),
				ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				null,
				null,
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 23, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findByHeader_StatusIn(List.of(TranslationBridgeStatus.USED))).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findByHeader_StatusIn(List.of(TranslationBridgeStatus.USED));
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters9() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				null,
				List.of("Creator"),
				null,
				null,
				null,
				null,
				null,
				List.of("InputDMTranslator"),
				ZonedDateTime.of(2026, 2, 23, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2026, 2, 24, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				null,
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findByHeader_CreatedByIn(List.of("Creator"))).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findByHeader_CreatedByIn(List.of("Creator"));
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters10() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				ZonedDateTime.of(2026, 2, 26, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2026, 2, 26, 8, 0, 0, 0, ZoneId.of(Constants.UTC)),
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		header.setAlivesAt(null);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findAll()).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findAll();
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters11() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
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
				ZonedDateTime.of(2026, 2, 26, 8, 0, 0, 0, ZoneId.of(Constants.UTC)),
				null,
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		header.setAlivesAt(ZonedDateTime.of(2026, 2, 26, 7, 0, 0, 0, ZoneId.of(Constants.UTC)));
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findAll()).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findAll();
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters12() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
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
				ZonedDateTime.of(2026, 2, 26, 8, 0, 0, 0, ZoneId.of(Constants.UTC)),
				ZonedDateTime.of(2026, 2, 26, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		header.setAlivesAt(ZonedDateTime.of(2026, 2, 26, 11, 0, 0, 0, ZoneId.of(Constants.UTC)));
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findAll()).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findAll();
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters13() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
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
				ZonedDateTime.of(2026, 2, 26, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				null,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		header.setAlivesAt(null);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findAll()).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findAll();
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters14() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
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
				ZonedDateTime.of(2026, 2, 26, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				5,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		header.setAlivesAt((ZonedDateTime.of(2026, 2, 26, 8, 0, 0, 0, ZoneId.of(Constants.UTC))));
		header.setUsageReportCount(2);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findAll()).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findAll();
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters15() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
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
				1);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		header.setAlivesAt((ZonedDateTime.of(2026, 2, 26, 8, 0, 0, 0, ZoneId.of(Constants.UTC))));
		header.setUsageReportCount(2);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findAll()).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findAll();
		verify(detailsRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters16() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
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
				1,
				5);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		header.setAlivesAt((ZonedDateTime.of(2026, 2, 26, 8, 0, 0, 0, ZoneId.of(Constants.UTC))));
		header.setUsageReportCount(2);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findAll()).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(1L), pageRequest)).thenReturn(new PageImpl<>(List.of(details)));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		assertEquals(details, result.getContent().get(0));

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findAll();
		verify(detailsRepository).findAllByIdIn(Set.of(1L), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetBridgeDetailsPageWithFilters17() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final String bridgeId = "82b6d4db-71e4-4db0-a546-8d3250525570";
		final UUID uuid = UUID.fromString(bridgeId);
		final NormalizedTranslationQueryRequestDTO dto = new NormalizedTranslationQueryRequestDTO(
				pageRequest,
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
				1,
				null);

		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.USED);
		header.setCreatedAt(ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));
		header.setAlivesAt((ZonedDateTime.of(2026, 2, 26, 8, 0, 0, 0, ZoneId.of(Constants.UTC))));
		header.setUsageReportCount(2);
		final BridgeDetails details = new BridgeDetails();
		details.setHeader(header);
		details.setId(1L);
		details.setConsumer("TestConsumer");
		details.setProvider("TestProvider");
		details.setServiceDefinition("testService");
		details.setInterfaceTranslator("InterfaceTranslator");
		details.setInputDmTranslator("InputDMTranslator");
		details.setResultDmTranslator("ResultDMTranslator");

		when(detailsRepository.findAll()).thenReturn(List.of(details));
		when(detailsRepository.findAllByIdIn(Set.of(1L), pageRequest)).thenReturn(new PageImpl<>(List.of(details)));

		final Page<BridgeDetails> result = dbService.getBridgeDetailsPage(dto);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		assertEquals(details, result.getContent().get(0));

		verify(detailsRepository, never()).findAll(pageRequest);
		verify(detailsRepository).findAll();
		verify(detailsRepository).findAllByIdIn(Set.of(1L), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetBridgeDiscoveriesCreatedBeforeNullThreshold() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getBridgeDiscoveriesCreatedBefore(null));

		assertEquals("threshold is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetBridgeDiscoveriesCreatedBeforeDbException() {
		final ZonedDateTime threshold = ZonedDateTime.of(2026, 2, 26, 8, 0, 0, 0, ZoneId.of(Constants.UTC));

		when(discoveryRepository.findByHeader_CreatedAtLessThan(threshold)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getBridgeDiscoveriesCreatedBefore(threshold));

		assertEquals("Database operation error", ex.getMessage());

		verify(discoveryRepository).findByHeader_CreatedAtLessThan(threshold);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetBridgeDiscoveriesCreatedBeforeOk() {
		final ZonedDateTime threshold = ZonedDateTime.of(2026, 2, 26, 8, 0, 0, 0, ZoneId.of(Constants.UTC));
		final BridgeDiscovery discovery = new BridgeDiscovery();
		discovery.setId(1L);

		when(discoveryRepository.findByHeader_CreatedAtLessThan(threshold)).thenReturn(List.of(discovery));

		final List<BridgeDiscovery> result = dbService.getBridgeDiscoveriesCreatedBefore(threshold);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(discovery, result.get(0));

		verify(discoveryRepository).findByHeader_CreatedAtLessThan(threshold);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleObsoletedBridgeDiscoveryNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.handleObsoletedBridgeDiscovery(null));

		assertEquals("record is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleObsoletedBridgeDiscoveryDbException() {
		final BridgeHeader header = new BridgeHeader();
		header.setStatus(TranslationBridgeStatus.CLOSED);
		final BridgeDiscovery discovery = new BridgeDiscovery();
		discovery.setHeader(header);

		doThrow(RuntimeException.class).when(discoveryRepository).delete(discovery);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.handleObsoletedBridgeDiscovery(discovery));

		assertEquals("Database operation error", ex.getMessage());

		verify(discoveryRepository).delete(discovery);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleObsoletedBridgeDiscoveryOk1() {
		final BridgeHeader header = new BridgeHeader();
		header.setStatus(TranslationBridgeStatus.USED);
		final BridgeDiscovery discovery = new BridgeDiscovery();
		discovery.setHeader(header);

		doNothing().when(discoveryRepository).delete(discovery);
		doNothing().when(discoveryRepository).flush();

		assertDoesNotThrow(() -> dbService.handleObsoletedBridgeDiscovery(discovery));

		verify(discoveryRepository).delete(discovery);
		verify(discoveryRepository).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleObsoletedBridgeDiscoveryOk2() {
		final BridgeHeader header = new BridgeHeader();
		header.setStatus(TranslationBridgeStatus.PENDING);
		final BridgeDiscovery discovery = new BridgeDiscovery();
		discovery.setHeader(header);

		when(headerRepository.saveAndFlush(header)).thenReturn(header);
		doNothing().when(discoveryRepository).delete(discovery);
		doNothing().when(discoveryRepository).flush();

		assertDoesNotThrow(() -> dbService.handleObsoletedBridgeDiscovery(discovery));
		assertEquals(TranslationBridgeStatus.CLOSED, header.getStatus());
		assertEquals("Closed by TranslationManager because of inactivity", header.getMessage());

		verify(headerRepository).saveAndFlush(header);
		verify(discoveryRepository).delete(discovery);
		verify(discoveryRepository).flush();
	}
}