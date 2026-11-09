/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import jakarta.servlet.http.HttpServletRequest;

public class TimeManagerAuthorizationTest {

	private static HttpServletRequest request(final String method, final String uri) {
		final HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn(method);
		when(request.getRequestURI()).thenReturn(uri);
		return request;
	}

	@Test
	public void protectsOnlyGetTime() {
		assertTrue(TimeManagerAuthorizationFilter.isGetTime(request("GET", "/timemanager/time")));
		assertFalse(TimeManagerAuthorizationFilter.isGetTime(request("POST", "/timemanager/time")));
		assertFalse(TimeManagerAuthorizationFilter.isGetTime(request("GET", "/timemanager/general/mgmt/get-config")));
	}

	@Test
	public void extractsSystemNameFromCertificateSubject() {
		assertEquals("LocalCloudManager",
				TimeManagerAuthorizationFilter.systemNameFromSubject("CN=LocalCloudManager.TestCloud.Company.arrowhead.eu,OU=Arrowhead Systems,O=Company,C=SE"));
	}

	@Test
	public void asksConsumerAuthorizationForTimeGetTimeAndCachesTheDecision() {
		final ArrowheadHttpService httpService = mock(ArrowheadHttpService.class);
		final AtomicLong now = new AtomicLong(1000);
		final TimeManagerAuthorizationService service = new TimeManagerAuthorizationService(httpService, now::get);
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenReturn(true);

		assertTrue(service.isAuthorized("SysMonitor", "time", "get-time"));
		assertTrue(service.isAuthorized("SysMonitor", "time", "get-time"));
		now.addAndGet(30001);
		assertTrue(service.isAuthorized("SysMonitor", "time", "get-time"));

		verify(httpService, times(2)).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class),
				argThat((AuthorizationVerifyRequestDTO dto) -> "TimeManager".equals(dto.provider()) && "time".equals(dto.target()) && "get-time".equals(dto.scope())));
	}

	@Test
	public void deniesWithoutGrantAndFailsClosed() {
		final ArrowheadHttpService denying = mock(ArrowheadHttpService.class);
		when(denying.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenReturn(false);
		assertFalse(new TimeManagerAuthorizationService(denying, () -> 0L).isAuthorized("AuditProbe", "time", "get-time"));

		final ArrowheadHttpService unreachable = mock(ArrowheadHttpService.class);
		when(unreachable.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenThrow(new ExternalServerError("down"));
		assertFalse(new TimeManagerAuthorizationService(unreachable, () -> 0L).isAuthorized("SysMonitor", "time", "get-time"));
	}
}
