/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.serviceorchestration.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.*;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationPlantRules;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationPlantRulesRepository;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;

class PlantRuleServiceTest {
    private final OrchestrationPlantRulesRepository repository = mock(OrchestrationPlantRulesRepository.class);
    private final PlantRuleService service = new PlantRuleService(repository);
    private OrchestrationPlantRuleDTO rule() {
        return new OrchestrationPlantRuleDTO("Consumer", "temperature", List.of(
                new eu.arrowhead.dto.OrchestrationPlantProviderDTO("primary", 1), new eu.arrowhead.dto.OrchestrationPlantProviderDTO("backup", 2)));
    }
    private OrchestrationCandidate candidate(String id) {
        var candidate = new OrchestrationCandidate(new ServiceInstanceResponseDTO(id, null, null, null, null, null, null, null, null), true, false);
        return candidate;
    }
    @Test void snapshotSurvivesServiceRestart() {
        var snapshot = new OrchestrationPlantRulesDTO(List.of(rule()));
        when(repository.findById(1)).thenReturn(Optional.of(new OrchestrationPlantRules(Utilities.toJson(snapshot))));
        assertEquals(snapshot, new PlantRuleService(repository).query());
        assertEquals(rule(), service.find("Consumer", "temperature"));
        assertNull(service.find("Other", "temperature"));
    }
    @Test void ranksOnlyPresentCandidatesAndUsesBackupWhenPrimaryWasRejected() {
        var primary = candidate("primary"); var backup = candidate("backup"); var unrelated = candidate("other");
        assertEquals(List.of(primary, backup), PlantRuleService.select(rule(), List.of(unrelated, backup, primary), false));
        assertEquals(List.of(backup), PlantRuleService.select(rule(), List.of(backup, unrelated), true));
        assertTrue(PlantRuleService.select(rule(), List.of(unrelated), true).isEmpty());
    }
    @Test void managedEmptyScopeDeniesAlternativesButUnmanagedScopeIsUnchanged() {
        var candidates = List.of(candidate("other"));
        assertTrue(PlantRuleService.select(new OrchestrationPlantRuleDTO("Consumer", "temperature", List.of()), candidates, false).isEmpty());
        assertSame(candidates, PlantRuleService.select(null, candidates, false));
    }
    @Test void rejectsMalformedSnapshotWithoutErasingExistingRules() {
        assertThrows(InvalidParameterException.class, () -> service.replace(new OrchestrationPlantRulesDTO(null)));
        assertThrows(InvalidParameterException.class, () -> service.replace(new OrchestrationPlantRulesDTO(List.of(rule(), rule()))));
        assertThrows(InvalidParameterException.class, () -> service.replace(new OrchestrationPlantRulesDTO(List.of(
                new OrchestrationPlantRuleDTO("Consumer", "temperature", List.of(new eu.arrowhead.dto.OrchestrationPlantProviderDTO("id", -1)))))));
        verifyNoInteractions(repository);
    }
    @Test void zeroPriorityFromExistingPlantDescriptionsRemainsValid() {
        var snapshot = new OrchestrationPlantRulesDTO(List.of(new OrchestrationPlantRuleDTO("Consumer", "temperature",
                List.of(new eu.arrowhead.dto.OrchestrationPlantProviderDTO("id", 0)))));
        assertEquals(snapshot, service.replace(snapshot));
    }
    @Test void explicitEmptySnapshotClearsActivePlant() {
        var empty = new OrchestrationPlantRulesDTO(List.of());
        assertEquals(empty, service.replace(empty));
        var captured = org.mockito.ArgumentCaptor.forClass(OrchestrationPlantRules.class);
        verify(repository).saveAndFlush(captured.capture());
        assertEquals(empty, Utilities.fromJson(captured.getValue().getPayload(), OrchestrationPlantRulesDTO.class));
    }
}
