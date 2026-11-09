/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.choreographer.api.dto.enums.SessionStepStatus;
import eu.arrowhead.choreographer.jpa.entity.Executor;
import eu.arrowhead.choreographer.jpa.entity.Session;
import eu.arrowhead.choreographer.jpa.entity.SessionStep;
import eu.arrowhead.choreographer.jpa.entity.Step;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface SessionStepRepository extends RefreshableRepository<SessionStep, Long> {

	List<SessionStep> findAllBySession(final Session session);

	List<SessionStep> findAllBySessionAndExecutionNumber(final Session session, final long executionNumber);

	Optional<SessionStep> findBySessionAndStepAndExecutionNumber(final Session session, final Step step, final long executionNumber);

	List<SessionStep> findAllBySessionAndExecutionNumberAndStepIn(final Session session, final long executionNumber, final List<Step> steps);

	List<SessionStep> findAllBySessionAndStatusIn(final Session session, final List<SessionStepStatus> statuses);

	List<SessionStep> findAllByExecutor(final Executor executor);

	boolean existsByExecutorAndStatusIn(final Executor executor, final List<SessionStepStatus> statuses);

	Page<SessionStep> findAllBySession(final Session session, final Pageable pageable);

	Page<SessionStep> findAllByStatus(final SessionStepStatus status, final Pageable pageable);

	Page<SessionStep> findAllBySessionAndStatus(final Session session, final SessionStepStatus status, final Pageable pageable);
}
