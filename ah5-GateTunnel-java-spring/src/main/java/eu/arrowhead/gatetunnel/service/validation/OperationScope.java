/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service.validation;

import static eu.arrowhead.gatetunnel.service.Wire.obj;
import static eu.arrowhead.gatetunnel.service.Wire.str;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Is a relayed request inside the operations negotiated for the session? Only an origin-relative path, without encoded
// separators, dot segments or empty segments, that matches the method and the path template of an operation.
public final class OperationScope {

	//=================================================================================================
	// members

	private static final List<String> UNSAFE_ENCODINGS = List.of("%2e", "%2f", "%5c", "%00", "%25");

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static boolean operationAllowed(final String method, final String raw, final String base, final Map<String, Object> operations) {
		try {
			final URI uri = URI.create(raw);
			final String path = uri.getRawPath();
			if (uri.isAbsolute() || uri.getRawAuthority() != null || path == null || !path.startsWith("/") || path.contains("\\") || path.contains("//")) {
				return false;
			}

			final String lower = path.toLowerCase(Locale.ROOT);
			for (final String unsafe : UNSAFE_ENCODINGS) {
				if (lower.contains(unsafe)) {
					return false;
				}
			}

			final String decoded = uri.getPath();
			if (decoded.indexOf(0) >= 0) {
				return false;
			}
			for (final String part : decoded.split("/", -1)) {
				if (".".equals(part) || "..".equals(part)) {
					return false;
				}
			}

			for (final Object value : operations.values()) {
				final Map<String, Object> operation = obj(value);
				if (!str(operation.get("method")).equalsIgnoreCase(method)) {
					continue;
				}

				final String prefix = base.replaceAll("/+$", "") + "/" + str(operation.get("path")).replaceAll("^/+|/+$", "");
				final String[] pattern = prefix.replaceAll("^/+|/+$", "").split("/", -1);
				final String[] actual = decoded.replaceAll("^/+|/+$", "").split("/", -1);
				if (actual.length < pattern.length) {
					continue;
				}

				boolean match = true;
				for (int i = 0; i < pattern.length; i++) {
					if (pattern[i].matches("\\{[^{}]+}")) {
						if (actual[i].isEmpty()) {
							match = false;
						}
					} else if (!pattern[i].equals(actual[i])) {
						match = false;
					}
				}
				if (match) {
					return true;
				}
			}
		} catch (final Exception ignored) {
			// not a valid request target
		}

		return false;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OperationScope() {
		throw new UnsupportedOperationException();
	}
}
