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
package eu.arrowhead.authorization.service.dto;

import java.util.HashMap;
import java.util.Map;

import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

public class NormalizedGrantRequest {

	//=================================================================================================
	// members

	private final AuthorizationLevel level;
	private String cloud;
	private String provider;
	private AuthorizationTargetType targetType;
	private String target;
	private String description;
	private Map<String, NormalizedAuthorizationPolicyRequest> policies;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedGrantRequest(final AuthorizationLevel level) {
		this.level = level;
	}

	//-------------------------------------------------------------------------------------------------
	public void addPolicy(final String scope, final NormalizedAuthorizationPolicyRequest policy) {
		if (policy != null) {
			if (this.policies == null) {
				this.policies = new HashMap<>();
			}

			final String normalizedScope = Utilities.isEmpty(scope) ? Defaults.DEFAULT_AUTHORIZATION_SCOPE : scope;
			this.policies.put(normalizedScope, policy);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "NormalizedGrantRequest [level=" + level + ", cloud=" + cloud + ", provider=" + provider + ", targetType=" + targetType + ", target=" + target + ", description=" + description
				+ ", policies=" + policies + "]";
	}

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public String cloud() {
		return cloud;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCloud(final String cloud) {
		this.cloud = Utilities.isEmpty(cloud) ? Defaults.DEFAULT_CLOUD : cloud;
	}

	//-------------------------------------------------------------------------------------------------
	public String provider() {
		return provider;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProvider(final String provider) {
		this.provider = provider;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTargetType targetType() {
		return targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetType(final AuthorizationTargetType targetType) {
		this.targetType = targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public String target() {
		return target;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTarget(final String target) {
		this.target = target;
	}

	//-------------------------------------------------------------------------------------------------
	public String description() {
		return description;
	}

	//-------------------------------------------------------------------------------------------------
	public void setDescription(final String description) {
		this.description = description;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, NormalizedAuthorizationPolicyRequest> policies() {
		return policies;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPolicies(final Map<String, NormalizedAuthorizationPolicyRequest> policies) {
		this.policies = policies;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationLevel level() {
		return level;
	}
}