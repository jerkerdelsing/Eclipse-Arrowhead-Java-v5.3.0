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
package eu.arrowhead.common.service.validation.meta.evaluator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.service.validation.meta.IMetaEvaluator;

public class TextOperationsTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalEqualsIgnoreCase() {
		final IMetaEvaluator evaluator = new EqualsIgnoreCaseEvaluator();

		assertAll("Equals ignore case tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval("text", "text")),
				() -> assertTrue(evaluator.eval("TEXT", "text")),
				() -> assertTrue(evaluator.eval("text", "TEXT")),
				() -> assertTrue(evaluator.eval("Text", "texT")),
				() -> assertFalse(evaluator.eval("TEXT", "test")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalIncludes() {
		final IMetaEvaluator evaluator = new IncludesEvaluator();

		assertAll("Includes tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval("text", "text")),
				() -> assertTrue(evaluator.eval("longtext", "text")),
				() -> assertTrue(evaluator.eval("textlong", "text")),
				() -> assertTrue(evaluator.eval("longtextlong", "text")),
				() -> assertFalse(evaluator.eval("longtext", "test")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalIncludesIgnoreCase() {
		final IMetaEvaluator evaluator = new IncludesIgnoreCaseEvaluator();

		assertAll("Includes ignore case tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval("text", "Text")),
				() -> assertTrue(evaluator.eval("LongTEXT", "text")),
				() -> assertTrue(evaluator.eval("TextLong", "text")),
				() -> assertTrue(evaluator.eval("longtextlong", "TeXt")),
				() -> assertFalse(evaluator.eval("longtext", "Test")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalStartsWith() {
		final IMetaEvaluator evaluator = new StartsWithEvaluator();

		assertAll("Starts with tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval("text", "text")),
				() -> assertTrue(evaluator.eval("textlong", "text")),
				() -> assertFalse(evaluator.eval("longtextlong", "text")),
				() -> assertFalse(evaluator.eval("longtext", "text")),
				() -> assertFalse(evaluator.eval("text12", "test")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalStartsWithIgnoreCase() {
		final IMetaEvaluator evaluator = new StartsWithIgnoreCaseEvaluator();

		assertAll("Starts with ignore case tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval("Text", "texT")),
				() -> assertTrue(evaluator.eval("TEXTlong", "text")),
				() -> assertFalse(evaluator.eval("longtextlong", "text")),
				() -> assertFalse(evaluator.eval("longtext", "text")),
				() -> assertFalse(evaluator.eval("text12", "test")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalEndsWith() {
		final IMetaEvaluator evaluator = new EndsWithEvaluator();

		assertAll("Ends with tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval("text", "text")),
				() -> assertTrue(evaluator.eval("longtext", "text")),
				() -> assertFalse(evaluator.eval("longtextlong", "text")),
				() -> assertFalse(evaluator.eval("textlong", "text")),
				() -> assertFalse(evaluator.eval("atext", "test")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalEndsWithIgnoreCase() {
		final IMetaEvaluator evaluator = new EndsWithIgnoreCaseEvaluator();

		assertAll("Ends with ignore case tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval("text", "text")),
				() -> assertTrue(evaluator.eval("longtext", "Text")),
				() -> assertFalse(evaluator.eval("longtextlong", "text")),
				() -> assertFalse(evaluator.eval("TEXTlong", "text")),
				() -> assertFalse(evaluator.eval("atext", "test")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalRegexp() {
		final IMetaEvaluator evaluator = new RegExpEvaluator();

		assertAll("Regexp tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval("text", "^[a-z]+$")),
				() -> assertTrue(evaluator.eval("longtext", "^[a-z]+$")),
				() -> assertFalse(evaluator.eval(" longtextlong", "^[a-z]+$")),
				() -> assertFalse(evaluator.eval("TEXTlong", "^[a-z]+$")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalSizeEquals() {
		final IMetaEvaluator evaluator = new SizeEqualsEvaluator();

		assertAll("Size equals tests (for texts)",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval("text", 4)),
				() -> assertTrue(evaluator.eval("longtext", 8)),
				() -> assertFalse(evaluator.eval("longtextlong", 2)),
				() -> assertFalse(evaluator.eval("TEXTlong", 9)));
	}
}