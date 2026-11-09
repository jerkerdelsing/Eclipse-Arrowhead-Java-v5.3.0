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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.MetadataValidation;
import jakarta.annotation.Nullable;

public final class MetadataKeyEvaluator {

	//=================================================================================================
	// members

	private static final String IDX_PREFIX = "[";
	private static final String IDX_SUFFIX = "]";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static Object getMetadataValueForCompositeKey(final Map<String, Object> metadata, final String compositeKey) {
		if (Utilities.isEmpty(metadata) || Utilities.isEmpty(compositeKey)) {
			return null;
		}

		Object result = metadata;
		final String[] parts = compositeKey.trim().split(MetadataValidation.METADATA_COMPOSITE_KEY_DELIMITER_REGEXP);
		for (final String key : parts) {
			result = getValueForKey(result, key);
			if (result == null) {
				return null;
			}
		}

		return result;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static Object getValueForKey(final Object struct, final String key) {
		final Map<?, ?> map = castToMap(struct);

		if (map != null) {
			boolean arrayAccessor = isArrayAccessor(key);
			if (arrayAccessor) {
				// key + index
				final Entry<String, Integer> pair = parseKeyAndIndex(key);
				if (pair.getValue().intValue() >= 0) {
					if (map.containsKey(pair.getKey())) {
						// map value has to be a list
						final List<?> list = castToList(map.get(pair.getKey()));
						if (list != null && list.size() > pair.getValue()) {
							return list.get(pair.getValue().intValue());
						}
					}
				} else {
					// Although key contains [] characters, it is not used for indexing
					arrayAccessor = false;
				}
			}

			if (!arrayAccessor) {
				// normal key
				if (map.containsKey(key)) {
					return map.get(key);
				}
			}
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private static boolean isArrayAccessor(final String key) {
		final int openIdx = key.indexOf(IDX_PREFIX);
		final int endIdx = key.indexOf(IDX_SUFFIX);

		return openIdx != -1 && endIdx == key.length() - 1 && openIdx != endIdx - 1;
	}

	//-------------------------------------------------------------------------------------------------
	// use only after isArrayAccessor()
	private static Entry<String, Integer> parseKeyAndIndex(final String key) {
		final int openIdx = key.indexOf(IDX_PREFIX);
		final int endIdx = key.indexOf(IDX_SUFFIX);

		final String keyPart = key.substring(0, openIdx);
		final String idxPartStr = key.substring(openIdx + 1, endIdx);
		int idxPart = -1;

		try {
			idxPart = Integer.parseInt(idxPartStr);
		} catch (final NumberFormatException __) {
			// intentionally blank
		}

		return Map.entry(keyPart, idxPart);
	}

	//-------------------------------------------------------------------------------------------------
	private static Map<?, ?> castToMap(final Object obj) {
		try {
			return (Map<?, ?>) obj;
		} catch (final ClassCastException __) {
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static List<?> castToList(final Object obj) {
		try {
			return (List<?>) obj;
		} catch (final ClassCastException __) {
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private MetadataKeyEvaluator() {
		throw new UnsupportedOperationException();
	}
}