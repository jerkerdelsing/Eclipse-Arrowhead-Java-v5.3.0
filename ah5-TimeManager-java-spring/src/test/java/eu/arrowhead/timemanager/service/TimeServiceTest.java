/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import eu.arrowhead.timemanager.api.dto.TimeResponseDTO;

public class TimeServiceTest {

	private static TimeTrustService trust(final long offset) {
		return new TimeTrustService(new SntpClient() {
			@Override
			public Measurement query(final String host, final int port, final int timeoutMs) throws IOException {
				return new Measurement(host, offset, 15, 1);
			}
		}, "ntp.example", 5000, 1000);
	}

	@Test
	public void reportsEpochZoneAndDaylightSaving() {
		final Instant summer = Instant.parse("2026-07-01T12:00:00.250Z");
		final TimeResponseDTO response = new TimeService("Europe/Stockholm", trust(10), Clock.fixed(summer, ZoneOffset.UTC)).getTime();
		assertEquals(summer.getEpochSecond(), response.epoch());
		assertEquals(summer.toEpochMilli(), response.epochMs());
		assertEquals("Europe/Stockholm", response.tz());
		assertTrue(response.dst());
		final Instant winter = Instant.parse("2026-01-15T12:00:00Z");
		assertFalse(new TimeService("Europe/Stockholm", trust(10), Clock.fixed(winter, ZoneOffset.UTC)).getTime().dst());
	}

	@Test
	public void reportsTrustAndItsSource() {
		final TimeTrustService service = trust(42);
		final TimeService timeService = new TimeService("UTC", service, Clock.systemUTC());
		assertFalse(timeService.getTime().trusted());
		service.check();
		final TimeResponseDTO response = timeService.getTime();
		assertTrue(response.trusted());
		assertEquals(42L, response.offsetMs());
		assertEquals("ntp.example", response.timeSource());
		assertNotNull(response.lastCheckTime());
	}

	@Test
	public void rejectsAnInvalidTimeZone() {
		assertThrows(DateTimeException.class, () -> new TimeService("Mars/Olympus", trust(0), Clock.systemUTC()));
	}
}
