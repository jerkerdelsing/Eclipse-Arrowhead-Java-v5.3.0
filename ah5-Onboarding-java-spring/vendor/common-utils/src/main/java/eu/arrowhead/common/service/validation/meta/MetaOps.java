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
package eu.arrowhead.common.service.validation.meta;

import eu.arrowhead.common.service.validation.meta.evaluator.EndsWithEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.EndsWithIgnoreCaseEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.EqualsEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.EqualsIgnoreCaseEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.GreaterThanEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.GreaterThanOrEqualsToEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.InEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.IncludesEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.IncludesIgnoreCaseEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.LessThanEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.LessThanOrEqualsToEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.ListContainsAnyEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.ListContainsEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.NotEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.RegExpEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.SizeEqualsEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.StartsWithEvaluator;
import eu.arrowhead.common.service.validation.meta.evaluator.StartsWithIgnoreCaseEvaluator;

public enum MetaOps {

	// all kind of values (any) - all kind of values (any)
	EQUALS(new EqualsEvaluator()),
	NOT_EQUALS(new NotEvaluator(new EqualsEvaluator())),

	// text - text
	EQUALS_IGNORE_CASE(new EqualsIgnoreCaseEvaluator()),
	NOT_EQUALS_IGNORE_CASE(new NotEvaluator(new EqualsIgnoreCaseEvaluator())),
	INCLUDES(new IncludesEvaluator()),
	NOT_INCLUDES(new NotEvaluator(new IncludesEvaluator())),
	INCLUDES_IGNORE_CASE(new IncludesIgnoreCaseEvaluator()),
	NOT_INCLUDES_IGNORE_CASE(new NotEvaluator(new IncludesIgnoreCaseEvaluator())),
	STARTS_WITH(new StartsWithEvaluator()),
	NOT_STARTS_WITH(new NotEvaluator(new StartsWithEvaluator())),
	STARTS_WITH_IGNORE_CASE(new StartsWithIgnoreCaseEvaluator()),
	NOT_STARTS_WITH_IGNORE_CASE(new NotEvaluator(new StartsWithIgnoreCaseEvaluator())),
	ENDS_WITH(new EndsWithEvaluator()),
	NOT_ENDS_WITH(new NotEvaluator(new EndsWithEvaluator())),
	ENDS_WITH_IGNORE_CASE(new EndsWithIgnoreCaseEvaluator()),
	NOT_ENDS_WITH_IGNORE_CASE(new NotEvaluator(new EndsWithIgnoreCaseEvaluator())),
	REGEXP(new RegExpEvaluator()),

	// number - number
	LESS_THAN(new LessThanEvaluator()),
	LESS_THAN_OR_EQUALS_TO(new LessThanOrEqualsToEvaluator()),
	GREATER_THAN(new GreaterThanEvaluator()),
	GREATER_THAN_OR_EQUALS_TO(new GreaterThanOrEqualsToEvaluator()),

	// text or list - number
	SIZE_EQUALS(new SizeEqualsEvaluator()),
	SIZE_NOT_EQUALS(new NotEvaluator(new SizeEqualsEvaluator())),

	// list - any
	CONTAINS(new ListContainsEvaluator()),
	NOT_CONTAINS(new NotEvaluator(new ListContainsEvaluator())),

	// list - list
	CONTAINS_ANY(new ListContainsAnyEvaluator()),
	NOT_CONTAINS_ANY(new NotEvaluator(new ListContainsAnyEvaluator())),

	// any - list
	IN(new InEvaluator()),
	NOT_IN(new NotEvaluator(new InEvaluator()));

	//=================================================================================================
	// members

	private final IMetaEvaluator evaluator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean eval(final Object left, final Object right) {
		return this.evaluator.eval(left, right);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private MetaOps(final IMetaEvaluator evaluator) {
		this.evaluator = evaluator;
	}
}