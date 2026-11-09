/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.serviceorchestration.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationPlantRuleDTO;
import eu.arrowhead.dto.OrchestrationPlantRulesDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationPlantRules;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationPlantRulesRepository;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;

@Service
public class PlantRuleService {
    public static final String DEFINITION = "serviceOrchestrationPlantManagement";
    public static final String PATH = "/serviceorchestration/mgmt/plant-rules";
    private final OrchestrationPlantRulesRepository repository;

    public PlantRuleService(OrchestrationPlantRulesRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public OrchestrationPlantRulesDTO query() {
        return repository.findById(1).map(row -> Utilities.fromJson(row.getPayload(), OrchestrationPlantRulesDTO.class))
                .orElseGet(() -> new OrchestrationPlantRulesDTO(List.of()));
    }

    @Transactional
    public synchronized OrchestrationPlantRulesDTO replace(OrchestrationPlantRulesDTO snapshot) {
        final OrchestrationPlantRulesDTO normalized = validate(snapshot);
        repository.saveAndFlush(new OrchestrationPlantRules(Utilities.toJson(normalized)));
        return normalized;
    }

    public OrchestrationPlantRuleDTO find(String consumer, String definition) {
        return query().rules().stream().filter(r -> r.consumer().equals(consumer) && r.serviceDefinition().equals(definition)).findFirst().orElse(null);
    }

    // Called before normal checks to restrict the admissible set, then after all checks to rank survivors.
    // This cannot grant authorization, bypass a lock, or resurrect a rejected candidate.
    public static List<OrchestrationCandidate> select(OrchestrationPlantRuleDTO rule, List<OrchestrationCandidate> candidates, boolean bestOnly) {
        if (rule == null) { return candidates; }
        Map<String, Integer> priorities = rule.providers().stream().collect(Collectors.toMap(
                eu.arrowhead.dto.OrchestrationPlantProviderDTO::serviceInstanceId, eu.arrowhead.dto.OrchestrationPlantProviderDTO::priority));
        List<OrchestrationCandidate> selected = candidates.stream()
                .filter(c -> priorities.containsKey(c.getServiceInstance().instanceId()))
                .sorted(Comparator.comparingInt(c -> priorities.get(c.getServiceInstance().instanceId())))
                .collect(Collectors.toCollection(ArrayList::new));
        if (bestOnly && !selected.isEmpty()) {
            int best = priorities.get(selected.getFirst().getServiceInstance().instanceId());
            selected.removeIf(c -> priorities.get(c.getServiceInstance().instanceId()) != best);
        }
        return selected;
    }

    static OrchestrationPlantRulesDTO validate(OrchestrationPlantRulesDTO snapshot) {
        if (snapshot == null || snapshot.rules() == null || snapshot.rules().size() > 10000) { throw invalid(); }
        var scopes = new HashSet<String>();
        var rules = new ArrayList<OrchestrationPlantRuleDTO>();
        for (var rule : snapshot.rules()) {
            if (rule == null || rule.consumer() == null || !rule.consumer().matches("[A-Z][A-Za-z0-9]{0,62}")
                    || rule.serviceDefinition() == null || !rule.serviceDefinition().matches("[a-z][A-Za-z0-9]{0,62}")
                    || rule.providers() == null || rule.providers().size() > 1000
                    || !scopes.add(rule.consumer() + "|" + rule.serviceDefinition())) { throw invalid(); }
            var ids = new HashSet<String>();
            for (var provider : rule.providers()) {
                if (provider == null || provider.serviceInstanceId() == null || provider.serviceInstanceId().isBlank()
                        || provider.serviceInstanceId().length() > 255 || provider.priority() == null || provider.priority() < 0
                        || !ids.add(provider.serviceInstanceId())) { throw invalid(); }
            }
            rules.add(new OrchestrationPlantRuleDTO(rule.consumer(), rule.serviceDefinition(), rule.providers().stream()
                    .sorted(Comparator.comparingInt(eu.arrowhead.dto.OrchestrationPlantProviderDTO::priority)
                            .thenComparing(eu.arrowhead.dto.OrchestrationPlantProviderDTO::serviceInstanceId)).toList()));
        }
        rules.sort(Comparator.comparing(OrchestrationPlantRuleDTO::consumer).thenComparing(OrchestrationPlantRuleDTO::serviceDefinition));
        return new OrchestrationPlantRulesDTO(List.copyOf(rules));
    }

    private static InvalidParameterException invalid() { return new InvalidParameterException("Invalid plant-rule snapshot"); }
}
