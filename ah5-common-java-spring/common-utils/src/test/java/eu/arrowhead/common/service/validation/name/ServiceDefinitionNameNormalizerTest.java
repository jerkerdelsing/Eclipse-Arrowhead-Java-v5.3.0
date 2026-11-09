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
package eu.arrowhead.common.service.validation.name;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.service.normalization.NormalizationMode;

public class ServiceDefinitionNameNormalizerTest {

	//=================================================================================================
	// members

	private final ServiceDefinitionNameNormalizer normalizer = new ServiceDefinitionNameNormalizer();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeEmptyInput() {
		assertAll("Empty input",
				() -> assertNull(normalizer.normalize(null)),
				() -> assertNull(normalizer.normalize("    ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSimpleMode() {
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.SIMPLE);

		assertAll("Simple mode tests",
				() -> assertEquals("serviceDef", normalizer.normalize("serviceDef")),
				() -> assertEquals("serviceDef", normalizer.normalize("   serviceDef    \n")),
				() -> assertEquals("not-valid-service-def", normalizer.normalize("not-valid-service-def")),
				() -> assertEquals("not-valid-service-def", normalizer.normalize("\tnot-valid-service-def   ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeExtendedMode() {
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);

		assertAll("Extended mode tests",
				() -> assertEquals("serviceDef", normalizer.normalize("serviceDef")),
				() -> assertEquals("serviceDef", normalizer.normalize("   serviceDef    \n")),
				() -> assertEquals("snakeCaseServiceDef", normalizer.normalize("snake_case_service_def")),
				() -> assertEquals("kebabCaseServiceDef", normalizer.normalize("kebab-case-service-def")),
				() -> assertEquals("serviceDefWithSpaces", normalizer.normalize("service def with spaces")),
				() -> assertEquals("tOOMUCHUNDERSCORE", normalizer.normalize("TOO____MUCH____UNDERSCORE")),
				() -> assertEquals("horribleMixedServiceDef9", normalizer.normalize("   \thorrible___---_- mixed\n---___service def9")));
	}
}