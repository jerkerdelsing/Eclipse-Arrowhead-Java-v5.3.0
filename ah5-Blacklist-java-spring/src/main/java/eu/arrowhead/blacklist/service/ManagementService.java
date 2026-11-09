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
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.blacklist.jpa.service.EntryDbService;
import eu.arrowhead.blacklist.service.dto.DTOConverter;
import eu.arrowhead.blacklist.service.dto.NormalizedBlacklistQueryRequestDTO;
import eu.arrowhead.blacklist.service.validation.Validation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.BlacklistCreateListRequestDTO;
import eu.arrowhead.dto.BlacklistCreateRequestDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;

@Service
public class ManagementService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private Validation validator;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private EntryDbService dbService;

	@Autowired
	private PageService pageService;

	@Autowired
	private WhitelistService whitelistService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public BlacklistEntryListResponseDTO query(final BlacklistQueryRequestDTO dto, final String origin) {
		logger.debug("ManagementService query started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final NormalizedBlacklistQueryRequestDTO normalized = validator.validateAndNormalizeBlacklistQueryRequestDTO(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, Entry.SORTABLE_FIELDS_BY, Entry.DEFAULT_SORT_FIELD, origin);

		try {
			final Page<Entry> matchingEntities = dbService.getPageByFilters(
					pageRequest,
					normalized.systemNames(),
					normalized.mode(),
					normalized.issuers(),
					normalized.revokers(),
					normalized.reason(),
					Utilities.parseUTCStringToZonedDateTime(normalized.alivesAt()));

			return dtoConverter.convertEntriesToBlacklistEntryListResponseDTO(matchingEntities.getContent(), matchingEntities.getTotalElements());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public BlacklistEntryListResponseDTO create(final BlacklistCreateListRequestDTO dto, final String requesterName, final String origin) {
		logger.debug("ManagementService create started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final BlacklistCreateListRequestDTO normalizedDto = validator.validateAndNormalizeBlacklistCreateListRequestDTO(dto, origin);
		final String normalizedRequesterName = validator.validateAndNormalizeSystemName(requesterName, origin);
		checkSelfBlacklisting(normalizedDto.entities(), normalizedRequesterName, origin);
		whitelistService.checkWhitelist(normalizedDto.entities().stream().map(e -> e.systemName()).collect(Collectors.toList()), origin);
		try {
			final List<Entry> createdEntities = dbService.createBulk(normalizedDto.entities(), normalizedRequesterName);

			return dtoConverter.convertEntriesToBlacklistEntryListResponseDTO(createdEntities, createdEntities.size());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void remove(final List<String> systemNameList, final boolean isSysop, final String revokerName, final String origin) {
		logger.debug("ManagementService remove started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalizedList = validator.validateAndNormalizeSystemNameList(systemNameList, origin);
		final String normalizedRevokerName = validator.validateAndNormalizeSystemName(revokerName, origin);

		// only sysop can remove itself from blacklist
		checkSysopRemoval(normalizedList, isSysop, normalizedRevokerName, origin);

		try {
			dbService.inactivateNameList(normalizedList, normalizedRevokerName);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void checkSelfBlacklisting(final List<BlacklistCreateRequestDTO> candidates, final String requesterName, final String origin) {
		logger.debug("checkSelfBlacklisting started...");

		for (final BlacklistCreateRequestDTO candidate : candidates) {
			if (candidate.systemName().equals(requesterName)) {
				throw new InvalidParameterException("It is not allowed to add yourself to the blacklist", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void checkSysopRemoval(final List<String> names, final boolean isSysop, final String requesterName, final String origin) {
		logger.debug("checkSysopRemoval started...");
		Assert.notNull(names, "System name list is null");

		if (names.contains(requesterName) && !isSysop) {
			throw new InvalidParameterException("Only sysop can remove itself from the blacklist", origin);
		}
	}
}