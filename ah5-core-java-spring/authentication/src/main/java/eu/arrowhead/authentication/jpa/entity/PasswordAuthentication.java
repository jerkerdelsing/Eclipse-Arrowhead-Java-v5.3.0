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
package eu.arrowhead.authentication.jpa.entity;

import java.util.Objects;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class PasswordAuthentication {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "systemId", referencedColumnName = "id", nullable = false, unique = true)
	private System system;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String password;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public PasswordAuthentication() {
	}

	//-------------------------------------------------------------------------------------------------
	public PasswordAuthentication(final System system, final String password) {
		this.system = system;
		this.password = password;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "PasswordAuthentication [id=" + id + ", system=" + system + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final long id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public System getSystem() {
		return system;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystem(final System system) {
		this.system = system;
	}

	//-------------------------------------------------------------------------------------------------
	public String getPassword() {
		return password;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPassword(final String password) {
		this.password = password;
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

		final PasswordAuthentication other = (PasswordAuthentication) obj;
		return id == other.id;
	}

}