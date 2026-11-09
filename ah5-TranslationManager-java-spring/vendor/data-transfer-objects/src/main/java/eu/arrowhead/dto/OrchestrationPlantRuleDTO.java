/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.dto;

import java.util.List;

// A managed consumer/service scope. An empty provider list deliberately denies alternatives.
public record OrchestrationPlantRuleDTO(String consumer, String serviceDefinition, List<OrchestrationPlantProviderDTO> providers) {
}
