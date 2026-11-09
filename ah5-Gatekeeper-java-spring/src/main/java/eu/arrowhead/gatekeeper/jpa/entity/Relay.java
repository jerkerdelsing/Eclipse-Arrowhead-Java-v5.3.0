/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.jpa.entity;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;

// A configured relay broker; the name is unique, as the native primary key was.
@Entity
public class Relay extends ArrowheadEntity {

	//=================================================================================================
	// members

	@Column(nullable = false, unique = true, length = VARCHAR_MEDIUM)
	private String name;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String address;

	@Column(nullable = false)
	private int port;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String type;

	@Column(nullable = false)
	private boolean secure = true;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String metadata;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Relay() {
	}

	//-------------------------------------------------------------------------------------------------
	public Relay(final String name) {
		this.name = name;
	}

	//-------------------------------------------------------------------------------------------------
	// a creation time set before persisting (migrate-sqlite) is kept
	@Override
	@PrePersist
	public void onCreate() {
		if (createdAt == null) {
			createdAt = Utilities.utcNow();
		}

		updatedAt = createdAt;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public String getName() {
		return name;
	}

	//-------------------------------------------------------------------------------------------------
	public String getAddress() {
		return address;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAddress(final String address) {
		this.address = address;
	}

	//-------------------------------------------------------------------------------------------------
	public int getPort() {
		return port;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPort(final int port) {
		this.port = port;
	}

	//-------------------------------------------------------------------------------------------------
	public String getType() {
		return type;
	}

	//-------------------------------------------------------------------------------------------------
	public void setType(final String type) {
		this.type = type;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isSecure() {
		return secure;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSecure(final boolean secure) {
		this.secure = secure;
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
	public java.time.ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedAt(final java.time.ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
