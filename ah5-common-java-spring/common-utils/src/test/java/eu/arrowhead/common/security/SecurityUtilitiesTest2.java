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
package eu.arrowhead.common.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ResourceUtils;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.security.SecurityUtilities.CommonNameAndType;

@ExtendWith(MockitoExtension.class)
public class SecurityUtilitiesTest2 {

	//=================================================================================================
	// members

	private MockedStatic<SecurityUtilities> mockStatic;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		mockStatic = mockStatic(SecurityUtilities.class);
	}

	//-------------------------------------------------------------------------------------------------
	@AfterEach
	public void tearDown() {
		mockStatic.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromRequestEmptyCN() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		mockStatic.when(() -> SecurityUtilities.getIdentificationDataFromSubjectDN(anyString())).thenReturn(new CommonNameAndType("", CertificateProfileType.SYSTEM));

		final KeyStore keyStore = initializeTestKeyStore("wrong.p12");
		mockStatic.when(() -> SecurityUtilities.getCertificateFromKeyStore(keyStore, "wrong.rubin.aitia.arrowhead.eu")).thenCallRealMethod();
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "wrong.rubin.aitia.arrowhead.eu");
		final X509Certificate[] certChain = new X509Certificate[1];
		certChain[0] = cert;
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, certChain);

		mockStatic.when(() -> SecurityUtilities.getIdentificationDataFromRequest(request)).thenCallRealMethod();

		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromRequest(request);
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromCertificateEmptyCN() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		mockStatic.when(() -> SecurityUtilities.getIdentificationDataFromSubjectDN(anyString())).thenReturn(new CommonNameAndType("", CertificateProfileType.SYSTEM));

		final KeyStore keyStore = initializeTestKeyStore("wrong.p12");
		mockStatic.when(() -> SecurityUtilities.getCertificateFromKeyStore(keyStore, "wrong.rubin.aitia.arrowhead.eu")).thenCallRealMethod();
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "wrong.rubin.aitia.arrowhead.eu");

		mockStatic.when(() -> SecurityUtilities.getIdentificationDataFromCertificate(cert)).thenCallRealMethod();

		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromCertificate(cert);
		assertNull(data);
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
