/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO;

// Compares the active plant with the registered systems and their sysMonitor observations (v4.6 mismatch detection;
// the sysMonitor history replaces the v4.6 monitorable ping).
public final class AlarmDetector {

	public enum Cause {
		SYSTEM_NOT_REGISTERED,
		MULTIPLE_MATCHES,
		SYSTEM_NOT_IN_DESCRIPTION,
		NOT_MONITORED,
		NO_RECENT_OBSERVATION;

		public boolean isMonitoring() {
			return this == NOT_MONITORED || this == NO_RECENT_OBSERVATION;
		}
	}

	public record Detected(String systemId, String systemName, Cause cause, String description) {

		public String key() {
			return cause + "|" + systemId + "|" + systemName;
		}
	}

	// lastObservations is null when sysMonitor monitoring is off or could not be queried; then no monitoring alarms are produced.
	public static List<Detected> detect(
			final List<PdeSystemDTO> plant,
			final List<SystemResponseDTO> registered,
			final Map<String, String> lastObservations,
			final long staleAfterSeconds,
			final ZonedDateTime now,
			final Set<String> ignoredSystems) {
		final List<Detected> result = new ArrayList<>();
		final Set<String> described = new HashSet<>();
		final Map<String, List<SystemResponseDTO>> matched = RuleResolver.matchSystems(plant, registered);
		for (final PdeSystemDTO system : plant) {
			final List<SystemResponseDTO> matches = matched.get(system.systemId());
			matches.forEach(m -> described.add(m.name()));
			if (matches.isEmpty()) {
				result.add(new Detected(system.systemId(), system.systemName(), Cause.SYSTEM_NOT_REGISTERED,
						"System '" + system.systemId() + "' is in the plant description but not registered"));
			} else if (matches.size() > 1) {
				result.add(new Detected(system.systemId(), system.systemName(), Cause.MULTIPLE_MATCHES,
						"System '" + system.systemId() + "' matches " + matches.size() + " registered systems"));
			} else if (lastObservations != null) {
				final String name = matches.get(0).name();
				final String last = lastObservations.get(name);
				if (last == null) {
					result.add(new Detected(system.systemId(), name, Cause.NOT_MONITORED,
							"System '" + name + "' has published no sysMonitor observations"));
				} else if (isStale(last, staleAfterSeconds, now)) {
					result.add(new Detected(system.systemId(), name, Cause.NO_RECENT_OBSERVATION,
							"System '" + name + "' has published no sysMonitor observation since " + last));
				}
			}
		}

		for (final SystemResponseDTO system : registered) {
			if (!described.contains(system.name()) && !ignoredSystems.contains(system.name())) {
				result.add(new Detected(null, system.name(), Cause.SYSTEM_NOT_IN_DESCRIPTION,
						"System '" + system.name() + "' is registered but not in the active plant description"));
			}
		}
		return result;
	}

	static boolean isStale(final String time, final long staleAfterSeconds, final ZonedDateTime now) {
		try {
			return OffsetDateTime.parse(time).toInstant().isBefore(now.toInstant().minusSeconds(staleAfterSeconds));
		} catch (final DateTimeParseException ex) {
			return true;
		}
	}

	private AlarmDetector() {
		throw new UnsupportedOperationException();
	}
}
