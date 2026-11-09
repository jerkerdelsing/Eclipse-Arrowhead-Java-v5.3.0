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

import java.util.List;

import eu.arrowhead.common.service.validation.meta.IMetaEvaluator;

public class SizeEqualsEvaluator implements IMetaEvaluator {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean eval(final Object left, final Object right) {
		if (!(right instanceof Number)) {
			return false;
		}

		final int size = ((Number) right).intValue();

		if (left instanceof String) {
			return left.toString().length() == size;
		}

		if (left instanceof final List<?> leftList) {
			return leftList.size() == size;
		}

		return false;
	}
}