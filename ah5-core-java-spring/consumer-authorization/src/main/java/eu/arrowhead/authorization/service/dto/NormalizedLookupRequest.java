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

import java.util.List;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

public class NormalizedLookupRequest {

	//=================================================================================================
	// members

	protected List<String> providers;
	protected List<String> instanceIds;
	protected List<String> cloudIdentifiers;
	protected List<String> targetNames;
	protected AuthorizationTargetType targetType;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedLookupRequest() {
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasFilter() {
		return !Utilities.isEmpty(providers)
				|| !Utilities.isEmpty(instanceIds)
				|| !Utilities.isEmpty(cloudIdentifiers)
				|| !Utilities.isEmpty(targetNames)
				|| targetType != null;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "NormalizedLookupRequest [providers=" + providers + ", instanceIds=" + instanceIds + ", cloudIdentifiers=" + cloudIdentifiers + ", targetNames=" + targetNames
				+ ", targetType=" + targetType + "]";
	}

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public List<String> providers() {
		return providers;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProviders(final List<String> providers) {
		this.providers = providers;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> instanceIds() {
		return instanceIds;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInstanceIds(final List<String> instanceIds) {
		this.instanceIds = instanceIds;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> cloudIdentifiers() {
		return cloudIdentifiers;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCloudIdentifiers(final List<String> cloudIdentifiers) {
		this.cloudIdentifiers = cloudIdentifiers;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> targetNames() {
		return targetNames;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetNames(final List<String> targetNames) {
		this.targetNames = targetNames;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTargetType targetType() {
		return targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetType(final AuthorizationTargetType targetType) {
		this.targetType = targetType;
	}
}