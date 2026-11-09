/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.choreographer.jpa.entity.Executor;
import eu.arrowhead.choreographer.jpa.entity.ExecutorServiceDefinition;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface ExecutorServiceDefinitionRepository extends RefreshableRepository<ExecutorServiceDefinition, Long> {

	List<ExecutorServiceDefinition> findAllByExecutor(final Executor executor);

	List<ExecutorServiceDefinition> findAllByServiceDefinition(final String serviceDefinition);

	Optional<ExecutorServiceDefinition> findByExecutorAndServiceDefinition(final Executor executor, final String serviceDefinition);
}
