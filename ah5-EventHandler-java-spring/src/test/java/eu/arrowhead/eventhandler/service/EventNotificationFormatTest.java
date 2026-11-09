/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import eu.arrowhead.eventhandler.api.dto.EventNotificationDTO;

class EventNotificationFormatTest {

	@Test
	void timestampIsRfc3339EvenWithNumericDateDefaults() throws Exception {
		final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		final EventNotificationDTO dto = new EventNotificationDTO("temperatureChanged", "21.5", Map.of(), Instant.parse("2026-09-20T10:15:30Z"), "Sensor");
		assertEquals("2026-09-20T10:15:30Z", mapper.readTree(mapper.writeValueAsString(dto)).get("timestamp").asText());
	}
}
