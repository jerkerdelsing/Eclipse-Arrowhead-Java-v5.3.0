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

// A public key the CA vouches for (certificateValidation check-trusted-key).
@Entity
@Table(name = "trusted_key")
public class TrustedKey {

	//=================================================================================================
	// members

	public static final int HASH_LENGTH = 64;
	public static final int DESCRIPTION_LENGTH = 1024;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_key_pem", nullable = false, columnDefinition = "TEXT")
	private String publicKeyPem;

	// SHA-256 of the exact PEM text: keeps the PEM unique and makes the lookup indexable
	@Column(name = "public_key_hash", nullable = false, unique = true, length = HASH_LENGTH)
	private String publicKeyHash;

	@Column(nullable = false, length = DESCRIPTION_LENGTH)
	private String description;

	@Column(name = "valid_after", nullable = false)
	private Instant validAfter;

	@Column(name = "valid_before", nullable = false)
	private Instant validBefore;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TrustedKey() {
	}

	//-------------------------------------------------------------------------------------------------
	public TrustedKey(final String publicKeyPem, final String publicKeyHash, final String description, final Instant validAfter, final Instant validBefore, final Instant createdAt) {
		this.publicKeyPem = publicKeyPem;
		this.publicKeyHash = publicKeyHash;
		this.description = description;
		this.validAfter = validAfter;
		this.validBefore = validBefore;
		this.createdAt = createdAt;
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
	public String getPublicKeyPem() {
		return publicKeyPem;
	}

	//-------------------------------------------------------------------------------------------------
	public String getPublicKeyHash() {
		return publicKeyHash;
	}

	//-------------------------------------------------------------------------------------------------
	public String getDescription() {
		return description;
	}

	//-------------------------------------------------------------------------------------------------
	public Instant getValidAfter() {
		return validAfter;
	}

	//-------------------------------------------------------------------------------------------------
	public Instant getValidBefore() {
		return validBefore;
	}

	//-------------------------------------------------------------------------------------------------
	public Instant getCreatedAt() {
		return createdAt;
	}
}
