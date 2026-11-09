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
package eu.arrowhead.common.service.validation.meta;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.MetadataRequirementDTO;

public class MetadataRequirementTokenizerTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void parseRequirementsDefault() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", "value");

		final List<MetadataRequirementExpression> expressions = MetadataRequirementTokenizer.parseRequirements(req);
		assertAll("Default requirement",
				() -> assertNotNull(expressions),
				() -> assertEquals(1, expressions.size()),
				() -> assertEquals("key", expressions.get(0).keyPath()),
				() -> assertEquals(MetaOps.EQUALS, expressions.get(0).operation()),
				() -> assertEquals("value", expressions.get(0).value()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void parseRequirementsDefault2() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", 10);

		final List<MetadataRequirementExpression> expressions = MetadataRequirementTokenizer.parseRequirements(req);
		assertAll("Default requirement (2)",
				() -> assertNotNull(expressions),
				() -> assertEquals(1, expressions.size()),
				() -> assertEquals("key", expressions.get(0).keyPath()),
				() -> assertEquals(MetaOps.EQUALS, expressions.get(0).operation()),
				() -> assertEquals(10, expressions.get(0).value()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void parseRequirementsNormal() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", Map.of("op", "SIZE_EQUALS", "value", List.of(1, 2, 3)));

		final List<MetadataRequirementExpression> expressions = MetadataRequirementTokenizer.parseRequirements(req);
		assertAll("Normal requirement",
				() -> assertNotNull(expressions),
				() -> assertEquals(1, expressions.size()),
				() -> assertEquals("key", expressions.get(0).keyPath()),
				() -> assertEquals(MetaOps.SIZE_EQUALS, expressions.get(0).operation()),
				() -> assertInstanceOf(List.class, expressions.get(0).value()),
				() -> assertEquals(3, ((List<?>) expressions.get(0).value()).size()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void parseRequirementsDefaultWithMap() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", Map.of("subkey", "something", "value", List.of(1, 2, 3)));

		final List<MetadataRequirementExpression> expressions = MetadataRequirementTokenizer.parseRequirements(req);
		assertAll("Default requirement with map",
				() -> assertNotNull(expressions),
				() -> assertEquals(1, expressions.size()),
				() -> assertEquals("key", expressions.get(0).keyPath()),
				() -> assertEquals(MetaOps.EQUALS, expressions.get(0).operation()),
				() -> assertInstanceOf(Map.class, expressions.get(0).value()),
				() -> assertEquals(2, ((Map<?, ?>) expressions.get(0).value()).size()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void parseRequirementsDefaultWithMap2() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", Map.of("subkey", "something", "op", "UNKNOWN"));

		final List<MetadataRequirementExpression> expressions = MetadataRequirementTokenizer.parseRequirements(req);
		assertAll("Default requirement with map",
				() -> assertNotNull(expressions),
				() -> assertEquals(1, expressions.size()),
				() -> assertEquals("key", expressions.get(0).keyPath()),
				() -> assertEquals(MetaOps.EQUALS, expressions.get(0).operation()),
				() -> assertInstanceOf(Map.class, expressions.get(0).value()),
				() -> assertEquals(2, ((Map<?, ?>) expressions.get(0).value()).size()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void parseRequirementsInvalidOperation() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", Map.of("op", "NOT_IMPLEMENTED", "value", List.of(1, 2, 3)));

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> {
			MetadataRequirementTokenizer.parseRequirements(req);

		});
		assertEquals("Invalid metadata operation requirement: NOT_IMPLEMENTED", ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void parseRequirementsMultipleReq() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", Map.of("op", "SIZE_EQUALS", "value", List.of(1, 2, 3)));
		req.put("key2", "value");

		final List<MetadataRequirementExpression> expressions = MetadataRequirementTokenizer.parseRequirements(req);
		assertAll("Multiple requirements",
				() -> assertNotNull(expressions),
				() -> assertEquals(2, expressions.size()));
	}
}