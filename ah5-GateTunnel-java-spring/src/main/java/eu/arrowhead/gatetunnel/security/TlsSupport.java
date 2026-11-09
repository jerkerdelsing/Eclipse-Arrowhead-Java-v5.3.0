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

import java.io.InputStream;
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

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

// TLS material outside the Spring web server: the relay identity (PEM, RELAY_*), the inter-cloud tunnel listeners and the
// provider calls (the system's PKCS12 stores). Ported from the 5.2.2 Tls class: optional pinning of the peer's CN and
// system profile, and fail-closed revocation against the cloud CA's CRL (security finding J4), re-read when it changes.
public final class TlsSupport {

	//=================================================================================================
	// members

	private static final Logger logger = LogManager.getLogger(TlsSupport.class);

	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static PrivateKey key(final Path file) throws Exception {
		try (PEMParser parser = new PEMParser(Files.newBufferedReader(file))) {
			final Object value = parser.readObject();
			final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
			if (value instanceof final PEMKeyPair pair) {
				return converter.getPrivateKey(pair.getPrivateKeyInfo());
			}
			if (value instanceof final PrivateKeyInfo info) {
				return converter.getPrivateKey(info);
			}

			throw new IllegalArgumentException("Unsupported private key encoding");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static X509Certificate cert(final Path file) throws Exception {
		try (InputStream in = Files.newInputStream(file)) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static String cn(final X509Certificate certificate) {
		return attribute(certificate, "CN");
	}

	//-------------------------------------------------------------------------------------------------
	// A subject attribute; "DNQ" is the dnQualifier (2.5.4.46) holding the Arrowhead certificate profile (sy, op, on, ...).
	public static String attribute(final X509Certificate certificate, final String attribute) {
		try {
			for (final Rdn rdn : new LdapName(certificate.getSubjectX500Principal().getName()).getRdns()) {
				if (rdn.getType().equalsIgnoreCase(attribute) || "DNQ".equals(attribute) && "2.5.4.46".equals(rdn.getType())) {
					final Object value = rdn.getValue();
					if (value instanceof final byte[] encoded) {
						return ((ASN1String) ASN1Primitive.fromByteArray(encoded)).getString();
					}

					return value == null ? "" : value.toString();
				}
			}
		} catch (final Exception ignored) {
			// not a parsable subject: no attribute
		}

		return "";
	}

	//-------------------------------------------------------------------------------------------------
	// PEM identity (certificate chain + key) and PEM trust anchors, as the relay connection uses.
	public static SSLContext pemContext(final String certFile, final String keyFile, final String caFile, final String crlFile, final String pinnedCn) throws Exception {
		final CertificateFactory factory = CertificateFactory.getInstance("X.509");
		final KeyStore keys = KeyStore.getInstance("PKCS12");
		keys.load(null, null);
		final Certificate[] chain;
		try (InputStream in = Files.newInputStream(Path.of(certFile))) {
			chain = factory.generateCertificates(in).toArray(Certificate[]::new);
		}
		keys.setKeyEntry("identity", key(Path.of(keyFile)), new char[0], chain);

		final KeyStore roots = KeyStore.getInstance(KeyStore.getDefaultType());
		roots.load(null, null);
		try (InputStream in = Files.newInputStream(Path.of(caFile))) {
			int i = 0;
			for (final Certificate certificate : factory.generateCertificates(in)) {
				roots.setCertificateEntry("ca" + i++, certificate);
			}
		}

		return context(keys, new char[0], roots, crlFile, pinnedCn);
	}

	//-------------------------------------------------------------------------------------------------
	// The system identity of the PKCS12 key store (only the entry of keyAlias) and the PKCS12 trust store.
	public static SSLContext storeContext(final KeyStore keyStore, final String keyAlias, final char[] keyPassword, final KeyStore trustStore, final String crlFile, final String pinnedCn)
			throws Exception {
		final KeyStore identity = KeyStore.getInstance("PKCS12");
		identity.load(null, null);
		final PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword);
		if (privateKey == null) {
			throw new IllegalStateException("Key store has no private key " + keyAlias);
		}
		identity.setKeyEntry("identity", privateKey, new char[0], keyStore.getCertificateChain(keyAlias));

		return context(identity, new char[0], trustStore, crlFile, pinnedCn);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static SSLContext context(final KeyStore keys, final char[] password, final KeyStore roots, final String crlFile, final String pinnedCn) throws Exception {
		final KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		km.init(keys, password);
		final TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tm.init(roots);
		X509ExtendedTrustManager base = (X509ExtendedTrustManager) Arrays.stream(tm.getTrustManagers())
				.filter(t -> t instanceof X509ExtendedTrustManager)
				.findFirst()
				.orElseThrow();
		if (crlFile != null && !crlFile.isBlank()) {
			base = new Revoking(base, Crl.of(Path.of(crlFile.trim()), roots));
		}

		final TrustManager[] trust = { pinnedCn != null ? new Pinned(base, pinnedCn) : base };
		final SSLContext context = SSLContext.getInstance("TLS");
		context.init(km.getKeyManagers(), trust, new SecureRandom());

		return context;
	}

	//-------------------------------------------------------------------------------------------------
	private TlsSupport() {
		throw new UnsupportedOperationException();
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	// Server certificate must carry the expected CN and the system profile.
	static final class Pinned extends X509ExtendedTrustManager {

		private final X509ExtendedTrustManager base;
		private final String expected;

		Pinned(final X509ExtendedTrustManager base, final String expected) {
			this.base = base;
			this.expected = expected;
		}

		private void pin(final X509Certificate[] chain) throws CertificateException {
			if (chain.length == 0 || !cn(chain[0]).equals(expected) || !"sy".equals(attribute(chain[0], "DNQ"))) {
				throw new CertificateException("Unexpected peer identity");
			}
		}

		@Override
		public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			base.checkClientTrusted(chain, authType);
		}

		@Override
		public void checkClientTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
			base.checkClientTrusted(chain, authType, socket);
		}

		@Override
		public void checkClientTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
			base.checkClientTrusted(chain, authType, engine);
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			base.checkServerTrusted(chain, authType);
			pin(chain);
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
			base.checkServerTrusted(chain, authType, socket);
			pin(chain);
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
			base.checkServerTrusted(chain, authType, engine);
			pin(chain);
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return base.getAcceptedIssuers();
		}
	}

	//-------------------------------------------------------------------------------------------------
	// CRL of the cloud CA, re-read when the file changes. Fail closed for certificates of a trusted CA: no readable CRL, a
	// CRL with a bad signature or past its nextUpdate refuses them. Other issuers are not checked.
	static final class Crl {

		private static final Map<Path, Crl> CACHE = new HashMap<>();

		private final Path file;
		private final List<X509Certificate> issuers = new ArrayList<>();
		private X509CRL crl;
		private FileTime loaded;
		private String problem = "no CRL loaded";

		private Crl(final Path file, final KeyStore roots) throws Exception {
			this.file = file;
			for (final String alias : Collections.list(roots.aliases())) {
				if (roots.getCertificate(alias) instanceof final X509Certificate certificate) {
					issuers.add(certificate);
				}
			}
		}

		static synchronized Crl of(final Path file, final KeyStore roots) throws Exception {
			Crl result = CACHE.get(file);
			if (result == null) {
				result = new Crl(file, roots);
				CACHE.put(file, result);
			}

			return result;
		}

		synchronized void refresh() {
			try {
				final FileTime modified = Files.getLastModifiedTime(file);
				if (modified.equals(loaded)) {
					return;
				}

				final X509CRL next;
				try (InputStream in = Files.newInputStream(file)) {
					next = (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(in);
				}
				final X509Certificate issuer = issuers.stream()
						.filter(c -> c.getSubjectX500Principal().equals(next.getIssuerX500Principal()))
						.findFirst()
						.orElseThrow(() -> new CRLException("CRL issuer is not a trusted CA"));
				next.verify(issuer.getPublicKey());
				crl = next;
				loaded = modified;
				problem = null;
			} catch (final Exception ex) {
				problem = "CRL " + file + " unusable: " + ex.getMessage();
				logger.error("revocation: {}", problem);
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
		public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			base.checkClientTrusted(chain, authType);
			revocation(chain);
		}

		@Override
		public void checkClientTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
			base.checkClientTrusted(chain, authType, socket);
			revocation(chain);
		}

		@Override
		public void checkClientTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
			base.checkClientTrusted(chain, authType, engine);
			revocation(chain);
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			base.checkServerTrusted(chain, authType);
			revocation(chain);
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
			base.checkServerTrusted(chain, authType, socket);
			revocation(chain);
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
			base.checkServerTrusted(chain, authType, engine);
			revocation(chain);
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return base.getAcceptedIssuers();
		}
	}
}
