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

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.certificateauthority.jpa.entity.IssuedCertificate;
import eu.arrowhead.certificateauthority.jpa.entity.TrustedKey;
import eu.arrowhead.certificateauthority.jpa.repository.IssuedCertificateRepository;
import eu.arrowhead.certificateauthority.jpa.repository.TrustedKeyRepository;
import eu.arrowhead.common.exception.InvalidParameterException;

// certificateValidation: certificate status and trusted keys. (get-crl is served by CrlService.)
@Service
public class CertificateValidationService {

	//=================================================================================================
	// members

	private final IssuerCredentials issuer;
	private final IssuedCertificateRepository certificates;
	private final TrustedKeyRepository keys;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public CertificateValidationService(final IssuerCredentials issuer, final IssuedCertificateRepository certificates, final TrustedKeyRepository keys) {
		this.issuer = issuer;
		this.certificates = certificates;
		this.keys = keys;
	}

	//-------------------------------------------------------------------------------------------------
	// GOOD, REVOKED, EXPIRED (outside its validity) or UNKNOWN (not signed by this cloud's issuer)
	public Map<String, Object> checkCertificate(final Map<String, Object> payload, final String origin) {
		final String encoded = CertificateSupport.text(payload.getOrDefault("certificate", payload.get("encodedCertificate")));
		if (encoded.isEmpty()) {
			throw new InvalidParameterException("certificate required", origin);
		}

		final X509Certificate certificate;
		try {
			certificate = CertificateSupport.certificate(encoded);
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		final String serial = certificate.getSerialNumber().toString();
		final Map<String, Object> result = new LinkedHashMap<>();
		if (!issuer.issued(certificate)) {
			result.put("status", "UNKNOWN");
			result.put("valid", false);
			result.put("serialNumber", serial);
			return result;
		}

		final Optional<IssuedCertificate> row = certificates.findBySerialNumber(serial);
		final Instant now = Instant.now();
		final String status;
		if (row.isPresent() && row.get().getRevokedAt() != null) {
			status = "REVOKED";
		} else if (now.isBefore(certificate.getNotBefore().toInstant()) || now.isAfter(certificate.getNotAfter().toInstant())) {
			status = "EXPIRED";
		} else {
			status = "GOOD";
		}

		result.put("status", status);
		result.put("valid", "GOOD".equals(status));
		result.put("serialNumber", serial);
		result.put("known", row.isPresent());

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> checkTrustedKey(final Map<String, Object> payload, final String origin) {
		final String key = CertificateSupport.text(payload.getOrDefault("publicKey", payload.get("publicKeyPem")));
		if (key.isBlank()) {
			throw new InvalidParameterException("publicKey required", origin);
		}

		final Optional<TrustedKey> row = keys.findByPublicKeyHash(CertificateSupport.sha256(key));
		final Instant now = Instant.now();
		final boolean trusted = row.isPresent() && !row.get().getValidAfter().isAfter(now) && !row.get().getValidBefore().isBefore(now);

		final Map<String, Object> result = new LinkedHashMap<>();
		result.put("trusted", trusted);
		result.put("key", row.map(CertificateViews::key).orElse(null));

		return result;
	}
}
