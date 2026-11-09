/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.service;

import eu.arrowhead.common.SystemInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.eventhandler.api.dto.EventPublishRequestDTO;
import eu.arrowhead.eventhandler.api.dto.SubscriptionRequestDTO;

@ExtendWith(MockitoExtension.class)
class EventHandlerValidationTest {

	private static final String ORIGIN = "TEST";
	private static final long PUBLISH_TOLERANCE_SECONDS = 60L;

	@InjectMocks
	private EventHandlerValidation validator;

	@Mock
	private EventTypeNameNormalizer eventTypeNormalizer;

	@Mock
	private EventTypeNameValidator eventTypeValidator;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(validator, "publishToleranceSeconds", PUBLISH_TOLERANCE_SECONDS);
		lenient().when(eventTypeNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		lenient().doNothing().when(eventTypeValidator).validateEventTypeName(anyString());
	}

	@Test
	void publishUsesCurrentTimeAndEmptyMetadataDefaults() {
		final EventPublishRequestDTO result = validator.validateAndNormalizePublish(
				new EventPublishRequestDTO("temperatureChanged", "21.5", null, null), ORIGIN);

		assertEquals("temperatureChanged", result.eventType());
		assertTrue(result.metadata().isEmpty());
		assertTrue(result.timestamp().isAfter(Instant.now().minusSeconds(2)));
	}

	@Test
	void publishRejectsTimestampOutsideTolerance() {
		final EventPublishRequestDTO dto = new EventPublishRequestDTO(
				"temperatureChanged",
				"21.5",
				Map.of(),
				Instant.now().minusSeconds(PUBLISH_TOLERANCE_SECONDS + 1));
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePublish(dto, ORIGIN));
	}

	@Test
	void publishRejectsMissingPayload() {
		final EventPublishRequestDTO dto = new EventPublishRequestDTO("temperatureChanged", null, Map.of(), Instant.now());
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePublish(dto, ORIGIN));
	}

	@Test
	void subscriptionRejectsRelativeNotifyUri() {
		final SubscriptionRequestDTO dto = new SubscriptionRequestDTO("temperatureChanged", "/events", false, Map.of(), null, null);
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeSubscription(dto, ORIGIN));
	}

	@Test
	void subscriptionRequiresFilterWhenMatchingMetadata() {
		final SubscriptionRequestDTO dto = new SubscriptionRequestDTO("temperatureChanged", "http://consumer/events", true, Map.of(), null, null);
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeSubscription(dto, ORIGIN));
	}

	@Test
	void subscriptionRejectsInvalidValidityWindow() {
		final Instant startsAt = Instant.now().plusSeconds(120);
		final SubscriptionRequestDTO dto = new SubscriptionRequestDTO("temperatureChanged", "https://consumer/events", false, Map.of(), startsAt, startsAt);
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeSubscription(dto, ORIGIN));
	}

	@Test
	void subscriptionRequiresHttpsCallbackWhenTlsIsEnabled() {
		final SystemInfo sysInfo = org.mockito.Mockito.mock(SystemInfo.class);
		org.mockito.Mockito.when(sysInfo.isSslEnabled()).thenReturn(true);
		ReflectionTestUtils.setField(validator, "sysInfo", sysInfo);
		final SubscriptionRequestDTO plain = new SubscriptionRequestDTO("temperatureChanged", "http://consumer/events", false, Map.of(), null, null);
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeSubscription(plain, ORIGIN));
		final SubscriptionRequestDTO secure = new SubscriptionRequestDTO("temperatureChanged", "https://consumer/events", false, Map.of(), null, null);
		validator.validateAndNormalizeSubscription(secure, ORIGIN);
	}
}
