/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for
 * {@link SecurityUtilities#getPublicKeyFromTrustStore(KeyStore, String)} and
 * {@link SecurityUtilities#getCommonNameFromSubjectDN(String)}.
 *
 * Note: {@link X500Principal} is a final class, so real instances (constructed
 * directly from a DN string) are used instead of mocks wherever a certificate's
 * subject principal is needed.
 */
public class SecurityUtilitiesTest3 {
	//=================================================================================================
	// getCommonNameFromSubjectDN

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetCommonNameFromSubjectDNNullDnReturnsNull() {
		assertNull(SecurityUtilities.getCommonNameFromSubjectDN(null));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetCommonNameFromSubjectDNEmptyDnReturnsNull() {
		assertNull(SecurityUtilities.getCommonNameFromSubjectDN(""));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetCommonNameFromSubjectDNValidDnWithCnReturnsCommonName() {
		final String dn = "CN=client1.cloud1.aitia.arrowhead.eu,O=aitia,C=HU";

		final String result = SecurityUtilities.getCommonNameFromSubjectDN(dn);

		assertEquals("client1.cloud1.aitia.arrowhead.eu", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetCommonNameFromSubjectDNLowerCaseCnTypeReturnsCommonName() {
		// rdn.getType() comparison uses equalsIgnoreCase, so "cn" must also match
		final String dn = "cn=lowercasecn,O=Test";

		final String result = SecurityUtilities.getCommonNameFromSubjectDN(dn);

		assertEquals("lowercasecn", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetCommonNameFromSubjectDNCnNotFirstRdnReturnsCommonName() {
		final String dn = "OU=Engineering,CN=middlecn,O=Company";

		final String result = SecurityUtilities.getCommonNameFromSubjectDN(dn);

		assertEquals("middlecn", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetCommonNameFromSubjectDNNoCnPresentReturnsNull() {
		final String dn = "O=OnlyOrg,C=US";

		final String result = SecurityUtilities.getCommonNameFromSubjectDN(dn);

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetCommonNameFromSubjectDNInvalidDnReturnsNull() {
		// no "attribute=value" pairs -> LdapName parsing throws InvalidNameException,
		// which is caught internally and results in a null return
		final String dn = "ThisIsNotAValidDistinguishedName";

		final String result = SecurityUtilities.getCommonNameFromSubjectDN(dn);

		assertNull(result);
	}

	//=================================================================================================
	// getPublicKeyFromTrustStore

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyFromTrustStoreNullTrustStoreThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> SecurityUtilities.getPublicKeyFromTrustStore(null, "someCn"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyFromTrustStoreNullCnThrowsException() {
		final KeyStore trustStore = mock(KeyStore.class);

		assertThrows(IllegalArgumentException.class, () -> SecurityUtilities.getPublicKeyFromTrustStore(trustStore, null));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyFromTrustStoreEmptyCnThrowsException() {
		final KeyStore trustStore = mock(KeyStore.class);

		assertThrows(IllegalArgumentException.class, () -> SecurityUtilities.getPublicKeyFromTrustStore(trustStore, ""));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyFromTrustStoreKeyStoreExceptionOnAliasesThrowsServiceConfigurationError() throws KeyStoreException {
		final KeyStore trustStore = mock(KeyStore.class);
		when(trustStore.aliases()).thenThrow(new KeyStoreException("boom"));

		assertThrows(ServiceConfigurationError.class, () -> SecurityUtilities.getPublicKeyFromTrustStore(trustStore, "someCn"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyFromTrustStoreMatchingCnReturnsPublicKey() throws KeyStoreException {
		final String targetCn = "matching.cn.arrowhead.eu";
		final KeyStore trustStore = mock(KeyStore.class);
		final X509Certificate cert = mock(X509Certificate.class);
		final PublicKey expectedKey = mock(PublicKey.class);

		when(trustStore.aliases()).thenReturn(Collections.enumeration(List.of("alias1")));
		when(trustStore.getCertificate("alias1")).thenReturn(cert);
		when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=" + targetCn + ",O=aitia"));
		when(cert.getPublicKey()).thenReturn(expectedKey);

		final PublicKey result = SecurityUtilities.getPublicKeyFromTrustStore(trustStore, targetCn);

		assertEquals(expectedKey, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyFromTrustStoreNoMatchingCnReturnsNull() throws KeyStoreException {
		final KeyStore trustStore = mock(KeyStore.class);
		final X509Certificate cert = mock(X509Certificate.class);

		when(trustStore.aliases()).thenReturn(Collections.enumeration(List.of("alias1")));
		when(trustStore.getCertificate("alias1")).thenReturn(cert);
		when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=other.cn.arrowhead.eu,O=aitia"));

		final PublicKey result = SecurityUtilities.getPublicKeyFromTrustStore(trustStore, "wanted.cn.arrowhead.eu");

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyFromTrustStoreNullCertificateForAliasSkipsAndReturnsNull() throws KeyStoreException {
		final KeyStore trustStore = mock(KeyStore.class);

		when(trustStore.aliases()).thenReturn(Collections.enumeration(List.of("aliasWithNoCert")));
		when(trustStore.getCertificate("aliasWithNoCert")).thenReturn((Certificate) null);

		final PublicKey result = SecurityUtilities.getPublicKeyFromTrustStore(trustStore, "anyCn");

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyFromTrustStoreMultipleAliasesFindsMatchInSecondAlias() throws KeyStoreException {
		final String targetCn = "second.cn.arrowhead.eu";
		final KeyStore trustStore = mock(KeyStore.class);
		final X509Certificate nonMatchingCert = mock(X509Certificate.class);
		final X509Certificate matchingCert = mock(X509Certificate.class);
		final PublicKey expectedKey = mock(PublicKey.class);

		when(trustStore.aliases()).thenReturn(Collections.enumeration(List.of("alias1", "alias2")));
		when(trustStore.getCertificate("alias1")).thenReturn(nonMatchingCert);
		when(nonMatchingCert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=first.cn.arrowhead.eu,O=aitia"));
		when(trustStore.getCertificate("alias2")).thenReturn(matchingCert);
		when(matchingCert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=" + targetCn + ",O=aitia"));
		when(matchingCert.getPublicKey()).thenReturn(expectedKey);

		final PublicKey result = SecurityUtilities.getPublicKeyFromTrustStore(trustStore, targetCn);

		assertEquals(expectedKey, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyFromTrustStoreNoAliasesAtAllReturnsNull() throws KeyStoreException {
		final KeyStore trustStore = mock(KeyStore.class);

		when(trustStore.aliases()).thenReturn(Collections.emptyEnumeration());

		final PublicKey result = SecurityUtilities.getPublicKeyFromTrustStore(trustStore, "anyCn");

		assertNull(result);
	}
}