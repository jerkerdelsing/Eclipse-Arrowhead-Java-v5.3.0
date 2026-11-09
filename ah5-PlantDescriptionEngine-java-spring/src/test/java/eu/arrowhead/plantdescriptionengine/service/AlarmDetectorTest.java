/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.registered;
import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.system;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import eu.arrowhead.plantdescriptionengine.service.AlarmDetector.Cause;

public class AlarmDetectorTest {

	private static final ZonedDateTime NOW = ZonedDateTime.parse("2026-10-12T10:00:00Z");

	private static List<Cause> causes(final List<AlarmDetector.Detected> detected) {
		return detected.stream().map(AlarmDetector.Detected::cause).toList();
	}

	@Test
	public void registrationMismatches() {
		final List<AlarmDetector.Detected> detected = AlarmDetector.detect(
				List.of(system("a", "Missing"), new eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO("b", null, Map.of("k", "v"), null)),
				List.of(registered("X", Map.of("k", "v")), registered("Y", Map.of("k", "v")), registered("ServiceRegistry", null)),
				null, 120, NOW, Set.of("ServiceRegistry"));
		assertEquals(List.of(Cause.SYSTEM_NOT_REGISTERED, Cause.MULTIPLE_MATCHES), causes(detected));
	}

	@Test
	public void unknownRegisteredSystemIsReported() {
		final List<AlarmDetector.Detected> detected = AlarmDetector.detect(List.of(system("a", "A")), List.of(registered("A", null), registered("Intruder", null)), null, 120, NOW, Set.of());
		assertEquals(List.of(Cause.SYSTEM_NOT_IN_DESCRIPTION), causes(detected));
		assertEquals("Intruder", detected.get(0).systemName());
	}

	@Test
	public void sysMonitorObservations() {
		final List<eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO> plant = List.of(system("a", "A"), system("b", "B"), system("c", "C"));
		final List<AlarmDetector.Detected> detected = AlarmDetector.detect(plant, List.of(registered("A", null), registered("B", null), registered("C", null)),
				Map.of("A", "2026-10-12T09:59:30Z", "B", "2026-10-12T09:00:00Z"), 120, NOW, Set.of());
		assertEquals(List.of(Cause.NO_RECENT_OBSERVATION, Cause.NOT_MONITORED), causes(detected));
		assertEquals("B", detected.get(0).systemName());
	}
}
