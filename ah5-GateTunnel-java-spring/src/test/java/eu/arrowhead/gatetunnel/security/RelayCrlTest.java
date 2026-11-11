/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.arrowhead.gatetunnel.TestPki;
import eu.arrowhead.gatetunnel.service.FederationSettings;

// SOS-6 (2026-11-10): the relay's server certificate is issued by the organisation CA, and the GateTunnel's relay
// client checks it against that CA's CRL, fail closed, as the Gatekeeper does (its RelayCrlTest). Before, the relay
// TLS context was built without a CRL.
public class RelayCrlTest {

	@TempDir
	Path dir;

	private static Path crl(final TestPki issuer, final Path file, final BigInteger... revoked) throws Exception {
		final X509v2CRLBuilder builder = new X509v2CRLBuilder(X500Name.getInstance(issuer.root.certificate().getSubjectX500Principal().getEncoded()),
				Date.from(Instant.now().minusSeconds(60)));
		builder.setNextUpdate(Date.from(Instant.now().plus(Duration.ofDays(30))));
		for (final BigInteger serial : revoked) {
			builder.addCRLEntry(serial, new Date(), CRLReason.keyCompromise);
		}
		Files.writeString(file, TestPki.pem(new JcaX509CRLConverter().getCRL(builder.build(new JcaContentSignerBuilder("SHA256withECDSA")
				.build(issuer.root.keys().getPrivate())))));
		return file;
	}

	// A TLS server on 127.0.0.1 with the relay certificate; the client is built as RelayClient builds it.
	private static void handshake(final TestPki organisation, final TestPki.Pair relay, final TestPki.Pair client, final String crlFile) throws Exception {
		final SSLContext server = TlsSupport.pemContext(relay.cert().toString(), relay.key().toString(), organisation.root.cert().toString(), null, null);
		try (SSLServerSocket listener = (SSLServerSocket) server.getServerSocketFactory().createServerSocket(0)) {
			final CompletableFuture<Void> accepted = CompletableFuture.runAsync(() -> {
				try (SSLSocket socket = (SSLSocket) listener.accept()) {
					socket.startHandshake();
				} catch (final Exception ignored) {
					// the client decides; a refused handshake ends here too
				}
			});
			final SSLContext tls = TlsSupport.pemContext(client.cert().toString(), client.key().toString(), organisation.root.cert().toString(), crlFile, null);
			try (SSLSocket socket = (SSLSocket) tls.getSocketFactory().createSocket("127.0.0.1", listener.getLocalPort())) {
				socket.startHandshake();
			} finally {
				accepted.get(5, TimeUnit.SECONDS);
			}
		}
	}

	@Test
	public void theRelayClientRefusesARevokedRelayCertificate() throws Exception {
		final TestPki organisation = new TestPki(dir.resolve("org"));
		final TestPki.Pair relay = organisation.issue("Relay", "sy");
		final TestPki.Pair client = organisation.issue("GateTunnel", "sy");
		final Path clean = crl(organisation, dir.resolve("org-ca.crl"));
		assertDoesNotThrow(() -> handshake(organisation, relay, client, clean.toString()));
		final Path revoking = crl(organisation, dir.resolve("org-ca-revoked.crl"), relay.certificate().getSerialNumber());
		assertThrows(SSLHandshakeException.class, () -> handshake(organisation, relay, client, revoking.toString()));
	}

	@Test
	public void aMissingOrganisationCrlRefusesTheRelay() throws Exception {
		final TestPki organisation = new TestPki(dir.resolve("org"));
		final TestPki.Pair relay = organisation.issue("Relay", "sy");
		final TestPki.Pair client = organisation.issue("GateTunnel", "sy");
		assertThrows(SSLHandshakeException.class, () -> handshake(organisation, relay, client, dir.resolve("missing.crl").toString()));
	}

	@Test
	public void theRelayCrlFileIsRequired() {
		final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
				() -> new FederationSettings("TestCloud", "Company", "ah-gt-1", "GoCloud", "", "gatetunnel", 43200, 3600, 120, 8200, 8299, 8000, 8099, "relay", 8883,
						"chain", "key", "ca", "", "", " "));
		assertTrue(refused.getMessage().contains("gatetunnel.relay.crl-file"), refused.getMessage());
	}

	@Test
	public void deploymentConfigurationUsesTheOrganisationCrl() throws Exception {
		final Properties config = new Properties();
		try (FileInputStream in = new FileInputStream("config/application.properties")) {
			config.load(in);
		}
		assertEquals("${RELAY_CRL_FILE:/org-crl/org-ca.crl}", config.getProperty("gatetunnel.relay.crl-file"));
	}
}
