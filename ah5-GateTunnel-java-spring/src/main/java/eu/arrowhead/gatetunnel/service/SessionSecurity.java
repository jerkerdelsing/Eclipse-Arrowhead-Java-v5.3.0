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
import static eu.arrowhead.gatetunnel.service.Wire.m;
import static eu.arrowhead.gatetunnel.service.Wire.obj;
import static eu.arrowhead.gatetunnel.service.Wire.parse;
import static eu.arrowhead.gatetunnel.service.Wire.str;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import eu.arrowhead.gatetunnel.security.TlsSupport;

// AH-GT-1: end-to-end protection of inter-cloud GateTunnel sessions (security finding J5). Ported verbatim in behaviour from
// the 5.2.2 native implementation; wire-compatible with the Go relay and the Rust implementation.
//
// Key exchange, once per session, carried by the Gatekeepers' ICN messages and the first tunnel request: each GateTunnel
// makes an ephemeral X25519 key and signs a transcript of the session parameters and the ephemeral key(s) with its relay
// key (RSA-PSS, SHA-256, salt 32). The signature comes with the relay chain (relay certificate + cross-certificate of its
// cloud CA), verified against the organization CA, the cross-certificate's name constraint and the expected peer name.
// Both sides derive one AES-256-GCM key per direction (HKDF-SHA256, salt = session id). Every relayed request and reply
// is then sealed (AAD = protocol, session, direction, sequence); requests carry a sequence number and a timestamp, so
// duplicates and stale messages are refused, and a reply must carry the sequence number of its request. The relay sees
// only ciphertext; the ephemeral keys give forward secrecy.
//
// Revocation (security finding SOS-4, 2026-11-10): after the path validates, the relay certificate must not be listed by
// a current CRL of its cloud CA, and the cross-certificate not by a current CRL of the organization CA. The CRL files
// (gatetunnel.relay.crl.files) are read again for every key exchange; a missing, forged or out-of-date CRL refuses it.
public class SessionSecurity {

	//=================================================================================================
	// members

	public static final String PROTOCOL = "AH-GT-1";

	private static final byte[] X25519_PREFIX = HexFormat.of().parseHex("302a300506032b656e032100");
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int KEY_LENGTH = 32;
	private static final int NONCE_LENGTH = 12;
	private static final int TAG_BITS = 128;
	private static final int PSS_SALT = 32;

	private final PrivateKey key;
	private final String chainPem;
	private final X509Certificate orgCa;
	private final RevocationCheck revocation;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// The CRL files must name at least one file; each must hold readable CRLs at start.
	public SessionSecurity(final Path keyFile, final Path chainFile, final Path orgCaFile, final List<Path> crlFiles) throws Exception {
		this(keyFile, chainFile, orgCaFile, crlRevocation(crlFiles));
	}

	//-------------------------------------------------------------------------------------------------
	// With another revocation check (tests whose fixture PKI has no CA keys). A null check refuses every peer.
	public SessionSecurity(final Path keyFile, final Path chainFile, final Path orgCaFile, final RevocationCheck revocation) throws Exception {
		this.key = TlsSupport.key(keyFile);
		this.chainPem = Files.readString(chainFile);
		this.orgCa = TlsSupport.cert(orgCaFile);
		this.revocation = revocation;
	}

	//-------------------------------------------------------------------------------------------------
	public static RevocationCheck crlRevocation(final List<Path> crlFiles) throws Exception {
		if (crlFiles == null || crlFiles.isEmpty()) {
			throw new IllegalArgumentException("AH-GT-1 needs the CRL files of the organization CA and the peer cloud CAs");
		}
		final List<Path> files = List.copyOf(crlFiles);
		for (final Path file : files) {
			if (crls(file).isEmpty()) {
				throw new IllegalArgumentException("No CRL in " + file);
			}
		}

		return (certificate, issuer) -> {
			final List<X509CRL> all = new ArrayList<>();
			for (final Path file : files) {
				try {
					all.addAll(crls(file));
				} catch (final Exception ex) {
					throw new SecurityError("CRL file " + file + " is unreadable", ex);
				}
			}
			checkRevocation(certificate, issuer, all, new Date());
		};
	}

