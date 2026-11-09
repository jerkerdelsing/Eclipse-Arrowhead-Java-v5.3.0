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

import java.util.List;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import jakarta.persistence.Entity;

@Entity
public class AuthProviderPolicyHeader extends AuthPolicyHeader {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "instanceId", "createdAt", "targetType", "cloud", "provider", "target", "createdBy");

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthProviderPolicyHeader() {
	}

	//-------------------------------------------------------------------------------------------------
	public AuthProviderPolicyHeader(
			final String instanceId,
			final AuthorizationTargetType targetType,
			final String cloud,
			final String provider,
			final String target,
			final String description) {
		this.instanceId = instanceId;
		this.targetType = targetType;
		this.cloud = cloud;
		this.provider = provider;
		this.target = target;
		this.description = Utilities.isEmpty(description) ? null : description;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "AuthProviderPolicyHeader [id=" + id + ", createdAt=" + createdAt + ", instanceId=" + instanceId + ", targetType=" + targetType + ", cloud=" + cloud + ", provider=" + provider
				+ ", target=" + target + ", description=" + description + "]";
	}
}