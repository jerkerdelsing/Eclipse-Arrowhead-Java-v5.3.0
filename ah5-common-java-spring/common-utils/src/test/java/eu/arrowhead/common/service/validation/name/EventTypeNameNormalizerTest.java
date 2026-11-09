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

public class EventTypeNameNormalizerTest {

	//=================================================================================================
	// members

	private final EventTypeNameNormalizer normalizer = new EventTypeNameNormalizer();

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
				() -> assertEquals("eventType", normalizer.normalize("eventType")),
				() -> assertEquals("eventType", normalizer.normalize("   eventType    \n")),
				() -> assertEquals("not-valid-event-type", normalizer.normalize("not-valid-event-type")),
				() -> assertEquals("not-valid-event-type", normalizer.normalize("\tnot-valid-event-type   ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeExtendedMode() {
		ReflectionTestUtils.setField(normalizer, "normalizationMode", NormalizationMode.EXTENDED);

		assertAll("Extended mode tests",
				() -> assertEquals("eventType", normalizer.normalize("eventType")),
				() -> assertEquals("eventType", normalizer.normalize("   eventType    \n")),
				() -> assertEquals("snakeCaseEventType", normalizer.normalize("snake_case_event_type")),
				() -> assertEquals("kebabCaseEventType", normalizer.normalize("kebab-case-event-type")),
				() -> assertEquals("eventTypeWithSpaces", normalizer.normalize("event type with spaces")),
				() -> assertEquals("tOOMUCHUNDERSCORE", normalizer.normalize("TOO____MUCH____UNDERSCORE")),
				() -> assertEquals("horribleMixedEventType9", normalizer.normalize("   \thorrible___---_- mixed\n---___event type9")));
	}
}