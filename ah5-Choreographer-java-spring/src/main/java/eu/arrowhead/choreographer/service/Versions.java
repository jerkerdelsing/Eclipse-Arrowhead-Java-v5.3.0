/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import java.util.List;

import eu.arrowhead.common.exception.InvalidParameterException;

// v5 versions are "major.minor.patch" strings; executors and steps are matched on the major version (as v4.6 integer versions).
public final class Versions {

	public static final int DEFAULT_MIN = 1;
	public static final int UNBOUNDED_MAX = Integer.MAX_VALUE;

	private Versions() {
		throw new UnsupportedOperationException();
	}

	public static int major(final String version) {
		if (version == null || version.isBlank()) {
			throw new InvalidParameterException("Version is empty");
		}
		final String head = version.trim().split("\\.")[0];
		try {
			final int major = Integer.parseInt(head);
			if (major < 0) {
				throw new InvalidParameterException("Invalid version: " + version);
			}
			return major;
		} catch (final NumberFormatException ex) {
			throw new InvalidParameterException("Invalid version: " + version);
		}
	}

	// [min, max] major window of a requirement's version list, or null when the list is empty
	public static int[] window(final List<String> versions) {
		if (versions == null || versions.isEmpty()) {
			return null;
		}
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (final String version : versions) {
			final int major = major(version);
			min = Math.min(min, major);
			max = Math.max(max, major);
		}
		return new int[] { min, max };
	}

	public static boolean overlaps(final Integer stepMin, final Integer stepMax, final int executorMin, final int executorMax) {
		final int min = stepMin == null ? DEFAULT_MIN : stepMin;
		final int max = stepMax == null ? UNBOUNDED_MAX : stepMax;
		return min <= executorMax && executorMin <= max;
	}
}
