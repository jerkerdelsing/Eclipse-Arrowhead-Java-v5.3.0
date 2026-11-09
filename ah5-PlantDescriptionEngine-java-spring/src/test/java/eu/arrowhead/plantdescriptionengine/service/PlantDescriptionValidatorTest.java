/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.connect;
import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.consumer;
import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.producer;
import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.system;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class PlantDescriptionValidatorTest {

	private static final PlantDescriptionBody VALID = new PlantDescriptionBody(
			List.of(),
			List.of(system("c", "Consumer", consumer("in", "temperature")), system("p", "Producer", producer("out", "temperature", null))),
			List.of(connect("c", "in", "p", "out", 1)));

	private static List<String> validate(final PlantDescriptionBody body, final Map<Long, PlantDescriptionBody> others) {
		return PlantDescriptionValidator.validate("Plant", 1L, body, id -> Optional.ofNullable(others.get(id)));
	}

	private static boolean has(final List<String> errors, final String fragment) {
		return errors.stream().anyMatch(e -> e.contains(fragment));
	}

	@Test
	public void validDescriptionHasNoErrors() {
		assertEquals(List.of(), validate(VALID, Map.of()));
	}

	@Test
	public void includeRules() {
		assertTrue(has(validate(new PlantDescriptionBody(List.of(1L), List.of(), List.of()), Map.of()), "includes itself"));
		assertTrue(has(validate(new PlantDescriptionBody(List.of(2L, 2L), List.of(), List.of()), Map.of(2L, new PlantDescriptionBody(null, null, null))), "more than once"));
		assertTrue(has(validate(new PlantDescriptionBody(List.of(3L), List.of(), List.of()), Map.of()), "does not exist"));
		final Map<Long, PlantDescriptionBody> cycle = Map.of(2L, new PlantDescriptionBody(List.of(3L), null, null), 3L, new PlantDescriptionBody(List.of(2L), null, null));
		assertTrue(has(validate(new PlantDescriptionBody(List.of(2L), List.of(), List.of()), cycle), "cycle"));
		final Map<Long, PlantDescriptionBody> backToSelf = Map.of(2L, new PlantDescriptionBody(List.of(1L), null, null));
		assertTrue(has(validate(new PlantDescriptionBody(List.of(2L), List.of(), List.of()), backToSelf), "cycle"));
	}

	@Test
	public void connectionsMayReferToIncludedSystems() {
		final PlantDescriptionBody base = new PlantDescriptionBody(null, List.of(system("p", "Producer", producer("out", "temperature", null))), null);
		final PlantDescriptionBody body = new PlantDescriptionBody(List.of(2L), List.of(system("c", "Consumer", consumer("in", "temperature"))), List.of(connect("c", "in", "p", "out", null)));
		assertEquals(List.of(), validate(body, Map.of(2L, base)));
	}

	@Test
	public void systemRules() {
		assertTrue(has(validate(new PlantDescriptionBody(null, List.of(system("unknown", "A")), null), Map.of()), "reserved"));
		assertTrue(has(validate(new PlantDescriptionBody(null, List.of(system("a", "A"), system("a", "B")), null), Map.of()), "Duplicate systemId"));
		assertTrue(has(validate(new PlantDescriptionBody(null, List.of(system("a", null)), null), Map.of()), "needs a systemName or metadata"));
		assertTrue(has(validate(new PlantDescriptionBody(null, List.of(system("a", "A"), system("b", "A")), null), Map.of()), "cannot be told apart"));
	}

	@Test
	public void portRules() {
		assertTrue(has(validate(new PlantDescriptionBody(null, List.of(system("a", "A", consumer("x", "temp"), consumer("x", "temp"))), null), Map.of()), "Duplicate portName"));
		assertTrue(has(validate(new PlantDescriptionBody(null, List.of(system("a", "A", consumer("x", "Temp"))), null), Map.of()), "camelCase"));
		assertTrue(has(validate(new PlantDescriptionBody(null, List.of(system("a", "A", producer("x", "temp", null), producer("y", "temp", null))), null), Map.of()), "needs metadata"));
		assertTrue(has(validate(new PlantDescriptionBody(null,
				List.of(system("a", "A", producer("x", "temp", Map.of("k", "1")), producer("y", "temp", Map.of("k", "1")))), null), Map.of()), "same metadata"));
		assertEquals(List.of(), validate(new PlantDescriptionBody(null,
				List.of(system("a", "A", producer("x", "temp", Map.of("k", "1")), producer("y", "temp", Map.of("k", "2")))), null), Map.of()));
	}

	@Test
	public void connectionRules() {
		final List<eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO> systems = List.of(
				system("c", "Consumer", consumer("in", "temperature"), consumer("in2", "pressure")),
				system("p", "Producer", producer("out", "temperature", null)));
		assertTrue(has(validate(new PlantDescriptionBody(null, systems, List.of(connect("p", "out", "c", "in", null))), Map.of()), "is not a consumer port"));
		assertTrue(has(validate(new PlantDescriptionBody(null, systems, List.of(connect("c", "in2", "p", "out", null))), Map.of()), "different service definitions"));
		assertTrue(has(validate(new PlantDescriptionBody(null, systems, List.of(connect("c", "in", "x", "out", null))), Map.of()), "unknown system"));
		assertTrue(has(validate(new PlantDescriptionBody(null, systems, List.of(connect("c", "in", "p", "nope", null))), Map.of()), "has no port"));
		assertTrue(has(validate(new PlantDescriptionBody(null, systems, List.of(connect("c", "in", "p", "out", -1))), Map.of()), "negative priority"));
	}
}
