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
package eu.arrowhead.deviceqosevaluator.service.validation;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.service.normalization.QualityEvaluationNormalization;
import eu.arrowhead.dto.QoSDeviceDataEvaluationConfigDTO;

@Service
public class QualityEvaluationValidation {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Autowired
	private QualityEvaluationNormalization normalizer;

	@Autowired
	private SystemNameValidator systemNameValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	private static final int THRESHOLD_BOTTOM = 0;
	private static final int THRESHOLD_TOP = 100;

	//=================================================================================================
	// methods

	// NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public Pair<List<String>, QoSDeviceDataEvaluationConfigDTO> validateAndNormalizeQoSEvaluationRequest(final List<String> systems, final QoSDeviceDataEvaluationConfigDTO config, final boolean needThreshold, final String origin) {
		logger.debug("validateAndNormalizeQoSEvaluationRequest started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateQoSEvaluationRequest(systems, config, needThreshold, origin);
		final List<String> normalizedSystemNames = normalizer.normalizeSystemNames(systems);

		try {
			for (final String provider : normalizedSystemNames) {
				systemNameValidator.validateSystemName(provider);
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		final QoSDeviceDataEvaluationConfigDTO normalizedConfig = normalizer.normalizeQoSDeviceDataEvaluationConfigDTO(config);
		for (final String metricName : normalizedConfig.metricNames()) {
			final int splitIdx = metricName.lastIndexOf(DeviceQoSEvaluatorConstants.OID_NAME_DELIMITER);
			if (splitIdx <= 0 || splitIdx == metricName.length() - 1) {
				throw new InvalidParameterException("Invalid metric name " + metricName, origin);
			}

			if (!Utilities.isEnumValue(metricName.substring(0, splitIdx), OidGroup.class)
					|| !Utilities.isEnumValue(metricName.substring(splitIdx + 1), OidMetric.class)) {
				throw new InvalidParameterException("Invalid metric name " + metricName, origin);
			}
		}

		return Pair.of(normalizedSystemNames, normalizedConfig);
	}

	//=================================================================================================
	// assistant methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateQoSEvaluationRequest(final List<String> systems, final QoSDeviceDataEvaluationConfigDTO config, final boolean needThreshold, final String origin) {
		logger.debug("validateQoSEvaluationRequest started");

		if (Utilities.isEmpty(systems)) {
			throw new InvalidParameterException("System list is empty", origin);
		}

		if (Utilities.containsNullOrEmpty(systems)) {
			throw new InvalidParameterException("System list contains empty element", origin);
		}

		if (config == null) {
			throw new InvalidParameterException("Configuration payload is missing", origin);
		}

		if (Utilities.isEmpty(config.metricNames())) {
			throw new InvalidParameterException("Metric names configuration is empty", origin);
		}

		if (Utilities.containsNullOrEmpty(config.metricNames())) {
			throw new InvalidParameterException("Metric names configuration contains empty element", origin);
		}

		if (!Utilities.isEmpty(config.metricWeights())) {
			if (Utilities.containsNull(config.metricWeights())) {
				throw new InvalidParameterException("Metric weights configuration contains null element", origin);
			}

			if (config.metricNames().size() != config.metricWeights().size()) {
				throw new InvalidParameterException("Metric names and weights configuration lists have different sizes", origin);
			}
		}

		if (needThreshold) {
			if (config.threshold() == null) {
				throw new InvalidParameterException("Threshold configuration is missing", origin);
			}

			if (config.threshold() < THRESHOLD_BOTTOM || config.threshold() > THRESHOLD_TOP) {
				throw new InvalidParameterException("Invalid threshold configuration, must be between 0 and 100", origin);
			}
		}

		if (config.timeWindow() != null) {
			if (config.timeWindow() <= 0) {
				throw new InvalidParameterException("Invalid time window configuration, must be greater than 0", origin);
			}

			if (config.timeWindow() > sysInfo.getEvaluationTimeWindow()) {
				throw new InvalidParameterException("Invalid time window configuration, must be not greater than " + sysInfo.getEvaluationTimeWindow(), origin);
			}
		}
	}
}
