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
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import eu.arrowhead.certificateauthority.CertificateAuthorityConstants;
import eu.arrowhead.certificateauthority.jpa.entity.IssuedCertificate;
import eu.arrowhead.certificateauthority.jpa.entity.TrustedKey;
import eu.arrowhead.certificateauthority.jpa.repository.IssuedCertificateRepository;
import eu.arrowhead.certificateauthority.jpa.repository.TrustedKeyRepository;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;

// certificateAuthorityManagement: issued certificates, revocation and trusted keys.
@Service
public class CertificateManagementService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	private final IssuerCredentials issuer;
	private final IssuedCertificateRepository certificates;
	private final TrustedKeyRepository keys;
	private final CrlService crlService;
	private final int certificateValidityDays;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public CertificateManagementService(
			final IssuerCredentials issuer,
			final IssuedCertificateRepository certificates,
			final TrustedKeyRepository keys,
			final CrlService crlService,
			@Value(CertificateAuthorityConstants.$CERT_VALIDITY_DAYS_WD) final int certificateValidityDays) {
		this.issuer = issuer;
		this.certificates = certificates;
		this.keys = keys;
		this.crlService = crlService;
		this.certificateValidityDays = certificateValidityDays;
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isManager(final String caller) {
		return CertificateAuthorityConstants.MANAGERS.contains(caller);
	}

	//-------------------------------------------------------------------------------------------------
	public void requireManager(final String caller, final String origin) {
		if (!isManager(caller)) {
			throw new ForbiddenException("Caller is not authorized for this operation", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<Map<String, Object>> listCertificates(final String caller, final String origin) {
		requireManager(caller, origin);
		return certificates.findAllByOrderByIdDesc().stream().map(CertificateViews::certificate).toList();
	}

	//-------------------------------------------------------------------------------------------------
	public List<Map<String, Object>> listKeys(final String caller, final String origin) {
		requireManager(caller, origin);
		return keys.findAllByOrderByIdDesc().stream().map(CertificateViews::key).toList();
	}

	//-------------------------------------------------------------------------------------------------
	// Onboarding may revoke only the certificates it requested itself; managers may revoke any.
	public boolean revokeById(final String caller, final long id, final String origin) {
		final boolean onboarding = CertificateAuthorityConstants.ONBOARDING.equals(caller);
		if (!onboarding) {
			requireManager(caller, origin);
		}

		final Optional<IssuedCertificate> found = certificates.findById(id);
		if (found.isEmpty() || found.get().getRevokedAt() != null || onboarding && !CertificateAuthorityConstants.ONBOARDING.equals(found.get().getCreatedBy())) {
			return false;
		}

		final IssuedCertificate certificate = found.get();
		certificate.setRevokedAt(Instant.now());
		certificates.saveAndFlush(certificate);
		publishCrl();

		return true;
	}

	//-------------------------------------------------------------------------------------------------
	// Revokes a certificate of this cloud given by content, also one that was not issued through this CA's database.
	public Map<String, Object> revokeByContent(final String caller, final Map<String, Object> payload, final String origin) {
		requireManager(caller, origin);

		final X509Certificate certificate;
		try {
			certificate = CertificateSupport.certificate(CertificateSupport.text(payload.getOrDefault("certificate", payload.get("encodedCertificate"))));
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		if (!issuer.issued(certificate)) {
			throw new InvalidParameterException("Certificate was not issued by this cloud", origin);
		}

		final String serial = certificate.getSerialNumber().toString();
		final Optional<IssuedCertificate> known = certificates.findBySerialNumber(serial);
		final long id;
		final boolean changed;
		if (known.isPresent()) {
			final IssuedCertificate row = known.get();
			id = row.getId();
			changed = row.getRevokedAt() == null;
			if (changed) {
				row.setRevokedAt(Instant.now());
				certificates.saveAndFlush(row);
			}
		} else {
			final IssuedCertificate row = certificates.saveAndFlush(new IssuedCertificate(
					serial,
					CertificateSupport.commonName(certificate),
					"revoked-by:" + caller,
					certificate.getNotBefore().toInstant(),
					certificate.getNotAfter().toInstant(),
					Instant.now(),
					CertificateSupport.pem(certificate)));
			id = row.getId();
			changed = true;
		}

		publishCrl();

		final Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", id);
		result.put("serialNumber", serial);
		result.put("revoked", true);
		result.put("changed", changed);

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public long addKey(final String caller, final Map<String, Object> payload, final String origin) {
		requireManager(caller, origin);

		final String key = CertificateSupport.text(payload.getOrDefault("publicKey", payload.get("publicKeyPem")));
		try {
			final Object parsed = CertificateSupport.readPem(key);
			if (!(parsed instanceof SubjectPublicKeyInfo info)) {
				throw new IllegalArgumentException("Public key PEM required");
			}

			new JcaPEMKeyConverter().getPublicKey(info);
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final Exception ex) {
			throw new InvalidParameterException("Public key PEM required", origin);
		}

		final Instant now = Instant.now();
		final Instant start;
		final Instant end;
		try {
			start = CertificateSupport.instant(payload.get("validAfter"), now);
			end = CertificateSupport.instant(payload.get("validBefore"), now.plus(Duration.ofDays(certificateValidityDays)));
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		if (!end.isAfter(start)) {
			throw new InvalidParameterException("Invalid trusted-key validity", origin);
		}

		final String description = CertificateSupport.text(payload.get("description"));
		if (description.length() > TrustedKey.DESCRIPTION_LENGTH) {
			throw new InvalidParameterException("Description is too long", origin);
		}

		try {
			return keys.saveAndFlush(new TrustedKey(key, CertificateSupport.sha256(key), description, start, end, now)).getId();
		} catch (final DataIntegrityViolationException ex) {
			throw new InvalidParameterException("Public key is already trusted", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean deleteKey(final String caller, final long id, final String origin) {
		requireManager(caller, origin);
		if (!keys.existsById(id)) {
			return false;
		}

		keys.deleteById(id);
		return true;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// the revocation is already stored; a failed publication is reported to the caller, as in the native implementation
	private void publishCrl() {
		try {
			crlService.publish();
		} catch (final IOException ex) {
			logger.error("CRL publication failed: {}", ex.getMessage());
			throw new InternalServerError("Revocation stored but the CRL could not be published");
		}
	}
}
