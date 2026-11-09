/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority;

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
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import eu.arrowhead.certificateauthority.service.CertificateSupport;
import eu.arrowhead.certificateauthority.service.IssuerCredentials;

// A throw-away cloud issuer (TestCloud.Company.arrowhead.eu) and helpers for CSRs and foreign certificates.
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
		this(SUFFIX);
	}

	//-------------------------------------------------------------------------------------------------
	public TestPki(final String rootCommonName) throws Exception {
		CertificateSupport.ensureProvider();
		rootKeys = key();
		final X500Name name = new X500Name("C=SE,O=Company,CN=" + rootCommonName);
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date.from(Instant.now().minusSeconds(3600)),
				Date.from(Instant.now().plus(Duration.ofDays(1095))), name, rootKeys.getPublic());
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
		builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
		root = new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(rootKeys.getPrivate())));
	}

	//-------------------------------------------------------------------------------------------------
	public IssuerCredentials credentials() {
		return new IssuerCredentials(root, rootKeys.getPrivate());
	}

	//-------------------------------------------------------------------------------------------------
	public static KeyPair key() throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);
		return generator.generateKeyPair();
	}

	//-------------------------------------------------------------------------------------------------
	// PEM CSR for "<commonName>", optionally with a DN qualifier and requested DNS names
	public static String csr(final String commonName, final String qualifier, final String... dnsNames) throws Exception {
		final KeyPair keys = key();
		final X500NameBuilder subject = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, commonName);
		if (qualifier != null) {
			subject.addRDN(BCStyle.DN_QUALIFIER, qualifier);
		}

		final JcaPKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject.build(), keys.getPublic());
		if (dnsNames.length > 0) {
			final GeneralName[] names = new GeneralName[dnsNames.length];
			for (int i = 0; i < dnsNames.length; ++i) {
				names[i] = new GeneralName(GeneralName.dNSName, dnsNames[i]);
			}

			final ExtensionsGenerator extensions = new ExtensionsGenerator();
			extensions.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));
			builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions.generate());
		}

		final PKCS10CertificationRequest request = builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keys.getPrivate()));
		return CertificateSupport.pem(request);
	}

	//-------------------------------------------------------------------------------------------------
	// an end-entity certificate of this issuer with the given profile (DN qualifier)
	public X509Certificate issue(final String systemName, final String profile, final Instant notBefore, final Instant notAfter) throws Exception {
		final KeyPair keys = key();
		final X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
				.addRDN(BCStyle.C, "SE")
				.addRDN(BCStyle.O, "Company")
				.addRDN(BCStyle.OU, "Arrowhead Systems")
				.addRDN(BCStyle.CN, systemName + "." + CertificateSupport.commonName(root))
				.addRDN(BCStyle.DN_QUALIFIER, profile)
				.build();
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(X500Name.getInstance(root.getSubjectX500Principal().getEncoded()),
				new BigInteger(100, new SecureRandom()), Date.from(notBefore), Date.from(notAfter), subject, keys.getPublic());
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
		return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(rootKeys.getPrivate())));
	}
}
