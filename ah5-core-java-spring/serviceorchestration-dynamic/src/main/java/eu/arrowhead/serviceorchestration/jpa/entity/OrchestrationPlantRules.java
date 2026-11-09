/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.serviceorchestration.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OrchestrationPlantRules {
    @Id
    private int id;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String payload;

    public OrchestrationPlantRules() { }
    public OrchestrationPlantRules(String payload) { this.id = 1; this.payload = payload; }
    public int getId() { return id; }
    public String getPayload() { return payload; }
}
