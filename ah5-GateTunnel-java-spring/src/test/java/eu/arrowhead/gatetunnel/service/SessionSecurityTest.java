/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service;

import static eu.arrowhead.gatetunnel.service.Wire.bytes;
import static eu.arrowhead.gatetunnel.service.Wire.integer;
import static eu.arrowhead.gatetunnel.service.Wire.m;
import static eu.arrowhead.gatetunnel.service.Wire.obj;
import static eu.arrowhead.gatetunnel.service.Wire.parse;
import static eu.arrowhead.gatetunnel.service.Wire.str;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// AH-GT-1 (J5): wire compatibility with the reference vector (src/test/resources/ahgt1/make_vector.py), key-exchange
// verification, sealing, and replay protection. Ported from the 5.2.2 native test.
public class SessionSecurityTest {

	//=================================================================================================
	// members

	static final Path DIR = Path.of("src/test/resources/ahgt1");
	static final String PROVIDER_CN = "GateTunnel.GoCloud.Company.arrowhead.eu";
	static final String CONSUMER_CN = "GateTunnel.TestCloud.Company.arrowhead.eu";
	// the vector's timestamps are from its creation
	static final int VECTOR_SKEW = 1_000_000_000;
	// the reference vector's PKI has no CA keys left to issue CRLs; revocation is tested with ahgt1/revocation
	public static final SessionSecurity.RevocationCheck NO_CRL = (certificate, issuer) -> { };
	static final Path REVOCATION = DIR.resolve("revocation");
	static final String PEER_CN = "GateTunnel.PeerCloud.Company.arrowhead.eu";

	private Map<String, Object> vector;
	private SessionSecurity consumer;
	private SessionSecurity provider;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void load() throws Exception {
		vector = obj(parse(Files.readString(DIR.resolve("vector.json"))));
		consumer = new SessionSecurity(DIR.resolve("TestCloud-relay.key"), DIR.resolve("TestCloud-relay-chain.crt"), DIR.resolve("org-ca.crt"), NO_CRL);
		provider = new SessionSecurity(DIR.resolve("GoCloud-relay.key"), DIR.resolve("GoCloud-relay-chain.crt"), DIR.resolve("org-ca.crt"), NO_CRL);
	}

