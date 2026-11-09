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
package eu.arrowhead.blacklist.service.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;

@Service
public class DTOConverter {
	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public BlacklistEntryListResponseDTO convertEntriesToBlacklistEntryListResponseDTO(final List<Entry> entryList, final long count) {
		logger.debug("convertEntryListToBlacklistEntryListResponseDTO started...");

		if (entryList == null) {
			return null;
		}

		return new BlacklistEntryListResponseDTO(
				entryList
				.stream()
				.map(e -> new BlacklistEntryDTO(
						// system name
						e.getSystemName(),
						// created by
						e.getCreatedBy(),
						// revoked by
						e.getRevokedBy(),
						// created at
						Utilities.convertZonedDateTimeToUTCString(e.getCreatedAt()),
						// updated at
						Utilities.convertZonedDateTimeToUTCString(e.getUpdatedAt()),
						// reason
						e.getReason(),
						// expires at
						Utilities.convertZonedDateTimeToUTCString(e.getExpiresAt()),
						// active
						e.getActive()
						))
				.collect(Collectors.toList()),
				count);
	}
}