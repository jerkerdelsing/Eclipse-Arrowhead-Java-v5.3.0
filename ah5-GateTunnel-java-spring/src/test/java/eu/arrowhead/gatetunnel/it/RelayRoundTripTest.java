/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.it;

import static eu.arrowhead.gatetunnel.service.Wire.m;
import static eu.arrowhead.gatetunnel.service.Wire.obj;
import static eu.arrowhead.gatetunnel.service.Wire.str;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.ObjectProvider;

import eu.arrowhead.gatetunnel.security.IdentityTokens;
import eu.arrowhead.gatetunnel.security.SystemTls;
import eu.arrowhead.gatetunnel.service.FederationSettings;
import eu.arrowhead.gatetunnel.service.InterCloudSessionService;
import eu.arrowhead.gatetunnel.service.ProviderClient;
import eu.arrowhead.gatetunnel.service.RelayClient;
import eu.arrowhead.gatetunnel.service.SessionSecurity;
import eu.arrowhead.gatetunnel.service.SessionSecurityTest;

// AH-GT-1 over a real relay broker (MQTT over mutual TLS): the GoCloud GateTunnel (provider side, the Spring service with
// its relay bean) and the TestCloud GateTunnel's consumer half, each with its own relay identity. The consumer half is
// driven step by step exactly as InterCloudSessionService.forward does it. Run with
// -Dgt.it.relay.host=<broker> -Dgt.it.relay.ca=<bundle: org CA + broker CA>; skipped otherwise.
@EnabledIfSystemProperty(named = "gt.it.relay.host", matches = ".+")
public class RelayRoundTripTest {

	//=================================================================================================
	// members

	private static final Path DIR = Path.of("src/test/resources/ahgt1");
	private static final String CONSUMER_GT = "GateTunnel.TestCloud.Company.arrowhead.eu";
	private static final String PROVIDER_GT = "GateTunnel.GoCloud.Company.arrowhead.eu";

