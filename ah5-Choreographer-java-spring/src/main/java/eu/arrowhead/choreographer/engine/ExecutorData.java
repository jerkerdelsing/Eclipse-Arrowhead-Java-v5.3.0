/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.engine;

import java.util.List;

import eu.arrowhead.choreographer.api.dto.ExecutorDependencyDTO;
import eu.arrowhead.choreographer.jpa.entity.Executor;

// A selected executor with the services it depends on (from its service-info answer).
public record ExecutorData(Executor executor, List<ExecutorDependencyDTO> dependencies, boolean usesOtherClouds) {
}
