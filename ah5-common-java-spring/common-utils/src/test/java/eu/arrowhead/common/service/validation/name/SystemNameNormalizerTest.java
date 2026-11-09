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

public class SystemNameNormalizerTest {

	//=================================================================================================
	// members

	private final SystemNameNormalizer normalizer = new SystemNameNormalizer();

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
				() -> assertEquals("SystemName", normalizer.normalize("SystemName")),
				() -> assertEquals("SystemName", normalizer.normalize("   SystemName    \n")),
				() -> assertEquals("not-valid-system", normalizer.normalize("not-valid-system")),
				() -> assertEquals("not-valid-system", normalizer.normalize("\tnot-valid-system   ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeExtendedMode() {
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);

		assertAll("Extended mode tests",
				() -> assertEquals("SystemName", normalizer.normalize("SystemName")),
				() -> assertEquals("SystemName", normalizer.normalize("   SystemName    \n")),
				() -> assertEquals("SnakeCaseSystem", normalizer.normalize("snake_case_system")),
				() -> assertEquals("KebabCaseSystem", normalizer.normalize("kebab-case-system")),
				() -> assertEquals("SystemWithSpaces", normalizer.normalize("system with spaces")),
				() -> assertEquals("TOOMUCHUNDERSCORE", normalizer.normalize("TOO____MUCH____UNDERSCORE")),
				() -> assertEquals("HorribleMixedSystem9", normalizer.normalize("   \thorrible___---_- mixed\n---___system 9")));
	}
}