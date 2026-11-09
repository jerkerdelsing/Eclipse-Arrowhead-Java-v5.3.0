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

import java.time.ZonedDateTime;
import java.util.List;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ServiceInstance extends ArrowheadEntity {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "serviceInstanceId", "createdAt");
	public static final String DEFAULT_SORT_FIELD = "serviceInstanceId";

	@Column(nullable = false, unique = true, length = VARCHAR_MEDIUM)
	private String serviceInstanceId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "systemId", referencedColumnName = "id", nullable = false)
	private System system;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "serviceDefinitionId", referencedColumnName = "id", nullable = false)
	private ServiceDefinition serviceDefinition;

	@Column(nullable = false, length = VARCHAR_TINY)
	private String version = "1.0.0";

	@Column(nullable = true)
	private ZonedDateTime expiresAt;

	@Column(nullable = true)
	private String metadata;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInstance() {
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstance(
			final String serviceInstanceId,
			final System system,
			final ServiceDefinition serviceDefinition,
			final String version,
			final ZonedDateTime expiresAt,
			final String metadata) {
		this.serviceInstanceId = serviceInstanceId;
		this.system = system;
		this.serviceDefinition = serviceDefinition;
		this.version = version;
		this.expiresAt = expiresAt;
		this.metadata = metadata;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "ServiceInstance [id=" + id + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", serviceInstanceId=" + serviceInstanceId + ", system=" + system
				+ ", serviceDefinition=" + serviceDefinition + ", version=" + version + ", expiresAt=" + expiresAt + ", metadata=" + metadata + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceInstanceId(final String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public System getSystem() {
		return system;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystem(final System system) {
		this.system = system;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinition getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(final ServiceDefinition serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public String getVersion() {
		return version;
	}

	//-------------------------------------------------------------------------------------------------
	public void setVersion(final String version) {
		this.version = version;
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
	public String getMetadata() {
		return metadata;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMetadata(final String metadata) {
		this.metadata = metadata;
	}
}