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
package eu.arrowhead.serviceregistry.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.dto.enums.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class SystemAddress extends ArrowheadEntity {

	//=================================================================================================
	// members

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "systemId", referencedColumnName = "id", nullable = false)
	private System system;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AddressType addressType;

	@Column(nullable = false, length = VARCHAR_LARGE)
	private String address;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SystemAddress() {
	}

	//-------------------------------------------------------------------------------------------------
	public SystemAddress(final System system, final AddressType addressType, final String address) {
		this.system = system;
		this.addressType = addressType;
		this.address = address;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "SystemAddress [id=" + id + ", updatedAt=" + updatedAt + ", createdAt=" + createdAt + ", system=" + system + ", addressType=" + addressType + ", address=" + address + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public System getSystem() {
		return system;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystem(final System system) {
		this.system = system;
	}

	//-------------------------------------------------------------------------------------------------
	public AddressType getAddressType() {
		return addressType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAddressType(final AddressType addressType) {
		this.addressType = addressType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getAddress() {
		return address;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAddress(final String address) {
		this.address = address;
	}
}