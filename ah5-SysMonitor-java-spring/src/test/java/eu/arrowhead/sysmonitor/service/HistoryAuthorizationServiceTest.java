/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;

// ConsumerAuthorization decides sysMonitorHistory operations; grants are cached for 30 s, refusals and failures are not.
public class HistoryAuthorizationServiceTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void grantIsCachedAndVerifyPayloadMatchesTheNativeImplementation() {
		final ArrowheadHttpService http = mock(ArrowheadHttpService.class);
		final AtomicLong clock = new AtomicLong(1000);
		when(http.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class))).thenReturn(true);
		final HistoryAuthorizationService service = new HistoryAuthorizationService(http, clock::get);

		assertTrue(service.isAuthorized("LocalCloudManager", "query"));
		assertTrue(service.isAuthorized("LocalCloudManager", "query"));
		final ArgumentCaptor<AuthorizationVerifyRequestDTO> payload = ArgumentCaptor.forClass(AuthorizationVerifyRequestDTO.class);
		verify(http, times(1)).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), payload.capture());
		assertEquals(new AuthorizationVerifyRequestDTO("SysMonitor", "LocalCloudManager", "LOCAL", "SERVICE_DEF", "sysMonitorHistory", "query"), payload.getValue());

		clock.addAndGet(30001);
		assertTrue(service.isAuthorized("LocalCloudManager", "query"));
		verify(http, times(2)).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void refusalAndFailureAreNotCached() {
		final ArrowheadHttpService http = mock(ArrowheadHttpService.class);
		when(http.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenReturn(false)
				.thenThrow(new IllegalStateException("unreachable"))
				.thenReturn(true);
		final HistoryAuthorizationService service = new HistoryAuthorizationService(http, () -> 0L);
		assertFalse(service.isAuthorized("AnySystem", "latest"));
		assertFalse(service.isAuthorized("AnySystem", "latest"));
		assertTrue(service.isAuthorized("AnySystem", "latest"));
	}
}
