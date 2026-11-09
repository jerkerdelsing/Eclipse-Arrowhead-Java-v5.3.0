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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record TranslationQueryResponseDTO(
		String bridgeId,
		String status,
		int usageReportCount,
		String alivesAt,
		String message,
		String consumer,
		String provider,
		String serviceDefinition,
		String operation,
		String interfaceTranslator,
		TranslationInterfaceTranslationDataDescriptorDTO interfaceTranslatorData,
		String inputDataModelTranslator,
		TranslationDataModelTranslationDataDescriptorDTO inputDataModelTranslatorData,
		String outputDataModelTranslator,
		TranslationDataModelTranslationDataDescriptorDTO outputDataModelTranslatorData,
		String createdBy,
		String createdAt,
		String updatedAt) {
}