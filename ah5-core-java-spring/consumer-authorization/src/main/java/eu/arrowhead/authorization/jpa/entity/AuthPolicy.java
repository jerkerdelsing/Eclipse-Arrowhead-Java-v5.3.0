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

import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AuthPolicy {

	//=================================================================================================
	// members

	public static final String LIST_POLICY_DELIMITER = ",";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AuthorizationLevel level;

	@Column(nullable = false)
	private long headerId; // can't be a foreign key because it can reference a record from one of two tables

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String scope = Defaults.DEFAULT_AUTHORIZATION_SCOPE;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AuthorizationPolicyType policyType;

	private String policy;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthPolicy() {

	}

	//-------------------------------------------------------------------------------------------------
	public AuthPolicy(
			final AuthorizationLevel level,
			final long headerId,
			final String scope,
			final AuthorizationPolicyType policyType,
			final String policy) {
		this.level = level;
		this.headerId = headerId;
		this.scope = scope;
		this.policyType = policyType;
		this.policy = policy;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "AuthPolicy [id=" + id + ", level=" + level + ", headerId=" + headerId + ", scope=" + scope + ", policyType=" + policyType + ", policy=" + policy + "]";
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
	public AuthorizationLevel getLevel() {
		return level;
	}

	//-------------------------------------------------------------------------------------------------
	public void setLevel(final AuthorizationLevel level) {
		this.level = level;
	}

	//-------------------------------------------------------------------------------------------------
	public long getHeaderId() {
		return headerId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setHeaderId(final long headerId) {
		this.headerId = headerId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getScope() {
		return scope;
	}

	//-------------------------------------------------------------------------------------------------
	public void setScope(final String scope) {
		this.scope = scope;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyType getPolicyType() {
		return policyType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPolicyType(final AuthorizationPolicyType policyType) {
		this.policyType = policyType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getPolicy() {
		return policy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPolicy(final String policy) {
		this.policy = policy;
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

		final AuthPolicy other = (AuthPolicy) obj;
		return id == other.id;
	}
}