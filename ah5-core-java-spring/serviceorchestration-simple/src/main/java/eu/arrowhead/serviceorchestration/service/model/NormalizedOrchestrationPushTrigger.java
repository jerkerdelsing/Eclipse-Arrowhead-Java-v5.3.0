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
import java.util.UUID;

public class NormalizedOrchestrationPushTrigger {

	//=================================================================================================
	// members

	private List<String> targetSystems;
	private List<UUID> subscriptionIds;

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public NormalizedOrchestrationPushTrigger() {
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedOrchestrationPushTrigger(final List<String> targetSystems, final List<UUID> subscriptionIds) {
		this.targetSystems = targetSystems;
		this.subscriptionIds = subscriptionIds;
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
	public List<UUID> getSubscriptionIds() {
		return subscriptionIds;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSubscriptionIds(final List<UUID> subscriptionIds) {
		this.subscriptionIds = subscriptionIds;
	}
}
