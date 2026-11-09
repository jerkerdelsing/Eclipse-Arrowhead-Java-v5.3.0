/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.datamanager.api.dto.SenMLRecordDTO;

@ExtendWith(MockitoExtension.class)
class ProxyServiceTest {

	@Mock
	private DataManagerNameService nameService;

	private ProxyService proxyService;

	@BeforeEach
	void init() {
		proxyService = new ProxyService(nameService, new SenMLValidation());
		when(nameService.normalizeSystemName("SystemOne")).thenReturn("SystemOne");
		when(nameService.normalizeServiceName("temperatureData")).thenReturn("temperatureData");
	}

	@Test
	void scopesServiceDataBySystemAndService() {
		when(nameService.normalizeSystemName("SystemTwo")).thenReturn("SystemTwo");
		proxyService.put("SystemOne", "temperatureData", pack(20D), "test");
		proxyService.put("SystemTwo", "temperatureData", pack(30D), "test");

		assertEquals(20D, proxyService.get("SystemOne", "temperatureData", "test").getFirst().v());
		assertEquals(30D, proxyService.get("SystemTwo", "temperatureData", "test").getFirst().v());
		assertEquals(List.of("SystemOne", "SystemTwo"), proxyService.getSystems());
	}

	@Test
	void reportsMissingEndpoint() {
		assertThrows(DataNotFoundException.class, () -> proxyService.get("SystemOne", "temperatureData", "test"));
	}

	private List<SenMLRecordDTO> pack(final double value) {
		return List.of(new SenMLRecordDTO("temperaturePack", 1D, null, null, null, null,
				"temperature", null, value, null, null, null, null, null, null));
	}
}
