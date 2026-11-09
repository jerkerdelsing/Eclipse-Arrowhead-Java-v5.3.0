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
package eu.arrowhead.common.service.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.arrowhead.dto.MetadataRequirementDTO;

public class MetadataRequirementsMatcherTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isMetadataMatchNoRequirements() {
		assertAll("Empty requirements",
				() -> assertTrue(MetadataRequirementsMatcher.isMetadataMatch(null, null)),
				() -> assertTrue(MetadataRequirementsMatcher.isMetadataMatch(null, new MetadataRequirementDTO())));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isMetadataMatchNoMetadata() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", "value");

		assertAll("Empty metadata",
				() -> assertFalse(MetadataRequirementsMatcher.isMetadataMatch(null, req)),
				() -> assertFalse(MetadataRequirementsMatcher.isMetadataMatch(new HashMap<>(), req)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isMetadataMatchInvalidKey() {
		final Map<String, Object> metadata = Map.of("key", Map.of("subkey", "value"));
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key.not_subkey", "value");

		assertFalse(MetadataRequirementsMatcher.isMetadataMatch(metadata, req));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isMetadataMatchNoMatch() {
		final Map<String, Object> metadata = Map.of("key", Map.of("subkey", "value"));
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key.subkey", "not_value");

		assertFalse(MetadataRequirementsMatcher.isMetadataMatch(metadata, req));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void isMetadataMatchMatch() {
		final Map<String, Object> metadata = Map.of("key", Map.of("subkey", 5));
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key.subkey", Map.of("op", "LESS_THAN", "value", 10));

		assertTrue(MetadataRequirementsMatcher.isMetadataMatch(metadata, req));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void isMetadataMatchMatch2() {
		final Map<String, Object> metadata = Map.of("key", List.of(1, 2, 4, 8, 16));
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key[3]", Map.of("op", "LESS_THAN", "value", 10));

		assertTrue(MetadataRequirementsMatcher.isMetadataMatch(metadata, req));
	}
}