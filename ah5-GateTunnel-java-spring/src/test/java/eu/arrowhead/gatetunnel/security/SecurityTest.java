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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.quartz.OutsourcedLoginJob;
import eu.arrowhead.gatetunnel.TestPki;
import eu.arrowhead.gatetunnel.service.FederationSettings;

// Ported from the 5.2.2 SecurityTest. Mutual TLS and the binding of the identity token to the client certificate on the
// Spring port are covered by api.GateTunnelRouteContractTest; here: the TLS contexts outside Spring (pinning, mutual TLS),
// renewal of the own token, and federation settings that must refuse the start.
public class SecurityTest {

	//=================================================================================================
	// members

	@TempDir
	Path dir;

	private final List<AutoCloseable> close = new ArrayList<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@AfterEach
	public void cleanup() {
		for (final AutoCloseable c : close) {
			try {
				c.close();
			} catch (final Exception ignored) {
				// cleanup
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private HttpsServer server(final TestPki p, final TestPki.Pair identity, final AtomicInteger calls) throws Exception {
		final SSLContext tls = TlsSupport.pemContext(identity.cert().toString(), identity.key().toString(), p.root.cert().toString(), null, null);
		final HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 16);
		server.setHttpsConfigurator(new HttpsConfigurator(tls) {
			@Override
			public void configure(final HttpsParameters parameters) {
				final SSLParameters ssl = tls.getDefaultSSLParameters();
				ssl.setNeedClientAuth(true);
				parameters.setSSLParameters(ssl);
			}
		});
		server.createContext("/", x -> {
			calls.incrementAndGet();
			final byte[] body = "{\"token\":\"t\"}".getBytes(StandardCharsets.UTF_8);
			x.sendResponseHeaders(200, body.length);
			x.getResponseBody().write(body);
			x.close();
		});
		server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		server.start();
		close.add(() -> server.stop(0));

		return server;
	}

	//-------------------------------------------------------------------------------------------------
	private HttpResponse<String> get(final SSLContext context, final HttpsServer server) throws Exception {
		final HttpClient client = HttpClient.newBuilder().sslContext(context).connectTimeout(Duration.ofSeconds(5)).build();
		close.add(client);

		return client.send(HttpRequest.newBuilder(URI.create("https://127.0.0.1:" + server.getAddress().getPort() + "/x")).build(), HttpResponse.BodyHandlers.ofString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void pinnedPeerRejectsOtherTrustedSystemBeforeSendingAnything() throws Exception {
		final TestPki p = new TestPki(dir.resolve("pki"));
		final AtomicInteger calls = new AtomicInteger();
		final HttpsServer evil = server(p, p.issue("OtherSystem", "sy"), calls);
		final TestPki.Pair own = p.issue("GateTunnel", "sy");
		final String providerCn = "Provider.TestCloud.Company.arrowhead.eu";

		assertThrows(Exception.class, () -> get(TlsSupport.pemContext(own.cert().toString(), own.key().toString(), p.root.cert().toString(), null, providerCn), evil));
		assertEquals(0, calls.get());

		// the expected system is accepted
		final HttpsServer provider = server(p, p.issue("Provider", "sy"), calls);
		assertEquals(200, get(TlsSupport.pemContext(own.cert().toString(), own.key().toString(), p.root.cert().toString(), null, providerCn), provider).statusCode());
		// a provider with an operator certificate is not a system
		final HttpsServer operator = server(p, p.issue("Provider", "op"), calls);
		assertThrows(Exception.class, () -> get(TlsSupport.pemContext(own.cert().toString(), own.key().toString(), p.root.cert().toString(), null, providerCn), operator));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void listenersRequireAClientCertificate() throws Exception {
		final TestPki p = new TestPki(dir.resolve("pki"));
		final HttpsServer server = server(p, p.issue("GateTunnel", "sy"), new AtomicInteger());
		final java.security.KeyStore trust = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
		trust.load(null, null);
		trust.setCertificateEntry("root", p.root.certificate());
		final javax.net.ssl.TrustManagerFactory tm = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
		tm.init(trust);
		final SSLContext noCertificate = SSLContext.getInstance("TLS");
		noCertificate.init(null, tm.getTrustManagers(), null);

		assertThrows(Exception.class, () -> get(noCertificate, server));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void certificateProfileIsReadFromTheDnQualifier() throws Exception {
		final TestPki p = new TestPki(dir.resolve("pki"));
		assertEquals("sy", TlsSupport.attribute(p.issue("GateTunnel", "sy").certificate(), "DNQ"));
		assertEquals("on", TlsSupport.attribute(p.issue("Device", "on").certificate(), "DNQ"));
		assertEquals("GateTunnel.TestCloud.Company.arrowhead.eu", TlsSupport.cn(p.issue("GateTunnel", "sy").certificate()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void concurrentRejectedTokensRenewOnlyOnce() throws Exception {
		final Map<String, Object> context = new ConcurrentHashMap<>();
		context.put(Constants.KEY_IDENTITY_TOKEN, "fixture-token-1");
		final SystemInfo sysInfo = mock(SystemInfo.class);
		when(sysInfo.getArrowheadContext()).thenReturn(context);
		when(sysInfo.getIdentityToken()).thenAnswer(i -> (String) context.get(Constants.KEY_IDENTITY_TOKEN));
		final AtomicInteger logins = new AtomicInteger(1);
		final OutsourcedLoginJob job = mock(OutsourcedLoginJob.class);
		doAnswer(i -> {
			synchronized (context) {
				if (!context.containsKey(Constants.KEY_IDENTITY_RENEWAL_THRESHOLD)) {
					context.put(Constants.KEY_IDENTITY_TOKEN, "fixture-token-" + logins.incrementAndGet());
					context.put(Constants.KEY_IDENTITY_RENEWAL_THRESHOLD, "later");
				}
			}
			return null;
		}).when(job).loginIfNeeded();
		@SuppressWarnings("unchecked")
		final ObjectProvider<OutsourcedLoginJob> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(job);

		final IdentityTokens tokens = new IdentityTokens();
		ReflectionTestUtils.setField(tokens, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(tokens, "loginJob", provider);

		assertEquals("fixture-token-1", tokens.current());
		final CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
			final List<Future<?>> tasks = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				tasks.add(workers.submit(() -> {
					start.await();
					tokens.invalidate("fixture-token-1");
					return null;
				}));
			}
			start.countDown();
			for (final Future<?> task : tasks) {
				task.get(10, TimeUnit.SECONDS);
			}
		}

		assertEquals(2, logins.get(), "Concurrent 401 responses must not invalidate another request's newly refreshed token");
		assertEquals("fixture-token-2", tokens.current());
	}

	//-------------------------------------------------------------------------------------------------
	static FederationSettings settings(final String profile, final String peers) {
		return new FederationSettings("TestCloud", "Company", profile, "GoCloud,RustDev", peers, "gatetunnel", 43200, 3600, 120, 8200, 8299, 8000, 8099, "relay", 8883,
				"chain", "key", "ca", "", "", "org.crl");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void federationSettingsRefuseUnknownProfilesAndOwnRemapping() {
		assertThrows(IllegalArgumentException.class, () -> settings("plain", ""));
		assertThrows(IllegalArgumentException.class, () -> settings("ah-gt-1", "TestCloud=Other"));
		final FederationSettings settings = settings("ah-gt-1", "RustDev=Lab");
		assertEquals(true, settings.trustedPeer("GateTunnel.GoCloud.Company.arrowhead.eu"));
		assertEquals(true, settings.trustedPeer("GateTunnel.RustDev.Lab.arrowhead.eu"));
		assertEquals(false, settings.trustedPeer("GateTunnel.RustDev.Company.arrowhead.eu"), "explicit binding required");
		assertEquals(false, settings.trustedPeer("GateTunnel.TestCloud.Company.arrowhead.eu"), "own cloud");
		assertEquals(false, settings.trustedPeer("GateTunnel.Other.Company.arrowhead.eu"), "not a neighbour");
		assertEquals(false, settings.trustedPeer("Gatekeeper.GoCloud.Company.arrowhead.eu"), "not a GateTunnel");
	}
}
