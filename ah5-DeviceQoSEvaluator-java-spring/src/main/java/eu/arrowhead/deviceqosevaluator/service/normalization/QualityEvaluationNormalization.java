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
package eu.arrowhead.deviceqosevaluator.service.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.dto.QoSDeviceDataEvaluationConfigDTO;

@Service
public class QualityEvaluationNormalization {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	private static final double TOLERANCE_BOTTOM = 0.9;
	private static final double TOLERANCE_TOP = 1.1;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeSystemNames(final List<String> names) {
		logger.debug("normalizeSystemNames started");
		Assert.notNull(names, "names is null");
		Assert.isTrue(!Utilities.containsNullOrEmpty(names), "names contains empty element");

		return names.stream().map(n -> systemNameNormalizer.normalize(n)).toList();
	}

	//-------------------------------------------------------------------------------------------------
	public QoSDeviceDataEvaluationConfigDTO normalizeQoSDeviceDataEvaluationConfigDTO(final QoSDeviceDataEvaluationConfigDTO dto) {
		logger.debug("normalizeQoSDeviceDataEvaluationConfigDTO started");
		Assert.notNull(dto, "dto is null");
		Assert.isTrue(!Utilities.isEmpty(dto.metricNames()), "metric names list is empty");
		Assert.isTrue(!Utilities.containsNullOrEmpty(dto.metricNames()), "metric names list contains empty element");

		return new QoSDeviceDataEvaluationConfigDTO(
				dto.metricNames().stream().map(mn -> mn.toUpperCase().trim()).toList(),
				normalizeMetricWeights(dto.metricWeights(), dto.metricNames().size()),
				normalizeTimeWindow(dto.timeWindow()),
				dto.threshold());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<Double> normalizeMetricWeights(final List<Double> weights, final int size) {
		logger.debug("normalizeMetricWeights started");

		if (Utilities.isEmpty(weights)) {
			final double w = 1.0 / size;
			return Collections.nCopies(size, w);
		}

		Assert.isTrue(!Utilities.containsNull(weights), "metric weights list contains empty element");

		double sum = 0;
		for (final Double w : weights) {
			sum += w;
		}

		List<Double> normalized = new ArrayList<>(size);
		if (sum < TOLERANCE_BOTTOM || sum > TOLERANCE_TOP) {
			for (final Double w : weights) {
				normalized.add(w / sum);
			}
		} else {
			normalized = weights;
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	private int normalizeTimeWindow(final Integer window) {
		logger.debug("normalizeTimeWindow started");

		if (window == null) {
			return (int) sysInfo.getEvaluationTimeWindow();
		}

		if (window < sysInfo.getAugmentedMeasurementJobInterval()) {
			return (int) sysInfo.getAugmentedMeasurementJobInterval();
		}

		return window;
	}
}