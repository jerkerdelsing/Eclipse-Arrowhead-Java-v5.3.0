/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.choreographer.api.dto.enums.SessionStatus;
import eu.arrowhead.choreographer.jpa.entity.Plan;
import eu.arrowhead.choreographer.jpa.entity.Session;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface SessionRepository extends RefreshableRepository<Session, Long> {

	List<Session> findAllByPlan(final Plan plan);

	List<Session> findAllByPlanAndStatusIn(final Plan plan, final List<SessionStatus> statuses);

	List<Session> findAllByStatusIn(final List<SessionStatus> statuses);

	Page<Session> findAllByPlan(final Plan plan, final Pageable pageable);

	Page<Session> findAllByStatus(final SessionStatus status, final Pageable pageable);

	Page<Session> findAllByPlanAndStatus(final Plan plan, final SessionStatus status, final Pageable pageable);
}
