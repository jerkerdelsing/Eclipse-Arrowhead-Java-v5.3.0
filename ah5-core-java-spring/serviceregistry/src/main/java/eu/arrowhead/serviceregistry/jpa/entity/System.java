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
import jakarta.persistence.Table;

@Entity
@Table(name = "system_")
public class System extends ArrowheadEntity {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "name", "createdAt");
	public static final String DEFAULT_SORT_FIELD = "createdAt";

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String name;

	@Column(nullable = true)
	private String metadata;

	@Column(nullable = false, length = VARCHAR_TINY)
	private String version = "1.0.0";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public System() {
	}

	//-------------------------------------------------------------------------------------------------
	public System(final String name, final String metadata, final String version) {
		this.name = name;
		this.metadata = metadata;
		this.version = version;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "System [id=" + id + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", name=" + name + ", metadata=" + metadata + ", version=" + version + "]";
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

	//-------------------------------------------------------------------------------------------------
	public String getVersion() {
		return version;
	}

	//-------------------------------------------------------------------------------------------------
	public void setVersion(final String version) {
		this.version = version;
	}
}