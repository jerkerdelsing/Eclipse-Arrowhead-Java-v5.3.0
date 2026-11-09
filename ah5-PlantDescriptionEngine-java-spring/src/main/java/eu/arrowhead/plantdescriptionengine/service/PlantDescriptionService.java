/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.plantdescriptionengine.PlantDescriptionEngineConstants;
import eu.arrowhead.plantdescriptionengine.api.dto.ConnectionDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionListResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionRequestDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionUpdateDTO;
import eu.arrowhead.plantdescriptionengine.jpa.entity.PlantDescription;
import eu.arrowhead.plantdescriptionengine.jpa.repository.PlantDescriptionRepository;

@Service
public class PlantDescriptionService {

	public static final List<String> SORT_FIELDS = List.of("id", "createdAt", "updatedAt");

	// The systems and connections in force: the active description and everything it includes.
	public record ActivePlant(PlantDescription description, List<PdeSystemDTO> systems, List<ConnectionDTO> connections) {
	}

	@Autowired
	private PlantDescriptionRepository repository;

	@Autowired
	private PageService pageService;

	@Autowired
	private ApplicationEventPublisher publisher;

	@Value(PlantDescriptionEngineConstants.$PLANT_DESCRIPTION_MAX_SIZE_WD)
	private int maxSize;

	@Transactional(readOnly = true)
	public PlantDescriptionListResponseDTO query(final PageDTO page, final Boolean active, final String origin) {
		final PageRequest request = pageService.getPageRequest(page, SORT_FIELDS, "id", origin);
		final Page<PlantDescription> result = active == null ? repository.findAll(request) : repository.findAllByActive(active, request);
		return new PlantDescriptionListResponseDTO(result.getContent().stream().map(this::toResponse).toList(), result.getTotalElements());
	}

	@Transactional(readOnly = true)
	public PlantDescriptionResponseDTO get(final long id, final String origin) {
		return toResponse(find(id, origin));
	}

	@Transactional
	public PlantDescriptionResponseDTO create(final PlantDescriptionRequestDTO dto, final String origin) {
		if (dto == null) {
			throw new InvalidParameterException("Request body is missing", origin);
		}
		final PlantDescriptionBody body = new PlantDescriptionBody(dto.include(), dto.systems(), dto.connections());
		final String json = check(dto.plantDescription(), null, body, origin);
		final boolean active = Boolean.TRUE.equals(dto.active());
		if (active) {
			deactivateAll();
		}
		final PlantDescription saved = repository.saveAndFlush(new PlantDescription(dto.plantDescription().trim(), active, json));
		publisher.publishEvent(new PlantDescriptionChangedEvent(saved.getId()));
		return toResponse(saved);
	}

	@Transactional
	public PlantDescriptionResponseDTO replace(final long id, final PlantDescriptionRequestDTO dto, final String origin) {
		if (dto == null) {
			throw new InvalidParameterException("Request body is missing", origin);
		}
		final PlantDescription entity = find(id, origin);
		return store(entity, dto.plantDescription(), Boolean.TRUE.equals(dto.active()), new PlantDescriptionBody(dto.include(), dto.systems(), dto.connections()), origin);
	}

	@Transactional
	public PlantDescriptionResponseDTO update(final long id, final PlantDescriptionUpdateDTO dto, final String origin) {
		if (dto == null) {
			throw new InvalidParameterException("Request body is missing", origin);
		}
		final PlantDescription entity = find(id, origin);
		final PlantDescriptionBody old = body(entity);
		final PlantDescriptionBody body = new PlantDescriptionBody(
				dto.include() != null ? dto.include() : old.include(),
				dto.systems() != null ? dto.systems() : old.systems(),
				dto.connections() != null ? dto.connections() : old.connections());
		return store(entity,
				dto.plantDescription() != null ? dto.plantDescription() : entity.getName(),
				dto.active() != null ? dto.active() : entity.isActive(),
				body,
				origin);
	}

	@Transactional
	public void remove(final long id, final String origin) {
		final PlantDescription entity = find(id, origin);
		for (final PlantDescription other : repository.findAll()) {
			if (other.getId() != id && body(other).include().contains(id)) {
				throw new InvalidParameterException("Plant description " + id + " is included by " + other.getId(), origin);
			}
		}
		repository.delete(entity);
		repository.flush();
		publisher.publishEvent(new PlantDescriptionChangedEvent(id));
	}

	@Transactional(readOnly = true)
	public Optional<ActivePlant> activePlant() {
		final Optional<PlantDescription> active = repository.findFirstByActiveTrue();
		if (active.isEmpty()) {
			return Optional.empty();
		}

		final Map<String, PdeSystemDTO> systems = new LinkedHashMap<>();
		final List<ConnectionDTO> connections = new ArrayList<>();
		final Set<Long> visited = new HashSet<>();
		gather(active.get(), systems, connections, visited);
		return Optional.of(new ActivePlant(active.get(), List.copyOf(systems.values()), connections));
	}

	@Transactional(readOnly = true)
	public List<PlantDescription> all() {
		return repository.findAll();
	}

	public PlantDescriptionBody body(final PlantDescription entity) {
		return Utilities.fromJson(entity.getBody(), PlantDescriptionBody.class);
	}

	public PlantDescriptionResponseDTO toResponse(final PlantDescription entity) {
		final PlantDescriptionBody body = body(entity);
		return new PlantDescriptionResponseDTO(
				entity.getId(),
				entity.getName(),
				entity.isActive(),
				body.include(),
				body.systems(),
				body.connections(),
				Utilities.convertZonedDateTimeToUTCString(entity.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(entity.getUpdatedAt()));
	}

	private void gather(final PlantDescription entity, final Map<String, PdeSystemDTO> systems, final List<ConnectionDTO> connections, final Set<Long> visited) {
		if (!visited.add(entity.getId())) {
			return;
		}
		final PlantDescriptionBody body = body(entity);
		body.systems().forEach(s -> systems.putIfAbsent(s.systemId(), s));
		connections.addAll(body.connections());
		for (final Long included : body.include()) {
			repository.findById(included).ifPresent(e -> gather(e, systems, connections, visited));
		}
	}

	private PlantDescriptionResponseDTO store(final PlantDescription entity, final String name, final boolean active, final PlantDescriptionBody body, final String origin) {
		final String json = check(name, entity.getId(), body, origin);
		if (active && !entity.isActive()) {
			deactivateAll();
		}
		entity.setName(name.trim());
		entity.setActive(active);
		entity.setBody(json);
		final PlantDescription saved = repository.saveAndFlush(entity);
		publisher.publishEvent(new PlantDescriptionChangedEvent(saved.getId()));
		return toResponse(saved);
	}

	private String check(final String name, final Long ownId, final PlantDescriptionBody body, final String origin) {
		final List<String> errors = PlantDescriptionValidator.validate(name, ownId, body, id -> repository.findById(id).map(this::body));
		if (!errors.isEmpty()) {
			throw new InvalidParameterException(String.join("; ", errors), origin);
		}
		final String json = Utilities.toJson(body);
		if (json.length() > maxSize) {
			throw new InvalidParameterException("Plant description exceeds " + maxSize + " characters", origin);
		}
		return json;
	}

	private void deactivateAll() {
		for (final PlantDescription active : repository.findAllByActiveTrue()) {
			active.setActive(false);
			repository.save(active);
		}
	}

	private PlantDescription find(final long id, final String origin) {
		return repository.findById(id).orElseThrow(() -> new DataNotFoundException("Plant description " + id + " not found", origin));
	}
}
