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
package eu.arrowhead.translationmanager.service.dto;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.TranslationDataModelTranslationDataDescriptorDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationInterfaceTranslationDataDescriptorDTO;
import eu.arrowhead.dto.TranslationQueryListResponseDTO;
import eu.arrowhead.dto.TranslationQueryResponseDTO;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;

public class DTOConverterTest {

	//=================================================================================================
	// members

	private DTOConverter converter = new DTOConverter();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertDiscoveryModelsBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertDiscoveryModels(null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertDiscoveryModelsListNull() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertDiscoveryModels(bridgeId, null));

		assertEquals("models list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertDiscoveryModelsListEmpty() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertDiscoveryModels(bridgeId, List.of()));

		assertEquals("models list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertDiscoveryModelsListContainsNull() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final List<TranslationDiscoveryModel> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertDiscoveryModels(bridgeId, list));

		assertEquals("models list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertDiscoveryModelsOk() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		model.setToInterfaceTemplate("generic_http");

		final TranslationDiscoveryResponseDTO result = converter.convertDiscoveryModels(bridgeId, List.of(model));

		assertNotNull(result);
		assertEquals(bridgeId.toString(), result.bridgeId());
		assertNotNull(result.candidates());
		assertEquals(1, result.candidates().size());
		assertEquals("TestProvider|testService|1.0.0", result.candidates().get(0).serviceInstanceId());
		assertEquals("generic_http", result.candidates().get(0).interfaceTemplateName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertBridgeDetailsPagePageNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertBridgeDetailsPage(null));

		assertEquals("page is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertBridgeDetailsPageOk() {
		final UUID bridgeId = UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe");
		final BridgeHeader header = new BridgeHeader(bridgeId, "TestCreator");
		header.setStatus(TranslationBridgeStatus.INITIALIZED);
		header.setUsageReportCount(0);
		header.setAlivesAt(ZonedDateTime.of(2026, 1, 30, 10, 11, 12, 0, ZoneId.of(Constants.UTC)));
		header.setCreatedAt(ZonedDateTime.of(2026, 1, 30, 10, 11, 12, 0, ZoneId.of(Constants.UTC)));
		header.setUpdatedAt(ZonedDateTime.of(2026, 1, 30, 10, 11, 12, 0, ZoneId.of(Constants.UTC)));
		final BridgeDetails details = new BridgeDetails(
				header,
				"TestConsumer",
				"TestProvider",
				"testService",
				"test-operation",
				"InterfaceTranslator",
				Utilities.toJson(Map.of(
						"fromInterfaceTemplate", "generic_http",
						"toInterfaceTemplate", "generic_mqtt",
						"token", "itToken",
						"interfaceProperties", Map.of("accessPort", 12345))),
				"DataModelTranslatorA",
				Utilities.toJson(Map.of(
						"fromModelId", "testJson",
						"toModelId", "testXml",
						"interfaceProperties", Map.of("accessPort", 22345))),
				"DataModelTranslatorB",
				Utilities.toJson(Map.of(
						"fromModelId", "testXml",
						"toModelId", "testJson",
						"interfaceProperties", Map.of("accessPort", 32345))));
		final Page<BridgeDetails> page = new PageImpl<>(List.of(details));

		final TranslationQueryResponseDTO expected = new TranslationQueryResponseDTO(
				bridgeId.toString(),
				"INITIALIZED",
				0,
				"2026-01-30T10:11:12Z",
				null,
				"TestConsumer",
				"TestProvider",
				"testService",
				"test-operation",
				"InterfaceTranslator",
				new TranslationInterfaceTranslationDataDescriptorDTO("generic_http", "generic_mqtt", "itToken", Map.of("accessPort", 12345), null),
				"DataModelTranslatorA",
				new TranslationDataModelTranslationDataDescriptorDTO("testJson", "testXml", Map.of("accessPort", 22345), null),
				"DataModelTranslatorB",
				new TranslationDataModelTranslationDataDescriptorDTO("testXml", "testJson", Map.of("accessPort", 32345), null),
				"TestCreator",
				"2026-01-30T10:11:12Z",
				"2026-01-30T10:11:12Z");

		final TranslationQueryListResponseDTO result = converter.convertBridgeDetailsPage(page);

		assertNotNull(result);
		assertEquals(1, result.count());
		assertNotNull(result.entries());
		assertEquals(expected, result.entries().get(0));
	}
}