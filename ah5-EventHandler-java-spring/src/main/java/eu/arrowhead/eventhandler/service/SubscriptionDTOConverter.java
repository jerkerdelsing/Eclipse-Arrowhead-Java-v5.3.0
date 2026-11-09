/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.eventhandler.api.dto.SubscriptionRequestDTO;
import eu.arrowhead.eventhandler.api.dto.SubscriptionResponseDTO;
import eu.arrowhead.eventhandler.jpa.entity.Subscription;

@Component
public class SubscriptionDTOConverter {

	private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<Map<String, String>>() { };

	@Autowired
	private ObjectMapper mapper;

	public Subscription toEntity(final String subscriberSystemName, final SubscriptionRequestDTO dto) {
		return new Subscription(
				subscriberSystemName,
				dto.eventType(),
				dto.notifyUri(),
				Boolean.TRUE.equals(dto.matchMetadata()),
				writeMetadata(dto.metadataFilter()),
				toZonedDateTime(dto.startsAt()),
				toZonedDateTime(dto.expiresAt()));
	}

	public void updateEntity(final Subscription entity, final SubscriptionRequestDTO dto) {
		entity.setEventType(dto.eventType());
		entity.setNotifyUri(dto.notifyUri());
		entity.setMatchMetadata(Boolean.TRUE.equals(dto.matchMetadata()));
		entity.setMetadataFilter(writeMetadata(dto.metadataFilter()));
		entity.setStartsAt(toZonedDateTime(dto.startsAt()));
		entity.setExpiresAt(toZonedDateTime(dto.expiresAt()));
	}

	public SubscriptionResponseDTO toResponse(final Subscription entity) {
		return new SubscriptionResponseDTO(
				entity.getId(),
				entity.getSubscriberSystemName(),
				entity.getEventType(),
				entity.getNotifyUri(),
				entity.isMatchMetadata(),
				readMetadata(entity.getMetadataFilter()),
				entity.getStartsAt() == null ? null : entity.getStartsAt().toInstant(),
				entity.getExpiresAt() == null ? null : entity.getExpiresAt().toInstant(),
				entity.getCreatedAt().toInstant(),
				entity.getUpdatedAt().toInstant());
	}

	public Map<String, String> readMetadata(final String json) {
		if (json == null || json.isBlank()) {
			return Map.of();
		}
		try {
			return mapper.readValue(json, MAP_TYPE);
		} catch (final JsonProcessingException ex) {
			throw new InternalServerError("Could not read subscription metadata", ex);
		}
	}

	private String writeMetadata(final Map<String, String> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return null;
		}
		try {
			return mapper.writeValueAsString(metadata);
		} catch (final JsonProcessingException ex) {
			throw new InternalServerError("Could not store subscription metadata", ex);
		}
	}

	private ZonedDateTime toZonedDateTime(final java.time.Instant instant) {
		return instant == null ? null : ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
