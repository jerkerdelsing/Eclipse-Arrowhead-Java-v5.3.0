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

import java.io.IOException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.certificateauthority.CertificateAuthorityConstants;

// The local cloud's issuer certificate and signing key (PEM, mounted read-only). Only the CertificateAuthority holds the key.
@Component
public class IssuerCredentials {

	//=================================================================================================
	// members

	private final X509Certificate root;
	private final PrivateKey rootKey;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public IssuerCredentials(
			@Value(CertificateAuthorityConstants.$CA_CERTIFICATE_FILE_WD) final String certificateFile,
			@Value(CertificateAuthorityConstants.$CA_KEY_FILE_WD) final String keyFile) throws IOException {
		this(CertificateSupport.readCertificate(Path.of(certificateFile)), CertificateSupport.readPrivateKey(Path.of(keyFile)));
	}

	//-------------------------------------------------------------------------------------------------
	public IssuerCredentials(final X509Certificate root, final PrivateKey rootKey) {
		CertificateSupport.ensureProvider();
		this.root = root;
		this.rootKey = rootKey;
	}

	//-------------------------------------------------------------------------------------------------
	public X509Certificate root() {
		return root;
	}

	//-------------------------------------------------------------------------------------------------
	public PrivateKey rootKey() {
		return rootKey;
	}

	//-------------------------------------------------------------------------------------------------
	// "<cloud>.<organization>.arrowhead.eu": every issued common name is "<System>." + this suffix
	public String cloudSuffix() {
		return CertificateSupport.commonName(root);
	}

	//-------------------------------------------------------------------------------------------------
	public String signatureAlgorithm() {
		return rootKey instanceof ECPrivateKey ? "SHA256withECDSA" : "SHA256withRSA";
	}

	//-------------------------------------------------------------------------------------------------
	public boolean issued(final X509Certificate certificate) {
		try {
			if (!certificate.getIssuerX500Principal().equals(root.getSubjectX500Principal())) {
				return false;
			}

			certificate.verify(root.getPublicKey());
			return true;
		} catch (final Exception ex) {
			return false;
		}
	}
}
