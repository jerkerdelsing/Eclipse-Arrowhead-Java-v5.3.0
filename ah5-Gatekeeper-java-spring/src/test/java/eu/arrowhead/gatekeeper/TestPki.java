/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper;

import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;

// A throw-away cloud issuer (TestCloud.Company.arrowhead.eu) issuing Arrowhead system/operator/on-boarding certificates.
public final class TestPki {

	//=================================================================================================
	// members

	public static final String SUFFIX = "TestCloud.Company.arrowhead.eu";

	public final KeyPair rootKeys;
	public final X509Certificate root;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TestPki() throws Exception {
		rootKeys = key();
		final X500Name name = new X500Name("C=SE,O=Company,CN=" + SUFFIX);
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date.from(Instant.now().minusSeconds(3600)),
				Date.from(Instant.now().plus(Duration.ofDays(1095))), name, rootKeys.getPublic());
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
		builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
		root = new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(rootKeys.getPrivate())));
	}

	//-------------------------------------------------------------------------------------------------
	// <system>.TestCloud.Company.arrowhead.eu with the given DN qualifier (sy, op, on, ...)
	public X509Certificate issue(final String system, final String profile) throws Exception {
		final KeyPair keys = key();
		final X500Name subject = new X500NameBuilder()
				.addRDN(BCStyle.C, "SE")
				.addRDN(BCStyle.O, "Company")
				.addRDN(BCStyle.OU, "Arrowhead Systems")
				.addRDN(BCStyle.CN, system + "." + SUFFIX)
				.addRDN(BCStyle.DN_QUALIFIER, profile)
				.build();
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(X500Name.getInstance(root.getSubjectX500Principal().getEncoded()),
				new BigInteger(100, new SecureRandom()), Date.from(Instant.now().minusSeconds(60)), Date.from(Instant.now().plusSeconds(3600)), subject, keys.getPublic());
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
		builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[] { KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth }));
		builder.addExtension(Extension.subjectAlternativeName, false,
				new GeneralNames(new GeneralName[] { new GeneralName(GeneralName.dNSName, "localhost"), new GeneralName(GeneralName.iPAddress, "127.0.0.1") }));
		return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(rootKeys.getPrivate())));
	}

	//-------------------------------------------------------------------------------------------------
	public static KeyPair key() throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);
		return generator.generateKeyPair();
	}

	//-------------------------------------------------------------------------------------------------
	public static String pem(final Object value) throws Exception {
		final StringWriter out = new StringWriter();
		try (JcaPEMWriter writer = new JcaPEMWriter(out)) {
			writer.writeObject(value instanceof PrivateKey key ? new PemObject("PRIVATE KEY", key.getEncoded()) : value);
		}

		return out.toString();
	}
}
