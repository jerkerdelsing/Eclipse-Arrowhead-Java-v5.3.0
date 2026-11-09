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

public class InterfaceTemplateNameNormalizerTest {

	//=================================================================================================
	// members

	private final InterfaceTemplateNameNormalizer normalizer = new InterfaceTemplateNameNormalizer();

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
				() -> assertEquals("my_interface", normalizer.normalize("my_interface")),
				() -> assertEquals("my_interface", normalizer.normalize("   my_interface    \n")),
				() -> assertEquals("not-valid-interface-template-name", normalizer.normalize("not-valid-interface-template-name")),
				() -> assertEquals("not-valid-interface-template-name", normalizer.normalize("\tnot-valid-interface-template-name   ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeExtendedMode() {
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);

		assertAll("Extended mode tests",
				() -> assertEquals("my_interface", normalizer.normalize("my_interface")),
				() -> assertEquals("my_interface", normalizer.normalize("   my_interface    \n")),
				() -> assertEquals("upper_case_interface", normalizer.normalize("UPPER_CASE_INTERFACE")),
				() -> assertEquals("kebab_case_interace", normalizer.normalize("kebab-case-interace")),
				() -> assertEquals("interface_with_spaces", normalizer.normalize("interface with spaces")),
				() -> assertEquals("too_much_underscore", normalizer.normalize("too____much____underscore")),
				() -> assertEquals("horrible_mixed_interface_9", normalizer.normalize("   \tHORRIBLE___---_- mixed\n---___interface   9")));
	}
}