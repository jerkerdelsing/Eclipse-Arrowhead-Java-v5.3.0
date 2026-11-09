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
package eu.arrowhead.deviceqosevaluator.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.statistics.descriptive.Max;
import org.apache.commons.statistics.descriptive.Mean;
import org.apache.commons.statistics.descriptive.Median;
import org.apache.commons.statistics.descriptive.Min;

public final class Stat {
	//=================================================================================================
	// members

	private static final Median median = Median.withDefaults();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static double min(final double[] values) {
		return round(Min.of(values).getAsDouble());
	}

	//-------------------------------------------------------------------------------------------------
	public static double max(final double[] values) {
		return round(Max.of(values).getAsDouble());
	}

	//-------------------------------------------------------------------------------------------------
	public static double mean(final double[] values) {
		return round(Mean.of(values).getAsDouble());
	}

	//-------------------------------------------------------------------------------------------------
	public static double median(final double[] values) {
		return round(median.evaluate(values));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static double round(final double value) {
		return BigDecimal.valueOf(value)
				.setScale(2, RoundingMode.HALF_UP)
				.doubleValue();
	}

	//-------------------------------------------------------------------------------------------------
	private Stat() {
		throw new UnsupportedOperationException();
	}
}
