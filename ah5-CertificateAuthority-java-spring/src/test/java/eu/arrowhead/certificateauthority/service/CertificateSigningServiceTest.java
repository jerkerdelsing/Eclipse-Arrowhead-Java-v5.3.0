/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.asn1.x509.GeneralName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import eu.arrowhead.certificateauthority.CertificateAuthorityDefaults;
import eu.arrowhead.certificateauthority.TestPki;
import eu.arrowhead.certificateauthority.jpa.entity.IssuedCertificate;
import eu.arrowhead.certificateauthority.jpa.repository.IssuedCertificateRepository;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;

public class CertificateSigningServiceTest {

	//=================================================================================================
	// members

	private TestPki pki;
	private IssuedCertificateRepository repository;
	private CertificateSigningService service;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() throws Exception {
		pki = new TestPki();
		repository = mock(IssuedCertificateRepository.class);
		when(repository.findByCommonNameAndRevokedAtIsNullAndCreatedByNot(anyString(), anyString())).thenReturn(List.of());
		when(repository.saveAndFlush(any(IssuedCertificate.class))).thenAnswer(call -> {
			final IssuedCertificate row = call.getArgument(0);
			row.setId(7L);
			return row;
		});
		service = new CertificateSigningService(pki.credentials(), repository, CertificateAuthorityDefaults.RESERVED_NAMES_DEFAULT, 365, 24);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void issuesEndEntitySystemCertificateWithChainAndAddresses() throws Exception {
		final Map<String, Object> result = service.sign(payload(TestPki.csr("Sensor." + TestPki.SUFFIX, null), "addresses", List.of("sensor", "10.0.0.5")), "Onboarding", "test");

		assertEquals(7L, result.get("id"));
		assertEquals("Sensor." + TestPki.SUFFIX, result.get("commonName"));
		assertEquals("SYSTEM", result.get("profile"));

		final List<?> chain = (List<?>) result.get("certificateChain");
		assertEquals(2, chain.size());
		final X509Certificate issued = CertificateSupport.certificate((String) chain.get(0));
		assertEquals(Base64.getEncoder().encodeToString(pki.root.getEncoded()), chain.get(1));
		issued.verify(pki.root.getPublicKey());
		assertEquals(-1, issued.getBasicConstraints(), "ca=false must be enforced");
		assertEquals("sy", CertificateSupport.attribute(issued, "DNQ"));
		assertEquals("Arrowhead Systems", CertificateSupport.attribute(issued, "OU"));
		assertEquals(issued.getSerialNumber().toString(), result.get("serialNumber"));
		assertTrue(issued.getSubjectAlternativeNames().stream().anyMatch(n -> Integer.valueOf(GeneralName.dNSName).equals(n.get(0)) && "sensor".equals(n.get(1))));
		assertTrue(issued.getSubjectAlternativeNames().stream().anyMatch(n -> Integer.valueOf(GeneralName.iPAddress).equals(n.get(0)) && "10.0.0.5".equals(n.get(1))));

		final ArgumentCaptor<IssuedCertificate> saved = ArgumentCaptor.forClass(IssuedCertificate.class);
		verify(repository).saveAndFlush(saved.capture());
		assertEquals("Onboarding", saved.getValue().getCreatedBy());
		assertTrue(saved.getValue().getCertificatePem().contains("BEGIN CERTIFICATE"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void takesProfileFromCsrQualifierAndAddressesFromCsrRequest() throws Exception {
		final Map<String, Object> result = service.sign(payload(TestPki.csr("Gateway." + TestPki.SUFFIX, "de", "gateway.local")), "Sysop", "test");

		assertEquals("DEVICE", result.get("profile"));
		final X509Certificate issued = CertificateSupport.certificate((String) ((List<?>) result.get("certificateChain")).get(0));
		assertTrue(issued.getSubjectAlternativeNames().stream().anyMatch(n -> "gateway.local".equals(n.get(1))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void refusesCallersWithoutProvisioningIdentity() {
		assertThrows(ForbiddenException.class, () -> service.sign(payload(TestPki.csr("Sensor." + TestPki.SUFFIX, null)), "LocalCloudManager", "test"));
		verify(repository, never()).saveAndFlush(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void reservedNamesNeedSysop() throws Exception {
		assertThrows(ForbiddenException.class, () -> service.sign(payload(TestPki.csr("ServiceRegistry." + TestPki.SUFFIX, null)), "Onboarding", "test"));
		assertEquals("SYSTEM", service.sign(payload(TestPki.csr("ServiceRegistry." + TestPki.SUFFIX, null)), "Sysop", "test").get("profile"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void profilesAreLimitedPerCaller() {
		assertThrows(ForbiddenException.class, () -> service.sign(payload(TestPki.csr("Admin." + TestPki.SUFFIX, null), "profile", "op"), "Onboarding", "test"));
		assertThrows(ForbiddenException.class, () -> service.sign(payload(TestPki.csr("Sensor." + TestPki.SUFFIX, null), "profile", "de"), "CertificateAuthority", "test"));
		assertThrows(InvalidParameterException.class, () -> service.sign(payload(TestPki.csr("Sensor." + TestPki.SUFFIX, null), "profile", "xx"), "Sysop", "test"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void commonNameMustBelongToTheLocalCloud() {
		assertThrows(InvalidParameterException.class, () -> service.sign(payload(TestPki.csr("Sensor.OtherCloud.Company.arrowhead.eu", null)), "Sysop", "test"));
		assertThrows(InvalidParameterException.class, () -> service.sign(payload(TestPki.csr("sensor." + TestPki.SUFFIX, null)), "Sysop", "test"));
		assertThrows(InvalidParameterException.class, () -> service.sign(Map.of(), "Sysop", "test"));
		assertThrows(InvalidParameterException.class, () -> service.sign(Map.of("encodedCSR", "not a csr"), "Sysop", "test"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validNameHeldByAnotherIssuerCannotBeTakenOver() throws Exception {
		final IssuedCertificate held = new IssuedCertificate("1", "Sensor." + TestPki.SUFFIX, "Sysop", Instant.now(), Instant.now().plus(Duration.ofDays(1)), null, "pem");
		when(repository.findByCommonNameAndRevokedAtIsNullAndCreatedByNot("Sensor." + TestPki.SUFFIX, "Onboarding")).thenReturn(List.of(held));

		assertThrows(ForbiddenException.class, () -> service.sign(payload(TestPki.csr("Sensor." + TestPki.SUFFIX, null)), "Onboarding", "test"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void onboardingCertificatesAreShortLived() throws Exception {
		final Map<String, Object> result = service.sign(payload(TestPki.csr("Device." + TestPki.SUFFIX, null), "profile", "on"), "Onboarding", "test");
		final Instant after = Instant.parse((String) result.get("validAfter"));
		final Instant before = Instant.parse((String) result.get("validBefore"));
		assertFalse(before.isAfter(after.plus(Duration.ofHours(24))));

		assertThrows(InvalidParameterException.class, () -> service.sign(payload(TestPki.csr("Device." + TestPki.SUFFIX, null), "profile", "on",
				"validBefore", Instant.now().plus(Duration.ofDays(2)).toString()), "Onboarding", "test"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void addressesAreValidated() {
		assertThrows(InvalidParameterException.class, () -> service.sign(payload(TestPki.csr("Sensor." + TestPki.SUFFIX, null), "addresses", "sensor"), "Sysop", "test"));
		assertThrows(InvalidParameterException.class, () -> service.sign(payload(TestPki.csr("Sensor." + TestPki.SUFFIX, null), "addresses", List.of("bad_name!")), "Sysop", "test"));
		assertThrows(InvalidParameterException.class, () -> service.sign(payload(TestPki.csr("Sensor." + TestPki.SUFFIX, null), "addresses",
				List.of("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10", "a11", "a12", "a13", "a14", "a15", "a16", "a17")), "Sysop", "test"));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static Map<String, Object> payload(final String csr, final Object... pairs) {
		final Map<String, Object> payload = new HashMap<>();
		payload.put("encodedCSR", csr);
		for (int i = 0; i < pairs.length; i += 2) {
			payload.put((String) pairs[i], pairs[i + 1]);
		}

		return payload;
	}
}
