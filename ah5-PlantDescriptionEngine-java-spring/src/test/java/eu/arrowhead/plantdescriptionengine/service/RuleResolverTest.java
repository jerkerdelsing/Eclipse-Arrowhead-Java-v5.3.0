/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.connect;
import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.consumer;
import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.instance;
import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.producer;
import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.registered;
import static eu.arrowhead.plantdescriptionengine.service.PlantFixtures.system;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO;

public class RuleResolverTest {

	@Test
	public void connectionBecomesRuleForMatchingInstance() {
		final List<PdeSystemDTO> systems = List.of(system("c", "Consumer", consumer("in", "temperature")), system("p", "Producer", producer("out", "temperature", null)));
		final Set<RuleResolver.Rule> rules = RuleResolver.resolve(systems, List.of(connect("c", "in", "p", "out", null)),
				List.of(registered("Consumer", null), registered("Producer", null), registered("Other", null)),
				List.of(instance("Producer|temperature|1.0.0", "Producer", "temperature", null), instance("Other|temperature|1.0.0", "Other", "temperature", null)),
				5);
		assertEquals(Set.of(new RuleResolver.Rule("Consumer", "Producer|temperature|1.0.0", 5)), rules);
	}

    @Test
    public void missingProducerKeepsManagedScopeWithoutProviders() {
        final var systems = List.of(system("c", "Consumer", consumer("in", "temperature")), system("p", "Producer", producer("out", "temperature", null)));
        final var snapshot = RuleResolver.snapshot(systems, List.of(connect("c", "in", "p", "out", null)), List.of(registered("Consumer", null)), List.of(), 1);
        assertEquals(List.of(new eu.arrowhead.dto.OrchestrationPlantRuleDTO("Consumer", "temperature", List.of())), snapshot.rules());
    }

    @Test
    public void snapshotDeduplicatesProviderAndKeepsLowestConnectionPriority() {
        final var systems = List.of(system("c", "Consumer", consumer("in", "temperature")), system("p", "Producer", producer("out", "temperature", null)));
        final var snapshot = RuleResolver.snapshot(systems, List.of(connect("c", "in", "p", "out", 2), connect("c", "in", "p", "out", 1)),
                List.of(registered("Consumer", null), registered("Producer", null)), List.of(instance("id", "Producer", "temperature", null)), 1);
        assertEquals(List.of(new eu.arrowhead.dto.OrchestrationPlantProviderDTO("id", 1)), snapshot.rules().getFirst().providers());
    }

	@Test
	public void producerPortMetadataSelectsInstance() {
		final List<PdeSystemDTO> systems = List.of(system("c", "Consumer", consumer("in", "temperature")),
				system("p", "Producer", producer("a", "temperature", Map.of("room", "a")), producer("b", "temperature", Map.of("room", "b"))));
		final Set<RuleResolver.Rule> rules = RuleResolver.resolve(systems, List.of(connect("c", "in", "p", "b", 2)),
				List.of(registered("Consumer", null), registered("Producer", null)),
				List.of(instance("i-a", "Producer", "temperature", Map.of("room", "a")), instance("i-b", "Producer", "temperature", Map.of("room", "b"))),
				1);
		assertEquals(Set.of(new RuleResolver.Rule("Consumer", "i-b", 2)), rules);
	}

	@Test
	public void collidingPrioritiesAreSpread() {
		final List<PdeSystemDTO> systems = List.of(system("c", "Consumer", consumer("in", "temperature")),
				system("p", "Producer", producer("out", "temperature", null)), system("q", "Backup", producer("out", "temperature", null)));
		final Set<RuleResolver.Rule> rules = RuleResolver.resolve(systems, List.of(connect("c", "in", "p", "out", 1), connect("c", "in", "q", "out", 1)),
				List.of(registered("Consumer", null), registered("Producer", null), registered("Backup", null)),
				List.of(instance("i-p", "Producer", "temperature", null), instance("i-q", "Backup", "temperature", null)),
				1);
		assertEquals(Set.of(new RuleResolver.Rule("Consumer", "i-p", 1), new RuleResolver.Rule("Consumer", "i-q", 2)), rules);
	}

	@Test
	public void ambiguousOrMissingSystemsGiveNoRule() {
		final List<PdeSystemDTO> systems = List.of(new PdeSystemDTO("c", null, Map.of("role", "client"), List.of(consumer("in", "temperature"))),
				system("p", "Producer", producer("out", "temperature", null)));
		final Set<RuleResolver.Rule> rules = RuleResolver.resolve(systems, List.of(connect("c", "in", "p", "out", null)),
				List.of(registered("C1", Map.of("role", "client")), registered("C2", Map.of("role", "client")), registered("Producer", null)),
				List.of(instance("i-p", "Producer", "temperature", null)),
				1);
		assertEquals(Set.of(), rules);
	}
}
