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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import org.apache.commons.lang3.tuple.Triple;
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
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationStoreRepository;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;

@Service
public class SimpleStoreDbService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private OrchestrationStoreRepository storeRepo;

	private static final Object LOCK = new Object();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<OrchestrationStore> createBulk(final List<OrchestrationSimpleStoreRequestDTO> candidates, final String requesterName) {
		logger.debug("createBulk started...");
		Assert.isTrue(!Utilities.isEmpty(requesterName), "requesterName is empty");
		Assert.isTrue(!Utilities.isEmpty(candidates), "candidate list is empty");
		Assert.isTrue(!Utilities.containsNull(candidates), "candidate list contains null element");

		final List<OrchestrationStore> toSave = candidates
				.stream().map(n -> new OrchestrationStore(
						n.consumer(),
						ServiceInstanceIdUtils.retrieveServiceDefinitionFromInstanceId(n.serviceInstanceId()),
						n.serviceInstanceId(),
						n.priority(),
						requesterName)).collect(Collectors.toList());
		try {
			checkUniqueFields(candidates);
			return storeRepo.saveAllAndFlush(toSave);
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:parameternumber")
	public Page<OrchestrationStore> getPageByFilters(
			final PageRequest pagination,
			final List<UUID> ids,
			final List<String> consumerNames,
			final List<String> serviceDefinitions,
			final List<String> serviceInstanceIds,
			final Integer minPriority,
			final Integer maxPriority,
			final String createdBy) {

		logger.debug("getPage started...");
		Assert.notNull(pagination, "page is null");

		try {
			// without filters
			if (Utilities.isEmpty(ids)
					&& Utilities.isEmpty(serviceInstanceIds)
					&& Utilities.isEmpty(serviceDefinitions)
					&& Utilities.isEmpty(consumerNames)
					&& Utilities.isEmpty(createdBy)
					&& minPriority == null
					&& maxPriority == null) {
				return storeRepo.findAll(pagination);
			}

			// with filters
			BaseFilter baseFilter = BaseFilter.NONE;
			List<OrchestrationStore> toFilter = new ArrayList<>();

			if (!Utilities.isEmpty(ids)) {
				baseFilter = BaseFilter.ID;
				toFilter = storeRepo.findAllById(ids);
			} else if (!Utilities.isEmpty(consumerNames)) {
				baseFilter = BaseFilter.CONSUMER;
				toFilter = storeRepo.findAllByConsumerIn(consumerNames);
			} else if (!Utilities.isEmpty(serviceDefinitions)) {
				baseFilter = BaseFilter.DEFINITION;
				toFilter = storeRepo.findAllByServiceDefinitionIn(serviceDefinitions);
			} else if (!Utilities.isEmpty(serviceInstanceIds)) {
				baseFilter = BaseFilter.INSTANCE;
				toFilter = storeRepo.findAllByServiceInstanceIdIn(serviceInstanceIds);
			} else if (!Utilities.isEmpty(createdBy)) {
				baseFilter = BaseFilter.CREATOR;
				toFilter = storeRepo.findAllByCreatedBy(createdBy);
			} else {
				toFilter = storeRepo.findAll();
			}
			final List<UUID> matchingIds = new ArrayList<UUID>();
			for (final OrchestrationStore entity : toFilter) {

				// match against id not needed, because if it was not empty, it was the baseFilter

				// match against consumer
				if (baseFilter != BaseFilter.CONSUMER && consumerNames != null && !consumerNames.contains(entity.getConsumer())) {
					continue;
				}

				// match against service definition
				if (baseFilter != BaseFilter.DEFINITION && serviceDefinitions != null && !serviceDefinitions.contains(entity.getServiceDefinition())) {
					continue;
				}

				// match against service instance id
				if (baseFilter != BaseFilter.INSTANCE && serviceInstanceIds != null && !serviceInstanceIds.contains(entity.getServiceInstanceId())) {
					continue;
				}

				// match against created by
				if (baseFilter != BaseFilter.CREATOR && createdBy != null && !createdBy.equals(entity.getCreatedBy())) {
					continue;
				}

				// match against minimum priority
				if (minPriority != null && entity.getPriority() < minPriority) {
					continue;
				}

				// match against maximum priority
				if (maxPriority != null && entity.getPriority() > maxPriority) {
					continue;
				}
				matchingIds.add(entity.getId());
			}

			return storeRepo.findAllByIdIn(matchingIds, pagination);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> getByConsumer(final String consumer) {
		logger.debug("getByConsumer started...");
		assertTrue(!Utilities.isEmpty(consumer), "consumer is empty");

		try {
			return storeRepo.findAllByConsumerOrderByPriorityAsc(consumer);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> getByConsumerAndServiceDefinition(final String consumer, final String serviceDefinition) {
		logger.debug("getByConsumerAndServiceDefinition started...");
		assertTrue(!Utilities.isEmpty(consumer), "consumer is empty");
		assertTrue(!Utilities.isEmpty(serviceDefinition), "serviceDefinition is empty");

		try {
			return storeRepo.findAllByConsumerAndServiceDefinitionOrderByPriorityAsc(consumer, serviceDefinition);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<OrchestrationStore> setPriorities(final Map<UUID, Integer> priorityCandidates, final String requester) {
		Assert.isTrue(!Utilities.isEmpty(requester), "requester is empty");
		Assert.notNull(priorityCandidates, "priorityCandidates is null");
		logger.debug("setPriorities started...");


		List<OrchestrationStore> modified = new ArrayList<>();

		try {
			synchronized (LOCK) {
				final List<OrchestrationStore> existingEntries = storeRepo.findAllById(priorityCandidates.keySet());

				// check if every candidate UUID is valid
				verifyCandidatesByUUID(priorityCandidates.keySet(), existingEntries);

				final String consumer = existingEntries.getFirst().getConsumer();
				final String serviceDefinition = existingEntries.getFirst().getServiceDefinition();

				// check if every candidate belongs to the same rule set
				verifyRuleSet(existingEntries, consumer, serviceDefinition);

				final List<OrchestrationStore> ruleSet = storeRepo.findAllByConsumerAndServiceDefinition(consumer, serviceDefinition);

				for (final OrchestrationStore rule : ruleSet) {
					if (priorityCandidates.containsKey(rule.getId())) {
						rule.setPriority(priorityCandidates.get(rule.getId()));
						modified.add(rule);
					}
				}

				checkDuplicates(ruleSet);
				return storeRepo.saveAllAndFlush(modified);
			}
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteBulk(final List<UUID> uuids) {
		Assert.isTrue(!Utilities.isEmpty(uuids), "UUID list is empty");

		try {
			storeRepo.deleteAllById(uuids);
			storeRepo.flush();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void verifyCandidatesByUUID(final Set<UUID> candidateIds, final List<OrchestrationStore> existingEntries) {
		final List<UUID> existingIds = existingEntries.stream().map(OrchestrationStore::getId).toList();

		for (final UUID candidateId : candidateIds) {
			if (!existingIds.contains(candidateId)) {
				throw new InvalidParameterException("Not existing UUID: " + candidateId.toString());
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void verifyRuleSet(final List<OrchestrationStore> existingEntries, final String consumer, final String serviceDefinition) {

		for (final OrchestrationStore entry : existingEntries) {
			if (!entry.getConsumer().equals(consumer) || !entry.getServiceDefinition().equals(serviceDefinition)) {
				throw new InvalidParameterException("Subscription ids should belong to the same rule set (same consumer and service definition)");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Throws exception, if there is an existing entity in the DB with the same unique fields
	private void checkUniqueFields(final List<OrchestrationSimpleStoreRequestDTO> candidates) {

		for (final OrchestrationSimpleStoreRequestDTO candidate : candidates) {

			// check if there is an existing record
			final String serviceDefinition = ServiceInstanceIdUtils.retrieveServiceDefinitionFromInstanceId(candidate.serviceInstanceId());
			final Optional<OrchestrationStore> entity = storeRepo.findByConsumerAndServiceDefinitionAndPriority(
					candidate.consumer(),
					serviceDefinition,
					candidate.priority());
			if (entity.isPresent()) {
				final OrchestrationStore existing = entity.get();
				throw new InvalidParameterException("There is already an existing entity with consumer name: " + existing.getConsumer() + ", service definition: "
						+ serviceDefinition + ", priority: " + existing.getPriority());
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Throws exception, if there are multiple entities with the same unique fields
	private void checkDuplicates(final List<OrchestrationStore> entities) {

		final List<Triple<String, String, Integer>> checked = new ArrayList<>(entities.size());

		for (final OrchestrationStore entity : entities) {
			final Triple<String, String, Integer> toCheck = Triple.of(entity.getConsumer(), entity.getServiceDefinition(), entity.getPriority());
			if (checked.contains(toCheck)) {
				throw new InvalidParameterException("Conflicting rules, the combination of the following fields should be unique: " + entity.getConsumer() + ", service definition: "
						+ entity.getServiceDefinition() + ", priority: " + entity.getPriority());
			} else {
				checked.add(toCheck);
			}
		}
	}

	//=================================================================================================
	// nested classes

	private enum BaseFilter {
		NONE, ID, CONSUMER, DEFINITION, INSTANCE, CREATOR;
	}
}
