/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.dto;

import java.util.List;
import java.util.Map;

public record TranslationDiscoveryMgmtRequestDTO(
		List<ServiceInstanceResponseDTO> candidates,
		String consumer,
		String operation,
		List<String> interfaceTemplateNames,
		String inputDataModelId,
		String outputDataModelId,
		Map<String, Boolean> flags) {
}