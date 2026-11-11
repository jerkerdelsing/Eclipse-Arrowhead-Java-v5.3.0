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

import java.util.List;

public record PackageRecord(String id, String designId, String designName, long designRevision, String imageMode, List<String> architectures,
		String fileName, String sha256, long sizeBytes, String createdBy, String createdAt) {
}
