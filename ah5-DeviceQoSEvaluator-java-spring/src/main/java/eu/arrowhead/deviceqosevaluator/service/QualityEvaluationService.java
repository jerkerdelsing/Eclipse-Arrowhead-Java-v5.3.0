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
package eu.arrowhead.deviceqosevaluator.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.engine.StatisticsEngine;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.service.model.OidMetricModel;
import eu.arrowhead.deviceqosevaluator.service.model.SystemEvalModel;
import eu.arrowhead.deviceqosevaluator.service.validation.QualityEvaluationValidation;
import eu.arrowhead.dto.QoSDeviceDataEvaluationConfigDTO;
import eu.arrowhead.dto.QoSEvaluationFilterResponseDTO;
import eu.arrowhead.dto.QoSEvaluationRequestDTO;
import eu.arrowhead.dto.QoSEvaluationSortResponseDTO;

@Service
public class QualityEvaluationService {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Autowired
	private QualityEvaluationValidation validator;

	@Autowired
	private StatisticsEngine statEngine;

	@Autowired
	private ObjectMapper mapper;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public QoSEvaluationFilterResponseDTO filter(final QoSEvaluationRequestDTO dto, final String origin) {
		logger.debug("filter started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		try {
			final List<String> systems = dto == null ? null : dto.providers();
			final QoSDeviceDataEvaluationConfigDTO config = dto == null || dto.configuration() == null ? null : mapper.readValue(Utilities.toJson(dto.configuration()), QoSDeviceDataEvaluationConfigDTO.class);

			final Pair<List<String>, QoSDeviceDataEvaluationConfigDTO> normalized = validator.validateAndNormalizeQoSEvaluationRequest(systems, config, true, origin);

			final List<SystemEvalModel> evaluated = evaluateRequest(normalized.getFirst(), normalized.getSecond());
			final List<String> passedProviders = new ArrayList<>();
			final List<String> droppedProviders = new ArrayList<>();
			final Map<String, List<String>> warnings = new HashMap<>();

			for (final SystemEvalModel sysResult : evaluated) {
				if (sysResult.getScore() <= config.threshold()) {
					passedProviders.add(sysResult.getName());
				} else {
					droppedProviders.add(sysResult.getName());
				}

				if (!Utilities.isEmpty(sysResult.getNoStat())) {
					warnings.put(sysResult.getName(), sysResult.getNoStat().stream().map(item -> item.name()).toList());
				}
			}

			return new QoSEvaluationFilterResponseDTO(passedProviders, droppedProviders, warnings);
		} catch (final JsonProcessingException ex) {
			throw new InvalidParameterException("Invalid configuration payload", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public QoSEvaluationSortResponseDTO sort(final QoSEvaluationRequestDTO dto, final String origin) {
		logger.debug("sort started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		try {
			final List<String> systems = dto == null ? null : dto.providers();
			final QoSDeviceDataEvaluationConfigDTO config = dto == null || dto.configuration() == null ? null : mapper.readValue(Utilities.toJson(dto.configuration()), QoSDeviceDataEvaluationConfigDTO.class);

			final Pair<List<String>, QoSDeviceDataEvaluationConfigDTO> normalized = validator.validateAndNormalizeQoSEvaluationRequest(systems, config, false, origin);

			final List<SystemEvalModel> evaluated = evaluateRequest(normalized.getFirst(), normalized.getSecond());
			final List<String> sortedProviders = new ArrayList<>();
			final Map<String, List<String>> warnings = new HashMap<>();

			evaluated.sort(Comparator.comparingDouble(SystemEvalModel::getScore)); // ascending
			for (final SystemEvalModel sysResult : evaluated) {
				sortedProviders.add(sysResult.getName());
				if (!Utilities.isEmpty(sysResult.getNoStat())) {
					warnings.put(sysResult.getName(), sysResult.getNoStat().stream().map(item -> item.name()).toList());
				}
			}

			return new QoSEvaluationSortResponseDTO(sortedProviders, warnings);
		} catch (final JsonProcessingException ex) {
			throw new InvalidParameterException("Invalid configuration payload", origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<SystemEvalModel> evaluateRequest(final List<String> systems, final QoSDeviceDataEvaluationConfigDTO config) {
		logger.debug("evaluateRequest started");

		final Set<String> providers = new HashSet<>(systems);
		final List<OidMetricModel> metricModels = parseMetricModels(config.metricNames(), config.metricWeights());
		long timeWindow = config.timeWindow() == null ? sysInfo.getEvaluationTimeWindow() : config.timeWindow();
		if (timeWindow < sysInfo.getAugmentedMeasurementJobInterval()) {
			timeWindow = sysInfo.getAugmentedMeasurementJobInterval();
		}

		return statEngine.evaluate(providers, metricModels, timeWindow);
	}

	//-------------------------------------------------------------------------------------------------
	private List<OidMetricModel> parseMetricModels(final List<String> metricNames, final List<Double> metricWeights) {
		logger.debug("parseMetricModels started");

		final Map<OidGroup, OidMetricModel> metricModels = new HashMap<>();
		for (int i = 0; i < metricNames.size(); ++i) {
			final int splitIdx = metricNames.get(i).lastIndexOf(DeviceQoSEvaluatorConstants.OID_NAME_DELIMITER);
			final OidGroup oidGroup = OidGroup.valueOf(metricNames.get(i).substring(0, splitIdx).toUpperCase());
			metricModels.putIfAbsent(oidGroup, new OidMetricModel(oidGroup, defineScaleTo(oidGroup)));
			metricModels.get(oidGroup).getMetricWeight().put(OidMetric.valueOf(metricNames.get(i).substring(splitIdx + 1)), metricWeights.get(i));
		}

		return new ArrayList<>(metricModels.values());
	}

	//-------------------------------------------------------------------------------------------------
	private Double defineScaleTo(final OidGroup oidGroup) {
		logger.debug("defineScaleTo started");

		return oidGroup != OidGroup.RTT ? null : Double.valueOf(sysInfo.getRttMeasurementTimeout());
	}
}
