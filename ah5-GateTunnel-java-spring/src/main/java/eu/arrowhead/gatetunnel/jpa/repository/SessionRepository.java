/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.jpa.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.gatetunnel.jpa.entity.Session;

@Repository
public interface SessionRepository extends RefreshableRepository<Session, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	Optional<Session> findBySessionId(final String sessionId);

	//-------------------------------------------------------------------------------------------------
	Optional<Session> findByQueueIdAndStateNot(final String queueId, final String state);

	//-------------------------------------------------------------------------------------------------
	Optional<Session> findBySessionIdAndStateNot(final String sessionId, final String state);

	//-------------------------------------------------------------------------------------------------
	Optional<Session> findByTunnelPortAndStateNot(final int tunnelPort, final String state);

	//-------------------------------------------------------------------------------------------------
	List<Session> findAllByOrderByCreatedAtDescIdDesc(final Pageable pageable);

	//-------------------------------------------------------------------------------------------------
	List<Session> findByStateAndUpdatedAtBefore(final String state, final ZonedDateTime threshold);
}
