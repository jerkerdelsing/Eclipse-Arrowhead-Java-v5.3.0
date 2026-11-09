/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

// Published after any plant description change so the rules and alarms are reconciled at once.
public record PlantDescriptionChangedEvent(long plantDescriptionId) {
}
