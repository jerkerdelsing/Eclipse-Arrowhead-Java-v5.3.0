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
package eu.arrowhead.blacklist.jpa.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.blacklist.jpa.repository.EntryRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.BlacklistCreateRequestDTO;
import eu.arrowhead.dto.enums.Mode;

@Service
public class EntryDbService {

	//=================================================================================================
	// members

	@Autowired
	private EntryRepository entryRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	private static final Object LOCK = new Object();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Entry> createBulk(final List<BlacklistCreateRequestDTO> candidates, final String requesterName) {
		logger.debug("createBulk started...");
		Assert.notNull(candidates, "candidates list is null");

		try {
			return entryRepo.saveAllAndFlush(createEntriesFromDTOs(candidates, requesterName));
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<Entry> getPageByFilters(
			final PageRequest pagination,
			final List<String> systemNames,
			final Mode mode,
			final List<String> issuers,
			final List<String> revokers,
			final String reason,
			final ZonedDateTime alivesAt) {
		logger.debug("getByFilters started...");
		Assert.notNull(pagination, "page is null");

		try {
			Page<Entry> entries = null;

			// Without filters
			if (Utilities.isEmpty(systemNames)
					&& mode == null
					&& Utilities.isEmpty(issuers)
					&& Utilities.isEmpty(revokers)
					&& Utilities.isEmpty(reason)
					&& alivesAt == null) {
				entries = entryRepo.findAll(pagination);
			}

			// With filters
			if (entries == null) {
				synchronized (LOCK) {
					final List<Long> matchingIDs = new ArrayList<>();
					final List<Entry> toFilter = Utilities.isEmpty(systemNames) ? entryRepo.findAll() : entryRepo.findAllBySystemNameIn(systemNames);
					for (final Entry entry : toFilter) {
						if (mode != null && !modeMatch(mode, entry.getActive())) {
							continue;
						}

						if (!Utilities.isEmpty(issuers) && !issuers.contains(entry.getCreatedBy())) {
							continue;
						}

						if (!Utilities.isEmpty(revokers) && (entry.getRevokedBy() == null || !revokers.contains(entry.getRevokedBy()))) {
							continue;
						}

						// reason match is case insensitive
						if (!Utilities.isEmpty(reason) && !entry.getReason().toLowerCase().contains(reason.toLowerCase())) {
							continue;
						}

						if (alivesAt != null && (!entry.getActive() || (entry.getExpiresAt() != null && alivesAt.isAfter(entry.getExpiresAt())))) {
							continue;
						}

						matchingIDs.add(entry.getId());
					}

					entries = entryRepo.findAllByIdIn(matchingIDs, pagination);
				}
			}

			return entries;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void inactivateNameList(final List<String> names, final String revokerName) {
		logger.debug("inactivateNameList started...");
		Assert.isTrue(!Utilities.isEmpty(names), "System name list is missing or empty");
		Assert.isTrue(!Utilities.isEmpty(revokerName), "Revoker name is missing or empty");

		try {
			synchronized (LOCK) {
				final List<Entry> toInactivate = entryRepo.findAllBySystemNameIn(names)
						.stream()
						.filter(Entry::getActive)
						.collect(Collectors.toList());
				for (final Entry entry : toInactivate) {
					entry.inactivate(revokerName);
				}
				entryRepo.saveAllAndFlush(toInactivate);
			}
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Returns true, if there is a record with the given system name, the active flag is set, and the expiration date is in the future.
	public boolean isActiveEntryForName(final String systemName) {
		logger.debug("isActiveEntryForName started, name: {}", systemName);
		Assert.isTrue(!Utilities.isEmpty(systemName), "System name is missing or empty");

		try {
			synchronized (LOCK) {
				// finding the matching system name
				final List<Entry> entries = entryRepo.findAllBySystemNameAndActive(systemName, true);
				for (final Entry entry : entries) {
					if (!isExpired(entry)) {
						return true;
					}
				}

				return false;
			}
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<Entry> getActiveEntriesForName(final String systemName) {
		logger.debug("getActiveEntriesForName started, name: {}", systemName);
		Assert.isTrue(!Utilities.isEmpty(systemName), "System name is missing or empty");

		try {
			synchronized (LOCK) {
				final List<Entry> entries = entryRepo.findAllBySystemNameAndActive(systemName, true);

				return entries
						.stream()
						.filter(e -> !isExpired(e))
						.toList();
			}
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<Entry> createEntriesFromDTOs(final List<BlacklistCreateRequestDTO> candidates, final String requesterName) {
		return candidates
				.stream()
				.map(c -> new Entry(
						// system name
						c.systemName(),
						// expires at
						Utilities.parseUTCStringToZonedDateTime(c.expiresAt()),
						// created by
						requesterName,
						// reason
						c.reason()))
				.collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	// Returns true, if the record activity state matches the filtering mode
	private boolean modeMatch(final Mode mode, final boolean active) {
		switch (mode) {
		case Mode.ACTIVES:
			return active;
		case Mode.INACTIVES:
			return !active;
		default:
			return true;
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Returns true, if the expiration date is not null and it is in the past
	private boolean isExpired(final Entry entry) {
		return entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(Utilities.utcNow());
	}
}