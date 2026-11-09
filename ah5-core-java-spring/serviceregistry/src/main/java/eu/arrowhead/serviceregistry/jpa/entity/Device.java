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

import java.util.List;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Device extends ArrowheadEntity {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "name", "createdAt");
	public static final String DEFAULT_SORT_FIELD = "name";

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String name;

	@Column(nullable = true)
	private String metadata;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Device() {
	}

	//-------------------------------------------------------------------------------------------------
	public Device(final String name, final String metadata) {
		this.name = name;
		this.metadata = metadata;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "Device [id=" + id + ", updatedAt=" + updatedAt + ", createdAt=" + createdAt + ", name=" + name + ", metadata=" + metadata + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getName() {
		return name;
	}

	//-------------------------------------------------------------------------------------------------
	public void setName(final String name) {
		this.name = name;
	}

	//-------------------------------------------------------------------------------------------------
	public String getMetadata() {
		return metadata;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMetadata(final String metadata) {
		this.metadata = metadata;
	}
}