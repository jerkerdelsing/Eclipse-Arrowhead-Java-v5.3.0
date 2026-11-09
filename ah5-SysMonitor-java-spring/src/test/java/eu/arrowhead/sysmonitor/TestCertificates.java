/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

// A throw-away cloud issuer (TestCloud.Company.arrowhead.eu) and Arrowhead certificates with a profile (DN qualifier).
public final class TestCertificates {

	//=================================================================================================
	// members

	public static final String SUFFIX = "TestCloud.Company.arrowhead.eu";

	private final KeyPair rootKeys;
	private final X500Name rootName = new X500Name("C=SE,O=Company,CN=" + SUFFIX);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TestCertificates() throws Exception {
		rootKeys = key();
	}

	//-------------------------------------------------------------------------------------------------
	public X509Certificate issue(final String systemName, final String profile) throws Exception {
		final X500NameBuilder subject = new X500NameBuilder(BCStyle.INSTANCE)
				.addRDN(BCStyle.C, "SE")
				.addRDN(BCStyle.O, "Company")
				.addRDN(BCStyle.OU, "Arrowhead Systems")
				.addRDN(BCStyle.CN, systemName + "." + SUFFIX);
		if (profile != null) {
			subject.addRDN(BCStyle.DN_QUALIFIER, profile);
		}

		final KeyPair keys = key();
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(rootName, new BigInteger(64, new SecureRandom()),
				Date.from(Instant.now().minusSeconds(60)), Date.from(Instant.now().plus(Duration.ofDays(1))), subject.build(), keys.getPublic());
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
		builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
		return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(rootKeys.getPrivate())));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static KeyPair key() throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);
		return generator.generateKeyPair();
	}
}
