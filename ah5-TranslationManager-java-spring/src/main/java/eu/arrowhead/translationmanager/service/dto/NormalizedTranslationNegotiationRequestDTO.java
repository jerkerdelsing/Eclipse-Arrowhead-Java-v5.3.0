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
package eu.arrowhead.translationmanager.service.dto;

import java.util.UUID;

public record NormalizedTranslationNegotiationRequestDTO(
		UUID bridgeId,
		NormalizedServiceInstanceDTO target,
		String operation,
		String interfaceTemplateName,
		String inputDataModelId,
		String outputDataModelId) {
}