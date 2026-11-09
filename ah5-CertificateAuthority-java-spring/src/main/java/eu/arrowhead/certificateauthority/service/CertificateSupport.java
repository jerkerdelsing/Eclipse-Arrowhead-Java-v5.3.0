/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;

// PEM, X.509 and time helpers, behaviourally identical to the native 5.2.2 implementation.
public final class CertificateSupport {

	//=================================================================================================
	// members

	public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
	private static final String DN_QUALIFIER_OID = "2.5.4.46";

	static {
		if (Security.getProvider(PROVIDER) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void ensureProvider() {
		// the static initializer registers BouncyCastle
	}

	//-------------------------------------------------------------------------------------------------
	public static X509Certificate readCertificate(final Path file) throws IOException {
		try (var in = Files.newInputStream(file)) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
		} catch (final java.security.cert.CertificateException ex) {
			throw new IOException("Invalid certificate file: " + file, ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static PrivateKey readPrivateKey(final Path file) throws IOException {
		try (var parser = new PEMParser(Files.newBufferedReader(file))) {
			final Object value = parser.readObject();
			final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(PROVIDER);
			if (value instanceof PEMKeyPair pair) {
				return converter.getPrivateKey(pair.getPrivateKeyInfo());
			}

			if (value instanceof PrivateKeyInfo info) {
				return converter.getPrivateKey(info);
			}

			throw new IOException("Unsupported private key encoding: " + file);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static Object readPem(final String value) throws IOException {
		try (var parser = new PEMParser(new StringReader(value))) {
			return parser.readObject();
		}
	}

	//-------------------------------------------------------------------------------------------------
	// PEM or base64 DER
	public static X509Certificate certificate(final String value) {
		try {
			final byte[] data = value.contains("BEGIN CERTIFICATE") ? value.getBytes(StandardCharsets.US_ASCII) : Base64.getDecoder().decode(value);
			try (var in = new ByteArrayInputStream(data)) {
				return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
			}
		} catch (final Exception ex) {
			throw new IllegalArgumentException("Invalid certificate");
		}
	}

	//-------------------------------------------------------------------------------------------------
	// PEM or base64 DER
	public static PKCS10CertificationRequest csr(final String value) {
		try {
			final PKCS10CertificationRequest request = value.contains("BEGIN") ? (PKCS10CertificationRequest) readPem(value) : new PKCS10CertificationRequest(Base64.getDecoder().decode(value));
			if (request == null) {
				throw new IllegalArgumentException();
			}

			return request;
		} catch (final Exception ex) {
			throw new IllegalArgumentException("Invalid CSR");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static String pem(final Object value) {
		final StringWriter text = new StringWriter();
		try (var writer = new JcaPEMWriter(text)) {
			if (value instanceof PrivateKey key) {
				writer.writeObject(new PemObject("PRIVATE KEY", key.getEncoded()));
			} else {
				writer.writeObject(value);
			}
		} catch (final IOException ex) {
			throw new IllegalStateException("PEM encoding failed", ex);
		}

		return text.toString();
	}

	//-------------------------------------------------------------------------------------------------
	public static String commonName(final X509Certificate certificate) {
		return attribute(certificate, "CN");
	}

	//-------------------------------------------------------------------------------------------------
	// "DNQ" selects the DN qualifier (OID 2.5.4.46), which carries the certificate profile
	public static String attribute(final X509Certificate certificate, final String attribute) {
		try {
			for (final Rdn rdn : new LdapName(certificate.getSubjectX500Principal().getName()).getRdns()) {
				if (rdn.getType().equalsIgnoreCase(attribute) || "DNQ".equals(attribute) && DN_QUALIFIER_OID.equals(rdn.getType())) {
					final Object value = rdn.getValue();
					if (value instanceof byte[] bytes) {
						return ((ASN1String) ASN1Primitive.fromByteArray(bytes)).getString();
					}

					return value == null ? "" : value.toString();
				}
			}
		} catch (final Exception ex) {
			return "";
		}

		return "";
	}

	//-------------------------------------------------------------------------------------------------
	// ISO-8601 instant, offset date-time or (UTC) local date-time; blank -> fallback
	public static Instant instant(final Object value, final Instant fallback) {
		if (value == null || value.toString().isBlank()) {
			return fallback;
		}

		final String text = value.toString();
		try {
			return Instant.parse(text);
		} catch (final Exception ignored) {
			try {
				return OffsetDateTime.parse(text).toInstant();
			} catch (final Exception ignored2) {
				try {
					return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
				} catch (final Exception ex) {
					throw new IllegalArgumentException("Invalid time: " + text);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static String sha256(final String text) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
		} catch (final NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static String text(final Object value) {
		return value == null ? "" : value.toString();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private CertificateSupport() {
		throw new UnsupportedOperationException();
	}
}
