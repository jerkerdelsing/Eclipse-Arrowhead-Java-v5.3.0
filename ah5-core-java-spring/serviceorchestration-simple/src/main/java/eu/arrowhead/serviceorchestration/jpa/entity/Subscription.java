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
import java.util.Objects;
import java.util.UUID;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"ownerSystem", "targetSystem", "serviceDefinition"})})
public class Subscription {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "ownerSystem", "targetSystem", "serviceDefinition", "createdAt");
	public static final String DEFAULT_SORT_FIELD = "id";

	@Id
	private UUID id;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String ownerSystem;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String targetSystem;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String serviceDefinition;

	@Column(nullable = true)
	private ZonedDateTime expiresAt;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_TINY)
	private String notifyProtocol;

	@Column(nullable = false)
	private String notifyProperties;

	@Column(nullable = false)
	private String orchestrationRequest;

	@Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime createdAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Subscription() {
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	public Subscription(
			final String ownerSystem,
			final String targetSystem,
			final String serviceDefinition,
			final ZonedDateTime expiresAt,
			final String notifyProtocol,
			final String notifyProperties,
			final String orchestrationRequest) {
		this.id = UUID.randomUUID();
		this.ownerSystem = ownerSystem;
		this.targetSystem = targetSystem;
		this.serviceDefinition = serviceDefinition;
		this.expiresAt = expiresAt;
		this.notifyProtocol = notifyProtocol;
		this.notifyProperties = notifyProperties;
		this.orchestrationRequest = orchestrationRequest;
	}

	//-------------------------------------------------------------------------------------------------
	@PrePersist
	public void onCreate() {
		this.createdAt = Utilities.utcNow();
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "System [id = " + id + ", ownerSystem = " + ownerSystem + ", targetSystem = " + targetSystem + ", serviceDefinition = " + serviceDefinition + ", expiresAt = " + expiresAt
				+ ", notifyProtocol = " + notifyProtocol + ", notifyProperties = " + notifyProperties + ", orchestrationRequest = " + orchestrationRequest + ", createdAt = " + createdAt + "]";
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

		final Subscription other = (Subscription) obj;
		return id.equals(other.id);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public UUID getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final UUID id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOwnerSystem() {
		return ownerSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOwnerSystem(final String ownerSystem) {
		this.ownerSystem = ownerSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTargetSystem() {
		return targetSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetSystem(final String targetSystem) {
		this.targetSystem = targetSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(final String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
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
	public String getNotifyProtocol() {
		return notifyProtocol;
	}

	//-------------------------------------------------------------------------------------------------
	public void setNotifyProtocol(final String notifyProtocol) {
		this.notifyProtocol = notifyProtocol;
	}

	//-------------------------------------------------------------------------------------------------
	public String getNotifyProperties() {
		return notifyProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public void setNotifyProperties(final String notifyProperties) {
		this.notifyProperties = notifyProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOrchestrationRequest() {
		return orchestrationRequest;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOrchestrationRequest(final String orchestrationRequest) {
		this.orchestrationRequest = orchestrationRequest;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedAt(final ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}
}