/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.authorization.jpa.entity;

import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class EncryptionKey extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String systemName;

	@Column(nullable = false)
	private String encryptedKey;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String algorithm;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "internal_auxiliary_id", referencedColumnName = "id", nullable = false)
	private CryptographerAuxiliary internalAuxiliary;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "external_auxiliary_id", referencedColumnName = "id", nullable = true)
	private CryptographerAuxiliary externalAuxiliary;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public EncryptionKey() {
	}

	//-------------------------------------------------------------------------------------------------
	public EncryptionKey(
			final String systemName,
			final String encryptedKey,
			final String algorithm,
			final CryptographerAuxiliary internalAuxiliary,
			final CryptographerAuxiliary externalAuxiliary) {
		this.systemName = systemName;
		this.encryptedKey = encryptedKey;
		this.algorithm = algorithm;
		this.internalAuxiliary = internalAuxiliary;
		this.externalAuxiliary = externalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "EncryptionKey [id=" + id + ", systemName=" + systemName + ", encryptedKey=" + encryptedKey + ", algorithm=" + algorithm + ", internalAuxiliary=" + internalAuxiliary
				+ ", externalAuxiliary=" + externalAuxiliary + ", createdAt=" + createdAt + "]";
	}

	//=================================================================================================
	// boilerplate methods

	//-------------------------------------------------------------------------------------------------
	public String getSystemName() {
		return systemName;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystemName(final String systemName) {
		this.systemName = systemName;
	}

	//-------------------------------------------------------------------------------------------------
	public String getEncryptedKey() {
		return encryptedKey;
	}

	//-------------------------------------------------------------------------------------------------
	public void setEncryptedKey(final String encryptedKey) {
		this.encryptedKey = encryptedKey;
	}

	//-------------------------------------------------------------------------------------------------
	public String getAlgorithm() {
		return algorithm;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAlgorithm(final String algorithm) {
		this.algorithm = algorithm;
	}

	//-------------------------------------------------------------------------------------------------
	public CryptographerAuxiliary getInternalAuxiliary() {
		return internalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInternalAuxiliary(final CryptographerAuxiliary internalAuxiliary) {
		this.internalAuxiliary = internalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public CryptographerAuxiliary getExternalAuxiliary() {
		return externalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExternalAuxiliary(final CryptographerAuxiliary externalAuxiliary) {
		this.externalAuxiliary = externalAuxiliary;
	}
}