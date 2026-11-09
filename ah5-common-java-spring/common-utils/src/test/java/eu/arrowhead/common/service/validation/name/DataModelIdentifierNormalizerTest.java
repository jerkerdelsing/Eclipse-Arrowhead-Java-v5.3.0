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

public class DataModelIdentifierNormalizerTest {

	//=================================================================================================
	// members

	private final DataModelIdentifierNormalizer normalizer = new DataModelIdentifierNormalizer();

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
				() -> assertEquals("modelId", normalizer.normalize("modelId")),
				() -> assertEquals("modelId", normalizer.normalize("   modelId    \n")),
				() -> assertEquals("not-valid-model-id", normalizer.normalize("not-valid-model-id")),
				() -> assertEquals("not-valid-model-id", normalizer.normalize("\tnot-valid-model-id   ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeExtendedMode() {
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);

		assertAll("Extended mode tests",
				() -> assertEquals("modelId", normalizer.normalize("modelId")),
				() -> assertEquals("modelId", normalizer.normalize("   modelId    \n")),
				() -> assertEquals("snakeCaseModelId", normalizer.normalize("snake_case_model_id")),
				() -> assertEquals("kebabCaseModelId", normalizer.normalize("kebab-case-model-id")),
				() -> assertEquals("modelIdWithSpaces", normalizer.normalize("model id with spaces")),
				() -> assertEquals("tOOMUCHUNDERSCORE", normalizer.normalize("TOO____MUCH____UNDERSCORE")),
				() -> assertEquals("horribleMixedModelId9", normalizer.normalize("   \thorrible___---_- mixed\n---___model id9")));
	}
}