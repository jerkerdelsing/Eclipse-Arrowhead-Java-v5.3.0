/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.service;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.eventhandler.EventHandlerConstants;
import eu.arrowhead.eventhandler.api.dto.EventPublishRequestDTO;
import eu.arrowhead.eventhandler.api.dto.SubscriptionRequestDTO;

@Service
public class EventHandlerValidation {

	@Value(EventHandlerConstants.$PUBLISH_TOLERANCE_SECONDS_WD)
	private long publishToleranceSeconds;

	@Autowired
	private EventTypeNameNormalizer eventTypeNormalizer;

	@Autowired(required = false)
	private SystemInfo sysInfo;

	@Autowired
	private EventTypeNameValidator eventTypeValidator;

	public EventPublishRequestDTO validateAndNormalizePublish(final EventPublishRequestDTO dto, final String origin) {
		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}
		if (dto.payload() == null) {
			throw new InvalidParameterException("Payload is missing", origin);
		}

		final String eventType = normalizeAndValidateEventType(dto.eventType(), origin);
		final Instant timestamp = dto.timestamp() == null ? Instant.now() : dto.timestamp();
		final Instant now = Instant.now();
		if (timestamp.isBefore(now.minus(publishToleranceSeconds, ChronoUnit.SECONDS))
				|| timestamp.isAfter(now.plus(publishToleranceSeconds, ChronoUnit.SECONDS))) {
			throw new InvalidParameterException("Timestamp is outside the configured tolerance", origin);
		}

		return new EventPublishRequestDTO(eventType, dto.payload(), dto.metadata() == null ? Map.of() : Map.copyOf(dto.metadata()), timestamp);
	}

	public SubscriptionRequestDTO validateAndNormalizeSubscription(final SubscriptionRequestDTO dto, final String origin) {
		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		final String eventType = normalizeAndValidateEventType(dto.eventType(), origin);
		final String notifyUri = validateNotifyUri(dto.notifyUri(), origin);
		final boolean matchMetadata = Boolean.TRUE.equals(dto.matchMetadata());
		final Map<String, String> metadataFilter = dto.metadataFilter() == null ? Map.of() : Map.copyOf(dto.metadataFilter());
		if (matchMetadata && metadataFilter.isEmpty()) {
			throw new InvalidParameterException("Metadata filter is required when matchMetadata is true", origin);
		}
		if (dto.startsAt() != null && dto.expiresAt() != null && !dto.expiresAt().isAfter(dto.startsAt())) {
			throw new InvalidParameterException("expiresAt must be later than startsAt", origin);
		}
		if (dto.expiresAt() != null && !dto.expiresAt().isAfter(Instant.now())) {
			throw new InvalidParameterException("expiresAt must be in the future", origin);
		}

		return new SubscriptionRequestDTO(eventType, notifyUri, matchMetadata, metadataFilter, dto.startsAt(), dto.expiresAt());
	}

	private String normalizeAndValidateEventType(final String eventType, final String origin) {
		if (Utilities.isEmpty(eventType)) {
			throw new InvalidParameterException("Event type is missing", origin);
		}

		final String normalized = eventTypeNormalizer.normalize(eventType);
		try {
			eventTypeValidator.validateEventTypeName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin, ex);
		}

		return normalized;
	}

	private String validateNotifyUri(final String notifyUri, final String origin) {
		if (Utilities.isEmpty(notifyUri)) {
			throw new InvalidParameterException("Notify URI is missing", origin);
		}

		try {
			final URI uri = URI.create(notifyUri.trim());
			if (!uri.isAbsolute() || uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
				throw new IllegalArgumentException();
			}
			// Deliveries carry EventHandler's identity token: with TLS enabled they are only sent over HTTPS (mutual TLS).
			if (sysInfo != null && sysInfo.isSslEnabled() && !"https".equalsIgnoreCase(uri.getScheme())) {
				throw new InvalidParameterException("Notify URI must use HTTPS when EventHandler runs with TLS", origin);
			}
			return uri.toString();
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException("Notify URI must be an absolute HTTP or HTTPS URI", origin, ex);
		}
	}
}
