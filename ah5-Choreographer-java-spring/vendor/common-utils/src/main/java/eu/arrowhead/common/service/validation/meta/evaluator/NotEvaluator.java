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

import eu.arrowhead.common.service.validation.meta.IMetaEvaluator;

public class NotEvaluator implements IMetaEvaluator {

	//=================================================================================================
	// members

	private final IMetaEvaluator evaluator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NotEvaluator(final IMetaEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean eval(final Object left, final Object right) {
		return !evaluator.eval(left, right);
	}
}