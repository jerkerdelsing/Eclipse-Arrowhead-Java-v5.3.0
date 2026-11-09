/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.eventhandler.api.dto.EventPublishRequestDTO;
import eu.arrowhead.eventhandler.api.dto.EventPublishResultDTO;
import eu.arrowhead.eventhandler.jpa.entity.Subscription;
import eu.arrowhead.eventhandler.jpa.service.SubscriptionDbService;

@ExtendWith(MockitoExtension.class)
class EventHandlerServiceTest {

	@InjectMocks
	private EventHandlerService service;

	@Mock
	private EventHandlerValidation validation;

	@Mock
	private SubscriptionDTOConverter converter;

	@Mock
	private SubscriptionDbService dbService;

	@Mock
	private EventDeliveryService deliveryService;

	@Test
	void publishDeliversToActiveMatchingSubscription() {
		final Instant now = Instant.now();
		final EventPublishRequestDTO dto = new EventPublishRequestDTO("temperatureChanged", "21.5", Map.of("room", "A"), now);
		final Subscription subscription = subscription(now.minusSeconds(1), now.plusSeconds(60), true);
		when(validation.validateAndNormalizePublish(dto, "TEST")).thenReturn(dto);
		when(dbService.findMatchingEventType("temperatureChanged")).thenReturn(List.of(subscription));
		when(converter.readMetadata(subscription.getMetadataFilter())).thenReturn(Map.of("room", "A"));

		final EventPublishResultDTO result = service.publish("TemperatureSensor", dto, "TEST");

		assertEquals(1, result.matchedSubscriptions());
		verify(deliveryService).deliver(eq(subscription), any());
	}

	@Test
	void publishSkipsExpiredSubscription() {
		final Instant now = Instant.now();
		final EventPublishRequestDTO dto = new EventPublishRequestDTO("temperatureChanged", "21.5", Map.of(), now);
		final Subscription subscription = subscription(now.minusSeconds(60), now.minusSeconds(1), false);
		when(validation.validateAndNormalizePublish(dto, "TEST")).thenReturn(dto);
		when(dbService.findMatchingEventType("temperatureChanged")).thenReturn(List.of(subscription));

		final EventPublishResultDTO result = service.publish("TemperatureSensor", dto, "TEST");

		assertEquals(0, result.matchedSubscriptions());
		verify(deliveryService, never()).deliver(any(), any());
	}

	@Test
	void publishSkipsMetadataMismatch() {
		final Instant now = Instant.now();
		final EventPublishRequestDTO dto = new EventPublishRequestDTO("temperatureChanged", "21.5", Map.of("room", "B"), now);
		final Subscription subscription = subscription(null, null, true);
		when(validation.validateAndNormalizePublish(dto, "TEST")).thenReturn(dto);
		when(dbService.findMatchingEventType("temperatureChanged")).thenReturn(List.of(subscription));
		when(converter.readMetadata(subscription.getMetadataFilter())).thenReturn(Map.of("room", "A"));

		final EventPublishResultDTO result = service.publish("TemperatureSensor", dto, "TEST");

		assertEquals(0, result.acceptedDeliveries());
		verify(deliveryService, never()).deliver(any(), any());
	}

	private Subscription subscription(final Instant startsAt, final Instant expiresAt, final boolean matchMetadata) {
		return new Subscription(
				"EventConsumer",
				"temperatureChanged",
				"http://consumer/events",
				matchMetadata,
				matchMetadata ? "{\"room\":\"A\"}" : null,
				startsAt == null ? null : ZonedDateTime.ofInstant(startsAt, ZoneOffset.UTC),
				expiresAt == null ? null : ZonedDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
	}
}
