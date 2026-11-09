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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationLockQueryRequestDTO;
import eu.arrowhead.serviceorchestration.service.enums.BaseFilter;

public class OrchestrationLockFilter {

	//=================================================================================================
	// members

	private List<Long> ids = new ArrayList<>();
	private List<String> orchestrationJobIds = new ArrayList<>();
	private List<String> serviceInstanceIds = new ArrayList<>();
	private List<String> owners = new ArrayList<>();
	private ZonedDateTime expiresBefore = null;
	private ZonedDateTime expiresAfter = null;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockFilter(
			final List<Long> ids,
			final List<String> orchestrationJobIds,
			final List<String> serviceInstanceIds,
			final List<String> owners,
			final ZonedDateTime expiresBefore,
			final ZonedDateTime expiresAfter) {
		this.ids = ids;
		this.orchestrationJobIds = orchestrationJobIds;
		this.serviceInstanceIds = serviceInstanceIds;
		this.owners = owners;
		this.expiresBefore = expiresBefore;
		this.expiresAfter = expiresAfter;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockFilter(final OrchestrationLockQueryRequestDTO dto) {
		this.ids = Utilities.isEmpty(dto.ids()) ? ids : dto.ids();
		this.orchestrationJobIds = Utilities.isEmpty(dto.orchestrationJobIds()) ? orchestrationJobIds : dto.orchestrationJobIds();
		this.serviceInstanceIds = Utilities.isEmpty(dto.serviceInstanceIds()) ? serviceInstanceIds : dto.serviceInstanceIds();
		this.owners = Utilities.isEmpty(dto.owners()) ? owners : dto.owners();
		this.expiresBefore = Utilities.isEmpty(dto.expiresBefore()) ? expiresBefore : Utilities.parseUTCStringToZonedDateTime(dto.expiresBefore());
		this.expiresAfter = Utilities.isEmpty(dto.expiresAfter()) ? expiresAfter : Utilities.parseUTCStringToZonedDateTime(dto.expiresAfter());
	}

	//-------------------------------------------------------------------------------------------------
	public BaseFilter getBaseFilter() {
		if (!Utilities.isEmpty(ids)) {
			return BaseFilter.ID;
		}
		if (!Utilities.isEmpty(orchestrationJobIds)) {
			return BaseFilter.JOB;
		}
		if (!Utilities.isEmpty(serviceInstanceIds)) {
			return BaseFilter.SERVICE;
		}
		if (!Utilities.isEmpty(owners)) {
			return BaseFilter.OWNER;
		}

		return BaseFilter.NONE;
	}

	//=================================================================================================
	// boilerplate methods

	//-------------------------------------------------------------------------------------------------
	public List<Long> getIds() {
		return ids;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getOrchestrationJobIds() {
		return orchestrationJobIds;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getServiceInstanceIds() {
		return serviceInstanceIds;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getOwners() {
		return owners;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExpiresBefore() {
		return expiresBefore;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExpiresAfter() {
		return expiresAfter;
	}
}