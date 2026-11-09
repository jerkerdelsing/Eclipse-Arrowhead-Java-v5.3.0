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
package eu.arrowhead.serviceorchestration.jpa.repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;

@Repository
public interface OrchestrationLockRepository extends RefreshableRepository<OrchestrationLock, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Page<OrchestrationLock> findAllByIdIn(final Collection<Long> ids, final Pageable pageable);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationLock> findAllByServiceInstanceIdIn(final List<String> serviceInstanceIds);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationLock> findAllByOrchestrationJobIdIn(final List<String> orchestrationJobIds);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationLock> findAllByOwnerIn(final List<String> owners);

	//-------------------------------------------------------------------------------------------------
	public Optional<OrchestrationLock> findByOrchestrationJobIdAndServiceInstanceId(final String orchestrationJobId, final String serviceInstanceId);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationLock> findAllByExpiresAtBefore(final ZonedDateTime time);
}