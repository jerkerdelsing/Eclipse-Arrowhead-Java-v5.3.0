/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.sysmonitor.jpa.entity.Subscription;
import jakarta.persistence.LockModeType;

@Repository
public interface SubscriptionRepository extends RefreshableRepository<Subscription, String> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	List<Subscription> findAllByOrderByProviderSystemAsc();

	//-------------------------------------------------------------------------------------------------
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Subscription s WHERE s.providerSystem = ?1")
	Optional<Subscription> findForUpdate(final String providerSystem);
}
