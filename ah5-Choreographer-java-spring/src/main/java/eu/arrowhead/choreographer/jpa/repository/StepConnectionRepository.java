/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.choreographer.jpa.entity.Step;
import eu.arrowhead.choreographer.jpa.entity.StepConnection;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface StepConnectionRepository extends RefreshableRepository<StepConnection, Long> {

	List<StepConnection> findAllByFrom(final Step from);

	List<StepConnection> findAllByTo(final Step to);

	List<StepConnection> findAllByFromIn(final List<Step> from);
}
