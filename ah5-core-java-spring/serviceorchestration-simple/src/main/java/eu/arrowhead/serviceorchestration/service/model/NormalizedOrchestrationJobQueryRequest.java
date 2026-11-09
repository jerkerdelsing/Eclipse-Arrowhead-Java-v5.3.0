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

import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

public class NormalizedOrchestrationJobQueryRequest {

	//=================================================================================================
	// members

	private PageRequest pagination;
	private List<UUID> ids;
	private List<OrchestrationJobStatus> statuses;
	private OrchestrationType type;
	private List<String> requesterSystems;
	private List<String> targetSystems;
	private List<String> serviceDefinitions;
	private List<UUID> subscriptionIds;

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumber")
	public NormalizedOrchestrationJobQueryRequest(
			final PageRequest pagination,
			final List<UUID> ids,
			final List<OrchestrationJobStatus> statuses,
			final OrchestrationType type,
			final List<String> requesterSystems,
			final List<String> targetSystems,
			final List<String> serviceDefinitions,
			final List<UUID> subscriptionIds) {
		this.pagination = pagination;
		this.ids = ids;
		this.statuses = statuses;
		this.type = type;
		this.requesterSystems = requesterSystems;
		this.targetSystems = targetSystems;
		this.serviceDefinitions = serviceDefinitions;
		this.subscriptionIds = subscriptionIds;
	}

	//-------------------------------------------------------------------------------------------------
	public PageRequest getPagination() {
		return pagination;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPagination(final PageRequest pagination) {
		this.pagination = pagination;
	}

	//-------------------------------------------------------------------------------------------------
	public List<UUID> getIds() {
		return ids;
	}

	//-------------------------------------------------------------------------------------------------
	public void setIds(final List<UUID> ids) {
		this.ids = ids;
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationJobStatus> getStatuses() {
		return statuses;
	}

	//-------------------------------------------------------------------------------------------------
	public void setStatuses(final List<OrchestrationJobStatus> statuses) {
		this.statuses = statuses;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationType getType() {
		return type;
	}

	//-------------------------------------------------------------------------------------------------
	public void setType(final OrchestrationType type) {
		this.type = type;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getRequesterSystems() {
		return requesterSystems;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRequesterSystems(final List<String> requesterSystems) {
		this.requesterSystems = requesterSystems;
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
	public List<String> getServiceDefinitions() {
		return serviceDefinitions;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinitions(final List<String> serviceDefinitions) {
		this.serviceDefinitions = serviceDefinitions;
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