	private RelayClient providerRelay;
	private RelayClient consumerRelay;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@AfterEach
	public void close() {
		if (providerRelay != null) {
			providerRelay.close();
		}
		if (consumerRelay != null) {
			consumerRelay.close();
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static FederationSettings settings(final String cloud, final String neighbor) {
		return new FederationSettings(cloud, "Company", "ah-gt-1", neighbor, "", "gatetunnel", 43200, 3600, 120, 8200, 8299, 8000, 8099,
				System.getProperty("gt.it.relay.host"), Integer.parseInt(System.getProperty("gt.it.relay.port", "8883")),
				DIR.resolve(cloud + "-relay-chain.crt").toString(), DIR.resolve(cloud + "-relay.key").toString(), System.getProperty("gt.it.relay.ca"), "", "", System.getProperty("gt.it.relay.crl", "org.crl"));
	}

	//-------------------------------------------------------------------------------------------------
	private static void awaitConnected(final RelayClient relay) throws Exception {
		relay.connect();
		for (int i = 0; i < 50 && !relay.isConnected(); i++) {
			Thread.sleep(100);
		}
		assertTrue(relay.isConnected());
		Thread.sleep(500); // inbox subscription after connectComplete
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void sealedRequestTravelsTheRelayAndIsExecutedOnceAtTheProvider() throws Exception {
		// provider side: GoCloud
		final FederationSettings goSettings = settings("GoCloud", "TestCloud");
		providerRelay = new RelayClient(goSettings);
		final ProviderClient providerClient = mock(ProviderClient.class);
		final byte[] providerBody = "{\"stored\":true}".getBytes(StandardCharsets.UTF_8);
		when(providerClient.call(eq("DataManager"), any(), eq("PUT"), any(), any(), any())).thenReturn(new ProviderClient.Response(201, providerBody, "application/json"));
		final SessionSecurity goSecurity = new SessionSecurity(DIR.resolve("GoCloud-relay.key"), DIR.resolve("GoCloud-relay-chain.crt"), DIR.resolve("org-ca.crt"), SessionSecurityTest.NO_CRL);
		@SuppressWarnings("unchecked")
		final ObjectProvider<SessionSecurity> goProvider = mock(ObjectProvider.class);
		when(goProvider.getIfAvailable()).thenReturn(goSecurity);
		final InterCloudSessionService provider = new InterCloudSessionService(goSettings, providerRelay, providerClient, mock(SystemTls.class), mock(IdentityTokens.class), goProvider);
		provider.bindRelay();

		// consumer side: TestCloud (relay link and AH-GT-1 identity)
		final FederationSettings testSettings = settings("TestCloud", "GoCloud");
		consumerRelay = new RelayClient(testSettings);
		consumerRelay.bind(testSettings::trustedPeer, (peer, message) -> m("accepted", false, "error", "consumer side"));
		final SessionSecurity testSecurity = new SessionSecurity(DIR.resolve("TestCloud-relay.key"), DIR.resolve("TestCloud-relay-chain.crt"), DIR.resolve("org-ca.crt"), SessionSecurityTest.NO_CRL);

		awaitConnected(providerRelay);
		awaitConnected(consumerRelay);

		// the local Gatekeeper of GoCloud opens the provider session (ICN), handing out the signed ephemeral key
		final String sessionId = "relay-it-session";
		final Map<String, Object> opened = provider.openProvider(m("sessionId", sessionId, "consumer", "RandomDataProducer", "provider", "DataManager",
				"serviceDefinition", "dataHistorian", "consumerCloud", "TestCloud|Company", "providerCloud", "GoCloud|Company", "peer", CONSUMER_GT,
				"target", m("address", "datamanager", "port", 8443, "basePath", "/datamanager/data-historian", "operations", m("put-data", m("method", "PUT", "path", "/put-data")))));
		final Map<String, Object> exchange = obj(opened.get("keyExchange"));

		// consumer GateTunnel: verify the provider's half, add its own, derive the keys (as openConsumer)
		final Map<String, Object> params = new LinkedHashMap<>(m("sessionId", sessionId, "consumer", "RandomDataProducer", "consumerCloud", "TestCloud|Company",
				"provider", "DataManager", "serviceDefinition", "dataHistorian", "consumerGateTunnel", CONSUMER_GT, "providerGateTunnel", PROVIDER_GT, "providerKey", exchange.get("providerKey")));
		testSecurity.verifyPeer(exchange.get("certificateChain"), PROVIDER_GT, exchange.get("signature"), SessionSecurity.transcript("provider", params));
		final KeyPair own = SessionSecurity.ephemeral();
		params.put("consumerKey", SessionSecurity.raw(own.getPublic()));
		final Map<String, Object> ownExchange = testSecurity.exchange("consumer", params);
		final Map<String, byte[]> keys = SessionSecurity.derive(own.getPrivate(), exchange.get("providerKey"), sessionId);

		// a sealed request (as forward)
		final byte[] requestBody = "{\"value\":21.5}".getBytes(StandardCharsets.UTF_8);
		final Map<String, Object> content = m("method", "PUT", "path", "/datamanager/data-historian/put-data", "contentType", "application/json",
				"body", Base64.getEncoder().encodeToString(requestBody));
		final Map<String, Object> message = new LinkedHashMap<>(m("type", "http-request", "sessionId", sessionId, "keyExchange", ownExchange));
		message.putAll(SessionSecurity.seal(keys.get("c2p"), sessionId, "c2p", 1, content));

		final Map<String, Object> sealedReply = consumerRelay.request("GoCloud", new LinkedHashMap<>(message));
		assertEquals(PROVIDER_GT, sealedReply.get("senderCN"));
		final SessionSecurity.Opened reply = SessionSecurity.open(keys.get("p2c"), sessionId, "p2c", sealedReply, 120);
		assertEquals(1, reply.sequence());
		assertEquals(201, ((Number) reply.content().get("status")).intValue());
		assertArrayEquals(providerBody, Base64.getDecoder().decode(str(reply.content().get("body"))));
		verify(providerClient).call(eq("DataManager"), eq("https://datamanager:8443/datamanager/data-historian/put-data"), eq("PUT"), eq("application/json"), eq(requestBody),
				eq("RandomDataProducer|TestCloud|Company"));

		// the same sealed request again (new relay message id): refused as a replay, nothing executed
		final Map<String, Object> replayed = consumerRelay.request("GoCloud", new LinkedHashMap<>(message));
		assertNull(replayed.get("ciphertext"));
		assertEquals("replayed or outdated request", replayed.get("error"));

		// an operation outside the negotiated session is answered 403 by the provider GateTunnel
		final Map<String, Object> outside = new LinkedHashMap<>(m("type", "http-request", "sessionId", sessionId, "keyExchange", ownExchange));
		outside.putAll(SessionSecurity.seal(keys.get("c2p"), sessionId, "c2p", 2, m("method", "DELETE", "path", "/datamanager/data-historian/put-data", "body", "")));
		final SessionSecurity.Opened refused = SessionSecurity.open(keys.get("p2c"), sessionId, "p2c", consumerRelay.request("GoCloud", outside), 120);
		assertEquals(403, ((Number) refused.content().get("status")).intValue());

		// a legacy (unsealed) request is not executed in the ah-gt-1 profile
		final Map<String, Object> legacy = consumerRelay.request("GoCloud", new LinkedHashMap<>(m("type", "http-request", "sessionId", sessionId, "method", "PUT",
				"path", "/datamanager/data-historian/put-data", "body", "")));
		assertEquals("AH-GT-1 is required; a legacy request is not executed", legacy.get("error"));
	}
}
