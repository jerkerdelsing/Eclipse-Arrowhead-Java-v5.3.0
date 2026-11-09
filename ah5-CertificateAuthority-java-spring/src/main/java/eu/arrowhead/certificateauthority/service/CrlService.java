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
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import eu.arrowhead.certificateauthority.CertificateAuthorityConstants;
import eu.arrowhead.certificateauthority.jpa.entity.IssuedCertificate;
import eu.arrowhead.certificateauthority.jpa.repository.IssuedCertificateRepository;
import eu.arrowhead.common.exception.InternalServerError;

// The certificate revocation list: served by certificateValidation.get-crl and published as <crl.dir>/ca.crl, which the
// other systems of the cloud mount read-only. Expired revoked certificates are left out.
@Service
public class CrlService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	private final IssuerCredentials issuer;
	private final IssuedCertificateRepository certificates;
	private final String directory;
	private final int validityHours;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public CrlService(
			final IssuerCredentials issuer,
			final IssuedCertificateRepository certificates,
			@Value(CertificateAuthorityConstants.$CRL_DIR_WD) final String directory,
			@Value(CertificateAuthorityConstants.$CRL_VALIDITY_HOURS_WD) final int validityHours) {
		this.issuer = issuer;
		this.certificates = certificates;
		this.directory = directory == null ? "" : directory.trim();
		this.validityHours = validityHours;
	}

	//-------------------------------------------------------------------------------------------------
	// PEM encoded X.509 v2 CRL
	public byte[] crl() {
		final Instant now = Instant.now();
		try {
			final X509v2CRLBuilder builder = new X509v2CRLBuilder(X500Name.getInstance(issuer.root().getSubjectX500Principal().getEncoded()), Date.from(now.minusSeconds(60)));
			builder.setNextUpdate(Date.from(now.plus(Duration.ofHours(validityHours))));
			builder.addExtension(Extension.cRLNumber, false, new CRLNumber(BigInteger.valueOf(System.currentTimeMillis())));
			builder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(issuer.root().getPublicKey()));
			for (final IssuedCertificate revoked : certificates.findByRevokedAtIsNotNull()) {
				if (revoked.getValidUntil().isAfter(now)) {
					builder.addCRLEntry(new BigInteger(revoked.getSerialNumber()), Date.from(revoked.getRevokedAt()), CRLReason.unspecified);
				}
			}

			final X509CRLHolder holder = builder.build(new JcaContentSignerBuilder(issuer.signatureAlgorithm()).setProvider(CertificateSupport.PROVIDER).build(issuer.rootKey()));
			return CertificateSupport.pem(holder).getBytes(StandardCharsets.US_ASCII);
		} catch (final Exception ex) {
			throw new InternalServerError("CRL generation failed");
		}
	}

	//-------------------------------------------------------------------------------------------------
	// atomic replacement of <crl.dir>/ca.crl; no-op when no directory is configured
	public void publish() throws IOException {
		if (directory.isEmpty()) {
			return;
		}

		final Path target = Path.of(directory, "ca.crl");
		final Path temporary = Files.createTempFile(target.getParent(), ".java-ca-crl-", ".tmp");
		try {
			Files.write(temporary, crl());
			try {
				Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-r--r--"));
			} catch (final UnsupportedOperationException ignored) {
				// non-POSIX file system
			}

			Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Scheduled(fixedDelayString = CertificateAuthorityConstants.$CRL_REFRESH_MILLIS_WD, initialDelayString = CertificateAuthorityConstants.$CRL_REFRESH_MILLIS_WD)
	public void publishQuietly() {
		try {
			publish();
		} catch (final Exception ex) {
			logger.error("CRL publication failed: {}", ex.getMessage());
			logger.debug(ex);
		}
	}
}
