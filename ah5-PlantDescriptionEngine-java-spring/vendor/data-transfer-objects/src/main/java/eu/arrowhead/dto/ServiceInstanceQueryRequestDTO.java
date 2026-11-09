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

public record ServiceInstanceQueryRequestDTO(
		PageDTO pagination,
		List<String> instanceIds,
		List<String> providerNames,
		List<String> serviceDefinitionNames,
		List<String> versions,
		String alivesAt,
		List<MetadataRequirementDTO> metadataRequirementsList,
		List<String> addressTypes,
		List<String> interfaceTemplateNames,
		List<MetadataRequirementDTO> interfacePropertyRequirementsList,
		List<String> policies) {
}