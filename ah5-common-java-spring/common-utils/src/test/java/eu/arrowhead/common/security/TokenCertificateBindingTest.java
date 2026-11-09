/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.common.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

public class TokenCertificateBindingTest {

	//=================================================================================================
	// members

	private final SystemNameNormalizer normalizer = mock(SystemNameNormalizer.class);
	private final MockHttpServletRequest request = new MockHttpServletRequest();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		when(normalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		request.setSecure(true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testMatchingCertificateIsAccepted() {
		presentCertificate("Blacklist");
		assertDoesNotThrow(() -> TokenCertificateBinding.check(request, "Blacklist", normalizer));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStolenTokenFromOtherCertificateIsRefused() {
		presentCertificate("Blacklist");
		assertThrows(AuthException.class, () -> TokenCertificateBinding.check(request, "LocalCloudManager", normalizer));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testMissingCertificateOverTlsIsRefused() {
		assertThrows(AuthException.class, () -> TokenCertificateBinding.check(request, "Blacklist", normalizer));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCertificateWithoutArrowheadProfileIsRefused() {
		final X509Certificate cert = mock(X509Certificate.class);
		when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=Blacklist.TestCloud.Company.arrowhead.eu,O=Company"));
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[] { cert });
		assertThrows(AuthException.class, () -> TokenCertificateBinding.check(request, "Blacklist", normalizer));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testMissingTokenSystemIsRefused() {
		presentCertificate("Blacklist");
		assertThrows(AuthException.class, () -> TokenCertificateBinding.check(request, null, normalizer));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPlainHttpIsLeftToTheToken() {
		request.setSecure(false);
		assertDoesNotThrow(() -> TokenCertificateBinding.check(request, "Blacklist", normalizer));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void presentCertificate(final String systemName) {
		final X509Certificate cert = mock(X509Certificate.class);
		when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=" + systemName + ".TestCloud.Company.arrowhead.eu, OID.2.5.4.46=sy, O=Company"));
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[] { cert });
	}
}
