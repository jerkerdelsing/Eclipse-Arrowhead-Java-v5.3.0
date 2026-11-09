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

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class UsageLimitedToken {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "headerId", referencedColumnName = "id", nullable = false)
	private TokenHeader header;

	@Column(nullable = false)
	private int usageLimit;

	@Column(nullable = false)
	private int usageLeft;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public UsageLimitedToken() {
	}

	//-------------------------------------------------------------------------------------------------
	public UsageLimitedToken(
			final TokenHeader header,
			final int usageLimit) {
		this.header = header;
		this.usageLimit = usageLimit;
		this.usageLeft = usageLimit;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "UsageLimitedToken [id=" + id + ", header=" + header + ", usageLimit=" + usageLimit + ", usageLeft=" + usageLeft + "]";
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
	public int getUsageLimit() {
		return usageLimit;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUsageLimit(final int usageLimit) {
		this.usageLimit = usageLimit;
	}

	//-------------------------------------------------------------------------------------------------
	public int getUsageLeft() {
		return usageLeft;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUsageLeft(final int usageLeft) {
		this.usageLeft = usageLeft;
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

		final UsageLimitedToken other = (UsageLimitedToken) obj;
		return id == other.id;
	}
}