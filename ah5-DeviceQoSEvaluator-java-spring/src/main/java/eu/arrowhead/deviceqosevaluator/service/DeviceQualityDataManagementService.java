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

import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.deviceqosevaluator.dto.DTOConverter;
import eu.arrowhead.deviceqosevaluator.engine.MeasurementEngine;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import eu.arrowhead.deviceqosevaluator.jpa.service.StatDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.model.StatQueryResultModel;
import eu.arrowhead.deviceqosevaluator.service.validation.DeviceQualityDataManagementValidation;
import eu.arrowhead.dto.QoSDeviceStatQueryRequestDTO;
import eu.arrowhead.dto.QoSDeviceStatQueryResponseDTO;

@Service
public class DeviceQualityDataManagementService {

	//=================================================================================================
	// members

	@Autowired
	private StatDbService statDbService;

	@Autowired
	private MeasurementEngine measurementEngine;

	@Autowired
	private PageService pageService;

	@Autowired
	private DeviceQualityDataManagementValidation validator;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public QoSDeviceStatQueryResponseDTO query(final QoSDeviceStatQueryRequestDTO dto, final String origin) {
		logger.debug("query started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final QoSDeviceStatQueryRequestDTO normalized = validator.validateAndNormalizeQueryRequest(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, StatEntity.SORTABLE_FIELDS_BY, StatEntity.DEFAULT_SORT_FIELD, origin);

		try {
			final Page<StatQueryResultModel> results = statDbService.query(
					normalized.systemNames(),
					Utilities.parseUTCStringToZonedDateTime(normalized.from()),
					Utilities.parseUTCStringToZonedDateTime(normalized.to()),
					OidGroup.valueOf(normalized.metricGroup()), pageRequest);

			return dtoConverter.convertStatQueryResultModelPageToDTO(results, normalized.aggregation().stream().map(m -> OidMetric.valueOf(m)).collect(Collectors.toSet()));
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public String reload(final String origin) {
		logger.debug("reload started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		try {
			final Pair<Integer, Integer> results = measurementEngine.organize();
			if (results == null) {
				return "Reload operation is already in proggress";
			}

			return results.getFirst() + " more systems found, " + results.getSecond() + " systems removed";
		} catch (final ExternalServerError ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new ExternalServerError(ex.getMessage(), origin);

		} catch (final InternalServerError | SchedulerException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}