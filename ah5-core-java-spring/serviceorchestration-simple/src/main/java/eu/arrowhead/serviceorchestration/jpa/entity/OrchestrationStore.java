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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"consumer", "serviceDefinition", "priority"})})
public class OrchestrationStore {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "consumer", "serviceDefinition", "priority", "serviceInstanceId", "createdAt");
	public static final String DEFAULT_SORT_FIELD = "id";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	protected UUID id;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String consumer;

	@Column(nullable = true, length = ArrowheadEntity.VARCHAR_SMALL)
	private String serviceDefinition;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String serviceInstanceId;

	@Column(nullable = false)
	private int priority;

	@Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	protected ZonedDateTime createdAt;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String createdBy;

	@Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	protected ZonedDateTime updatedAt;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String updatedBy;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationStore() {

	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationStore(final String consumer, final String serviceDefinition, final String serviceInstanceId, final int priority, final String createdBy) {
		this.consumer = consumer;
		this.serviceDefinition = serviceDefinition;
		this.serviceInstanceId = serviceInstanceId;
		this.priority = priority;
		this.createdBy = createdBy;
		this.updatedBy = createdBy;

	}

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
	public int hashCode() {
		return Objects.hash(id);
	}

	//-------------------------------------------------------------------------------------------------
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

		final OrchestrationStore other = (OrchestrationStore) obj;
		return Objects.equals(id, other.id);
	}

	//-------------------------------------------------------------------------------------------------
	public String toString() {
		return "ServiceInstance [id = " + id + ", serviceDefinition = " + serviceDefinition + ", serviceInstanceId = " + serviceInstanceId + ", priority = " + priority
				+ ", createdBy = " + createdBy + ", updatedBy = " + updatedBy + ", createdAt " + createdAt + ", updatedAt " + updatedAt + "]";
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
	public String getConsumer() {
		return consumer;
	}

	//-------------------------------------------------------------------------------------------------
	public void setConsumer(final String consumer) {
		this.consumer = consumer;
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
	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceInstanceId(final String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public int getPriority() {
		return priority;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPriority(final int priority) {
		this.priority = priority;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCreatedBy() {
		return createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public String getUpdatedBy() {
		return updatedBy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUpdatedBy(final String updatedBy) {
		this.updatedBy = updatedBy;
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
}
