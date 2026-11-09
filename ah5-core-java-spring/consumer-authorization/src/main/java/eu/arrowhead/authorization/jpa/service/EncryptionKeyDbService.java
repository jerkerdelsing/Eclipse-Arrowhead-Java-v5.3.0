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
package eu.arrowhead.authorization.jpa.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.CryptographerAuxiliary;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.repository.CryptographerAuxiliaryRepository;
import eu.arrowhead.authorization.jpa.repository.EncryptionKeyRepository;
import eu.arrowhead.authorization.service.model.EncryptionKeyModel;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class EncryptionKeyDbService {

	//=================================================================================================
	// members

	@Autowired
	private EncryptionKeyRepository keyRepo;

	@Autowired
	private CryptographerAuxiliaryRepository auxiliaryRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<EncryptionKey> get(final String systemName) {
		logger.debug("get started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");

		try {
			return keyRepo.findBySystemName(systemName);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<EncryptionKey, Boolean> save(final EncryptionKeyModel candidate) {
		logger.debug("save started...");
		Assert.notNull(candidate, "EncryptionKeyModel candidate is null");
		Assert.isTrue(!Utilities.isEmpty(candidate.getSystemName()), "EncryptionKeyModel.systemName is empty");
		Assert.isTrue(!Utilities.isEmpty(candidate.getKeyEncryptedValue()), "EncryptionKeyModel.keyEncryptedValue is empty");
		Assert.isTrue(!Utilities.isEmpty(candidate.getAlgorithm()), "EncryptionKeyModel.algorithm is empty");
		Assert.isTrue(!Utilities.isEmpty(candidate.getInternalAuxiliary()), "EncryptionKeyModel.internalAuxiliary is empty");

		try {
			boolean override = false;
			final Optional<EncryptionKey> optional = keyRepo.findBySystemName(candidate.getSystemName());
			if (optional.isPresent()) {
				keyRepo.delete(optional.get());
				auxiliaryRepo.delete(optional.get().getInternalAuxiliary());
				if (optional.get().getExternalAuxiliary() != null) {
					auxiliaryRepo.delete(optional.get().getExternalAuxiliary());
				}
				override = true;
			}

			final CryptographerAuxiliary internalAuxiliaryRecord = auxiliaryRepo.saveAndFlush(new CryptographerAuxiliary(candidate.getInternalAuxiliary()));
			final CryptographerAuxiliary externalAuxiliaryRecord = !candidate.hasExternalAuxiliary() ? null : auxiliaryRepo.saveAndFlush(new CryptographerAuxiliary(candidate.getExternalAuxiliary()));
			final EncryptionKey toSave = new EncryptionKey(candidate.getSystemName(), candidate.getKeyEncryptedValue(), candidate.getAlgorithm(), internalAuxiliaryRecord, externalAuxiliaryRecord);

			return Pair.of(keyRepo.saveAndFlush(toSave), !override);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<EncryptionKey> save(final List<EncryptionKeyModel> candidates) {
		logger.debug("save started...");
		Assert.notNull(candidates, "EncryptionKeyModel candidate list is null");

		try {
			final Set<String> sysNames = new HashSet<>();
			final List<EncryptionKey> keysToDelete = new ArrayList<>();
			final List<CryptographerAuxiliary> auxiliariesToDelete = new ArrayList<>();
			final List<EncryptionKey> keysToSave = new ArrayList<>();
			final List<CryptographerAuxiliary> auxiliariesToSave = new ArrayList<>();

			for (final EncryptionKeyModel candidate : candidates) {
				Assert.notNull(candidate, "EncryptionKeyModel candidate is null");
				Assert.isTrue(!Utilities.isEmpty(candidate.getSystemName()), "EncryptionKeyModel.systemName is empty");
				Assert.isTrue(!Utilities.isEmpty(candidate.getKeyEncryptedValue()), "EncryptionKeyModel.keyEncryptedValue is empty");
				Assert.isTrue(!Utilities.isEmpty(candidate.getAlgorithm()), "EncryptionKeyModel.algorithm is empty");
				Assert.isTrue(!Utilities.isEmpty(candidate.getInternalAuxiliary()), "EncryptionKeyModel.internalAuxiliary is empty");

				Assert.isTrue(!sysNames.contains(candidate.getSystemName()), "Duplicate EncryptionKeyModel for system: " + candidate.getSystemName());
				sysNames.add(candidate.getSystemName());

				final Optional<EncryptionKey> keyOpt = keyRepo.findBySystemName(candidate.getSystemName());
				if (keyOpt.isPresent()) {
					keysToDelete.add(keyOpt.get());
					auxiliariesToDelete.add(keyOpt.get().getInternalAuxiliary());
					if (keyOpt.get().getExternalAuxiliary() != null) {
						auxiliariesToDelete.add(keyOpt.get().getExternalAuxiliary());
					}
				}

				final EncryptionKey toSave = new EncryptionKey(
						candidate.getSystemName(),
						candidate.getKeyEncryptedValue(),
						candidate.getAlgorithm(),
						new CryptographerAuxiliary(candidate.getInternalAuxiliary()),
						!candidate.hasExternalAuxiliary() ? null : new CryptographerAuxiliary(candidate.getExternalAuxiliary()));

				keysToSave.add(toSave);
				auxiliariesToSave.add(toSave.getInternalAuxiliary());
				if (candidate.hasExternalAuxiliary()) {
					auxiliariesToSave.add(toSave.getExternalAuxiliary());
				}
			}

			keyRepo.deleteAll(keysToDelete);
			auxiliaryRepo.deleteAll(auxiliariesToDelete);
			auxiliaryRepo.flush();
			keyRepo.flush();

			auxiliaryRepo.saveAllAndFlush(auxiliariesToSave);

			return keyRepo.saveAllAndFlush(keysToSave);
		} catch (final IllegalArgumentException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean delete(final String systemName) {
		logger.debug("delete started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");

		try {
			final Optional<EncryptionKey> optional = keyRepo.findBySystemName(systemName);
			if (optional.isEmpty()) {
				return false;
			}

			final EncryptionKey entry = optional.get();

			keyRepo.deleteById(entry.getId());
			auxiliaryRepo.deleteById(entry.getInternalAuxiliary().getId());
			if (entry.getExternalAuxiliary() != null) {
				auxiliaryRepo.deleteById(entry.getExternalAuxiliary().getId());
			}

			keyRepo.flush();
			auxiliaryRepo.flush();

			return true;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void delete(final List<String> systemNames) {
		logger.debug("delete started...");
		Assert.isTrue(!Utilities.isEmpty(systemNames), "systemNames list is empty");
		Assert.isTrue(!Utilities.containsNullOrEmpty(systemNames), "systemNames list contains null or empty element");

		try {
			final List<CryptographerAuxiliary> auxiliariesToDelete = new ArrayList<>();
			final List<EncryptionKey> keysToDelete = new ArrayList<>(systemNames.size());

			for (final String name : systemNames) {
				final Optional<EncryptionKey> optional = keyRepo.findBySystemName(name);
				if (optional.isEmpty()) {
					continue;
				}

				final EncryptionKey entry = optional.get();
				auxiliariesToDelete.add(entry.getInternalAuxiliary());
				if (entry.getExternalAuxiliary() != null) {
					auxiliariesToDelete.add(entry.getExternalAuxiliary());
				}
				keysToDelete.add(entry);
			}

			auxiliaryRepo.deleteAllInBatch(auxiliariesToDelete);
			keyRepo.deleteAllInBatch(keysToDelete);
			auxiliaryRepo.flush();
			keyRepo.flush();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}