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

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import eu.arrowhead.deviceqosevaluator.service.normalization.DeviceQualityDataManagementNormalization;
import eu.arrowhead.dto.QoSDeviceStatQueryRequestDTO;

@Service
public class DeviceQualityDataManagementValidation {

	//-------------------------------------------------------------------------------------------------
	// members

	@Autowired
	private PageValidator pageValidator;

	@Autowired
	private SystemNameValidator sysNameValidator;

	@Autowired
	private DeviceQualityDataManagementNormalization normalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATE & NORMALIZE

	//-------------------------------------------------------------------------------------------------
	public QoSDeviceStatQueryRequestDTO validateAndNormalizeQueryRequest(final QoSDeviceStatQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryRequest started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateQueryRequest(dto, origin);
		final QoSDeviceStatQueryRequestDTO normalized = normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto);

		if (!Utilities.isEnumValue(normalized.metricGroup(), OidGroup.class)) {
			throw new InvalidParameterException("Invalid metricGroup: " + normalized.metricGroup(), origin);
		}

		ZonedDateTime from = null;
		ZonedDateTime to = null;
		try {
			if (!Utilities.isEmpty(normalized.from())) {
				from = Utilities.parseUTCStringToZonedDateTime(normalized.from());
			}
		} catch (final DateTimeParseException ex) {
			throw new InvalidParameterException("Invalid 'from' time: " + normalized.from(), origin);
		}
		try {
			if (!Utilities.isEmpty(normalized.to())) {
				to = Utilities.parseUTCStringToZonedDateTime(normalized.to());
			}
		} catch (final DateTimeParseException ex) {
			throw new InvalidParameterException("Invalid 'to' time: " + normalized.to(), origin);
		}

		if (from != null && to != null) {
			if (from.isAfter(to)) {
				throw new InvalidParameterException("Invalid period", origin);
			}
		}

		if (!Utilities.isEmpty(normalized.aggregation())) {
			normalized.aggregation().forEach(metric -> {
				if (!Utilities.isEnumValue(metric, OidMetric.class)) {
					throw new InvalidParameterException("Invalid aggregation: " + metric, origin);
				}
			});
		}

		if (!Utilities.isEmpty(normalized.systemNames())) {
			normalized.systemNames().forEach(sys -> {
				try {
					sysNameValidator.validateSystemName(sys);
				} catch (final InvalidParameterException ex) {
					throw new InvalidParameterException(ex.getMessage(), origin);
				}
			});
		}

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateQueryRequest(final QoSDeviceStatQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryRequest started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		pageValidator.validatePageParameter(dto.pagination(), StatEntity.SORTABLE_FIELDS_BY, origin);

		if (Utilities.isEmpty(dto.metricGroup())) {
			throw new InvalidParameterException("metricGroup is missing", origin);
		}
	}
}
