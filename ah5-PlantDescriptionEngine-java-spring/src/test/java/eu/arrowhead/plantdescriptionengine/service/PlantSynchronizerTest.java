/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import eu.arrowhead.dto.OrchestrationPlantRuleDTO;
import eu.arrowhead.dto.OrchestrationPlantRulesDTO;

class PlantSynchronizerTest {
    private final PlantDescriptionService plants = mock(PlantDescriptionService.class);
    private final AlarmService alarms = mock(AlarmService.class);
    private final CloudClient cloud = mock(CloudClient.class);
    private PlantSynchronizer synchronizer() {
        var sync = new PlantSynchronizer();
        ReflectionTestUtils.setField(sync, "plantService", plants);
        ReflectionTestUtils.setField(sync, "alarmService", alarms);
        ReflectionTestUtils.setField(sync, "cloud", cloud);
        when(plants.activePlant()).thenReturn(Optional.empty());
        when(cloud.registeredSystems()).thenReturn(List.of());
        return sync;
    }
    @Test void deactivationClearsOldDsoSnapshot() {
        var sync = synchronizer();
        when(cloud.ownRules()).thenReturn(new OrchestrationPlantRulesDTO(List.of(new OrchestrationPlantRuleDTO("Consumer", "temperature", List.of()))));
        sync.sync();
        verify(cloud).replaceRules(new OrchestrationPlantRulesDTO(List.of()));
        assertTrue(sync.state().ok()); assertEquals(0, sync.state().rules());
    }
    @Test void unchangedSnapshotIsNotRewritten() {
        var sync = synchronizer();
        when(cloud.ownRules()).thenReturn(new OrchestrationPlantRulesDTO(List.of()));
        sync.sync();
        verify(cloud, never()).replaceRules(any()); assertTrue(sync.state().ok());
    }
    @Test void failedDsoQueryDoesNotClearRulesOrReportSuccess() {
        var sync = synchronizer();
        when(cloud.ownRules()).thenThrow(new IllegalStateException("DSO unavailable"));
        sync.sync();
        verify(cloud, never()).replaceRules(any()); verify(cloud).forgetEndpoints(); assertFalse(sync.state().ok());
        verify(alarms).replace(anyList(), any());
    }
}
