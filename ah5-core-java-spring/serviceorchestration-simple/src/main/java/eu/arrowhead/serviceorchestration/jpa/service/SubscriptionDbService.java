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

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.repository.SubscriptionRepository;
import eu.arrowhead.serviceorchestration.service.enums.BaseFilter;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionDbService {

	//=================================================================================================
	// members

	@Autowired
	private SubscriptionRepository subscriptionRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Subscription> create(final List<SimpleOrchestrationSubscriptionRequest> candidates, final String requesterSystemName) {
		logger.debug("create started..");
		Assert.isTrue(!Utilities.isEmpty(requesterSystemName), "requesterSystemName is empty");
		Assert.isTrue(!Utilities.isEmpty(candidates), "subscription candidate list is empty");
		Assert.isTrue(!Utilities.containsNull(candidates), "subscription candidate list contains null element");

		try {
			final List<UUID> toRemove = new ArrayList<>();
			final List<Subscription> toSave = new ArrayList<>(candidates.size());
			final ZonedDateTime now = Utilities.utcNow();
			for (final SimpleOrchestrationSubscriptionRequest candidate : candidates) {
				final Optional<Subscription> optional = subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(
						requesterSystemName,
						candidate.getTargetSystemName(),
						candidate.getOrchestrationRequest().getServiceDefinition());

				if (optional.isPresent()) {
					toRemove.add(optional.get().getId());
				}

				final ZonedDateTime expiresAt = candidate.getDuration() == null ? null : now.plusSeconds(candidate.getDuration());
				toSave.add(new Subscription(
						requesterSystemName,
						candidate.getTargetSystemName(),
						candidate.getOrchestrationRequest().getServiceDefinition(),
						expiresAt,
						candidate.getNotifyInterface().protocol(),
						Utilities.toJson(candidate.getNotifyInterface().properties()),
						Utilities.toJson((candidate.getOrchestrationRequest()))));
			}

			if (!Utilities.isEmpty(toRemove)) {
				subscriptionRepo.deleteAllById(toRemove);
				subscriptionRepo.flush();
			}

			return subscriptionRepo.saveAllAndFlush(toSave);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<Subscription> get(final String ownerSystem, final String targetSystem, final String serviceDefinition) {
		logger.debug("get started...");
		Assert.isTrue(!Utilities.isEmpty(ownerSystem), "ownerSystem is empty");
		Assert.isTrue(!Utilities.isEmpty(targetSystem), "targetSystem is empty");

		try {
			return subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(ownerSystem, targetSystem, serviceDefinition);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<Subscription> get(final UUID id) {
		logger.debug("get started...");
		Assert.notNull(id, "subscription id is null");

		try {
			return subscriptionRepo.findById(id);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<Subscription> get(final List<UUID> ids) {
		logger.debug("get started...");
		Assert.isTrue(!Utilities.isEmpty(ids), "subscription id list is empty");
		Assert.isTrue(!Utilities.containsNull(ids), "subscription id list contains null element");

		try {
			return subscriptionRepo.findAllById(ids);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean deleteById(final UUID id) {
		logger.debug("deleteById started..");
		Assert.notNull(id, "subscription id is null");

		try {
			if (subscriptionRepo.existsById(id)) {
				subscriptionRepo.deleteById(id);
				subscriptionRepo.flush();

				return true;
			}

			return false;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<Subscription> query(final List<String> ownerSystems, final List<String> targetSystems, final List<String> serviceDefinitions, final PageRequest pagination) {
		logger.debug("query started..");
		Assert.notNull(pagination, "pagination is null");

		try {
			BaseFilter baseFilter = BaseFilter.NONE;
			List<Subscription> toFilter;

			if (!Utilities.isEmpty(ownerSystems)) {
				toFilter = subscriptionRepo.findAllByOwnerSystemIn(ownerSystems);
				baseFilter = BaseFilter.OWNER;
			} else if (!Utilities.isEmpty(targetSystems)) {
				toFilter = subscriptionRepo.findAllByTargetSystemIn(targetSystems);
				baseFilter = BaseFilter.TARGET;
			} else if (!Utilities.isEmpty(serviceDefinitions)) {
				toFilter = subscriptionRepo.findAllByServiceDefinitionIn(serviceDefinitions);
				baseFilter = BaseFilter.SERVICE;
			} else {
				return subscriptionRepo.findAll(pagination);
			}

			final List<UUID> matchingIds = new ArrayList<>();
			for (final Subscription subscription : toFilter) {
				boolean matching = true;

				// No need to check owners, because it is the base filter if not empty

				if (baseFilter != BaseFilter.TARGET && !Utilities.isEmpty(targetSystems) && !targetSystems.contains(subscription.getTargetSystem())) {
					matching = false;

				} else if (baseFilter != BaseFilter.SERVICE && !Utilities.isEmpty(serviceDefinitions) && !serviceDefinitions.contains(subscription.getServiceDefinition())) {
					matching = false;
				}

				if (matching) {
					matchingIds.add(subscription.getId());
				}
			}

			return subscriptionRepo.findAllByIdIn(matchingIds, pagination);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteInBatch(final Collection<UUID> ids) {
		logger.debug("deleteInBatch started..");
		Assert.isTrue(!Utilities.isEmpty(ids), "subscription id list is empty");
		Assert.isTrue(!Utilities.containsNull(ids), "subscription id list contains null element");

		try {
			subscriptionRepo.deleteAllByIdInBatch(ids);
			subscriptionRepo.flush();
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
			final List<Subscription> toDelete = subscriptionRepo.findAllByExpiresAtBefore(time);
			if (!Utilities.isEmpty(toDelete)) {
				subscriptionRepo.deleteAllInBatch(toDelete);
				subscriptionRepo.flush();
			}
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

}
