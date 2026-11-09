/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.choreographer.jpa.entity.Action;
import eu.arrowhead.choreographer.jpa.entity.Plan;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface ActionRepository extends RefreshableRepository<Action, Long> {

	List<Action> findAllByPlan(final Plan plan);

	Optional<Action> findByPlanAndName(final Plan plan, final String name);
}
