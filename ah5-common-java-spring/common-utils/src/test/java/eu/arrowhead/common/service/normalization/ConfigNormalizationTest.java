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
package eu.arrowhead.common.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ConfigNormalizationTest {

	//=================================================================================================
	// members

	private final ConfigNormalization normalizer = new ConfigNormalization();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeConfigKeyListEmptyInput() {
		assertAll("empty input",
				() -> assertEquals(List.of(), normalizer.normalizeConfigKeyList(null)),
				() -> assertEquals(List.of(), normalizer.normalizeConfigKeyList(List.of())));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeConfigKeyListOk() {
		final List<String> expected = List.of("key1", "key2");

		assertEquals(expected, normalizer.normalizeConfigKeyList(List.of(" key1", "key1", "key2 ")));
	}
}