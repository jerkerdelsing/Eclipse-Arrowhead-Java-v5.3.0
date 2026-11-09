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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;

@Repository
public interface SubscriptionRepository extends RefreshableRepository<Subscription, UUID> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<Subscription> findByOwnerSystemAndTargetSystemAndServiceDefinition(final String ownerSystem, final String targetSystem, final String serviceDefinition);

	//-------------------------------------------------------------------------------------------------
	public List<Subscription> findAllByOwnerSystemIn(final List<String> ownerSystems);

	//-------------------------------------------------------------------------------------------------
	public List<Subscription> findAllByTargetSystemIn(final List<String> targetSystems);

	//-------------------------------------------------------------------------------------------------
	public List<Subscription> findAllByServiceDefinitionIn(final List<String> serviceDefinitions);

	//-------------------------------------------------------------------------------------------------
	public Page<Subscription> findAllByIdIn(final List<UUID> ids, final Pageable pagination);

	//-------------------------------------------------------------------------------------------------
	public List<Subscription> findAllByExpiresAtBefore(final ZonedDateTime time);
}
