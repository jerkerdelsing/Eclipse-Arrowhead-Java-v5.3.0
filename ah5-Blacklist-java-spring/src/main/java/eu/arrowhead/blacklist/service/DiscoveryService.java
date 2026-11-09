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
package eu.arrowhead.blacklist.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.blacklist.jpa.service.EntryDbService;
import eu.arrowhead.blacklist.service.dto.DTOConverter;
import eu.arrowhead.blacklist.service.validation.Validation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;

@Service
public class DiscoveryService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private EntryDbService dbService;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private Validation validator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean check(final String systemName, final String origin) {
		logger.debug("check started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedName = validator.validateAndNormalizeSystemName(systemName, origin);

		try {
			return dbService.isActiveEntryForName(normalizedName);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public BlacklistEntryListResponseDTO lookup(final String requester, final String origin) {
		logger.debug("lookup started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedName = validator.validateAndNormalizeSystemName(requester, origin);

		try {
			final List<Entry> entries = dbService.getActiveEntriesForName(normalizedName);

			return dtoConverter.convertEntriesToBlacklistEntryListResponseDTO(entries, entries.size());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}