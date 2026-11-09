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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;

@Repository
public interface OrchestrationStoreRepository extends RefreshableRepository<OrchestrationStore, UUID> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<OrchestrationStore> findByConsumerAndServiceDefinitionAndPriority(
			final String consumer, final String serviceDefinition, final int priority);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> findAllByConsumerIn(final List<String> consumerNames);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> findAllByServiceDefinitionIn(final List<String> serviceDefinitionNames);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> findAllByServiceInstanceIdIn(final List<String> serviceInstanceIds);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> findAllByCreatedBy(final String createdBy);

	//-------------------------------------------------------------------------------------------------
	public Page<OrchestrationStore> findAllByIdIn(final List<UUID> matchingIds, final PageRequest pagination);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> findAllByConsumerOrderByPriorityAsc(final String consumer);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> findAllByConsumerAndServiceDefinition(final String consumer, final String serviceDefinition);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> findAllByConsumerAndServiceDefinitionOrderByPriorityAsc(final String consumer, final String serviceDefinition);
}
