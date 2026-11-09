/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.LogEntityRepository;
import eu.arrowhead.configuration.jpa.entity.Logs;

@Repository
public interface LogsRepository extends LogEntityRepository<Logs> {
}
