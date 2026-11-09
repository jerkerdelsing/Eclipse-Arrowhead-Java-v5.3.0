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

public class GeneralOperationsTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalEquals() {
		final IMetaEvaluator evaluator = new EqualsEvaluator();

		assertAll("Equals tests",
				() -> assertTrue(evaluator.eval(null, null)),
				() -> assertTrue(evaluator.eval("text", "text")),
				() -> assertTrue(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval(List.of(1, 2, 3), List.of(1, 2, 3))),
				() -> assertFalse(evaluator.eval(null, "text")),
				() -> assertFalse(evaluator.eval("text", null)),
				() -> assertFalse(evaluator.eval(10, "text")),
				() -> assertFalse(evaluator.eval("text", "text2")),
				() -> assertFalse(evaluator.eval(11, 10)),
				() -> assertFalse(evaluator.eval(List.of(1, 2, 3), List.of(1, 3, 2))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalNot() {
		final IMetaEvaluator evaluator = new NotEvaluator(new EqualsEvaluator());

		assertAll("Not tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval("text", "text")),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertFalse(evaluator.eval(List.of(1, 2, 3), List.of(1, 2, 3))),
				() -> assertTrue(evaluator.eval(null, "text")),
				() -> assertTrue(evaluator.eval("text", null)),
				() -> assertTrue(evaluator.eval(10, "text")),
				() -> assertTrue(evaluator.eval("text", "text2")),
				() -> assertTrue(evaluator.eval(11, 10)),
				() -> assertTrue(evaluator.eval(List.of(1, 2, 3), List.of(1, 3, 2))));
	}
}