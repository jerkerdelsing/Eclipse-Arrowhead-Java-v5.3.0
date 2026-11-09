/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.serviceorchestration.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationPlantRules;

public interface OrchestrationPlantRulesRepository extends JpaRepository<OrchestrationPlantRules, Integer> { }
