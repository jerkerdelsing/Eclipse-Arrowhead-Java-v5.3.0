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
package eu.arrowhead.serviceorchestration.service.model;

import java.util.List;

import eu.arrowhead.dto.OrchestrationPushTriggerDTO;

public class OrchestrationPushTrigger {

	//=================================================================================================
	// members

	private String requesterSystem;
	private List<String> targetSystems;
	private List<String> subscriptionIds;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationPushTrigger(final String requesterSystem, final OrchestrationPushTriggerDTO dto) {
		this.requesterSystem = requesterSystem;
		if (dto != null) {
			this.targetSystems = dto.targetSystems();
			this.subscriptionIds = dto.subscriptionIds();
		}
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getRequesterSystem() {
		return requesterSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRequesterSystem(final String requesterSystem) {
		this.requesterSystem = requesterSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getTargetSystems() {
		return targetSystems;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetSystems(final List<String> targetSystems) {
		this.targetSystems = targetSystems;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getSubscriptionIds() {
		return subscriptionIds;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSubscriptionIds(final List<String> subscriptionIds) {
		this.subscriptionIds = subscriptionIds;
	}
}