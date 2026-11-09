/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.serviceorchestration.api.http;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.dto.OrchestrationPlantRulesDTO;
import eu.arrowhead.serviceorchestration.service.PlantRuleService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

// The common management filter must grant the operation AND the authenticated caller must be PDE/Sysop.
@RestController
@RequestMapping("/serviceorchestration/mgmt/plant-rules")
public class OrchestrationPlantManagementAPI {
    private final PlantRuleService rules;
    public OrchestrationPlantManagementAPI(PlantRuleService rules) { this.rules = rules; }

    @Operation(summary = "Returns the active engineered plant snapshot")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "401"), @ApiResponse(responseCode = "403")})
    @PostMapping("/query")
    public OrchestrationPlantRulesDTO query(HttpServletRequest request) {
        String caller = HttpUtilities.acquireName(request, PlantRuleService.PATH);
        if (!"PlantDescriptionEngine".equals(caller) && !"LocalCloudManager".equals(caller)
                && !HttpUtilities.isSysop(request, PlantRuleService.PATH)) { throw new ForbiddenException("Plant rules are restricted"); }
        return rules.query();
    }

    @Operation(summary = "Atomically replaces all active-plant scopes; empty rules clears the snapshot")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "401"), @ApiResponse(responseCode = "403")})
    @PutMapping(path = "/replace")
    public OrchestrationPlantRulesDTO replace(HttpServletRequest request, @RequestBody OrchestrationPlantRulesDTO snapshot) {
        String caller = HttpUtilities.acquireName(request, PlantRuleService.PATH);
        if (!"PlantDescriptionEngine".equals(caller) && !HttpUtilities.isSysop(request, PlantRuleService.PATH)) {
            throw new ForbiddenException("Only PlantDescriptionEngine or Sysop may replace plant rules");
        }
        return rules.replace(snapshot);
    }
}
