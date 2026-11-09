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
package eu.arrowhead.common.intf.properties.validators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.intf.properties.IPropertyValidator;

@Service
public class PortValidator implements IPropertyValidator {

	//=================================================================================================
	// members

	@SuppressWarnings("checkstyle:nowhitespaceafter")
	private static final String[] MIN_MAX = { String.valueOf(Constants.MIN_PORT), String.valueOf(Constants.MAX_PORT) };

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private MinMaxValidator minMaxValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// propertyValue should be an integer number
	@Override
	public Object validateAndNormalize(final Object propertyValue, final String... args) throws InvalidParameterException {
		logger.debug("PortValidator.validateAndNormalize started...");

		if (propertyValue instanceof Number && !(propertyValue instanceof Double || propertyValue instanceof Float)) {
			return minMaxValidator.validateAndNormalize(propertyValue, MIN_MAX);
		}

		throw new InvalidParameterException("Property value should be an integer");
	}
}