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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;

public class NormalizedTranslationQueryRequestDTOTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersFalse() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
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

		assertFalse(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersBridgeId() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
				null,
				List.of(UUID.fromString("3b40df99-1468-4d84-bd8e-bfe6d895ebbe")),
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

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersCreator() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
				null,
				null,
				List.of("TestCreator"),
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

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersStatus() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
				null,
				null,
				null,
				List.of(TranslationBridgeStatus.INITIALIZED),
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

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersConsumer() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
				null,
				null,
				null,
				null,
				List.of("TestConsumer"),
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

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersProvider() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
				null,
				null,
				null,
				null,
				null,
				List.of("TestProvider"),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersService() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
				null,
				null,
				null,
				null,
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

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersInterfaceTranslator() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				List.of("InterfaceTranslator"),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersDataModelTranslator() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				List.of("DataModelTranslator"),
				null,
				null,
				null,
				null,
				null,
				null);

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersCreationFrom() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				ZonedDateTime.of(2026, 1, 30, 10, 11, 12, 0, ZoneId.of(Constants.UTC)),
				null,
				null,
				null,
				null,
				null);

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersCreationTo() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
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
				ZonedDateTime.of(2026, 1, 30, 10, 11, 12, 0, ZoneId.of(Constants.UTC)),
				null,
				null,
				null,
				null);

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersAlivesFrom() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
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
				ZonedDateTime.of(2026, 1, 30, 10, 11, 12, 0, ZoneId.of(Constants.UTC)),
				null,
				null,
				null);

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersAlivesTo() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
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
				ZonedDateTime.of(2026, 1, 30, 10, 11, 12, 0, ZoneId.of(Constants.UTC)),
				null,
				null);

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersMinUsage() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
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
				10,
				null);

		assertTrue(request.hasAnyFilter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAnyFiltersMaxUsage() {
		final NormalizedTranslationQueryRequestDTO request = new NormalizedTranslationQueryRequestDTO(
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
				10);

		assertTrue(request.hasAnyFilter());
	}
}