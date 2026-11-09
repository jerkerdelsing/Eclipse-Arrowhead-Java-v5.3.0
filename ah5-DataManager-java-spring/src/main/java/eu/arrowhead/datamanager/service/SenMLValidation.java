/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.datamanager.api.dto.SenMLRecordDTO;

@Service
public class SenMLValidation {

	public List<SenMLRecordDTO> validateAndComplete(final List<SenMLRecordDTO> records, final String origin) {
		if (Utilities.isEmpty(records) || Utilities.containsNull(records)) {
			throw new InvalidParameterException("SenML pack is missing, empty or contains a null record", origin);
		}

		int baseNameCount = 0;
		int baseTimeCount = 0;
		int baseUnitCount = 0;
		for (final SenMLRecordDTO record : records) {
			baseNameCount += record.bn() == null ? 0 : 1;
			baseTimeCount += record.bt() == null ? 0 : 1;
			baseUnitCount += record.bu() == null ? 0 : 1;

			final int valueCount = (record.v() == null ? 0 : 1)
					+ (record.vs() == null ? 0 : 1)
					+ (record.vb() == null ? 0 : 1)
					+ (record.vd() == null ? 0 : 1);
			if (valueCount > 1 && record.s() == null) {
				throw new InvalidParameterException("A SenML record contains more than one value field", origin);
			}
		}

		if (baseNameCount != 1 || records.getFirst().bn() == null) {
			throw new InvalidParameterException("SenML base name (bn) must occur exactly once in the first record", origin);
		}
		if (baseTimeCount > 1 || (baseTimeCount == 1 && records.getFirst().bt() == null)) {
			throw new InvalidParameterException("SenML base time (bt) may occur only in the first record", origin);
		}
		if (baseUnitCount > 1 || (baseUnitCount == 1 && records.getFirst().bu() == null)) {
			throw new InvalidParameterException("SenML base unit (bu) may occur only in the first record", origin);
		}

		final double baseTime = records.getFirst().bt() == null ? Instant.now().toEpochMilli() / 1000D : records.getFirst().bt();
		if (baseTime < 0D) {
			throw new InvalidParameterException("SenML base time (bt) cannot be negative", origin);
		}

		final List<SenMLRecordDTO> result = new ArrayList<>(records);
		result.set(0, records.getFirst().withBaseTime(baseTime));
		return List.copyOf(result);
	}
}
