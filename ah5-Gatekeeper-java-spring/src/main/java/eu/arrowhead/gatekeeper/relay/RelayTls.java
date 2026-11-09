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

import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

// TLS context of the relay identity (organisation-signed PEM chain and key, organisation trust), ported unchanged from
// the native 5.2.2 Gatekeeper. Revocation (J4): peers issued by a trusted CA that signs the CRL file are checked against
// that CRL on every handshake, fail closed.
public final class RelayTls {

	//=================================================================================================
	// members

	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static SSLContext context(final String certFile, final String keyFile, final String caFile, final String crlFile) throws Exception {
		final CertificateFactory factory = CertificateFactory.getInstance("X.509");
		final KeyStore keys = KeyStore.getInstance("PKCS12");
		keys.load(null, null);
		final Certificate[] chain;
		try (var in = Files.newInputStream(Path.of(certFile))) {
			chain = factory.generateCertificates(in).toArray(Certificate[]::new);
		}

		keys.setKeyEntry("identity", key(Path.of(keyFile)), new char[0], chain);
		final KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		km.init(keys, new char[0]);

		final KeyStore roots = KeyStore.getInstance(KeyStore.getDefaultType());
		roots.load(null, null);
		try (var in = Files.newInputStream(Path.of(caFile))) {
			int i = 0;
			for (final Certificate c : factory.generateCertificates(in)) {
				roots.setCertificateEntry("ca" + i++, c);
			}
		}

		final TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tm.init(roots);
		X509ExtendedTrustManager base = (X509ExtendedTrustManager) Arrays.stream(tm.getTrustManagers())
				.filter(t -> t instanceof X509ExtendedTrustManager)
				.findFirst()
				.orElseThrow();
		if (crlFile != null && !crlFile.isEmpty()) {
			base = new Revoking(base, Crl.of(Path.of(crlFile), roots));
		}

		final SSLContext context = SSLContext.getInstance("TLS");
		context.init(km.getKeyManagers(), new TrustManager[] { base }, new SecureRandom());

		return context;
	}

	//-------------------------------------------------------------------------------------------------
	static PrivateKey key(final Path file) throws Exception {
		try (PEMParser parser = new PEMParser(Files.newBufferedReader(file))) {
			final Object value = parser.readObject();
			final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
			if (value instanceof PEMKeyPair pair) {
				return converter.getPrivateKey(pair.getPrivateKeyInfo());
			}

			if (value instanceof PrivateKeyInfo info) {
				return converter.getPrivateKey(info);
			}

			throw new IllegalArgumentException("Unsupported private key encoding");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private RelayTls() {
		throw new UnsupportedOperationException();
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	// CRL of the cloud CA, re-read when the file changes. Fail closed for certificates of a trusted CA: no readable CRL, a
	// CRL with a bad signature or past its nextUpdate refuses them (the brokers behave the same). Other issuers are not checked.
	static final class Crl {

		private static final Map<Path, Crl> CACHE = new HashMap<>();
		private static final Logger LOGGER = LogManager.getLogger(Crl.class);

		private final Path file;
		private final List<X509Certificate> issuers = new ArrayList<>();
		private X509CRL crl;
		private FileTime loaded;
		private String problem = "no CRL loaded";

		Crl(final Path file, final KeyStore roots) throws Exception {
			this.file = file;
			for (final String alias : Collections.list(roots.aliases())) {
				if (roots.getCertificate(alias) instanceof X509Certificate c) {
					issuers.add(c);
				}
			}
		}

		static synchronized Crl of(final Path file, final KeyStore roots) throws Exception {
			Crl crl = CACHE.get(file);
			if (crl == null) {
				crl = new Crl(file, roots);
				CACHE.put(file, crl);
			}

			return crl;
		}

		synchronized void refresh() {
			try {
				final FileTime time = Files.getLastModifiedTime(file);
				if (time.equals(loaded)) {
					return;
				}

				final X509CRL next;
				try (var in = Files.newInputStream(file)) {
					next = (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(in);
				}

				final X509Certificate issuer = issuers.stream()
						.filter(c -> c.getSubjectX500Principal().equals(next.getIssuerX500Principal()))
						.findFirst()
						.orElseThrow(() -> new CRLException("CRL issuer is not a trusted CA"));
				next.verify(issuer.getPublicKey());
				crl = next;
				loaded = time;
				problem = null;
			} catch (final Exception ex) {
				problem = "CRL " + file + " unusable: " + ex.getMessage();
				LOGGER.error("revocation: {}", problem);
			}
		}

		synchronized void check(final X509Certificate leaf) throws CertificateException {
			final boolean ours = issuers.stream().anyMatch(c -> c.getSubjectX500Principal().equals(leaf.getIssuerX500Principal()));
			refresh();
			if (crl == null) {
				if (ours) {
					throw new CertificateException("Revocation status unknown: " + problem);
				}

				return;
			}

			if (!crl.getIssuerX500Principal().equals(leaf.getIssuerX500Principal())) {
				return;
			}

			if (crl.getNextUpdate() != null && crl.getNextUpdate().before(new Date())) {
				throw new CertificateException("CRL expired at " + crl.getNextUpdate().toInstant());
			}

			if (crl.isRevoked(leaf)) {
				throw new CertificateException("Certificate " + leaf.getSerialNumber().toString(16) + " is revoked");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	static final class Revoking extends X509ExtendedTrustManager {

		private final X509ExtendedTrustManager base;
		private final Crl crl;

		Revoking(final X509ExtendedTrustManager base, final Crl crl) {
			this.base = base;
			this.crl = crl;
		}

		private void revocation(final X509Certificate[] chain) throws CertificateException {
			if (chain.length > 0) {
				crl.check(chain[0]);
			}
		}

		@Override
		public void checkClientTrusted(final X509Certificate[] c, final String a) throws CertificateException {
			base.checkClientTrusted(c, a);
			revocation(c);
		}

		@Override
		public void checkClientTrusted(final X509Certificate[] c, final String a, final Socket s) throws CertificateException {
			base.checkClientTrusted(c, a, s);
			revocation(c);
		}

		@Override
		public void checkClientTrusted(final X509Certificate[] c, final String a, final SSLEngine e) throws CertificateException {
			base.checkClientTrusted(c, a, e);
			revocation(c);
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] c, final String a) throws CertificateException {
			base.checkServerTrusted(c, a);
			revocation(c);
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] c, final String a, final Socket s) throws CertificateException {
			base.checkServerTrusted(c, a, s);
			revocation(c);
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] c, final String a, final SSLEngine e) throws CertificateException {
			base.checkServerTrusted(c, a, e);
			revocation(c);
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return base.getAcceptedIssuers();
		}
	}
}
