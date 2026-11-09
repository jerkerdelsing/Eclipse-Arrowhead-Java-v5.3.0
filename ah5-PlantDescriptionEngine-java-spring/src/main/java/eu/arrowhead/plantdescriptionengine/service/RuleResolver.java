/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.plantdescriptionengine.api.dto.ConnectionDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PortDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.SystemPortDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;

// Turns the active plant into DSO provider rules: every connection becomes (consumer system, producer service instance, priority).
public final class RuleResolver {

	public record Rule(String consumer, String serviceInstanceId, int priority) {
	}

	// A plant system matches a registered system when the names agree (if named) and its metadata is a subset.
	public static boolean matches(final PdeSystemDTO plant, final SystemResponseDTO registered) {
		if (!Utilities.isEmpty(plant.systemName()) && !plant.systemName().equals(registered.name())) {
			return false;
		}
		return subset(plant.metadata(), registered.metadata());
	}

	public static Map<String, List<SystemResponseDTO>> matchSystems(final List<PdeSystemDTO> plant, final List<SystemResponseDTO> registered) {
		final Map<String, List<SystemResponseDTO>> result = new LinkedHashMap<>();
		for (final PdeSystemDTO system : plant) {
			result.put(system.systemId(), registered.stream().filter(r -> matches(system, r)).toList());
		}
		return result;
	}

	public static Set<Rule> resolve(
			final List<PdeSystemDTO> systems,
			final List<ConnectionDTO> connections,
			final List<SystemResponseDTO> registered,
			final List<ServiceInstanceResponseDTO> instances,
			final int defaultPriority) {
		final Map<String, List<SystemResponseDTO>> matched = matchSystems(systems, registered);
		final Set<Rule> rules = new LinkedHashSet<>();
		// Keep a deterministic rank per consumer/service; colliding plant priorities move to the next free priority
		final Map<String, Set<Integer>> usedPriorities = new LinkedHashMap<>();
		for (final ConnectionDTO connection : connections) {
			final Optional<PortDTO> consumerPort = port(systems, connection.consumer());
			final Optional<PortDTO> producerPort = port(systems, connection.producer());
			// ambiguous matches are reported as alarms and get no rule
			final List<SystemResponseDTO> consumers = matched.getOrDefault(connection.consumer().systemId(), List.of());
			final List<SystemResponseDTO> producers = matched.getOrDefault(connection.producer().systemId(), List.of());
			if (consumerPort.isEmpty() || producerPort.isEmpty() || consumers.size() != 1 || producers.size() != 1) {
				continue;
			}

			final PortDTO port = producerPort.get();
			final String iface = Utilities.isEmpty(port.serviceInterface()) ? consumerPort.get().serviceInterface() : port.serviceInterface();
			final int requested = connection.priority() == null ? defaultPriority : connection.priority();
			for (final ServiceInstanceResponseDTO instance : instances) {
				if (instance.provider() != null
						&& producers.get(0).name().equals(instance.provider().name())
						&& instance.serviceDefinition() != null
						&& port.serviceDefinition().equals(instance.serviceDefinition().name())
						&& subset(port.metadata(), instance.metadata())
						&& offers(instance, iface)) {
					final Set<Integer> used = usedPriorities.computeIfAbsent(consumers.get(0).name() + "|" + port.serviceDefinition(), k -> new LinkedHashSet<>());
					int priority = requested;
					while (!used.add(priority)) {
						++priority;
					}
					rules.add(new Rule(consumers.get(0).name(), instance.instanceId(), priority));
				}
			}
		}
		return rules;
	}

    public static eu.arrowhead.dto.OrchestrationPlantRulesDTO snapshot(
            final List<PdeSystemDTO> systems, final List<ConnectionDTO> connections,
            final List<SystemResponseDTO> registered, final List<ServiceInstanceResponseDTO> instances, final int defaultPriority) {
        final Set<Rule> resolved = resolve(systems, connections, registered, instances, defaultPriority);
        final Map<String, List<SystemResponseDTO>> matched = matchSystems(systems, registered);
        final Map<String, eu.arrowhead.dto.OrchestrationPlantRuleDTO> scopes = new java.util.TreeMap<>();
        for (final ConnectionDTO connection : connections) {
            final Optional<PortDTO> port = port(systems, connection.consumer());
            if (port.isEmpty()) { continue; }
            final PdeSystemDTO described = systems.stream().filter(s -> s.systemId().equals(connection.consumer().systemId())).findFirst().orElseThrow();
            final Set<String> consumers = new java.util.TreeSet<>();
            matched.getOrDefault(described.systemId(), List.of()).forEach(c -> consumers.add(c.name()));
            if (!Utilities.isEmpty(described.systemName())) { consumers.add(described.systemName()); }
            for (final String consumer : consumers) {
                final String definition = port.get().serviceDefinition();
                final Set<String> ids = instances.stream().filter(i -> i.serviceDefinition() != null && definition.equals(i.serviceDefinition().name()))
                        .map(ServiceInstanceResponseDTO::instanceId).collect(java.util.stream.Collectors.toSet());
                final List<eu.arrowhead.dto.OrchestrationPlantProviderDTO> providers = resolved.stream()
                        .filter(r -> consumer.equals(r.consumer()) && ids.contains(r.serviceInstanceId()))
                        .collect(java.util.stream.Collectors.toMap(Rule::serviceInstanceId, Rule::priority, Math::min)).entrySet().stream()
                        .map(e -> new eu.arrowhead.dto.OrchestrationPlantProviderDTO(e.getKey(), e.getValue()))
                        .sorted(java.util.Comparator.comparingInt(eu.arrowhead.dto.OrchestrationPlantProviderDTO::priority)
                                .thenComparing(eu.arrowhead.dto.OrchestrationPlantProviderDTO::serviceInstanceId)).toList();
                scopes.put(consumer + "|" + definition, new eu.arrowhead.dto.OrchestrationPlantRuleDTO(consumer, definition, providers));
            }
        }
        return new eu.arrowhead.dto.OrchestrationPlantRulesDTO(List.copyOf(scopes.values()));
    }

	private static boolean offers(final ServiceInstanceResponseDTO instance, final String iface) {
		return Utilities.isEmpty(iface)
				|| (instance.interfaces() != null && instance.interfaces().stream().anyMatch(i -> iface.equalsIgnoreCase(i.templateName())));
	}

	private static Optional<PortDTO> port(final List<PdeSystemDTO> systems, final SystemPortDTO ref) {
		if (ref == null) {
			return Optional.empty();
		}
		return systems.stream()
				.filter(s -> Objects.equals(s.systemId(), ref.systemId()))
				.flatMap(s -> PlantDescriptionValidator.ports(s).stream())
				.filter(p -> Objects.equals(p.portName(), ref.portName()))
				.findFirst();
	}

	static boolean subset(final Map<String, String> required, final Map<String, Object> actual) {
		if (Utilities.isEmpty(required)) {
			return true;
		}
		if (actual == null) {
			return false;
		}
		return required.entrySet().stream().allMatch(e -> actual.containsKey(e.getKey()) && Objects.equals(e.getValue(), String.valueOf(actual.get(e.getKey()))));
	}

	private RuleResolver() {
		throw new UnsupportedOperationException();
	}
}
