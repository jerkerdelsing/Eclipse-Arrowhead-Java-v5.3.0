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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.arrowhead.certificateauthority.TestPki;
import eu.arrowhead.certificateauthority.jpa.entity.IssuedCertificate;
import eu.arrowhead.certificateauthority.jpa.entity.TrustedKey;
import eu.arrowhead.certificateauthority.jpa.repository.IssuedCertificateRepository;
import eu.arrowhead.certificateauthority.jpa.repository.TrustedKeyRepository;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;

// Revocation, CRL publication and certificate/trusted-key validation against an in-memory store.
public class RevocationAndValidationTest {

	//=================================================================================================
	// members

	@TempDir
	Path crlDirectory;

	private TestPki pki;
	private final List<IssuedCertificate> rows = new ArrayList<>();
	private final List<TrustedKey> keyRows = new ArrayList<>();
	private CrlService crl;
	private CertificateManagementService management;
	private CertificateValidationService validation;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() throws Exception {
		pki = new TestPki();
		final IssuedCertificateRepository certificates = mock(IssuedCertificateRepository.class);
		when(certificates.saveAndFlush(any(IssuedCertificate.class))).thenAnswer(call -> {
			final IssuedCertificate row = call.getArgument(0);
			if (row.getId() == null) {
				row.setId((long) rows.size() + 1);
				rows.add(row);
			}
			return row;
		});
		when(certificates.findById(any())).thenAnswer(call -> rows.stream().filter(r -> r.getId().equals(call.getArgument(0))).findFirst());
		when(certificates.findBySerialNumber(any())).thenAnswer(call -> rows.stream().filter(r -> r.getSerialNumber().equals(call.getArgument(0))).findFirst());
		when(certificates.findByRevokedAtIsNotNull()).thenAnswer(call -> rows.stream().filter(r -> r.getRevokedAt() != null).toList());

		final TrustedKeyRepository keys = mock(TrustedKeyRepository.class);
		when(keys.saveAndFlush(any(TrustedKey.class))).thenAnswer(call -> {
			final TrustedKey row = call.getArgument(0);
			row.setId((long) keyRows.size() + 1);
			keyRows.add(row);
			return row;
		});
		when(keys.findByPublicKeyHash(any())).thenAnswer(call -> keyRows.stream().filter(r -> r.getPublicKeyHash().equals(call.getArgument(0))).findFirst());

		crl = new CrlService(pki.credentials(), certificates, crlDirectory.toString(), 72);
		management = new CertificateManagementService(pki.credentials(), certificates, keys, crl, 365);
		validation = new CertificateValidationService(pki.credentials(), certificates, keys);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void onboardingRevokesOnlyItsOwnCertificatesAndOthersNeedManagement() throws Exception {
		final IssuedCertificate own = row("Sensor", "Onboarding", Instant.now().plus(Duration.ofDays(1)));
		final IssuedCertificate other = row("Pump", "Sysop", Instant.now().plus(Duration.ofDays(1)));

		assertFalse(management.revokeById("Onboarding", other.getId(), "test"));
		assertTrue(management.revokeById("Onboarding", own.getId(), "test"));
		assertFalse(management.revokeById("Onboarding", own.getId(), "test"), "already revoked");
		assertThrows(ForbiddenException.class, () -> management.revokeById("LocalCloudManager", other.getId(), "test"));
		assertTrue(management.revokeById("Sysop", other.getId(), "test"));
		assertFalse(management.revokeById("Sysop", 99, "test"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void publishedCrlListsUnexpiredRevocationsOnly() throws Exception {
		final IssuedCertificate valid = row("Sensor", "Sysop", Instant.now().plus(Duration.ofDays(1)));
		final IssuedCertificate expired = row("Old", "Sysop", Instant.now().minus(Duration.ofDays(1)));
		valid.setRevokedAt(Instant.now());
		expired.setRevokedAt(Instant.now());

		crl.publish();
		final byte[] file = Files.readAllBytes(crlDirectory.resolve("ca.crl"));
		final X509CRL parsed = (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(new ByteArrayInputStream(file));
		parsed.verify(pki.root.getPublicKey());
		assertNotNull(parsed.getRevokedCertificate(new BigInteger(valid.getSerialNumber())));
		assertNull(parsed.getRevokedCertificate(new BigInteger(expired.getSerialNumber())));
		assertTrue(parsed.getNextUpdate().toInstant().isAfter(Instant.now().plus(Duration.ofHours(71))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void revocationByContentCoversCertificatesMissingFromTheStore() throws Exception {
		final var foreignHeld = pki.issue("Rogue", "sy", Instant.now().minusSeconds(60), Instant.now().plus(Duration.ofDays(1)));
		final Map<String, Object> result = management.revokeByContent("Sysop", Map.of("certificate", CertificateSupport.pem(foreignHeld)), "test");

		assertEquals(true, result.get("changed"));
		assertEquals("revoked-by:Sysop", rows.getFirst().getCreatedBy());
		assertEquals("REVOKED", validation.checkCertificate(Map.of("certificate", CertificateSupport.pem(foreignHeld)), "test").get("status"));
		assertEquals(false, management.revokeByContent("Sysop", Map.of("encodedCertificate", CertificateSupport.pem(foreignHeld)), "test").get("changed"));

		final var otherCloud = new TestPki("OtherCloud.Company.arrowhead.eu").issue("Rogue", "sy", Instant.now().minusSeconds(60), Instant.now().plus(Duration.ofDays(1)));
		assertThrows(InvalidParameterException.class, () -> management.revokeByContent("Sysop", Map.of("certificate", CertificateSupport.pem(otherCloud)), "test"));
		assertThrows(ForbiddenException.class, () -> management.revokeByContent("Onboarding", Map.of("certificate", CertificateSupport.pem(foreignHeld)), "test"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void certificateStatus() throws Exception {
		final var good = pki.issue("Sensor", "sy", Instant.now().minusSeconds(60), Instant.now().plus(Duration.ofDays(1)));
		final var expired = pki.issue("Old", "sy", Instant.now().minus(Duration.ofDays(2)), Instant.now().minus(Duration.ofDays(1)));
		final var foreign = new TestPki("OtherCloud.Company.arrowhead.eu").issue("Sensor", "sy", Instant.now().minusSeconds(60), Instant.now().plus(Duration.ofDays(1)));

		final Map<String, Object> goodResult = validation.checkCertificate(Map.of("certificate", CertificateSupport.pem(good)), "test");
		assertEquals("GOOD", goodResult.get("status"));
		assertEquals(true, goodResult.get("valid"));
		assertEquals(false, goodResult.get("known"));
		assertEquals("EXPIRED", validation.checkCertificate(Map.of("certificate", CertificateSupport.pem(expired)), "test").get("status"));
		assertEquals("UNKNOWN", validation.checkCertificate(Map.of("certificate", CertificateSupport.pem(foreign)), "test").get("status"));
		assertThrows(InvalidParameterException.class, () -> validation.checkCertificate(Map.of(), "test"));
		assertThrows(InvalidParameterException.class, () -> validation.checkCertificate(Map.of("certificate", "garbage"), "test"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void trustedKeysHonourTheirValidity() throws Exception {
		final String current = CertificateSupport.pem(org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(TestPki.key().getPublic().getEncoded()));
		final String future = CertificateSupport.pem(org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(TestPki.key().getPublic().getEncoded()));

		management.addKey("Sysop", Map.of("publicKey", current, "description", "gateway"), "test");
		management.addKey("Sysop", Map.of("publicKeyPem", future, "validAfter", Instant.now().plus(Duration.ofDays(1)).toString()), "test");

		final Map<String, Object> currentResult = validation.checkTrustedKey(Map.of("publicKey", current), "test");
		assertEquals(true, currentResult.get("trusted"));
		assertEquals("gateway", ((Map<?, ?>) currentResult.get("key")).get("description"));
		assertEquals(false, validation.checkTrustedKey(Map.of("publicKey", future), "test").get("trusted"));
		assertEquals(false, validation.checkTrustedKey(Map.of("publicKey", "unknown"), "test").get("trusted"));
		assertNull(validation.checkTrustedKey(Map.of("publicKey", "unknown"), "test").get("key"));

		assertThrows(InvalidParameterException.class, () -> management.addKey("Sysop", Map.of("publicKey", "not a key"), "test"));
		assertThrows(InvalidParameterException.class, () -> management.addKey("Sysop", Map.of("publicKey", current, "validAfter", "2030-01-02T00:00:00Z", "validBefore", "2030-01-01T00:00:00Z"), "test"));
		assertThrows(ForbiddenException.class, () -> management.addKey("Onboarding", Map.of("publicKey", current), "test"));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private IssuedCertificate row(final String system, final String createdBy, final Instant validUntil) {
		final IssuedCertificate row = new IssuedCertificate(String.valueOf(1000 + rows.size()), system + "." + TestPki.SUFFIX, createdBy, Instant.now().minusSeconds(60), validUntil, null, "pem");
		row.setId((long) rows.size() + 1);
		rows.add(row);
		return Optional.of(row).get();
	}
}
