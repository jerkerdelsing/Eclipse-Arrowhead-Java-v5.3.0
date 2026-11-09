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

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.sysmonitor.jpa.entity.Outbox;

@Repository
public interface OutboxRepository extends RefreshableRepository<Outbox, String> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Query("SELECT o FROM Outbox o WHERE o.mqttPublished = false AND o.nextAttempt <= ?1 ORDER BY o.createdAt")
	List<Outbox> findPending(final double now, final Pageable page);

	//-------------------------------------------------------------------------------------------------
	long countByMqttPublishedFalse();
}
