/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.certificateauthority.TestPki;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;

public class CallerProfileFilterTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void systemAndOperatorCertificatesMayCall() throws Exception {
		final TestPki pki = new TestPki();
		assertDoesNotThrow(() -> CallerProfileFilter.check(request(pki.issue("Onboarding", "sy", Instant.now().minusSeconds(60), Instant.now().plus(Duration.ofDays(1))))));
		assertDoesNotThrow(() -> CallerProfileFilter.check(request(pki.issue("Sysop", "op", Instant.now().minusSeconds(60), Instant.now().plus(Duration.ofDays(1))))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void onboardingAndDeviceCertificatesAreRefused() throws Exception {
		final TestPki pki = new TestPki();
		assertThrows(ForbiddenException.class, () -> CallerProfileFilter.check(request(pki.issue("Device", "on", Instant.now().minusSeconds(60), Instant.now().plus(Duration.ofDays(1))))));
		assertThrows(ForbiddenException.class, () -> CallerProfileFilter.check(request(pki.issue("Gateway", "de", Instant.now().minusSeconds(60), Instant.now().plus(Duration.ofDays(1))))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void clientCertificateIsRequired() {
		assertThrows(AuthException.class, () -> CallerProfileFilter.check(new MockHttpServletRequest("POST", "/certificate-authority/sign")));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static MockHttpServletRequest request(final X509Certificate certificate) {
		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/certificate-authority/sign");
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[] { certificate });
		return request;
	}
}