	//-------------------------------------------------------------------------------------------------
	// The certificate must not be listed by any current CRL that the issuer signed, and at least one such CRL must exist.
	public static void checkRevocation(final X509Certificate certificate, final X509Certificate issuer, final List<X509CRL> crls, final Date now) throws SecurityError {
		boolean signed = false;
		boolean current = false;
		for (final X509CRL crl : crls) {
			if (!crl.getIssuerX500Principal().equals(issuer.getSubjectX500Principal())) {
				continue;
			}
			try {
				crl.verify(issuer.getPublicKey());
			} catch (final Exception ex) {
				continue;
			}
			signed = true;
			if (crl.getThisUpdate().after(now) || crl.getNextUpdate() == null || crl.getNextUpdate().before(now)) {
				continue;
			}
			current = true;
			if (crl.isRevoked(certificate)) {
				throw new SecurityError("peer certificate " + TlsSupport.cn(certificate) + " is revoked");
			}
		}
		if (!signed) {
			throw new SecurityError("no CRL of " + TlsSupport.cn(issuer) + " is configured");
		}
		if (!current) {
			throw new SecurityError("the CRL of " + TlsSupport.cn(issuer) + " is out of date");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public String sign(final byte[] transcript) throws Exception {
		final Signature signature = pss();
		signature.initSign(key);
		signature.update(transcript);

		return Base64.getEncoder().encodeToString(signature.sign());
	}

	//-------------------------------------------------------------------------------------------------
	// The own half of the key exchange: ephemeral key, signature over the transcript, relay chain.
	public Map<String, Object> exchange(final String role, final Map<String, Object> session) throws Exception {
		return m(role + "Key", session.get(role + "Key"), "signature", sign(transcript(role, session)), "certificateChain", chainPem);
	}

	//-------------------------------------------------------------------------------------------------
	// Verifies relay certificate -> cross-certificate -> organization CA (PKIX: signatures, validity, CA flags and the
	// directory-name constraint), the expected peer name and profile, and the signature over the transcript.
	public void verifyPeer(final Object chain, final String expectedCn, final Object signature, final byte[] transcript) throws SecurityError {
		try {
			final CertificateFactory factory = CertificateFactory.getInstance("X.509");
			final List<X509Certificate> certificates = new ArrayList<>();
			for (final Certificate certificate : factory.generateCertificates(new ByteArrayInputStream(str(chain).getBytes(StandardCharsets.US_ASCII)))) {
				certificates.add((X509Certificate) certificate);
			}
			if (certificates.size() != 2) {
				throw new SecurityError("peer chain must be relay certificate + cross-certificate");
			}

			final PKIXParameters parameters = new PKIXParameters(Set.of(new TrustAnchor(orgCa, null)));
			parameters.setRevocationEnabled(false);
			try {
				CertPathValidator.getInstance("PKIX").validate(factory.generateCertPath(certificates), parameters);
			} catch (final CertPathValidatorException ex) {
				throw new SecurityError("peer chain does not verify: " + ex.getMessage(), ex);
			}

			final X509Certificate leaf = certificates.getFirst();
			if (revocation == null) {
				throw new SecurityError("no revocation check is configured");
			}
			revocation.check(leaf, certificates.get(1));
			revocation.check(certificates.get(1), orgCa);
			if (leaf.getBasicConstraints() >= 0) {
				throw new SecurityError("peer relay certificate must not be a CA");
			}
			if (!TlsSupport.cn(leaf).equals(expectedCn)) {
				throw new SecurityError("peer relay certificate is not " + expectedCn);
			}
			if (!"sy".equals(TlsSupport.attribute(leaf, "DNQ"))) {
				throw new SecurityError("peer relay certificate is not a system (sy) certificate");
			}

			final Signature verifier = pss();
			verifier.initVerify(leaf.getPublicKey());
			verifier.update(transcript);
			if (!verifier.verify(Base64.getDecoder().decode(str(signature)))) {
				throw new SecurityError("peer key-exchange signature is invalid");
			}
		} catch (final SecurityError ex) {
			throw ex;
		} catch (final Exception ex) {
			throw new SecurityError("peer key exchange is malformed", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Canonical bytes both sides sign: protocol, role, session parameters and the ephemeral key(s) so far.
	public static byte[] transcript(final String role, final Map<String, Object> session) {
		final List<String> fields = new ArrayList<>(List.of(
				PROTOCOL,
				role,
				str(session.get("sessionId")),
				str(session.get("consumer")),
				str(session.get("consumerCloud")),
				str(session.get("provider")),
				str(session.get("serviceDefinition")),
				str(session.get("consumerGateTunnel")),
				str(session.get("providerGateTunnel")),
				str(session.get("providerKey"))));
		if ("consumer".equals(role)) {
			fields.add(str(session.get("consumerKey")));
		}

		return String.join("\n", fields).getBytes(StandardCharsets.UTF_8);
	}

	//-------------------------------------------------------------------------------------------------
	public static KeyPair ephemeral() throws Exception {
		return KeyPairGenerator.getInstance("X25519").generateKeyPair();
	}

	//-------------------------------------------------------------------------------------------------
	// The raw 32-byte X25519 public key, Base64.
	public static String raw(final PublicKey key) {
		final byte[] encoded = key.getEncoded();
		return Base64.getEncoder().encodeToString(Arrays.copyOfRange(encoded, encoded.length - KEY_LENGTH, encoded.length));
	}

	//-------------------------------------------------------------------------------------------------
	public static Map<String, byte[]> derive(final PrivateKey own, final Object peerPublic, final String sessionId) throws SecurityError {
		try {
			final byte[] rawPeer = Base64.getDecoder().decode(str(peerPublic));
			if (rawPeer.length != KEY_LENGTH) {
				throw new SecurityError("peer key is not an X25519 public key");
			}

			final byte[] encoded = new byte[X25519_PREFIX.length + KEY_LENGTH];
			System.arraycopy(X25519_PREFIX, 0, encoded, 0, X25519_PREFIX.length);
			System.arraycopy(rawPeer, 0, encoded, X25519_PREFIX.length, KEY_LENGTH);

			final KeyAgreement agreement = KeyAgreement.getInstance("X25519");
			agreement.init(own);
			agreement.doPhase(KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(encoded)), true);
			final byte[] shared = agreement.generateSecret();

			final Map<String, byte[]> keys = new HashMap<>();
			for (final String direction : List.of("c2p", "p2c")) {
				keys.put(direction, hkdf(shared, sessionId.getBytes(StandardCharsets.UTF_8), (PROTOCOL + " " + direction).getBytes(StandardCharsets.UTF_8)));
			}

			return keys;
		} catch (final SecurityError ex) {
			throw ex;
		} catch (final Exception ex) {
			throw new SecurityError("key agreement failed", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// HKDF-SHA256 (RFC 5869) with a 32-byte output: one expand block.
	public static byte[] hkdf(final byte[] ikm, final byte[] salt, final byte[] info) throws Exception {
		final Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(salt, "HmacSHA256"));
		final byte[] prk = mac.doFinal(ikm);
		mac.init(new SecretKeySpec(prk, "HmacSHA256"));
		mac.update(info);
		mac.update((byte) 1);

		return mac.doFinal();
	}

	//-------------------------------------------------------------------------------------------------
	public static byte[] aad(final String sessionId, final String direction, final long sequence) {
		return (PROTOCOL + "|" + sessionId + "|" + direction + "|" + sequence).getBytes(StandardCharsets.UTF_8);
	}

	//-------------------------------------------------------------------------------------------------
	public static Map<String, Object> seal(final byte[] key, final String sessionId, final String direction, final long sequence, final Map<String, Object> content) throws Exception {
		final byte[] nonce = new byte[NONCE_LENGTH];
		RANDOM.nextBytes(nonce);
		final Map<String, Object> plain = new LinkedHashMap<>(content);
		plain.put("timestamp", System.currentTimeMillis() / 1000.0);

		final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
		cipher.updateAAD(aad(sessionId, direction, sequence));

		return m("seq", sequence, "nonce", Base64.getEncoder().encodeToString(nonce), "ciphertext", Base64.getEncoder().encodeToString(cipher.doFinal(bytes(plain))));
	}

	//-------------------------------------------------------------------------------------------------
	// Opens a sealed envelope; its timestamp must lie within clockSkewSeconds of now.
	public static Opened open(final byte[] key, final String sessionId, final String direction, final Map<String, Object> envelope, final int clockSkewSeconds) throws SecurityError {
		final long sequence;
		final Map<String, Object> content;
		try {
			sequence = ((Number) envelope.get("seq")).longValue();
			final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, Base64.getDecoder().decode(str(envelope.get("nonce")))));
			cipher.updateAAD(aad(sessionId, direction, sequence));
			content = obj(parse(cipher.doFinal(Base64.getDecoder().decode(str(envelope.get("ciphertext"))))));
		} catch (final Exception ex) {
			throw new SecurityError("message does not decrypt or authenticate", ex);
		}

		final double timestamp = content.get("timestamp") instanceof final Number number ? number.doubleValue() : 0;
		if (Math.abs(System.currentTimeMillis() / 1000.0 - timestamp) > clockSkewSeconds) {
			throw new SecurityError("message is outside the accepted time window");
		}

		return new Opened(sequence, content);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// PEM (one or more CRLs) or DER.
	private static List<X509CRL> crls(final Path file) throws Exception {
		final List<X509CRL> out = new ArrayList<>();
		try (var in = new ByteArrayInputStream(Files.readAllBytes(file))) {
			for (final var crl : CertificateFactory.getInstance("X.509").generateCRLs(in)) {
				out.add((X509CRL) crl);
			}
		}

		return out;
	}

	//-------------------------------------------------------------------------------------------------
	private static Signature pss() throws Exception {
		final Signature signature = Signature.getInstance("RSASSA-PSS");
		signature.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSS_SALT, 1));

		return signature;
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static final class SecurityError extends Exception {

		private static final long serialVersionUID = 1L;

		public SecurityError(final String message) {
			super(message);
		}

		public SecurityError(final String message, final Throwable cause) {
			super(message, cause);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@FunctionalInterface
	public interface RevocationCheck {
		void check(X509Certificate certificate, X509Certificate issuer) throws SecurityError;
	}

	//-------------------------------------------------------------------------------------------------
	public record Opened(long sequence, Map<String, Object> content) {
	}

	//-------------------------------------------------------------------------------------------------
	// Accepts each sequence number once, within a sliding window above the highest seen.
	public static final class Replay {

		private static final int DEFAULT_SIZE = 4096;

		private final int size;
		private long highest;
		private final TreeSet<Long> seen = new TreeSet<>();

		public Replay(final int size) {
			this.size = size;
		}

		public Replay() {
			this(DEFAULT_SIZE);
		}

		public synchronized boolean accept(final long sequence) {
			if (sequence <= 0 || seen.contains(sequence) || sequence <= highest - size) {
				return false;
			}

			seen.add(sequence);
			highest = Math.max(highest, sequence);
			seen.headSet(highest - size, true).clear();

			return true;
		}
	}
}
