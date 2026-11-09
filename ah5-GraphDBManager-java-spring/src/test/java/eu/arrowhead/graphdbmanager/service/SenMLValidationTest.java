/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.graphdbmanager.api.dto.SenMLRecordDTO;

class SenMLValidationTest {

	private final SenMLValidation validator = new SenMLValidation();

	@Test
	void addsMissingBaseTime() {
		final List<SenMLRecordDTO> result = validator.validateAndComplete(
				List.of(record("temperaturePack", null, "temperature", 22.5, null)),
				"test");

		assertNotNull(result.getFirst().bt());
	}

	@Test
	void rejectsMoreThanOneValueField() {
		final SenMLRecordDTO invalid = new SenMLRecordDTO(
				"temperaturePack", 1D, null, null, null, null,
				"temperature", null, 22.5, "warm", null, null, null, null, null);

		assertThrows(InvalidParameterException.class, () -> validator.validateAndComplete(List.of(invalid), "test"));
	}

	@Test
	void rejectsBaseNameOutsideFirstRecord() {
		final SenMLRecordDTO first = record(null, 1D, "temperature", 22.5, null);
		final SenMLRecordDTO second = record("temperaturePack", null, "humidity", 50D, 1D);

		assertThrows(InvalidParameterException.class, () -> validator.validateAndComplete(List.of(first, second), "test"));
	}

	private SenMLRecordDTO record(final String baseName, final Double baseTime, final String name, final Double value, final Double time) {
		return new SenMLRecordDTO(baseName, baseTime, null, null, null, null, name, null, value, null, null, null, null, time, null);
	}
}
