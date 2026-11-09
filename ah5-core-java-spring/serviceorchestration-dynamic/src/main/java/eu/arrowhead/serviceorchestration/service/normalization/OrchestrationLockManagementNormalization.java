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
package eu.arrowhead.serviceorchestration.service.normalization;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.dto.OrchestrationLockQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationLockRequestDTO;

@Service
public class OrchestrationLockManagementNormalization {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockListRequestDTO normalizeOrchestrationLockListRequestDTO(final OrchestrationLockListRequestDTO dto) {
		logger.debug("normalizeOrchestrationLockListRequestDTO started...");
		Assert.notNull(dto, "dto is null");

		return new OrchestrationLockListRequestDTO(
				dto.locks()
						.stream()
						.map(lock -> new OrchestrationLockRequestDTO(
								serviceInstanceIdNormalizer.normalize(lock.serviceInstanceId()),
								systemNameNormalizer.normalize(lock.owner()),
								lock.expiresAt().trim()))
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockQueryRequestDTO normalizeOrchestrationLockQueryRequestDTO(final OrchestrationLockQueryRequestDTO dto) {
		logger.debug("normalizeOrchestrationLockQueryRequestDTO started...");

		if (dto == null) {
			return new OrchestrationLockQueryRequestDTO(null, null, null, null, null, null, null);
		}

		return new OrchestrationLockQueryRequestDTO(
				dto.pagination(), // no need to normalize, because it will happen in the getPageRequest method
				Utilities.isEmpty(dto.ids()) ? new ArrayList<>() : dto.ids(),
				Utilities.isEmpty(dto.orchestrationJobIds()) ? new ArrayList<>() : dto.orchestrationJobIds().stream().map(id -> id.trim()).toList(),
				Utilities.isEmpty(dto.serviceInstanceIds()) ? new ArrayList<>() : dto.serviceInstanceIds().stream().map(instance -> serviceInstanceIdNormalizer.normalize(instance)).toList(),
				Utilities.isEmpty(dto.owners()) ? new ArrayList<>() : dto.owners().stream().map(owner -> systemNameNormalizer.normalize(owner)).toList(),
				Utilities.isEmpty(dto.expiresBefore()) ? null : dto.expiresBefore().trim(),
				Utilities.isEmpty(dto.expiresAfter()) ? null : dto.expiresAfter().trim());

	}

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeServiceInstanceIds(final List<String> instanceIds) {
		logger.debug("normalizeServiceInstanceIds started...");
		Assert.isTrue(!Utilities.isEmpty(instanceIds), "Service instance id list is empty");

		return instanceIds.stream().map(id -> serviceInstanceIdNormalizer.normalize(id)).toList();
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeSystemName(final String name) {
		logger.debug("normalizeSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(name), "System name is empty");

		return systemNameNormalizer.normalize(name);
	}
}