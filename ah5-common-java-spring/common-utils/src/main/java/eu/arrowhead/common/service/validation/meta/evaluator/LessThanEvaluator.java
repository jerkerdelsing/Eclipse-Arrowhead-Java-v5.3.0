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

public class LessThanEvaluator implements IMetaEvaluator {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean eval(final Object left, final Object right) {
		if (left instanceof final Number leftNum && right instanceof final Number rightNum) {
			final boolean leftIsReal = (left instanceof Double || left instanceof Float);
			final boolean rightIsReal = (right instanceof Double || right instanceof Float);

			if (leftIsReal) {
				return leftNum.doubleValue() < (rightIsReal ? rightNum.doubleValue() : rightNum.longValue());
			}

			return leftNum.longValue() < (rightIsReal ? rightNum.doubleValue() : rightNum.longValue());
		}

		return false;
	}
}