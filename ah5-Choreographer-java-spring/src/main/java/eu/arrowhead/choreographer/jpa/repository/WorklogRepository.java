/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.choreographer.jpa.entity.Worklog;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface WorklogRepository extends RefreshableRepository<Worklog, Long> {

	List<Worklog> findAllBySessionId(final Long sessionId);
}
