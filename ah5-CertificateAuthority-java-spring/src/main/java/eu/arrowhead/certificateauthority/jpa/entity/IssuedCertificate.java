/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.jpa.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A certificate issued by this CA, or a foreign-held certificate of this CA that was revoked by content.
@Entity
@Table(name = "issued_certificate")
public class IssuedCertificate {

	//=================================================================================================
	// members

	public static final int SERIAL_LENGTH = 64;
	public static final int NAME_LENGTH = 255;
	public static final int ISSUER_LENGTH = 127;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "serial_number", nullable = false, unique = true, length = SERIAL_LENGTH)
	private String serialNumber;

	@Column(name = "common_name", nullable = false, length = NAME_LENGTH)
	private String commonName;

	// the identity that requested the certificate, or "revoked-by:<identity>" for a revocation by content
	@Column(name = "created_by", nullable = false, length = ISSUER_LENGTH)
	private String createdBy;

	@Column(name = "valid_from", nullable = false)
	private Instant validFrom;

	@Column(name = "valid_until", nullable = false)
	private Instant validUntil;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "certificate_pem", nullable = false, columnDefinition = "TEXT")
	private String certificatePem;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IssuedCertificate() {
	}

	//-------------------------------------------------------------------------------------------------
	public IssuedCertificate(final String serialNumber, final String commonName, final String createdBy, final Instant validFrom, final Instant validUntil, final Instant revokedAt,
			final String certificatePem) {
		this.serialNumber = serialNumber;
		this.commonName = commonName;
		this.createdBy = createdBy;
		this.validFrom = validFrom;
		this.validUntil = validUntil;
		this.revokedAt = revokedAt;
		this.certificatePem = certificatePem;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public Long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final Long id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public String getSerialNumber() {
		return serialNumber;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCommonName() {
		return commonName;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCreatedBy() {
		return createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public Instant getValidFrom() {
		return validFrom;
	}

	//-------------------------------------------------------------------------------------------------
	public Instant getValidUntil() {
		return validUntil;
	}

	//-------------------------------------------------------------------------------------------------
	public Instant getRevokedAt() {
		return revokedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRevokedAt(final Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCertificatePem() {
		return certificatePem;
	}
}
