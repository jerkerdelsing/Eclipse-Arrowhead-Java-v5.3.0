/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;

public class TimeTrustServiceTest {

	// Answers with a fixed offset per server; a server without an entry is unreachable.
	private static SntpClient stub(final Map<String, Long> offsets) {
		return new SntpClient() {
			@Override
			public Measurement query(final String host, final int port, final int timeoutMs) throws IOException {
				final Long offset = offsets.get(host);
				if (offset == null) {
					throw new IOException("timeout");
				}
				return new Measurement(host, offset, 20, 2);
			}
		};
	}

	@Test
	public void untrustedUntilTheFirstCheck() {
		final TimeTrustService service = new TimeTrustService(stub(Map.of("a", 10L)), "a", 5000, 1000);
		assertFalse(service.state().trusted());
		assertTrue(service.check().trusted());
	}

	@Test
	public void trustsSmallOffsetsInBothDirections() {
		assertTrue(new TimeTrustService(stub(Map.of("a", 4999L)), "a", 5000, 1000).check().trusted());
		assertTrue(new TimeTrustService(stub(Map.of("a", -4999L)), "a", 5000, 1000).check().trusted());
	}

	@Test
	public void distrustsLargeNegativeOffsets() {
		// v4.6 compared the signed offset and therefore trusted any clock that ran ahead of NTP
		final TimeTrustService.TrustState state = new TimeTrustService(stub(Map.of("a", -60000L)), "a", 5000, 1000).check();
		assertFalse(state.trusted());
		assertEquals(-60000L, state.offsetMs());
	}

	@Test
	public void fallsBackToTheNextServer() {
		final TimeTrustService.TrustState state = new TimeTrustService(stub(Map.of("b", 12L)), " a , b ", 5000, 1000).check();
		assertTrue(state.trusted());
		assertEquals("b", state.server());
	}

	@Test
	public void distrustsWhenNoServerAnswers() {
		final TimeTrustService.TrustState state = new TimeTrustService(stub(Map.of()), "a,b", 5000, 1000).check();
		assertFalse(state.trusted());
		assertNull(state.offsetMs());
		assertTrue(state.detail().contains("No NTP server reachable"));
	}

	@Test
	public void sysMonitorEchoFollowsTrust() {
		final TimeTrustService service = new TimeTrustService(stub(Map.of("a", 99999L)), "a", 5000, 1000);
		final TimeManagerHealthIndicator indicator = new TimeManagerHealthIndicator(service);
		service.check();
		final SysMonitorHealth health = indicator.health();
		assertFalse(health.healthy());
		assertTrue(health.detail().contains("exceeds"));
	}
}
