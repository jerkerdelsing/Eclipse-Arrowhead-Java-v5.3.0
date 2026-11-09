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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.IPropertyValidator;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameValidator;

@Service
public class HttpOperationsValidator implements IPropertyValidator {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private ServiceOperationNameNormalizer operationNameNormalizer;

	@Autowired
	private ServiceOperationNameValidator operationNameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// propertyValue should be a map with string keys (operation name that should follow the naming conventions) and HttpOperationModel value
	// at least one pair should be in the map
	// an HttpOperationModel object should contain a not null method and a non-blank path
	// normalization: operation names using the specific operation name normalizer
	// normalization: path will be trimmed
	@Override
	public Object validateAndNormalize(final Object propertyValue, final String... args) throws InvalidParameterException {
		logger.debug("HttpOperationsValidator.validateAndNormalize started...");

		if (propertyValue instanceof final Map<?, ?> map) {
			if (map.isEmpty()) {
				throw new InvalidParameterException("Property value should be a non-empty map");
			}

			final Map<String, HttpOperationModel> normalized = new HashMap<>(map.size());
			for (final Entry<?, ?> entry : map.entrySet()) {
				final String normalizedKey = validateAndNormalizeKey(entry.getKey());
				final HttpOperationModel normalizedModel = validateAndNormalizeValue(entry.getValue());

				normalized.put(normalizedKey, normalizedModel);
			}

			return normalized;
		}

		throw new InvalidParameterException("Property value should be a map of operations");
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private String validateAndNormalizeKey(final Object key) {
		if (key instanceof String stringKey) {
			final String normalized = operationNameNormalizer.normalize(stringKey);
			operationNameValidator.validateServiceOperationName(normalized);

			return normalized;
		} else {
			throw new InvalidParameterException("Key should be a string");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private HttpOperationModel validateAndNormalizeValue(final Object value) {
		try {
			// value should be a map which has the exact same structure that a HttpOperationModel => try to convert it
			final HttpOperationModel model = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
			if (Utilities.isEmpty(model.path())) {
				throw new InvalidParameterException("Path should be non-empty");
			}

			if (!HttpUtilities.isValidHttpMethod(model.method())) {
				throw new InvalidParameterException("Method should be a standard HTTP method");
			}

			return new HttpOperationModel(model.path().trim(), model.method().toUpperCase().trim());
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final ArrowheadException ex) {
			throw new InvalidParameterException("Value should be a HttpOperationModel record");
		}
	}
}