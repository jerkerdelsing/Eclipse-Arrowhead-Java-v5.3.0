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

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.service.validation.meta.IMetaEvaluator;

public class ListOperationsTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalSizeEquals() {
		final IMetaEvaluator evaluator = new SizeEqualsEvaluator();

		assertAll("Size equals tests (for lists)",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, List.of())),
				() -> assertFalse(evaluator.eval(List.of(), null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval(List.of(1, 2, 3, 4), 4)),
				() -> assertTrue(evaluator.eval(List.of("a", "b", "c", "d", "e", "f", "g", "h"), 8)),
				() -> assertFalse(evaluator.eval(List.of(1, 2, 3, 4), 2)),
				() -> assertFalse(evaluator.eval(List.of(1, 2), 9)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalContains() {
		final IMetaEvaluator evaluator = new ListContainsEvaluator();

		assertAll("Contains (for lists)",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, List.of())),
				() -> assertFalse(evaluator.eval(List.of(), new Object())),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval(List.of(1, 2, 3, 4), 4)),
				() -> assertTrue(evaluator.eval(List.of("a", "b", "c", "d", "e", "f", "g", "h"), "c")),
				() -> assertFalse(evaluator.eval(List.of(1, 2, 3, 4), 5)),
				() -> assertFalse(evaluator.eval(List.of(1, 2), "c")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalContainsAny() {
		final IMetaEvaluator evaluator = new ListContainsAnyEvaluator();

		assertAll("Contains any (for lists)",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, List.of())),
				() -> assertFalse(evaluator.eval(List.of(), null)),
				() -> assertFalse(evaluator.eval(List.of(), 1)),
				() -> assertFalse(evaluator.eval(1, List.of())),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval(List.of(1, 2, 3, 4), List.of(4, 5))),
				() -> assertTrue(evaluator.eval(List.of("a", "b", "c", "d", "e", "f", "g", "h"), List.of("x", "c", "e"))),
				() -> assertFalse(evaluator.eval(List.of(1, 2, 3, 4), List.of(5, 6))),
				() -> assertFalse(evaluator.eval(List.of(1, 2), List.of("c"))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalIn() {
		final IMetaEvaluator evaluator = new InEvaluator();

		assertAll("In",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(new Object(), List.of())),
				() -> assertFalse(evaluator.eval(10, null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval(4, List.of(1, 2, 3, 4))),
				() -> assertTrue(evaluator.eval("c", List.of("a", "b", "c", "d", "e", "f", "g", "h"))),
				() -> assertFalse(evaluator.eval(5, List.of(1, 2, 3, 4))),
				() -> assertFalse(evaluator.eval("c", List.of(1, 2))));
	}
}