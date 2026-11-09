/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;

class EventHandlerAuthorizationTest {

	@Test
	void filterTargetsPublishSubscribeAndUnsubscribeOnly() {
		assertArrayEquals(new String[] { "eventPublish", "publish" }, EventHandlerAuthorizationFilter.target(new MockHttpServletRequest("POST", "/eventhandler/event-publish/publish")));
		assertArrayEquals(new String[] { "eventSubscription", "subscribe" }, EventHandlerAuthorizationFilter.target(new MockHttpServletRequest("POST", "/eventhandler/event-subscription/subscribe")));
		assertArrayEquals(new String[] { "eventSubscription", "unsubscribe" }, EventHandlerAuthorizationFilter.target(new MockHttpServletRequest("DELETE", "/eventhandler/event-subscription/unsubscribe/7")));
		assertNull(EventHandlerAuthorizationFilter.target(new MockHttpServletRequest("GET", "/eventhandler/management/subscriptions")));
		assertNull(EventHandlerAuthorizationFilter.target(new MockHttpServletRequest("GET", "/eventhandler/event-publish/publish")));
	}

	@Test
	void decisionsAreCachedBriefly() {
		final ArrowheadHttpService http = mock(ArrowheadHttpService.class);
		when(http.consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class))).thenReturn(true);
		final AtomicLong now = new AtomicLong(1000);
		final EventHandlerAuthorizationService service = new EventHandlerAuthorizationService(http, now::get);
		assertTrue(service.isAuthorized("RandomDataProducer", "eventPublish", "publish"));
		assertTrue(service.isAuthorized("RandomDataProducer", "eventPublish", "publish"));
		verify(http, times(1)).consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
		now.addAndGet(31000);
		assertTrue(service.isAuthorized("RandomDataProducer", "eventPublish", "publish"));
		verify(http, times(2)).consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
	}

	@Test
	void failsClosedWhenConsumerAuthorizationIsUnavailable() {
		final ArrowheadHttpService http = mock(ArrowheadHttpService.class);
		when(http.consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class))).thenThrow(new RuntimeException("down"));
		assertFalse(new EventHandlerAuthorizationService(http).isAuthorized("LocalCloudManager", "eventSubscription", "subscribe"));
	}
}
