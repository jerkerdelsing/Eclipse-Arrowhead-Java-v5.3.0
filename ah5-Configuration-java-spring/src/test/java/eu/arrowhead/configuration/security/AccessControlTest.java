/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.configuration.ConfigurationConstants;
import eu.arrowhead.configuration.api.http.ConfigurationAPI;
import eu.arrowhead.configuration.jpa.repository.ConfigurationDataRepository;
import eu.arrowhead.configuration.service.ConfigurationService;

public class AccessControlTest {

	private static RequestIdentity identity(final boolean ssl) {
		final SystemInfo sysInfo = mock(SystemInfo.class);
		when(sysInfo.isSslEnabled()).thenReturn(ssl);
		final RequestIdentity identity = new RequestIdentity();
		ReflectionTestUtils.setField(identity, "sysInfo", sysInfo);
		return identity;
	}

	private static MockHttpServletRequest request(final String system, final boolean sysop) {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		if (system != null) {
			request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, system);
		}
		request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST, sysop);
		return request;
	}

	private static ProxyAccess proxy(final boolean authorizationEnabled, final ConfigurationAuthorizationService authorization) {
		final ProxyAccess access = new ProxyAccess();
		ReflectionTestUtils.setField(access, "identity", identity(false));
		ReflectionTestUtils.setField(access, "authorizationService", authorization);
		ReflectionTestUtils.setField(access, "serviceAuthorizationEnabled", authorizationEnabled);
		return access;
	}

	@Test
	public void extractsTheSystemNameFromTheCertificateSubject() {
		assertEquals("Thermostat", RequestIdentity.systemNameFromSubject("CN=Thermostat.TestCloud.Company.arrowhead.eu,OU=Arrowhead Systems,O=Company,C=SE"));
		assertNull(RequestIdentity.systemNameFromSubject("not a DN"));
	}

	@Test
	public void requiresAnAuthenticatedSystemBoundToItsCertificate() {
		assertThrows(AuthException.class, () -> identity(false).requester(request(null, false), "o"));
		assertEquals("Thermostat", identity(false).requester(request("Thermostat", false), "o"));
		// with TLS, a request without a client certificate cannot be bound to the authenticated system
		assertThrows(AuthException.class, () -> identity(true).requester(request("Thermostat", false), "o"));
	}

	@Test
	public void aSystemReadsOnlyItsOwnConfiguration() {
		final SystemNameNormalizer normalizer = mock(SystemNameNormalizer.class);
		when(normalizer.normalize("Valve")).thenReturn("Valve");
		final ConfigurationService service = new ConfigurationService(mock(ConfigurationDataRepository.class), normalizer, mock(SystemNameValidator.class), 1024);
		final ConfigurationAPI api = new ConfigurationAPI();
		ReflectionTestUtils.setField(api, "configurationService", service);
		ReflectionTestUtils.setField(api, "identity", identity(false));
		assertThrows(ForbiddenException.class, () -> api.getConfiguration("Valve", request("Thermostat", false)));
		assertThrows(ForbiddenException.class, () -> api.getRawConfiguration("Valve", request("Thermostat", false)));
	}

	@Test
	public void proxyNeedsSysopWithoutAuthorizationCheck() {
		final ConfigurationAuthorizationService authorization = mock(ConfigurationAuthorizationService.class);
		assertEquals("LocalCloudManager", proxy(false, authorization).check(request("LocalCloudManager", true), "get-configuration", "o"));
		assertThrows(ForbiddenException.class, () -> proxy(false, authorization).check(request("Gateway", false), "get-configuration", "o"));
	}

	@Test
	public void proxyFollowsConsumerAuthorizationWhenEnabled() {
		final ConfigurationAuthorizationService authorization = mock(ConfigurationAuthorizationService.class);
		when(authorization.isAuthorized("Gateway", ConfigurationConstants.SERVICE_DEF_CONFIGURATION_PROXY, "store-configuration")).thenReturn(true);
		assertEquals("Gateway", proxy(true, authorization).check(request("Gateway", false), "store-configuration", "o"));
		assertThrows(ForbiddenException.class, () -> proxy(true, authorization).check(request("Gateway", false), "get-configuration", "o"));
	}
}
