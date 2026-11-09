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
package eu.arrowhead.common.service.validation.version;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class VersionNormalizerTest {

	//=================================================================================================
	// members

	private final VersionNormalizer normalizer = new VersionNormalizer();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeEmpty() {
		assertAll("Empty input",
				() -> assertEquals("1.0.0", normalizer.normalize(null)),
				() -> assertEquals("1.0.0", normalizer.normalize("")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePartial() {
		assertAll("Partial input",
				() -> assertEquals("2.0.0", normalizer.normalize("2")),
				() -> assertEquals("2.3.0", normalizer.normalize("2.3")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeFull() {
		assertAll("Full input",
				() -> assertEquals("2.0.0", normalizer.normalize("2.0.0")),
				() -> assertEquals("2.3.4", normalizer.normalize("2.3.4")));
	}
}