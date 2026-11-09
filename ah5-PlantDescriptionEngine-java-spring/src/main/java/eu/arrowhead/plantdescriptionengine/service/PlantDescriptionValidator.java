/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.plantdescriptionengine.api.dto.ConnectionDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PortDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.SystemPortDTO;

// The v4.6 plant description rules, with Arrowhead 5 naming: service definitions are camelCase.
public final class PlantDescriptionValidator {

	public static final String BLACKLISTED_SYSTEM_ID = "unknown";

	private static final Pattern SERVICE_DEFINITION = Pattern.compile("^[a-z][A-Za-z0-9]*$");
	private static final Pattern SYSTEM_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9]*$");

	// Returns the violations; empty when the description is valid. lookup resolves the body of another description.
	public static List<String> validate(
			final String name,
			final Long ownId,
			final PlantDescriptionBody body,
			final Function<Long, Optional<PlantDescriptionBody>> lookup) {
		final List<String> errors = new ArrayList<>();
		if (Utilities.isEmpty(name)) {
			errors.add("plantDescription name is missing");
		}

		final List<PdeSystemDTO> allSystems = new ArrayList<>(body.systems());
		validateIncludes(ownId, body, lookup, errors, allSystems);
		validateSystems(body.systems(), errors);
		validateConnections(body.connections(), allSystems, errors);
		return errors;
	}

	private static void validateIncludes(
			final Long ownId,
			final PlantDescriptionBody body,
			final Function<Long, Optional<PlantDescriptionBody>> lookup,
			final List<String> errors,
			final List<PdeSystemDTO> allSystems) {
		final Set<Long> seen = new HashSet<>();
		for (final Long id : body.include()) {
			if (id == null) {
				errors.add("include contains a null id");
				continue;
			}
			if (id.equals(ownId)) {
				errors.add("Plant description includes itself");
			}
			if (!seen.add(id)) {
				errors.add("Plant description " + id + " is included more than once");
			}
		}

		// walk the include graph; a path back to a description already on the path is a cycle
		for (final Long id : seen) {
			if (id.equals(ownId)) {
				continue;
			}
			collect(id, ownId, lookup, new HashSet<>(), new HashSet<>(), errors, allSystems);
		}
	}

	private static void collect(
			final Long id,
			final Long ownId,
			final Function<Long, Optional<PlantDescriptionBody>> lookup,
			final Set<Long> path,
			final Set<Long> visited,
			final List<String> errors,
			final List<PdeSystemDTO> allSystems) {
		if (path.contains(id) || (ownId != null && id.equals(ownId))) {
			errors.add("Include cycle through plant description " + id);
			return;
		}
		if (!visited.add(id)) {
			return;
		}

		final Optional<PlantDescriptionBody> included = lookup.apply(id);
		if (included.isEmpty()) {
			errors.add("Included plant description " + id + " does not exist");
			return;
		}

		allSystems.addAll(included.get().systems());
		path.add(id);
		for (final Long next : included.get().include()) {
			collect(next, ownId, lookup, path, visited, errors, allSystems);
		}
		path.remove(id);
	}

	private static void validateSystems(final List<PdeSystemDTO> systems, final List<String> errors) {
		final Set<String> ids = new HashSet<>();
		final Set<String> identities = new HashSet<>();
		for (final PdeSystemDTO system : systems) {
			if (Utilities.isEmpty(system.systemId())) {
				errors.add("A system has no systemId");
				continue;
			}
			final String id = system.systemId();
			if (BLACKLISTED_SYSTEM_ID.equalsIgnoreCase(id)) {
				errors.add("systemId '" + id + "' is reserved");
			}
			if (!ids.add(id)) {
				errors.add("Duplicate systemId '" + id + "'");
			}
			if (Utilities.isEmpty(system.systemName()) && Utilities.isEmpty(system.metadata())) {
				errors.add("System '" + id + "' needs a systemName or metadata");
			}
			if (!Utilities.isEmpty(system.systemName()) && !SYSTEM_NAME.matcher(system.systemName()).matches()) {
				errors.add("System '" + id + "' has an invalid systemName '" + system.systemName() + "'");
			}
			if (!identities.add(system.systemName() + "|" + normalized(system.metadata()))) {
				errors.add("System '" + id + "' cannot be told apart from another system by name and metadata");
			}
			validatePorts(system, errors);
		}
	}

	private static void validatePorts(final PdeSystemDTO system, final List<String> errors) {
		final Set<String> portNames = new HashSet<>();
		final Map<String, List<PortDTO>> byDefinition = new HashMap<>();
		for (final PortDTO port : ports(system)) {
			final String where = "Port '" + port.portName() + "' of system '" + system.systemId() + "'";
			if (Utilities.isEmpty(port.portName())) {
				errors.add("A port of system '" + system.systemId() + "' has no portName");
				continue;
			}
			if (!portNames.add(port.portName())) {
				errors.add("Duplicate portName '" + port.portName() + "' in system '" + system.systemId() + "'");
			}
			if (Utilities.isEmpty(port.serviceDefinition()) || !SERVICE_DEFINITION.matcher(port.serviceDefinition()).matches()) {
				errors.add(where + " has an invalid serviceDefinition '" + port.serviceDefinition() + "' (camelCase expected)");
				continue;
			}
			if (isConsumer(port) && !Utilities.isEmpty(port.metadata())) {
				errors.add(where + " is a consumer port and must not have metadata");
			}
			if (!isConsumer(port)) {
				byDefinition.computeIfAbsent(port.serviceDefinition(), k -> new ArrayList<>()).add(port);
			}
		}

		for (final Map.Entry<String, List<PortDTO>> entry : byDefinition.entrySet()) {
			if (entry.getValue().size() < 2) {
				continue;
			}
			final Set<String> metadata = new HashSet<>();
			for (final PortDTO port : entry.getValue()) {
				if (Utilities.isEmpty(port.metadata())) {
					errors.add("System '" + system.systemId() + "' produces " + entry.getKey() + " on several ports; port '" + port.portName() + "' needs metadata");
				} else if (!metadata.add(normalized(port.metadata()))) {
					errors.add("System '" + system.systemId() + "' produces " + entry.getKey() + " on several ports with the same metadata");
				}
			}
		}
	}

	private static void validateConnections(final List<ConnectionDTO> connections, final List<PdeSystemDTO> allSystems, final List<String> errors) {
		for (int i = 0; i < connections.size(); ++i) {
			final ConnectionDTO connection = connections.get(i);
			final String where = "Connection " + i;
			if (connection.consumer() == null || connection.producer() == null) {
				errors.add(where + " needs a consumer and a producer");
				continue;
			}
			if (connection.priority() != null && connection.priority() < 0) {
				errors.add(where + " has a negative priority");
			}

			final Optional<PortDTO> consumer = findPort(allSystems, connection.consumer(), where + " consumer", errors);
			final Optional<PortDTO> producer = findPort(allSystems, connection.producer(), where + " producer", errors);
			if (consumer.isPresent() && !isConsumer(consumer.get())) {
				errors.add(where + ": consumer port '" + connection.consumer().portName() + "' is not a consumer port");
			}
			if (producer.isPresent() && isConsumer(producer.get())) {
				errors.add(where + ": producer port '" + connection.producer().portName() + "' is a consumer port");
			}
			if (consumer.isPresent() && producer.isPresent()) {
				if (!Objects.equals(consumer.get().serviceDefinition(), producer.get().serviceDefinition())) {
					errors.add(where + ": the ports have different service definitions");
				}
				final String ci = consumer.get().serviceInterface();
				final String pi = producer.get().serviceInterface();
				if (!Utilities.isEmpty(ci) && !Utilities.isEmpty(pi) && !ci.equals(pi)) {
					errors.add(where + ": the ports have different service interfaces");
				}
			}
		}
	}

	private static Optional<PortDTO> findPort(final List<PdeSystemDTO> systems, final SystemPortDTO ref, final String where, final List<String> errors) {
		final Optional<PdeSystemDTO> system = systems.stream().filter(s -> Objects.equals(s.systemId(), ref.systemId())).findFirst();
		if (system.isEmpty()) {
			errors.add(where + ": unknown system '" + ref.systemId() + "'");
			return Optional.empty();
		}
		final Optional<PortDTO> port = ports(system.get()).stream().filter(p -> Objects.equals(p.portName(), ref.portName())).findFirst();
		if (port.isEmpty()) {
			errors.add(where + ": system '" + ref.systemId() + "' has no port '" + ref.portName() + "'");
		}
		return port;
	}

	public static boolean isConsumer(final PortDTO port) {
		return Boolean.TRUE.equals(port.consumer());
	}

	public static List<PortDTO> ports(final PdeSystemDTO system) {
		return system.ports() == null ? List.of() : system.ports();
	}

	private static String normalized(final Map<String, String> metadata) {
		return metadata == null ? "{}" : new java.util.TreeMap<>(metadata).toString();
	}

	private PlantDescriptionValidator() {
		throw new UnsupportedOperationException();
	}
}
