/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.timemanager.TimeManagerConstants;
import eu.arrowhead.timemanager.api.dto.TimeResponseDTO;

@Service
public class TimeService {

	private final ZoneId zone;
	private final TimeTrustService trustService;
	private final Clock clock;

	@Autowired
	public TimeService(@Value(TimeManagerConstants.$TIME_TIMEZONE_WD) final String timezone, final TimeTrustService trustService) {
		this(timezone, trustService, Clock.systemUTC());
	}

	TimeService(final String timezone, final TimeTrustService trustService, final Clock clock) {
		this.zone = ZoneId.of(timezone); // fails start-up on an invalid time.timezone
		this.trustService = trustService;
		this.clock = clock;
	}

	public TimeResponseDTO getTime() {
		final Instant now = clock.instant();
		final TimeTrustService.TrustState trust = trustService.state();
		return new TimeResponseDTO(
				now.getEpochSecond(),
				now.toEpochMilli(),
				zone.getId(),
				zone.getRules().isDaylightSavings(now),
				trust.trusted(),
				trust.offsetMs(),
				trust.server(),
				trust.checkedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(trust.checkedAt()));
	}
}
