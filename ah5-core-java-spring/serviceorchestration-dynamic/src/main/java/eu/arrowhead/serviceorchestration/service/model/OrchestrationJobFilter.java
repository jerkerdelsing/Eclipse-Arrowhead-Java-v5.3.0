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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.service.enums.BaseFilter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;

public class OrchestrationJobFilter {

	//=================================================================================================
	// members

	private List<UUID> ids = new ArrayList<>();
	private List<OrchestrationJobStatus> statuses = new ArrayList<>();
	private final OrchestrationType type;
	private List<String> requesterSystems = new ArrayList<>();
	private List<String> targetSystems = new ArrayList<>();
	private List<String> serviceDefinitions = new ArrayList<>();
	private List<String> subscriptionIds = new ArrayList<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationJobFilter(
			final List<UUID> ids,
			final List<OrchestrationJobStatus> statuses,
			final OrchestrationType type,
			final List<String> requesterSystems,
			final List<String> targetSystems,
			final List<String> serviceDefinitions,
			final List<String> subscriptionIds) {
		this.ids = ids;
		this.statuses = statuses;
		this.type = type;
		this.requesterSystems = requesterSystems;
		this.targetSystems = targetSystems;
		this.serviceDefinitions = serviceDefinitions;
		this.subscriptionIds = subscriptionIds;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationJobFilter(final OrchestrationHistoryQueryRequestDTO dto) {
		this.ids = Utilities.isEmpty(dto.ids()) ? ids : dto.ids().stream().map(id -> UUID.fromString(id)).toList();
		this.statuses = Utilities.isEmpty(dto.statuses()) ? statuses : dto.statuses().stream().map(s -> OrchestrationJobStatus.valueOf(s)).toList();
		this.type = Utilities.isEmpty(dto.type()) ? null : OrchestrationType.valueOf(dto.type());
		this.requesterSystems = Utilities.isEmpty(dto.requesterSystems()) ? requesterSystems : dto.requesterSystems();
		this.targetSystems = Utilities.isEmpty(dto.targetSystems()) ? targetSystems : dto.targetSystems();
		this.serviceDefinitions = Utilities.isEmpty(dto.serviceDefinitions()) ? serviceDefinitions : dto.serviceDefinitions();
		this.subscriptionIds = Utilities.isEmpty(dto.subscriptionIds()) ? subscriptionIds : dto.subscriptionIds();
	}

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public BaseFilter getBaseFilter() {
		if (!Utilities.isEmpty(ids)) {
			return BaseFilter.ID;
		}
		if (!Utilities.isEmpty(statuses)) {
			return BaseFilter.STATUS;
		}
		if (!Utilities.isEmpty(requesterSystems)) {
			return BaseFilter.OWNER;
		}
		if (!Utilities.isEmpty(targetSystems)) {
			return BaseFilter.TARGET;
		}
		if (!Utilities.isEmpty(serviceDefinitions)) {
			return BaseFilter.SERVICE;
		}

		return BaseFilter.NONE;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public List<UUID> getIds() {
		return ids;
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationJobStatus> getStatuses() {
		return statuses;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationType getType() {
		return type;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getRequesterSystems() {
		return requesterSystems;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getTargetSystems() {
		return targetSystems;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getServiceDefinitions() {
		return serviceDefinitions;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getSubscriptionIds() {
		return subscriptionIds;
	}
}