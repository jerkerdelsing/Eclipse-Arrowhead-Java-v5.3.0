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
package eu.arrowhead.deviceqosevaluator.service.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;

public class OidMetricModel {

	//=================================================================================================
	// members

	private final OidGroup group;
	private final Double scaleTo;
	private final Map<OidMetric, Double> metricWeight = new HashMap<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OidMetricModel(final OidGroup group, final Double scaleTo) {
		this.group = group;
		this.scaleTo = scaleTo;
	}

	//-------------------------------------------------------------------------------------------------
	public OidGroup getGroup() {
		return group;
	}

	//-------------------------------------------------------------------------------------------------
	public Double getScaleTo() {
		return scaleTo;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<OidMetric, Double> getMetricWeight() {
		return metricWeight;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return Objects.hash(group, metricWeight, scaleTo);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final OidMetricModel other = (OidMetricModel) obj;

		return group == other.group && Objects.equals(metricWeight, other.metricWeight) && Objects.equals(scaleTo, other.scaleTo);
	}
}