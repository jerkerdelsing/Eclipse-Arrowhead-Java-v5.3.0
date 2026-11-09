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
package eu.arrowhead.common.service.validation.name;

import eu.arrowhead.common.Utilities;
import jakarta.annotation.Nullable;

public final class NormalizationUtils {

	//=================================================================================================
	// members

	public static final char CH_UNDERSCORE = '_';
	public static final String UNDERSCORE = String.valueOf(CH_UNDERSCORE);
	public static final String HYPHEN = "-";

	public static final String DELIMITER_REGEXP = "(_|\\-)+";
	public static final String WHITESPACE_REGEXP = "(?U)\\s+";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String convertSnakeCaseToPascalCase(final String snake) {
		return convertSnakeCaseToPascalOrCamelCase(snake, true);
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String convertSnakeCaseToCamelCase(final String snake) {
		return convertSnakeCaseToPascalOrCamelCase(snake, false);
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String convertSnakeCaseToKebabCase(final String snake) {
		if (Utilities.isEmpty(snake)) {
			return null;
		}

		return snake.toLowerCase().replaceAll(DELIMITER_REGEXP, HYPHEN);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private NormalizationUtils() {
		throw new UnsupportedOperationException();
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	private static String convertSnakeCaseToPascalOrCamelCase(final String snake, final boolean pascal) {
		if (Utilities.isEmpty(snake)) {
			return null;
		}

		boolean foundUnderscore = false;
		final StringBuffer result = new StringBuffer(snake.length());
		final char first = pascal
				? Character.toUpperCase(snake.charAt(0)) // first character is upper-case
				: Character.toLowerCase(snake.charAt(0)); // // first character is lower-case

		result.append(first);

		for (int i = 1; i < snake.length(); ++i) {
			final char character = snake.charAt(i);
			if (character == CH_UNDERSCORE) { // underscore => skip it
				foundUnderscore = true;
			} else if (foundUnderscore) { // previous character was underscore => make the current character upper-case
				result.append(Character.toUpperCase(character));
				foundUnderscore = false;
			} else { // keep case
				result.append(character);
			}
		}

		return result.toString();
	}
}