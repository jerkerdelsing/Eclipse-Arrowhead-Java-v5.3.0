/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.service;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import eu.arrowhead.timemanager.TimeManagerConstants;

// Decides whether the local clock can be trusted by comparing it with the configured NTP servers.
@Service
public class TimeTrustService {

	public record TrustState(boolean trusted, Long offsetMs, Long delayMs, String server, ZonedDateTime checkedAt, String detail) {

		static TrustState unknown() {
			return new TrustState(false, null, null, null, null, "No time check completed yet");
		}
	}

	private final Logger logger = LogManager.getLogger(this.getClass());

	private final SntpClient client;
	private final List<String> servers;
	private final long offsetThresholdMs;
	private final int timeoutMs;
	private final AtomicReference<TrustState> state = new AtomicReference<>(TrustState.unknown());

	public TimeTrustService(
			final SntpClient client,
			@Value(TimeManagerConstants.$NTP_SERVER_LIST_WD) final String serverList,
			@Value(TimeManagerConstants.$TIME_OFFSET_THRESHOLD_WD) final long offsetThresholdMs,
			@Value(TimeManagerConstants.$NTP_TIMEOUT_WD) final int timeoutMs) {
		this.client = client;
		this.servers = Arrays.stream(serverList.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
		this.offsetThresholdMs = offsetThresholdMs;
		this.timeoutMs = timeoutMs;
	}

	public TrustState state() {
		return state.get();
	}

	// The first server that answers decides; the others are fallbacks.
	@Scheduled(fixedDelayString = TimeManagerConstants.$NTP_CHECK_INTERVAL_WD, initialDelay = 1000)
	public TrustState check() {
		final List<String> failures = new ArrayList<>();
		for (final String server : servers) {
			try {
				final SntpClient.Measurement measurement = client.query(server, SntpClient.NTP_PORT, timeoutMs);
				final boolean trusted = Math.abs(measurement.offsetMs()) < offsetThresholdMs;
				final String detail = trusted
						? "Clock offset " + measurement.offsetMs() + " ms to " + server + " is within " + offsetThresholdMs + " ms"
						: "Clock offset " + measurement.offsetMs() + " ms to " + server + " exceeds " + offsetThresholdMs + " ms";
				logger.debug("NTP {}: offset {} ms, delay {} ms, stratum {}", server, measurement.offsetMs(), measurement.delayMs(), measurement.stratum());
				return record(new TrustState(trusted, measurement.offsetMs(), measurement.delayMs(), server, now(), detail));
			} catch (final Exception ex) {
				logger.debug("NTP query to {} failed: {}", server, ex.getMessage());
				failures.add(server + ": " + ex.getMessage());
			}
		}

		return record(new TrustState(false, null, null, null, now(), servers.isEmpty() ? "No NTP server configured" : "No NTP server reachable (" + String.join("; ", failures) + ")"));
	}

	private TrustState record(final TrustState next) {
		final TrustState previous = state.getAndSet(next);
		if (previous.trusted() != next.trusted()) {
			logger.info("Time trust changed to {}: {}", next.trusted(), next.detail());
		}
		return next;
	}

	private static ZonedDateTime now() {
		return ZonedDateTime.now(ZoneOffset.UTC);
	}
}
