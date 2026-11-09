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
package eu.arrowhead.serviceorchestration.jpa.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationLockRepository;
import eu.arrowhead.serviceorchestration.service.enums.BaseFilter;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationLockFilter;

@Service
public class OrchestrationLockDbService {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationLockRepository lockRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<OrchestrationLock> create(final List<OrchestrationLock> candidates) {
		logger.debug("create started...");
		Assert.isTrue(!Utilities.isEmpty(candidates), "Orchestration lock list is empty");
		Assert.isTrue(!Utilities.containsNull(candidates), "Orchestration lock list contains null element");

		try {
			return lockRepo.saveAllAndFlush(candidates);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationLock> getByServiceInstanceId(final List<String> ids) {
		logger.debug("getByServiceInstanceId started...");
		Assert.isTrue(!Utilities.isEmpty(ids), "Service instance id list is empty");
		Assert.isTrue(!Utilities.containsNullOrEmpty(ids), "Service instance id list contains null or empty element");

		try {
			return lockRepo.findAllByServiceInstanceIdIn(ids);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationLock> getAll() {
		logger.debug("getAll started...");

		try {
			return lockRepo.findAll();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<OrchestrationLock> query(final OrchestrationLockFilter filter, final PageRequest pagination) {
		logger.debug("query started...");
		Assert.notNull(filter, "filter is null");
		Assert.notNull(pagination, "pagination is null");

		try {
			final BaseFilter baseFilter = filter.getBaseFilter();
			List<OrchestrationLock> toFilter;

			if (baseFilter == BaseFilter.ID) {
				toFilter = lockRepo.findAllById(filter.getIds());
			} else if (baseFilter == BaseFilter.JOB) {
				toFilter = lockRepo.findAllByOrchestrationJobIdIn(filter.getOrchestrationJobIds());
			} else if (baseFilter == BaseFilter.SERVICE) {
				toFilter = lockRepo.findAllByServiceInstanceIdIn(filter.getServiceInstanceIds());
			} else if (baseFilter == BaseFilter.OWNER) {
				toFilter = lockRepo.findAllByOwnerIn(filter.getOwners());
			} else {
				toFilter = lockRepo.findAll();
			}

			final List<Long> matchingIds = new ArrayList<>();
			for (final OrchestrationLock lock : toFilter) {
				boolean matching = true;

				// Match against to lock ids
				if (baseFilter != BaseFilter.ID && !Utilities.isEmpty(filter.getIds()) && !filter.getIds().contains(lock.getId())) {
					matching = false; // cannot happen theoretically

					// Match against to orchestration job id
				} else if (baseFilter != BaseFilter.JOB
						&& !Utilities.isEmpty(filter.getOrchestrationJobIds())
						&& !Utilities.isEmpty(lock.getOrchestrationJobId())
						&& !filter.getOrchestrationJobIds().contains(lock.getOrchestrationJobId())) {
					matching = false;

					// Match against to service instance id
				} else if (baseFilter != BaseFilter.SERVICE && !Utilities.isEmpty(filter.getServiceInstanceIds()) && !filter.getServiceInstanceIds().contains(lock.getServiceInstanceId())) {
					matching = false;

					// Match against to owner
				} else if (baseFilter != BaseFilter.OWNER && !Utilities.isEmpty(filter.getOwners()) && !filter.getOwners().contains(lock.getOwner())) {
					matching = false;

					// Match against to expiration date
				} else if (lock.getExpiresAt() != null) {
					if ((filter.getExpiresBefore() != null && !lock.getExpiresAt().isBefore(filter.getExpiresBefore()))
							|| (filter.getExpiresAfter() != null && !lock.getExpiresAt().isAfter(filter.getExpiresAfter()))) {
						matching = false;
					}
				}

				if (matching) {
					matchingIds.add(lock.getId());
				}
			}

			return lockRepo.findAllByIdIn(matchingIds, pagination);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Optional<OrchestrationLock> changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(
			final String orchestrationJobId,
			final String serviceInstanceId,
			final ZonedDateTime time,
			final boolean isTemporary) {
		logger.debug("changeExpiresAtByOrchestrationJobIdAndServiceInstanceId started...");
		Assert.isTrue(!Utilities.isEmpty(orchestrationJobId), "Orchestration job id is empty");
		Assert.isTrue(!Utilities.isEmpty(serviceInstanceId), "Service instance id is empty");

		try {
			final Optional<OrchestrationLock> optional = lockRepo.findByOrchestrationJobIdAndServiceInstanceId(orchestrationJobId, serviceInstanceId);
			if (optional.isPresent()) {
				optional.get().setExpiresAt(time);
				optional.get().setTemporary(isTemporary);
				lockRepo.saveAndFlush(optional.get());
			}

			return optional;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteInBatch(final Collection<Long> ids) {
		logger.debug("deleteInBatch started...");
		Assert.isTrue(!Utilities.isEmpty(ids), "Id list is empty");
		Assert.isTrue(!Utilities.containsNull(ids), "Id list is contains null element");

		try {
			lockRepo.deleteAllByIdInBatch(ids);
			lockRepo.flush();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteInBatchByExpiredBefore(final ZonedDateTime time) {
		logger.debug("deleteInBatchByExpiredBefore started...");
		Assert.notNull(time, "time is null");

		try {
			final List<OrchestrationLock> toDelete = lockRepo.findAllByExpiresAtBefore(time);
			if (!Utilities.isEmpty(toDelete)) {
				lockRepo.deleteAllInBatch(toDelete);
				lockRepo.flush();
			}
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}