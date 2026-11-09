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

import eu.arrowhead.common.exception.InvalidParameterException;

public final class MetadataValidation {

	//=================================================================================================
	// members

	public static final String METADATA_COMPOSITE_KEY_DELIMITER = ".";
	public static final String METADATA_COMPOSITE_KEY_DELIMITER_REGEXP = "\\.";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void validateMetadataKey(final Map<String, Object> metadata) throws InvalidParameterException {
		validateMetadataObjectKey(metadata);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private static void validateMetadataObjectKey(final Object metadataObject) {
		if (isValidMap(metadataObject)) {
			final Map<String, Object> map = (Map<String, Object>) metadataObject;
			for (final String key : map.keySet()) {

				// check the key
				if (key.contains(METADATA_COMPOSITE_KEY_DELIMITER)) {
					throw new InvalidParameterException("Invalid metadata key: " + key + ", it should not contain " + METADATA_COMPOSITE_KEY_DELIMITER + " character");
				}

				// check the value
				validateMetadataObjectKey(map.get(key));
			}
		} else if (metadataObject instanceof final List list) {
			for (final Object element : list) {
				validateMetadataObjectKey(element);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	// checks if the type of map is Map<String, Object>
	private static boolean isValidMap(final Object object) {
		if (!(object instanceof Map)) {
			return false;
		}

		final Map<?, ?> map = (Map<?, ?>) object;
		boolean isValid = true;

		for (final Map.Entry<?, ?> entry : map.entrySet()) {
			if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof Object)) {
				isValid = false;
				break;
			}
		}

		return isValid;
	}

	//-------------------------------------------------------------------------------------------------
	private MetadataValidation() {
		throw new UnsupportedOperationException();
	}
}