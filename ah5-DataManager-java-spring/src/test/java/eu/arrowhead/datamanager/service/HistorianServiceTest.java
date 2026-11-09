/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.datamanager.api.dto.SenMLRecordDTO;
import eu.arrowhead.datamanager.jpa.entity.HistorianRecord;
import eu.arrowhead.datamanager.jpa.repository.HistorianRecordRepository;

@ExtendWith(MockitoExtension.class)
class HistorianServiceTest {

	@Mock
	private HistorianRecordRepository repository;

	@Mock
	private DataManagerNameService nameService;

	private HistorianService historianService;

	@BeforeEach
	void init() {
		historianService = new HistorianService(repository, nameService, new SenMLValidation(), new ObjectMapper());
		when(nameService.normalizeSystemName("WeatherStation")).thenReturn("WeatherStation");
		when(nameService.normalizeServiceName("temperatureData")).thenReturn("temperatureData");
	}

	@Test
	void persistsCompleteSenMLPack() {
		historianService.put("WeatherStation", "temperatureData", pack(), "test");
		verify(repository).saveAndFlush(any(HistorianRecord.class));
	}

	@Test
	void returnsStoredPack() throws Exception {
		final String json = new ObjectMapper().writeValueAsString(pack());
		final HistorianRecord entity = new HistorianRecord("WeatherStation", "temperatureData", 100D, 100D, 101D, json);
		when(repository.findInTimeRange(anyString(), anyString(), anyDouble(), anyDouble(), any(Pageable.class))).thenReturn(List.of(entity));

		final List<SenMLRecordDTO> result = historianService.get("WeatherStation", "temperatureData", null, null, 1, null, null, "test");
		assertEquals(pack(), result);
	}

	@Test
	void reportsMissingHistory() {
		when(repository.findInTimeRange(anyString(), anyString(), anyDouble(), anyDouble(), any(Pageable.class))).thenReturn(List.of());

		assertThrows(DataNotFoundException.class,
				() -> historianService.get("WeatherStation", "temperatureData", null, null, 1, null, null, "test"));
	}

	private List<SenMLRecordDTO> pack() {
		return List.of(
				new SenMLRecordDTO("temperaturePack", 100D, "Cel", null, null, null,
						"temperature", null, 21.5, null, null, null, null, null, null),
				new SenMLRecordDTO(null, null, null, null, null, null,
						"temperature", null, 22D, null, null, null, null, 1D, null));
	}
}
