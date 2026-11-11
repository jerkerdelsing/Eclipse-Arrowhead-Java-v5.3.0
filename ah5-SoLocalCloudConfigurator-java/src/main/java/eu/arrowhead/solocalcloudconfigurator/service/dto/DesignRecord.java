/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.service.dto;

import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign;

public record DesignRecord(String id, long revision, String updatedBy, String updatedAt, SolcDesign design) {
}
