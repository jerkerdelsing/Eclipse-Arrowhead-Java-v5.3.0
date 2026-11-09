/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.choreographer.jpa.entity.Action;
import eu.arrowhead.choreographer.jpa.entity.Step;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface StepRepository extends RefreshableRepository<Step, Long> {

	List<Step> findAllByAction(final Action action);

	List<Step> findAllByActionIn(final List<Action> actions);

	List<Step> findAllByActionAndFirstStepTrue(final Action action);
}
