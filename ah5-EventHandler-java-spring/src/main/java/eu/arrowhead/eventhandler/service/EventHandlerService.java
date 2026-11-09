/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.eventhandler.EventHandlerConstants;
import eu.arrowhead.eventhandler.api.dto.EventNotificationDTO;
import eu.arrowhead.eventhandler.api.dto.EventPublishRequestDTO;
import eu.arrowhead.eventhandler.api.dto.EventPublishResultDTO;
import eu.arrowhead.eventhandler.api.dto.SubscriptionListResponseDTO;
import eu.arrowhead.eventhandler.api.dto.SubscriptionRequestDTO;
import eu.arrowhead.eventhandler.api.dto.SubscriptionResponseDTO;
import eu.arrowhead.eventhandler.jpa.entity.Subscription;
import eu.arrowhead.eventhandler.jpa.service.SubscriptionDbService;

@Service
public class EventHandlerService {

	@Autowired
	private EventHandlerValidation validation;

	@Autowired
	private SubscriptionDTOConverter converter;

	@Autowired
	private SubscriptionDbService dbService;

	@Autowired
	private EventDeliveryService deliveryService;

	@Transactional
	public SubscriptionResponseDTO subscribe(final String subscriberSystemName, final SubscriptionRequestDTO dto, final String origin) {
		final SubscriptionRequestDTO normalized = validation.validateAndNormalizeSubscription(dto, origin);
		return converter.toResponse(dbService.save(converter.toEntity(subscriberSystemName, normalized)));
	}

	@Transactional
	public boolean unsubscribe(final long subscriptionId, final String subscriberSystemName, final String origin) {
		final Subscription subscription = dbService.findOwnedById(subscriptionId, subscriberSystemName, origin);
		dbService.delete(subscription);
		return true;
	}

	public EventPublishResultDTO publish(final String sourceSystemName, final EventPublishRequestDTO dto, final String origin) {
		final EventPublishRequestDTO normalized = validation.validateAndNormalizePublish(dto, origin);
		final List<Subscription> matching = dbService.findMatchingEventType(normalized.eventType())
				.stream()
				.filter(subscription -> isActive(subscription, normalized.timestamp()))
				.filter(subscription -> metadataMatches(subscription, normalized.metadata()))
				.toList();

		final EventNotificationDTO notification = new EventNotificationDTO(
				normalized.eventType(),
				normalized.payload(),
				normalized.metadata(),
				normalized.timestamp(),
				sourceSystemName);
		matching.forEach(subscription -> deliveryService.deliver(subscription, notification));

		return new EventPublishResultDTO(matching.size(), matching.size());
	}

	public SubscriptionListResponseDTO querySubscriptions(final Pageable pageable) {
		final Page<Subscription> page = dbService.findAll(pageable);
		return new SubscriptionListResponseDTO(page.stream().map(converter::toResponse).toList(), page.getTotalElements());
	}

	@Transactional
	public SubscriptionResponseDTO updateSubscription(final long id, final SubscriptionRequestDTO dto, final String origin) {
		final SubscriptionRequestDTO normalized = validation.validateAndNormalizeSubscription(dto, origin);
		final Subscription entity = dbService.findById(id, origin);
		converter.updateEntity(entity, normalized);
		return converter.toResponse(dbService.save(entity));
	}

	@Transactional
	public boolean removeSubscription(final long id, final String origin) {
		final Subscription entity = dbService.findById(id, origin);
		dbService.delete(entity);
		return true;
	}

	@Scheduled(fixedDelayString = EventHandlerConstants.$EXPIRATION_CLEANUP_INTERVAL_WD)
	public void removeExpiredSubscriptions() {
		dbService.deleteExpired(ZonedDateTime.now(ZoneOffset.UTC));
	}

	private boolean isActive(final Subscription subscription, final Instant timestamp) {
		return (subscription.getStartsAt() == null || !timestamp.isBefore(subscription.getStartsAt().toInstant()))
				&& (subscription.getExpiresAt() == null || timestamp.isBefore(subscription.getExpiresAt().toInstant()));
	}

	private boolean metadataMatches(final Subscription subscription, final Map<String, String> eventMetadata) {
		if (!subscription.isMatchMetadata()) {
			return true;
		}

		final Map<String, String> filter = converter.readMetadata(subscription.getMetadataFilter());
		return eventMetadata != null && eventMetadata.entrySet().containsAll(filter.entrySet());
	}
}
