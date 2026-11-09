/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Which observation types of a provider SysMonitor binds; providers without a subscription follow the default subscription.
@Entity
@Table(name = "subscription")
public class Subscription {

	//=================================================================================================
	// members

	public static final int NAME_LENGTH = 63;
	public static final int TYPES_LENGTH = 2048;
	public static final int TIME_LENGTH = 40;

	@Id
	@Column(name = "provider_system", nullable = false, length = NAME_LENGTH)
	private String providerSystem;

	// JSON array of the sorted observation types, or ["*"]
	@Column(name = "observation_types", nullable = false, length = TYPES_LENGTH)
	private String observationTypes;

	@Column(nullable = false)
	private boolean enabled;

	@Column(nullable = false)
	private int revision;

	@Column(name = "updated_at", nullable = false, length = TIME_LENGTH)
	private String updatedAt;

	@Column(name = "updated_by", nullable = true, length = NAME_LENGTH)
	private String updatedBy;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Subscription() {
	}

	//-------------------------------------------------------------------------------------------------
	public Subscription(final String providerSystem) {
		this.providerSystem = providerSystem;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getProviderSystem() {
		return providerSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public String getObservationTypes() {
		return observationTypes;
	}

	//-------------------------------------------------------------------------------------------------
	public void setObservationTypes(final String observationTypes) {
		this.observationTypes = observationTypes;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isEnabled() {
		return enabled;
	}

	//-------------------------------------------------------------------------------------------------
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	//-------------------------------------------------------------------------------------------------
	public int getRevision() {
		return revision;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRevision(final int revision) {
		this.revision = revision;
	}

	//-------------------------------------------------------------------------------------------------
	public String getUpdatedAt() {
		return updatedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUpdatedAt(final String updatedAt) {
		this.updatedAt = updatedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public String getUpdatedBy() {
		return updatedBy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUpdatedBy(final String updatedBy) {
		this.updatedBy = updatedBy;
	}
}
