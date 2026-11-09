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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
public class Device {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "createdAt");
	public static final String DEFAULT_SORT_FIELD = "createdAt";

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = ArrowheadEntity.VARCHAR_LARGE)
	private String address;

	@Column(nullable = true)
	private Integer rttPort;

	@Column(nullable = false, columnDefinition = "INT(1)")
	private boolean augmented = false;

	@Column(nullable = false, columnDefinition = "INT(1)")
	private boolean inactive = false;

	@Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime createdAt;

	@Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private ZonedDateTime updatedAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@PrePersist
	public void onCreate() {
		this.createdAt = Utilities.utcNow();
		this.updatedAt = this.createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	@PreUpdate
	public void onUpdate() {
		this.updatedAt = Utilities.utcNow();
	}

	//-------------------------------------------------------------------------------------------------
	public Device() {
	}

	//-------------------------------------------------------------------------------------------------
	public Device(final UUID id, final String address, final Integer rttPort, final boolean augmented, final boolean inactive) {
		this.id = id;
		this.address = address;
		this.rttPort = rttPort;
		this.augmented = augmented;
		this.inactive = inactive;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "Device [id=" + id + ", address=" + address + ", rttPort=" + rttPort + ", augmented=" + augmented + ", inactive=" + inactive + ", createdAt=" + createdAt
				+ ", updatedAt=" + updatedAt + "]";
	}

	//-------------------------------------------------------------------------------------------------
	public UUID getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final UUID id) {
		this.id = id;
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
	public Integer getRttPort() {
		return rttPort;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRttPort(final Integer rttPort) {
		this.rttPort = rttPort;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isAugmented() {
		return augmented;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAugmented(final boolean augmented) {
		this.augmented = augmented;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isInactive() {
		return inactive;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInactive(final boolean inactive) {
		this.inactive = inactive;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedAt(final ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getUpdatedAt() {
		return updatedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUpdatedAt(final ZonedDateTime updatedAt) {
		this.updatedAt = updatedAt;
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

		final Device other = (Device) obj;

		return Objects.equals(id, other.id);
	}
}
