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
package eu.arrowhead.common.service.validation;

import java.util.List;
import java.util.Map;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.meta.MetadataKeyEvaluator;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementExpression;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementTokenizer;
import eu.arrowhead.dto.MetadataRequirementDTO;

public final class MetadataRequirementsMatcher {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static boolean isMetadataMatch(final Map<String, Object> metadata, final MetadataRequirementDTO requirements) throws InvalidParameterException {
		if (Utilities.isEmpty(requirements)) {
			return true;
		}

		if (Utilities.isEmpty(metadata)) {
			return false;
		}

		final List<MetadataRequirementExpression> expressions = MetadataRequirementTokenizer.parseRequirements(requirements);
		for (final MetadataRequirementExpression expression : expressions) {
			final Object actValue = MetadataKeyEvaluator.getMetadataValueForCompositeKey(metadata, expression.keyPath());
			if (actValue == null) {
				// key is invalid
				return false;
			}

			if (!expression.operation().eval(actValue, expression.value())) {
				return false;
			}
		}

		return true;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private MetadataRequirementsMatcher() {
		throw new UnsupportedOperationException();
	}
}