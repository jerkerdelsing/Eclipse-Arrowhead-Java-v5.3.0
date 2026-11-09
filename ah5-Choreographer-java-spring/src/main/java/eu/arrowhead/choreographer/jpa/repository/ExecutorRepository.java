/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.choreographer.jpa.entity.Executor;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface ExecutorRepository extends RefreshableRepository<Executor, Long> {

	Optional<Executor> findByName(final String name);
}
