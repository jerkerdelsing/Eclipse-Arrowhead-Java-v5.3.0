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

public class ServiceOperationNameNormalizerTest {

	//=================================================================================================
	// members

	private final ServiceOperationNameNormalizer normalizer = new ServiceOperationNameNormalizer();

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
				() -> assertEquals("my-op", normalizer.normalize("my-op")),
				() -> assertEquals("my-op", normalizer.normalize("   my-op    \n")),
				() -> assertEquals("not_valid_op_name", normalizer.normalize("not_valid_op_name")),
				() -> assertEquals("not_valid_op_name", normalizer.normalize("\tnot_valid_op_name   ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeExtendedMode() {
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);

		assertAll("Extended mode tests",
				() -> assertEquals("my-op", normalizer.normalize("my-op")),
				() -> assertEquals("my-op", normalizer.normalize("   my-op    \n")),
				() -> assertEquals("upper-case-op", normalizer.normalize("UPPER-CASE-OP")),
				() -> assertEquals("snake-case-op", normalizer.normalize("snake_case_op")),
				() -> assertEquals("op-with-spaces", normalizer.normalize("op with spaces")),
				() -> assertEquals("too-much-underscore", normalizer.normalize("too____much____underscore")),
				() -> assertEquals("horrible-mixed-op-9", normalizer.normalize("   \tHORRIBLE___---_- mixed\n---___op   9")));
	}
}