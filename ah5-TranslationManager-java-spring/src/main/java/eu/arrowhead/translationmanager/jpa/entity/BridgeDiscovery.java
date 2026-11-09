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
package eu.arrowhead.translationmanager.jpa.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class BridgeDiscovery {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "headerId", referencedColumnName = "id", nullable = false)
	private BridgeHeader header;

	@Column(nullable = false)
	private String data;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public BridgeDiscovery() {
	}

	//-------------------------------------------------------------------------------------------------
	public BridgeDiscovery(final BridgeHeader header, final String data) {
		this.header = header;
		this.data = data;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "BridgeDiscovery [id=" + id + ", header=" + header + ", data=" + data + "]";
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

		final BridgeDiscovery other = (BridgeDiscovery) obj;
		return id == other.id;
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
	public BridgeHeader getHeader() {
		return header;
	}

	//-------------------------------------------------------------------------------------------------
	public void setHeader(final BridgeHeader header) {
		this.header = header;
	}

	//-------------------------------------------------------------------------------------------------
	public String getData() {
		return data;
	}

	//-------------------------------------------------------------------------------------------------
	public void setData(final String data) {
		this.data = data;
	}
}