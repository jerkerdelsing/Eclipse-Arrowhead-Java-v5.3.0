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

import java.time.ZonedDateTime;
import java.util.Objects;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class SelfContainedToken {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "headerId", referencedColumnName = "id", nullable = false)
	private TokenHeader header;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String variant;

	@Column(nullable = false)
	protected ZonedDateTime expiresAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SelfContainedToken() {
	}

	//-------------------------------------------------------------------------------------------------
	public SelfContainedToken(
			final TokenHeader header,
			final String variant,
			final ZonedDateTime expiresAt) {
		this.header = header;
		this.variant = variant;
		this.expiresAt = expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "JsonWebToken [id=" + id + ", header=" + header + ", variant=" + variant + ", expiresAt=" + expiresAt + "]";
	}

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final long id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public TokenHeader getHeader() {
		return header;
	}

	//-------------------------------------------------------------------------------------------------
	public void setHeader(final TokenHeader header) {
		this.header = header;
	}

	//-------------------------------------------------------------------------------------------------
	public String getVariant() {
		return variant;
	}

	//-------------------------------------------------------------------------------------------------
	public void setVariant(final String variant) {
		this.variant = variant;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExpiresAt() {
		return expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExpiresAt(final ZonedDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final SelfContainedToken other = (SelfContainedToken) obj;
		return id == other.id;
	}
}