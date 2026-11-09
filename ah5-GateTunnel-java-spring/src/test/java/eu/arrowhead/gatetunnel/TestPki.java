/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
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

// A throw-away cloud CA issuing Arrowhead certificates (<Name>.TestCloud.Company.arrowhead.eu, dnQualifier = profile),
// written as PEM files.
public final class TestPki {

	//=================================================================================================
	// members

	public record Pair(Path cert, Path key, X509Certificate certificate, KeyPair keys) {
	}

	public final Pair root;
	private final Path folder;
	private int n;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TestPki(final Path folder) throws Exception {
		this.folder = folder;
		Files.createDirectories(folder);
		final KeyPair keys = key();
		final X500Name name = new X500Name("C=SE,O=Company,CN=TestCloud.Company.arrowhead.eu");
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date.from(Instant.now().minusSeconds(3600)),
				Date.from(Instant.now().plus(Duration.ofDays(1095))), name, keys.getPublic());
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
		builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
		root = save(new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keys.getPrivate()))), keys);
	}

	//-------------------------------------------------------------------------------------------------
	public Pair issue(final String name, final String profile) throws Exception {
		final KeyPair keys = key();
		final X500Name subject = new X500NameBuilder()
				.addRDN(BCStyle.C, "SE").addRDN(BCStyle.O, "Company").addRDN(BCStyle.OU, "Arrowhead Systems")
				.addRDN(BCStyle.CN, name + ".TestCloud.Company.arrowhead.eu").addRDN(BCStyle.DN_QUALIFIER, profile).build();
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(X500Name.getInstance(root.certificate().getSubjectX500Principal().getEncoded()),
				new BigInteger(100, new SecureRandom()), Date.from(Instant.now().minusSeconds(60)), Date.from(Instant.now().plusSeconds(3600)), subject, keys.getPublic());
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
		builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[] { KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth }));
		builder.addExtension(Extension.subjectAlternativeName, false,
				new GeneralNames(new GeneralName[] { new GeneralName(GeneralName.dNSName, "localhost"), new GeneralName(GeneralName.iPAddress, "127.0.0.1") }));

		return save(new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(root.keys().getPrivate()))), keys);
	}

	//-------------------------------------------------------------------------------------------------
	public static String pem(final Object value) throws Exception {
		final StringWriter out = new StringWriter();
		try (JcaPEMWriter writer = new JcaPEMWriter(out)) {
			writer.writeObject(value instanceof final PrivateKey key ? new PemObject("PRIVATE KEY", key.getEncoded()) : value);
		}

		return out.toString();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static KeyPair key() throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);

		return generator.generateKeyPair();
	}

	//-------------------------------------------------------------------------------------------------
	private Pair save(final X509Certificate cert, final KeyPair keys) throws Exception {
		final Path certPath = folder.resolve("cert-" + n + ".pem");
		final Path keyPath = folder.resolve("key-" + n++ + ".pem");
		Files.writeString(certPath, pem(cert));
		Files.writeString(keyPath, pem(keys.getPrivate()));

		return new Pair(certPath, keyPath, cert, keys);
	}
}
