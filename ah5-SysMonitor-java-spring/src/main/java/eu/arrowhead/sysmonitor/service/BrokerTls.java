/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.sysmonitor.SysMonitorConstants;

// Client TLS of the AMQP intake and the MQTT output: the system's own PKCS12 identity (sysmonitor.ssl.* or server.ssl.*), the
// cloud trust store and, as in the native implementation (security finding J4), the broker certificate checked against the
// CRL of the cloud CA (arrowhead.ssl.crl-file) on every handshake.
@Component
public class BrokerTls {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	private final SSLProperties serverSsl;

	@Value(Constants.$SYS_MONITOR_SSL_ENABLED_WD)
	private boolean enabled;

	@Value(Constants.$SYS_MONITOR_SSL_KEY_STORE_WD)
	private String keyStore;

	@Value(Constants.$SYS_MONITOR_SSL_KEY_STORE_PASSWORD_WD)
	private String keyStorePassword;

	@Value(Constants.$SYS_MONITOR_SSL_TRUST_STORE_WD)
	private String trustStore;

	@Value(Constants.$SYS_MONITOR_SSL_TRUST_STORE_PASSWORD_WD)
	private String trustStorePassword;

	@Value(SysMonitorConstants.$CRL_FILE_WD)
	private String crlFile;

	private SSLContext context;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public BrokerTls(final SSLProperties serverSsl) {
		this.serverSsl = serverSsl;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isEnabled() {
		return enabled;
	}

	//-------------------------------------------------------------------------------------------------
	public synchronized SSLContext context() throws Exception {
		if (context == null) {
			context = build();
		}

		return context;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SSLContext build() throws Exception {
		final DefaultResourceLoader loader = new DefaultResourceLoader();
		final Resource keys = Utilities.isEmpty(keyStore) ? serverSsl.getKeyStore() : loader.getResource(keyStore);
		final Resource trust = Utilities.isEmpty(trustStore) ? serverSsl.getTrustStore() : loader.getResource(trustStore);
		final char[] keyPassword = (Utilities.isEmpty(keyStorePassword) ? serverSsl.getKeyStorePassword() : keyStorePassword).toCharArray();
		final char[] trustPassword = (Utilities.isEmpty(trustStorePassword) ? serverSsl.getTrustStorePassword() : trustStorePassword).toCharArray();
		final String type = Utilities.isEmpty(serverSsl.getKeyStoreType()) ? "PKCS12" : serverSsl.getKeyStoreType();

		final KeyStore identity = load(keys, type, keyPassword);
		final KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagers.init(identity, keyPassword);

		final KeyStore roots = load(trust, type, trustPassword);
		final TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagers.init(roots);
		X509ExtendedTrustManager base = (X509ExtendedTrustManager) Arrays.stream(trustManagers.getTrustManagers())
				.filter(t -> t instanceof X509ExtendedTrustManager)
				.findFirst()
				.orElseThrow();
		if (!Utilities.isEmpty(crlFile)) {
			base = new RevokingTrustManager(base, new Crl(Path.of(crlFile.split(",")[0].trim()), roots));
			logger.info("Broker certificates are checked against the CRL {}", crlFile);
		}

		final SSLContext result = SSLContext.getInstance("TLS");
		result.init(keyManagers.getKeyManagers(), new TrustManager[] { base }, new SecureRandom());
		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private static KeyStore load(final Resource resource, final String type, final char[] password) throws Exception {
		final KeyStore store = KeyStore.getInstance(type);
		try (InputStream in = resource.getInputStream()) {
			store.load(in, password);
		}

		return store;
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	// CRL of the cloud CA, re-read when the file changes. Fail closed for certificates of that CA: no readable CRL, a CRL with a
	// bad signature or past its nextUpdate refuses them (the brokers behave the same). Other issuers are not checked.
	static final class Crl {

		private final Logger logger = LogManager.getLogger(getClass());
		private final Path file;
		private final List<X509Certificate> issuers = new ArrayList<>();
		private X509CRL crl;
		private FileTime loaded;
		private String problem = "no CRL loaded";

		//-------------------------------------------------------------------------------------------------
		Crl(final Path file, final KeyStore roots) throws Exception {
			this.file = file;
			for (final String alias : Collections.list(roots.aliases())) {
				if (roots.getCertificate(alias) instanceof X509Certificate certificate) {
					issuers.add(certificate);
				}
			}
		}

		//-------------------------------------------------------------------------------------------------
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

		//-------------------------------------------------------------------------------------------------
		private void refresh() {
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
	}

	//-------------------------------------------------------------------------------------------------
	static final class RevokingTrustManager extends X509ExtendedTrustManager {

		private final X509ExtendedTrustManager base;
		private final Crl crl;

		//-------------------------------------------------------------------------------------------------
		RevokingTrustManager(final X509ExtendedTrustManager base, final Crl crl) {
			this.base = base;
			this.crl = crl;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			base.checkClientTrusted(chain, authType);
			revocation(chain);
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void checkClientTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
			base.checkClientTrusted(chain, authType, socket);
			revocation(chain);
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void checkClientTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
			base.checkClientTrusted(chain, authType, engine);
			revocation(chain);
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			base.checkServerTrusted(chain, authType);
			revocation(chain);
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
			base.checkServerTrusted(chain, authType, socket);
			revocation(chain);
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
			base.checkServerTrusted(chain, authType, engine);
			revocation(chain);
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return base.getAcceptedIssuers();
		}

		//-------------------------------------------------------------------------------------------------
		private void revocation(final X509Certificate[] chain) throws CertificateException {
			if (chain.length > 0) {
				crl.check(chain[0]);
			}
		}
	}
}
