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
package eu.arrowhead.authorization.jpa.entity;

import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AuthPolicyHeader extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members

	public static final String DEFAULT_SORT_FIELD = "instanceId";

	@Column(nullable = false, unique = true, length = VARCHAR_LARGE)
	protected String instanceId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	protected AuthorizationTargetType targetType;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	protected String cloud = Defaults.DEFAULT_CLOUD;

	@Column(nullable = false, length = VARCHAR_SMALL)
	protected String provider;

	@Column(nullable = false, length = VARCHAR_SMALL)
	protected String target;

	@Column(nullable = true, length = VARCHAR_LARGE)
	protected String description;

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public String getInstanceId() {
		return instanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInstanceId(final String instanceId) {
		this.instanceId = instanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTargetType getTargetType() {
		return targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetType(final AuthorizationTargetType targetType) {
		this.targetType = targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCloud() {
		return cloud;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCloud(final String cloud) {
		this.cloud = cloud;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProvider() {
		return provider;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProvider(final String provider) {
		this.provider = provider;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTarget() {
		return target;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTarget(final String target) {
		this.target = target;
	}

	//-------------------------------------------------------------------------------------------------
	public String getDescription() {
		return description;
	}

	//-------------------------------------------------------------------------------------------------
	public void setDescription(final String description) {
		this.description = description;
	}
}