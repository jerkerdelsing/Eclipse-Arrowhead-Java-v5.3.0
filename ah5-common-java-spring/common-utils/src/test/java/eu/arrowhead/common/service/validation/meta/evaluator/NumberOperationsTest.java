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

public class NumberOperationsTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalLessThan() {
		final IMetaEvaluator evaluator = new LessThanEvaluator();

		assertAll("Less than tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, 10)),
				() -> assertFalse(evaluator.eval(10, null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval(9, 10)),
				() -> assertTrue(evaluator.eval(6.5, 7.1)),
				() -> assertTrue(evaluator.eval(6.5, 7)),
				() -> assertTrue(evaluator.eval(6, 8.2)),
				() -> assertTrue(evaluator.eval(6.5f, 7)),
				() -> assertTrue(evaluator.eval(6, 8.2f)),
				() -> assertFalse(evaluator.eval(11, 2)),
				() -> assertFalse(evaluator.eval(11.2, 2)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalLessThanOrEqualsTo() {
		final IMetaEvaluator evaluator = new LessThanOrEqualsToEvaluator();

		assertAll("Less than or equals to tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, 10)),
				() -> assertFalse(evaluator.eval(10, null)),
				() -> assertTrue(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval(9, 10)),
				() -> assertTrue(evaluator.eval(6.5, 7.1)),
				() -> assertTrue(evaluator.eval(6.5, 7)),
				() -> assertTrue(evaluator.eval(6, 8.2)),
				() -> assertTrue(evaluator.eval(6.5f, 7.1f)),
				() -> assertTrue(evaluator.eval(6.5f, 7)),
				() -> assertTrue(evaluator.eval(6, 8.2f)),
				() -> assertFalse(evaluator.eval(11, 2)),
				() -> assertFalse(evaluator.eval(11.2, 2)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalGreaterThan() {
		final IMetaEvaluator evaluator = new GreaterThanEvaluator();

		assertAll("Greater than tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, 10)),
				() -> assertFalse(evaluator.eval(10, null)),
				() -> assertFalse(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval(10, 9)),
				() -> assertTrue(evaluator.eval(7.1, 6.5)),
				() -> assertTrue(evaluator.eval(7, 6.5)),
				() -> assertTrue(evaluator.eval(8.2, 6)),
				() -> assertTrue(evaluator.eval(7, 6.5f)),
				() -> assertTrue(evaluator.eval(8.2f, 6)),
				() -> assertFalse(evaluator.eval(2, 11)),
				() -> assertFalse(evaluator.eval(2.2, 11)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void evalGreaterThanOrEqualsTo() {
		final IMetaEvaluator evaluator = new GreaterThanOrEqualsToEvaluator();

		assertAll("Greater than or equals to tests",
				() -> assertFalse(evaluator.eval(null, null)),
				() -> assertFalse(evaluator.eval(null, 10)),
				() -> assertFalse(evaluator.eval(10, null)),
				() -> assertTrue(evaluator.eval(10, 9)),
				() -> assertTrue(evaluator.eval(10, 10)),
				() -> assertTrue(evaluator.eval(7.1, 6.5)),
				() -> assertTrue(evaluator.eval(7, 6.5)),
				() -> assertTrue(evaluator.eval(8.2, 6)),
				() -> assertTrue(evaluator.eval(7, 6.5f)),
				() -> assertTrue(evaluator.eval(8.2f, 6)),
				() -> assertFalse(evaluator.eval(2, 11)),
				() -> assertFalse(evaluator.eval(2.2, 11)));
	}
}