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
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.gatetunnel.service.FederationSettings;

// TLS contexts with the system identity of server.ssl.* (the PKCS12 key and trust stores of the web server) for the
// connections Spring's web server does not handle: the inter-cloud consumer tunnel listeners and the calls to providers.
@Component
public class SystemTls {

	//=================================================================================================
	// members

	@Autowired
	private SSLProperties sslProperties;

	@Autowired
	private FederationSettings settings;

	private volatile KeyStore keyStore;
	private volatile KeyStore trustStore;
	private volatile SSLContext server;
	private final Map<String, SSLContext> pinned = new ConcurrentHashMap<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// For listeners: no pinning; client certificates are checked against the trust store and the CRL.
	public SSLContext serverContext() throws Exception {
		SSLContext result = server;
		if (result == null) {
			load();
			result = TlsSupport.storeContext(keyStore, sslProperties.getKeyAlias(), password(sslProperties.getKeyPassword()), trustStore, settings.getCrlFile(), null);
			server = result;
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	// For calls to a system: its certificate must carry the expected CN and the system profile.
	public SSLContext clientContext(final String expectedCn) throws Exception {
		final SSLContext known = pinned.get(expectedCn);
		if (known != null) {
			return known;
		}

		load();
		final SSLContext created = TlsSupport.storeContext(keyStore, sslProperties.getKeyAlias(), password(sslProperties.getKeyPassword()), trustStore, settings.getCrlFile(), expectedCn);
		pinned.put(expectedCn, created);

		return created;
	}

	//-------------------------------------------------------------------------------------------------
	public X509Certificate certificate() throws Exception {
		load();
		return (X509Certificate) keyStore.getCertificate(sslProperties.getKeyAlias());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private synchronized void load() throws Exception {
		if (keyStore != null) {
			return;
		}

		final KeyStore keys = KeyStore.getInstance(sslProperties.getKeyStoreType() == null ? "PKCS12" : sslProperties.getKeyStoreType());
		try (InputStream in = sslProperties.getKeyStore().getInputStream()) {
			keys.load(in, password(sslProperties.getKeyStorePassword()));
		}
		final KeyStore trust = KeyStore.getInstance("PKCS12");
		try (InputStream in = sslProperties.getTrustStore().getInputStream()) {
			trust.load(in, password(sslProperties.getTrustStorePassword()));
		}
		trustStore = trust;
		keyStore = keys;
	}

	//-------------------------------------------------------------------------------------------------
	private static char[] password(final String value) {
		return value == null ? new char[0] : value.toCharArray();
	}
}
