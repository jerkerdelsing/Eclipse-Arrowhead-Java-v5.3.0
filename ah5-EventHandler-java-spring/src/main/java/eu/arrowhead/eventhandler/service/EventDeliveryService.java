/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.eventhandler.EventHandlerConstants;
import eu.arrowhead.eventhandler.api.dto.EventNotificationDTO;
import eu.arrowhead.eventhandler.jpa.entity.Subscription;

@Service
public class EventDeliveryService {

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Value(EventHandlerConstants.$DELIVERY_ATTEMPTS_WD)
	private int deliveryAttempts;

	@Autowired
	private HttpService httpService;

	@Autowired
	private SystemInfo systemInfo;

	@Async
	public void deliver(final Subscription subscription, final EventNotificationDTO notification) {
		final Map<String, String> headers = new HashMap<>();
		final String authorization = HttpUtilities.calculateAuthorizationHeader(systemInfo);
		if (authorization != null) {
			headers.put(HttpHeaders.AUTHORIZATION, authorization);
		}

		final int attempts = Math.max(1, deliveryAttempts);
		for (int attempt = 1; attempt <= attempts; attempt++) {
			try {
				httpService.sendRequest(
						UriComponentsBuilder.fromUriString(subscription.getNotifyUri()).build(),
						HttpMethod.POST,
						Void.class,
						notification,
						null,
						headers);
				return;
			} catch (final RuntimeException ex) {
				logger.warn("Event delivery attempt {}/{} failed for subscription {}: {}", attempt, attempts, subscription.getId(), ex.getMessage());
				logger.debug(ex);
			}
		}
	}
}
