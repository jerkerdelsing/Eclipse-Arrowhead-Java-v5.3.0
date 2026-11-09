/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.common.http.filter.authentication;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ResourceUtils;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;

@ExtendWith(MockitoExtension.class)
public class CertificateFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private final CertificateFilter filter = new CertificateFilterTestHelper(); // this is the trick

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Mock
	private ApplicationContext appContext;

	@Mock
	private FilterChain chain;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalMissingCert() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/test/");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("Unauthenticated access attempt: http://localhost/test", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalInvalidCertType() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, ServletException {
		final KeyStore keyStore = initializeTestKeyStore("device.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "device.rubin.aitia.arrowhead.eu");
		final X509Certificate[] certChain = new X509Certificate[1];
		certChain[0] = cert;
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, certChain);
		request.setRequestURI("/test/");

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("Unauthorized access: http://localhost/test, invalid certificate type: DEVICE", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalNotInTheCloud() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, ServletException {
		final KeyStore keyStore = initializeTestKeyStore("test.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "test.rubin.aitia.arrowhead.eu");
		final X509Certificate[] certChain = new X509Certificate[1];
		certChain[0] = cert;
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, certChain);
		request.setRequestURI("/test/");
		final Map<String, Object> context = Map.of(Constants.SERVER_COMMON_NAME, "ServiceRegistry.TestCloud.Aitia.arrowhead.eu");
		ReflectionTestUtils.setField(filter, "arrowheadContext", context);

		when(appContext.getBean(CloudIdentifierNormalizer.class)).thenReturn(cloudIdentifierNormalizer);
		when(cloudIdentifierNormalizer.normalize("rubin|aitia")).thenReturn("Rubin|Aitia");
		when(cloudIdentifierNormalizer.normalize("TestCloud|Aitia")).thenReturn("TestCloud|Aitia");

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(appContext).getBean(CloudIdentifierNormalizer.class);
		verify(cloudIdentifierNormalizer).normalize("rubin|aitia");
		verify(cloudIdentifierNormalizer).normalize("TestCloud|Aitia");
		verify(chain, never()).doFilter(any(ServletRequest.class), isNull());

		assertEquals("Unauthorized access: http://localhost/test, from foreign cloud", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalOkSystem() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, ServletException {
		final KeyStore keyStore = initializeTestKeyStore("test.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "test.rubin.aitia.arrowhead.eu");
		final X509Certificate[] certChain = new X509Certificate[1];
		certChain[0] = cert;
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, certChain);
		request.setRequestURI("/test/");
		final Map<String, Object> context = Map.of(Constants.SERVER_COMMON_NAME, "ServiceRegistry.Rubin.Aitia.arrowhead.eu");
		ReflectionTestUtils.setField(filter, "arrowheadContext", context);

		when(appContext.getBean(CloudIdentifierNormalizer.class)).thenReturn(cloudIdentifierNormalizer);
		when(cloudIdentifierNormalizer.normalize("rubin|aitia")).thenReturn("Rubin|Aitia");
		when(cloudIdentifierNormalizer.normalize("Rubin|Aitia")).thenReturn("Rubin|Aitia");
		when(systemNameNormalizer.normalize("test")).thenReturn("Test");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(appContext).getBean(CloudIdentifierNormalizer.class);
		verify(cloudIdentifierNormalizer).normalize("rubin|aitia");
		verify(cloudIdentifierNormalizer).normalize("Rubin|Aitia");
		verify(systemNameNormalizer).normalize("test");
		verify(chain).doFilter(any(ServletRequest.class), isNull());

		assertAll("doFilterInternal - ok (system)",
				() -> assertNotNull(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM)),
				() -> assertEquals("Test", request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM).toString()),
				() -> assertNotNull(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST)),
				() -> assertFalse((boolean) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterInternalOkSysOp() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, ServletException {
		final KeyStore keyStore = initializeTestKeyStore("management.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "management.rubin.aitia.arrowhead.eu");
		final X509Certificate[] certChain = new X509Certificate[1];
		certChain[0] = cert;
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, certChain);
		request.setRequestURI("/test/");
		final Map<String, Object> context = Map.of(Constants.SERVER_COMMON_NAME, "ServiceRegistry.Rubin.Aitia.arrowhead.eu");
		ReflectionTestUtils.setField(filter, "arrowheadContext", context);

		when(appContext.getBean(CloudIdentifierNormalizer.class)).thenReturn(cloudIdentifierNormalizer);
		when(cloudIdentifierNormalizer.normalize("rubin|aitia")).thenReturn("Rubin|Aitia");
		when(cloudIdentifierNormalizer.normalize("Rubin|Aitia")).thenReturn("Rubin|Aitia");
		when(systemNameNormalizer.normalize("management")).thenReturn("Management");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(appContext).getBean(CloudIdentifierNormalizer.class);
		verify(cloudIdentifierNormalizer).normalize("rubin|aitia");
		verify(cloudIdentifierNormalizer).normalize("Rubin|Aitia");
		verify(systemNameNormalizer).normalize("management");
		verify(chain).doFilter(any(ServletRequest.class), isNull());

		assertAll("doFilterInternal - ok (sysop)",
				() -> assertNotNull(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM)),
				() -> assertEquals("Management", request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM).toString()),
				() -> assertNotNull(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST)),
				() -> assertTrue((boolean) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST)));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private KeyStore initializeTestKeyStore(final String fileName) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keystore = KeyStore.getInstance("pkcs12");
		try (InputStream input = new FileInputStream(ResourceUtils.getFile("src/test/resources/certs/" + fileName))) {
			keystore.load(input, "123456".toCharArray());
		}

		return keystore;
	}
}