	//-------------------------------------------------------------------------------------------------
	static PrivateKey x25519(final Object raw) throws Exception {
		final byte[] prefix = HexFormat.of().parseHex("302e020100300506032b656e04220420");
		final byte[] key = Base64.getDecoder().decode(str(raw));
		final byte[] der = new byte[prefix.length + 32];
		System.arraycopy(prefix, 0, der, 0, prefix.length);
		System.arraycopy(key, 0, der, prefix.length, 32);

		return KeyFactory.getInstance("X25519").generatePrivate(new PKCS8EncodedKeySpec(der));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void verifiesTheReferenceKeyExchangeAndRefusesWrongPeersOrTranscripts() throws Exception {
		final Map<String, Object> session = obj(vector.get("session"));
		final Map<String, Object> pe = obj(vector.get("providerExchange"));
		final Map<String, Object> ce = obj(vector.get("consumerExchange"));
		consumer.verifyPeer(pe.get("certificateChain"), PROVIDER_CN, pe.get("signature"), SessionSecurity.transcript("provider", session));
		provider.verifyPeer(ce.get("certificateChain"), CONSUMER_CN, ce.get("signature"), SessionSecurity.transcript("consumer", session));
		assertThrows(SessionSecurity.SecurityError.class, () -> consumer.verifyPeer(pe.get("certificateChain"), CONSUMER_CN, pe.get("signature"), SessionSecurity.transcript("provider", session)));

		final Map<String, Object> tampered = new LinkedHashMap<>(session);
		tampered.put("provider", "OtherProvider");
		assertThrows(SessionSecurity.SecurityError.class, () -> consumer.verifyPeer(pe.get("certificateChain"), PROVIDER_CN, pe.get("signature"), SessionSecurity.transcript("provider", tampered)));

		// a chain whose relay certificate is outside its cross-certificate's name constraint: GoCloud's leaf under TestCloud's CA
		final String goChain = Files.readString(DIR.resolve("GoCloud-relay-chain.crt"));
		final String testChain = Files.readString(DIR.resolve("TestCloud-relay-chain.crt"));
		final String mixed = goChain.substring(0, goChain.indexOf("-----END CERTIFICATE-----") + 26) + testChain.substring(testChain.indexOf("-----END CERTIFICATE-----") + 26);
		assertThrows(SessionSecurity.SecurityError.class, () -> consumer.verifyPeer(mixed, PROVIDER_CN, pe.get("signature"), SessionSecurity.transcript("provider", session)));

		// an own signature verifies for the peer
		final String own = provider.sign(SessionSecurity.transcript("provider", session));
		consumer.verifyPeer(pe.get("certificateChain"), PROVIDER_CN, own, SessionSecurity.transcript("provider", session));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void derivesTheReferenceKeysAndOpensItsMessages() throws Exception {
		final Map<String, Object> session = obj(vector.get("session"));
		final String id = str(session.get("sessionId"));
		final Map<String, byte[]> fromConsumer = SessionSecurity.derive(x25519(vector.get("consumerEphemeralPrivate")), session.get("providerKey"), id);
		final Map<String, byte[]> fromProvider = SessionSecurity.derive(x25519(vector.get("providerEphemeralPrivate")), session.get("consumerKey"), id);
		for (final String direction : List.of("c2p", "p2c")) {
			assertArrayEquals(Base64.getDecoder().decode(str(vector.get(direction + "Key"))), fromConsumer.get(direction));
			assertArrayEquals(fromConsumer.get(direction), fromProvider.get(direction));
		}

		final SessionSecurity.Opened request = SessionSecurity.open(fromProvider.get("c2p"), id, "c2p", obj(vector.get("request")), VECTOR_SKEW);
		assertEquals(7, request.sequence());
		assertEquals("/datamanager/data-historian/put-data", request.content().get("path"));
		final SessionSecurity.Opened reply = SessionSecurity.open(fromConsumer.get("p2c"), id, "p2c", obj(vector.get("reply")), VECTOR_SKEW);
		assertEquals(201, integer(reply.content().get("status"), 0));

		// wrong direction or session, or a changed ciphertext, does not open
		assertThrows(SessionSecurity.SecurityError.class, () -> SessionSecurity.open(fromProvider.get("c2p"), id, "p2c", obj(vector.get("request")), VECTOR_SKEW));
		assertThrows(SessionSecurity.SecurityError.class, () -> SessionSecurity.open(fromProvider.get("c2p"), "other-session", "c2p", obj(vector.get("request")), VECTOR_SKEW));
		final Map<String, Object> changed = new LinkedHashMap<>(obj(vector.get("request")));
		final String c = str(changed.get("ciphertext"));
		changed.put("ciphertext", (c.charAt(0) == 'A' ? "B" : "A") + c.substring(1));
		assertThrows(SessionSecurity.SecurityError.class, () -> SessionSecurity.open(fromProvider.get("c2p"), id, "c2p", changed, VECTOR_SKEW));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void sealedMessagesRoundTripAndStaleOrReplayedOnesAreRefused() throws Exception {
		final KeyPair a = SessionSecurity.ephemeral();
		final KeyPair b = SessionSecurity.ephemeral();
		final Map<String, byte[]> ka = SessionSecurity.derive(a.getPrivate(), SessionSecurity.raw(b.getPublic()), "s1");
		final Map<String, byte[]> kb = SessionSecurity.derive(b.getPrivate(), SessionSecurity.raw(a.getPublic()), "s1");
		final Map<String, Object> sealed = SessionSecurity.seal(ka.get("c2p"), "s1", "c2p", 3, m("method", "GET", "path", "/x"));
		assertEquals("/x", SessionSecurity.open(kb.get("c2p"), "s1", "c2p", obj(parse(bytes(sealed))), 120).content().get("path"));

		// a message outside the clock-skew window is refused (a negative window makes every message stale)
		assertThrows(SessionSecurity.SecurityError.class, () -> SessionSecurity.open(kb.get("c2p"), "s1", "c2p", obj(parse(bytes(sealed))), -1));

		final SessionSecurity.Replay replay = new SessionSecurity.Replay(4);
		assertTrue(replay.accept(1));
		assertTrue(replay.accept(3));
		assertFalse(replay.accept(3));
		assertTrue(replay.accept(2));
		assertFalse(replay.accept(0));
		assertTrue(replay.accept(9));
		assertFalse(replay.accept(4));
		assertTrue(replay.accept(6));
	}

	//-------------------------------------------------------------------------------------------------
	// SOS-4: the peer's own key exchange verified with the given CRL files of ahgt1/revocation; null if accepted
	static String verdict(final String... crls) throws Exception {
		final List<Path> files = java.util.Arrays.stream(crls).map(REVOCATION::resolve).toList();
		final SessionSecurity peer = new SessionSecurity(REVOCATION.resolve("peer-relay.key"), REVOCATION.resolve("peer-relay-chain.crt"), REVOCATION.resolve("org-ca.crt"), files);
		final Map<String, Object> session = new LinkedHashMap<>(m("sessionId", "revocation", "providerKey", "k"));
		final Map<String, Object> exchange = peer.exchange("provider", session);
		try {
			peer.verifyPeer(exchange.get("certificateChain"), PEER_CN, exchange.get("signature"), SessionSecurity.transcript("provider", session));

			return null;
		} catch (final SessionSecurity.SecurityError ex) {
			return ex.getMessage();
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void peerChainIsCheckedAgainstTheCloudAndOrganizationCrls() throws Exception {
		assertEquals(null, verdict("peer-ca.crl", "org-ca.crl"));
		assertEquals("peer certificate GateTunnel.PeerCloud.Company.arrowhead.eu is revoked", verdict("peer-ca-revoked.crl", "org-ca.crl"));
		assertEquals("peer certificate PeerCloud.Company.arrowhead.eu is revoked", verdict("peer-ca.crl", "org-ca-revoked.crl"));
		// a revoking CRL wins over an empty one of the same issuer
		assertEquals("peer certificate GateTunnel.PeerCloud.Company.arrowhead.eu is revoked", verdict("peer-ca.crl", "peer-ca-revoked.crl", "org-ca.crl"));
		assertEquals("no CRL of PeerCloud.Company.arrowhead.eu is configured", verdict("org-ca.crl"));
		assertEquals("no CRL of Company.arrowhead.eu is configured", verdict("peer-ca.crl"));
		assertEquals("no CRL of PeerCloud.Company.arrowhead.eu is configured", verdict("peer-ca-forged.crl", "org-ca.crl"));
		assertEquals("the CRL of PeerCloud.Company.arrowhead.eu is out of date", verdict("peer-ca-expired.crl", "org-ca.crl"));
		assertEquals(null, verdict("peer-ca-expired.crl", "peer-ca.crl", "org-ca.crl"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void crlFilesAreMandatoryAndAMissingCheckRefusesEveryPeer() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> SessionSecurity.crlRevocation(List.of()));
		assertThrows(Exception.class, () -> SessionSecurity.crlRevocation(List.of(REVOCATION.resolve("missing.crl"))));
		assertThrows(Exception.class, () -> SessionSecurity.crlRevocation(List.of(REVOCATION.resolve("org-ca.crt"))));
		final SessionSecurity unchecked = new SessionSecurity(REVOCATION.resolve("peer-relay.key"), REVOCATION.resolve("peer-relay-chain.crt"), REVOCATION.resolve("org-ca.crt"), (SessionSecurity.RevocationCheck) null);
		final Map<String, Object> session = new LinkedHashMap<>(m("sessionId", "revocation", "providerKey", "k"));
		final Map<String, Object> exchange = unchecked.exchange("provider", session);
		assertThrows(SessionSecurity.SecurityError.class, () -> unchecked.verifyPeer(exchange.get("certificateChain"), PEER_CN, exchange.get("signature"), SessionSecurity.transcript("provider", session)));
	}
}
