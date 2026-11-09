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

@Entity
public class CryptographerAuxiliary extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members

	@Column(nullable = false)
	private String value;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public CryptographerAuxiliary() {

	}

	//-------------------------------------------------------------------------------------------------
	public CryptographerAuxiliary(final String value) {
		this.value = value;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "CryptographerAuxiliary [id=" + id + ", value=" + value + ", createdAt=" + createdAt + "]";
	}

	//=================================================================================================
	// boilerplate methods

	//-------------------------------------------------------------------------------------------------
	public String getValue() {
		return value;
	}

	//-------------------------------------------------------------------------------------------------
	public void setValue(final String value) {
		this.value = value;
	}
}