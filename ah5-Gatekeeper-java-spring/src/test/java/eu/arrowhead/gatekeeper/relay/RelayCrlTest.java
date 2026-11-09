/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.relay;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.arrowhead.gatekeeper.TestPki;

// The relay's server certificate is issued by the organisation CA; its revocation must be checked against that CA's CRL.
// With the cloud CA's CRL (the former default, CRL_FILE) the relay connection always failed (2026-11-09).
public class RelayCrlTest {

	@TempDir
	Path dir;

	private static Path crl(final TestPki issuer, final Path file, final BigInteger... revoked) throws Exception {
		final X509v2CRLBuilder builder = new X509v2CRLBuilder(X500Name.getInstance(issuer.root.getSubjectX500Principal().getEncoded()),
				Date.from(Instant.now().minusSeconds(60)));
		builder.setNextUpdate(Date.from(Instant.now().plus(Duration.ofDays(30))));
		for (final BigInteger serial : revoked) {
			builder.addCRLEntry(serial, new Date(), CRLReason.keyCompromise);
		}
		Files.writeString(file, TestPki.pem(new JcaX509CRLConverter().getCRL(builder.build(new JcaContentSignerBuilder("SHA256withECDSA")
				.build(issuer.rootKeys.getPrivate())))));
		return file;
	}

	private static KeyStore roots(final X509Certificate root) throws Exception {
		final KeyStore store = KeyStore.getInstance("PKCS12");
		store.load(null, null);
		store.setCertificateEntry("org", root);
		return store;
	}

	@Test
	public void theCloudCrlCannotCheckTheRelayCertificate() throws Exception {
		final TestPki organisation = new TestPki();
		final TestPki cloud = new TestPki();
		final X509Certificate relay = organisation.issue("Relay", "sy");
		final RelayTls.Crl crl = new RelayTls.Crl(crl(cloud, dir.resolve("cloud.crl")), roots(organisation.root));
		final CertificateException refused = assertThrows(CertificateException.class, () -> crl.check(relay));
		assertTrue(refused.getMessage().startsWith("Revocation status unknown"), refused.getMessage());
	}

	@Test
	public void theOrganisationCrlChecksTheRelayCertificate() throws Exception {
		final TestPki organisation = new TestPki();
		final X509Certificate relay = organisation.issue("Relay", "sy");
		final X509Certificate revoked = organisation.issue("OldRelay", "sy");
		final RelayTls.Crl crl = new RelayTls.Crl(crl(organisation, dir.resolve("org-ca.crl"), revoked.getSerialNumber()), roots(organisation.root));
		assertDoesNotThrow(() -> crl.check(relay));
		assertThrows(CertificateException.class, () -> crl.check(revoked));
	}

	@Test
	public void deploymentConfigurationUsesTheOrganisationCrl() throws Exception {
		final Properties config = new Properties();
		try (FileInputStream in = new FileInputStream("config/application.properties")) {
			config.load(in);
		}
		assertEquals("${RELAY_CRL_FILE:/certs/org-ca.crl}", config.getProperty("gatekeeper.relay.crl-file"));
	}
}
