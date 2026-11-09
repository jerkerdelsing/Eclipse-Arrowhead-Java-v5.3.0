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
package eu.arrowhead.authentication.service.dto;

import java.time.ZonedDateTime;

import org.springframework.data.domain.PageRequest;

public record NormalizedIdentityQueryRequestDTO(
		PageRequest pageRequest,
		String namePart,
		Boolean isSysop,
		String createdBy,
		ZonedDateTime creationFrom,
		ZonedDateTime creationTo,
		Boolean hasSession) {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean hasFilters() {
		return namePart != null
				|| isSysop != null
				|| createdBy != null
				|| creationFrom != null
				|| creationTo != null
				|| hasSession != null;
	}
}