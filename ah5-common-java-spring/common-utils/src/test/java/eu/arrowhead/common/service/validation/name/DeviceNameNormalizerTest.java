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

public class DeviceNameNormalizerTest {

	//=================================================================================================
	// members

	private final DeviceNameNormalizer normalizer = new DeviceNameNormalizer();

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
				() -> assertEquals("MY_DEVICE", normalizer.normalize("MY_DEVICE")),
				() -> assertEquals("MY_DEVICE", normalizer.normalize("   MY_DEVICE    \n")),
				() -> assertEquals("not-valid-device-name", normalizer.normalize("not-valid-device-name")),
				() -> assertEquals("not-valid-device-name", normalizer.normalize("\tnot-valid-device-name   ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeExtendedMode() {
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);

		assertAll("Extended mode tests",
				() -> assertEquals("MY_DEVICE", normalizer.normalize("MY_DEVICE")),
				() -> assertEquals("MY_DEVICE", normalizer.normalize("   MY_DEVICE    \n")),
				() -> assertEquals("LOWER_CASE_DEVICE", normalizer.normalize("lower_case_device")),
				() -> assertEquals("KEBAB_CASE_DEVICE", normalizer.normalize("kebab-case-device")),
				() -> assertEquals("DEVICE_WITH_SPACES", normalizer.normalize("device with spaces")),
				() -> assertEquals("TOO_MUCH_UNDERSCORE", normalizer.normalize("TOO____MUCH____UNDERSCORE")),
				() -> assertEquals("HORRIBLE_MIXED_DEVICE_9", normalizer.normalize("   \tHORRIBLE___---_- mixed\n---___device   9")));
	}
}