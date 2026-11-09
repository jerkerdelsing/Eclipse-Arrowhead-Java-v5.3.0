/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.serviceorchestration.api.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.dto.OrchestrationPlantRulesDTO;
import eu.arrowhead.serviceorchestration.service.PlantRuleService;

class OrchestrationPlantManagementAPITest {
    private final PlantRuleService rules = mock(PlantRuleService.class);
    private final OrchestrationPlantManagementAPI api = new OrchestrationPlantManagementAPI(rules);
    private MockHttpServletRequest request(String name) {
        var request = new MockHttpServletRequest();
        request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, name);
        return request;
    }
    @Test void consoleMayReadButCannotReplaceEvenIfManagementPolicyWereBroadened() {
        var request = request("LocalCloudManager");
        api.query(request);
        assertThrows(ForbiddenException.class, () -> api.replace(request, new OrchestrationPlantRulesDTO(List.of())));
        verify(rules).query(); verifyNoMoreInteractions(rules);
    }
    @Test void unrelatedCallerCannotReadOrWrite() {
        assertThrows(ForbiddenException.class, () -> api.query(request("Other")));
        assertThrows(ForbiddenException.class, () -> api.replace(request("Other"), new OrchestrationPlantRulesDTO(List.of())));
        verifyNoInteractions(rules);
    }
    @Test void authenticatedPdeCanReplace() {
        var snapshot = new OrchestrationPlantRulesDTO(List.of());
        api.replace(request("PlantDescriptionEngine"), snapshot);
        verify(rules).replace(snapshot);
    }
}
