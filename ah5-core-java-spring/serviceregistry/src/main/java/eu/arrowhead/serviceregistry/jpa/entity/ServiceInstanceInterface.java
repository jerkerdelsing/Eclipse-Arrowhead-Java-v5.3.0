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

import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ServiceInstanceInterface extends ArrowheadEntity {

	//=================================================================================================
	// members

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "serviceInstanceId", referencedColumnName = "id", nullable = false)
	private ServiceInstance serviceInstance;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "serviceInterfaceTemplateId", referencedColumnName = "id", nullable = false)
	private ServiceInterfaceTemplate serviceInterfaceTemplate;

	@Column(nullable = false)
	private String properties;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ServiceInterfacePolicy policy;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceInterface() {
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceInterface(final ServiceInstance serviceInstance, final ServiceInterfaceTemplate serviceInterfaceTemplate, final String properties, final ServiceInterfacePolicy policy) {
		this.serviceInstance = serviceInstance;
		this.serviceInterfaceTemplate = serviceInterfaceTemplate;
		this.properties = properties;
		this.policy = policy;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "ServiceInstanceInterface [id=" + id + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", serviceInstance=" + serviceInstance + ", serviceInterfaceTemplate="
				+ serviceInterfaceTemplate + ", properties=" + properties + ", policy=" + policy + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public ServiceInstance getServiceInstance() {
		return serviceInstance;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceInstance(final ServiceInstance serviceInstance) {
		this.serviceInstance = serviceInstance;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplate getServiceInterfaceTemplate() {
		return serviceInterfaceTemplate;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceInterfaceTemplate(final ServiceInterfaceTemplate serviceInterfaceTemplate) {
		this.serviceInterfaceTemplate = serviceInterfaceTemplate;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProperties() {
		return properties;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProperties(final String properties) {
		this.properties = properties;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfacePolicy getPolicy() {
		return policy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPolicy(final ServiceInterfacePolicy policy) {
		this.policy = policy;
	}
}