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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.dto.QoSDeviceStatQueryRequestDTO;

@Service
public class DeviceQualityDataManagementNormalization {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer sysNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public QoSDeviceStatQueryRequestDTO normalizeQoSDeviceStatQueryRequestDTO(final QoSDeviceStatQueryRequestDTO dto) {
		logger.debug("normalizeQoSDeviceStatQueryRequestDTO");
		Assert.notNull(dto, "dto is null");
		Assert.notNull(dto.metricGroup(), "metric group is null");

		return new QoSDeviceStatQueryRequestDTO(
				dto.pagination(),
				dto.metricGroup().trim().toUpperCase(),
				Utilities.isEmpty(dto.from()) ? null : dto.from().trim(),
				Utilities.isEmpty(dto.to()) ? null : dto.to().trim(),
				Utilities.isEmpty(dto.aggregation()) ? OidMetric.all().stream().map(v -> v.name()).toList() : dto.aggregation().stream().map(a -> a.trim().toUpperCase()).toList(),
				Utilities.isEmpty(dto.systemNames()) ? null : dto.systemNames().stream().map(sys -> sysNameNormalizer.normalize(sys)).toList());
	}
}