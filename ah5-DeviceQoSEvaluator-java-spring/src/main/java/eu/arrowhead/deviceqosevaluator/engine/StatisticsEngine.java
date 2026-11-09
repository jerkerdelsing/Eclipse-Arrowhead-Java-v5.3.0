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
package eu.arrowhead.deviceqosevaluator.engine;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import eu.arrowhead.deviceqosevaluator.jpa.service.StatDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.SystemDbService;
import eu.arrowhead.deviceqosevaluator.service.model.OidMetricModel;
import eu.arrowhead.deviceqosevaluator.service.model.SystemEvalModel;
import eu.arrowhead.deviceqosevaluator.util.Stat;

@Service
public class StatisticsEngine {

	//=================================================================================================
	// members

	@Autowired
	private SystemDbService sysDbService;

	@Autowired
	private StatDbService statDbService;

	private static final int HUNDRED = 100;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<SystemEvalModel> evaluate(final Set<String> systemNames, final List<OidMetricModel> metrics, final long timeWindow) {
		logger.debug("evaluate started");
		Assert.notNull(systemNames, "systemNames is null");
		Assert.isTrue(!Utilities.containsNullOrEmpty(systemNames), "systemNames contains empty element");
		Assert.notNull(metrics, "metrics is null");
		Assert.isTrue(!Utilities.containsNull(metrics), "metrics contains null element");

		final Map<UUID, List<SystemEvalModel>> deviceMap = new HashMap<>(systemNames.size());
		final List<SystemEvalModel> unknownList = new ArrayList<>();

		final List<System> systemRecords = sysDbService.findByNames(systemNames);
		for (final String sysName : systemNames) {
			boolean found = false;
			for (final System sysRecord : systemRecords) {
				if (sysName.equals(sysRecord.getName())) {
					deviceMap.putIfAbsent(sysRecord.getDevice().getId(), new ArrayList<>());
					deviceMap.get(sysRecord.getDevice().getId()).add(new SystemEvalModel(sysName));
					found = true;
					break;
				}
			}

			if (!found) {
				final SystemEvalModel evalModel = new SystemEvalModel(sysName);
				double score = 0.;
				for (final OidMetricModel metric : metrics) {
					evalModel.addNoStat(metric.getGroup());
					// calculating score using specified weights and "worst" values
					for (final Entry<OidMetric, Double> metricEntry : metric.getMetricWeight().entrySet()) {
						score = score + calculateMetricScore(metricEntry.getKey(), metric.getScaleTo(), metricEntry.getValue(), List.of(), metric.getGroup().getWorstStat());
					}
				}
				evalModel.setScore(score);
				unknownList.add(evalModel);
			}
		}

		final ZonedDateTime afterTimestamp = Utilities.utcNow().minusSeconds(timeWindow);

		final List<SystemEvalModel> result = new ArrayList<>(systemNames.size());
		for (final Entry<UUID, List<SystemEvalModel>> entry : deviceMap.entrySet()) {
			final Set<OidGroup> noStat = new HashSet<>();
			final double score = calculateDeviceMetrics(entry.getKey(), metrics, afterTimestamp, noStat);
			entry.getValue().forEach(sys -> {
				sys.setScore(score);
				sys.setNoStat(noStat);
				result.add(sys);
			});
		}
		result.addAll(unknownList);

		return result;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private double calculateDeviceMetrics(final UUID deviceId, final List<OidMetricModel> metrics, final ZonedDateTime afterTimestamp, final Set<OidGroup> noStat) {
		logger.debug("calculateDeviceMetrics started");

		double score = 0;
		for (final OidMetricModel metricModel : metrics) {
			final List<StatEntity> data = statDbService.getByDeviceIdAfterTimestamp(metricModel.getGroup(), deviceId, afterTimestamp);
			if (!hasData(data)) {
				noStat.add(metricModel.getGroup());
			}

			for (final Entry<OidMetric, Double> metricEntry : metricModel.getMetricWeight().entrySet()) {
				score = score + calculateMetricScore(metricEntry.getKey(), metricModel.getScaleTo(), metricEntry.getValue(), data, metricModel.getGroup().getWorstStat());
			}
		}

		return score;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean hasData(final List<StatEntity> data) {
		logger.debug("hasData started");

		if (Utilities.isEmpty(data)) {
			return false;
		}

		for (final StatEntity stat : data) {
			if (stat.getMean() != -1) {
				return true;
			}
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private double calculateMetricScore(final OidMetric metric, final Double scaleTo, final Double weight, final List<StatEntity> data, final double worstStat) {
		logger.debug("calculateMetricScore started");

		if (Utilities.isEmpty(data)) {
			return worstStat * weight;
		}

		if (metric == OidMetric.CURRENT) {
			data.sort(Comparator.comparing(StatEntity::getTimestamp)); // ascending
			return getValue(metric, data.getLast(), scaleTo, worstStat) * weight;
		}

		final double[] values = new double[data.size()];
		for (int i = 0; i < values.length; ++i) {
			values[i] = getValue(metric, data.get(i), scaleTo, worstStat);
		}

		switch (metric) {
		case MINIMUM:
			return Stat.min(values) * weight;

		case MAXIMUM:
			return Stat.max(values) * weight;

		case MEAN:
			return Stat.mean(values) * weight;

		case MEDIAN:
			return Stat.median(values) * weight;

		default:
			final String errorMsg = "Unhandled OID metric: " + metric;
			logger.error(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private double getValue(final OidMetric metric, final StatEntity stat, final Double scaleTo, final double worstStat) {
		logger.debug("getValue started");

		double value = -1d;
		switch (metric) {
		case MINIMUM:
			value = stat.getMinimum();
			break;

		case MAXIMUM:
			value = stat.getMaximum();
			break;

		case MEAN:
			value = stat.getMean();
			break;

		case MEDIAN:
			value = stat.getMedian();
			break;

		case CURRENT:
			value = stat.getCurrent();
			break;

		default:
			logger.error("Unhandled OID metric: " + metric);
		}

		if (value == -1) {
			return worstStat;
		}

		if (scaleTo != null) {
			value = (value / scaleTo) * HUNDRED;
		}

		return value;
	}
}