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
import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.intf.properties.IPropertyValidator;

@Service
public class MinMaxValidator implements IPropertyValidator {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// propertyValue should be a number
	// args has at least 2 elements, both are the string representation of a number
	@Override
	public Object validateAndNormalize(final Object propertyValue, final String... args) throws InvalidParameterException {
		logger.debug("MinMaxValidator.validateAndNormalize started...");

		if (propertyValue instanceof final Number number) {
			if (args == null || args.length < 2) {
				throw new InvalidParameterException("Missing minimum and/or maximum values");
			}

			final boolean isReal = (number instanceof Double || number instanceof Float);

			if (isReal) {
				try {
					final double min = Double.parseDouble(args[0]);
					final double max = Double.parseDouble(args[1]);
					final double value = number.doubleValue();

					if (value < min || value > max) {
						throw new InvalidParameterException("Invalid property value");
					}
				} catch (final NumberFormatException ex) {
					throw new InvalidParameterException("Invalid minimum and/or maximum values");
				}
			} else {
				try {
					final long min = Long.parseLong(args[0]);
					final long max = Long.parseLong(args[1]);
					final long value = number.longValue();

					if (value < min || value > max) {
						throw new InvalidParameterException("Invalid property value");
					}
				} catch (final NumberFormatException ex) {
					throw new InvalidParameterException("Invalid minimum and/or maximum values");
				}
			}

			return propertyValue;
		}

		throw new InvalidParameterException("Property value should be a number");
	}
}