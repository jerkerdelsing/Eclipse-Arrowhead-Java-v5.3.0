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
package eu.arrowhead.deviceqosevaluator.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_")
public class System extends ArrowheadEntity {

	//=================================================================================================
	// members

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String name;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "deviceId", referencedColumnName = "id", nullable = true)
	private Device device;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public System() {
	}

	//-------------------------------------------------------------------------------------------------
	public System(final String name, final Device device) {
		this.name = name;
		this.device = device;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "System [id=" + id + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", name=" + name + ", device=" + device + "]";
	}

	//-------------------------------------------------------------------------------------------------
	public String getName() {
		return name;
	}

	//-------------------------------------------------------------------------------------------------
	public void setName(final String name) {
		this.name = name;
	}

	//-------------------------------------------------------------------------------------------------
	public Device getDevice() {
		return device;
	}

	//-------------------------------------------------------------------------------------------------
	public void setDevice(final Device device) {
		this.device = device;
	}
}