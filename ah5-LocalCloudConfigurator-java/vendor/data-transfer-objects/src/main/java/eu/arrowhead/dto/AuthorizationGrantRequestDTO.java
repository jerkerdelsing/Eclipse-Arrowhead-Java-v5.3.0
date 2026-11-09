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
package eu.arrowhead.dto;

import java.util.HashMap;
import java.util.Map;

import eu.arrowhead.dto.enums.AuthorizationTargetType;

public record AuthorizationGrantRequestDTO(
		String cloud,
		String targetType,
		String target,
		String description,
		AuthorizationPolicyRequestDTO defaultPolicy,
		Map<String, AuthorizationPolicyRequestDTO> scopedPolicies) {

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static class Builder {

		//=================================================================================================
		// members

		private String cloud = DTODefaults.DEFAULT_CLOUD;
		private final AuthorizationTargetType targetType;
		private String target;
		private String description;
		private AuthorizationPolicyRequestDTO defaultPolicy;
		private Map<String, AuthorizationPolicyRequestDTO> scopedPolicies;

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public Builder(final AuthorizationTargetType targetType) {
			this.targetType = targetType;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder cloud(final String cloud) {
			this.cloud = cloud == null || cloud.isBlank() ? DTODefaults.DEFAULT_CLOUD : cloud;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder target(final String target) {
			this.target = target;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder description(final String description) {
			this.description = description;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder defaultPolicy(final AuthorizationPolicyRequestDTO defaultPolicy) {
			this.defaultPolicy = defaultPolicy;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder scopedPolicy(final String scope, final AuthorizationPolicyRequestDTO policy) {
			if (this.scopedPolicies == null) {
				this.scopedPolicies = new HashMap<>();
			}

			if (scope != null) {
				final boolean isDefault = DTODefaults.DEFAULT_AUTHORIZATION_SCOPE.equals(scope.trim());
				if (isDefault) {
					this.defaultPolicy = policy;
				} else {
					this.scopedPolicies.put(scope.trim(), policy);
				}
			}

			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder scopedPolicies(final Map<String, AuthorizationPolicyRequestDTO> scopedPolicies) {
			this.scopedPolicies = scopedPolicies;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public AuthorizationGrantRequestDTO build() {
			return new AuthorizationGrantRequestDTO(
					this.cloud,
					this.targetType.name(),
					this.target,
					this.description,
					this.defaultPolicy,
					this.scopedPolicies);
		}
	}
}