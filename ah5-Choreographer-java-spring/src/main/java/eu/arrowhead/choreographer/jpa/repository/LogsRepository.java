/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.choreographer.jpa.entity.Logs;
import eu.arrowhead.common.jpa.LogEntityRepository;

@Repository
public interface LogsRepository extends LogEntityRepository<Logs> {
}
