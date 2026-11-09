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
package eu.arrowhead.serviceorchestration.jpa.entity;

import java.time.ZonedDateTime;
import java.util.List;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class OrchestrationLock {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "owner", "serviceInstanceId", "expiresAt");
	public static final String DEFAULT_SORT_FIELD = "id";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = true, length = ArrowheadEntity.VARCHAR_SMALL)
	private String orchestrationJobId;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_MEDIUM)
	private String serviceInstanceId;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String owner;

	@Column(nullable = true)
	private ZonedDateTime expiresAt;

	@Column(nullable = false, columnDefinition = "INT(1)")
	private boolean temporary = false;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLock() {
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLock(final String orchestrationJobId, final String serviceInstanceId, final String owner, final ZonedDateTime expiresAt, final boolean temporary) {
		this.orchestrationJobId = orchestrationJobId;
		this.serviceInstanceId = serviceInstanceId;
		this.owner = owner;
		this.expiresAt = expiresAt;
		this.temporary = temporary;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLock(final String serviceInstanceId, final String owner, final ZonedDateTime expiresAt) {
		this.serviceInstanceId = serviceInstanceId;
		this.owner = owner;
		this.expiresAt = expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "OrchestrationLock [id = " + id + ", orchestrationJobId = " + orchestrationJobId + ", serviceInstanceId = " + serviceInstanceId + ", owner = " + owner + ", expiresAt = "
				+ expiresAt + ", temporary = " + temporary + "]";
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
	public String getOrchestrationJobId() {
		return orchestrationJobId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOrchestrationJobId(final String orchestrationJobId) {
		this.orchestrationJobId = orchestrationJobId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceInstanceId(final String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOwner() {
		return owner;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOwner(final String owner) {
		this.owner = owner;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExpiresAt() {
		return expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExpiresAt(final ZonedDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isTemporary() {
		return temporary;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTemporary(final boolean temporary) {
		this.temporary = temporary;
	}
}