/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.dto;

import java.util.List;

// Atomically replaced snapshot of the active plant, including scopes with no matching provider.
public record OrchestrationPlantRulesDTO(List<OrchestrationPlantRuleDTO> rules) { }
