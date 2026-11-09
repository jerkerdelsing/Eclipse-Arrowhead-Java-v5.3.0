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
package eu.arrowhead.deviceqosevaluator.dto;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.deviceqosevaluator.enums.OidMetric;
import eu.arrowhead.deviceqosevaluator.jpa.service.model.StatQueryResultModel;
import eu.arrowhead.dto.QoSDeviceStatQueryResponseDTO;
import eu.arrowhead.dto.QoSDeviceStatRecordDTO;

@Service
public class DTOConverter {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public QoSDeviceStatQueryResponseDTO convertStatQueryResultModelPageToDTO(final Page<StatQueryResultModel> page, final Set<OidMetric> metricsNeeded) {
		Assert.notNull(page, "page is null");
		Assert.notNull(metricsNeeded, "metricsNeeded is null");

		return new QoSDeviceStatQueryResponseDTO(page.stream().map(m -> convertStatQueryResultModelToDTO(m, metricsNeeded)).toList(), page.getTotalElements());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private QoSDeviceStatRecordDTO convertStatQueryResultModelToDTO(final StatQueryResultModel model, final Set<OidMetric> metricsNeeded) {
		return new QoSDeviceStatRecordDTO(
				model.group().name(),
				model.stat().getId(),
				Utilities.convertZonedDateTimeToUTCString(model.stat().getTimestamp()),
				model.stat().getUuid().toString(),
				!metricsNeeded.contains(OidMetric.MINIMUM) ? null : model.stat().getMinimum(),
				!metricsNeeded.contains(OidMetric.MAXIMUM) ? null : model.stat().getMaximum(),
				!metricsNeeded.contains(OidMetric.MEAN) ? null : model.stat().getMean(),
				!metricsNeeded.contains(OidMetric.MEDIAN) ? null : model.stat().getMedian(),
				!metricsNeeded.contains(OidMetric.CURRENT) ? null : model.stat().getCurrent(),
				model.system().stream().map((sys -> sys.getName())).toList());
	}
}