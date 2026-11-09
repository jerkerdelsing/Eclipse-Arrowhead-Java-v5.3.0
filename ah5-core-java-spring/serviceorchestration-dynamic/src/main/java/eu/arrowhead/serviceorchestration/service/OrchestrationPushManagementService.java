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
package eu.arrowhead.serviceorchestration.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import eu.arrowhead.dto.enums.OrchestrationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationJobFilter;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationFromContextValidation;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationPushManagementValidation;
import jakarta.annotation.Resource;

@Service
public class OrchestrationPushManagementService {

	//=================================================================================================
	// members

	private static final String DELIMITER = "#";

	@Autowired
	private OrchestrationFromContextValidation formContextValidator;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private OrchestrationPushManagementValidation validator;

	@Autowired
	private PageService pageService;

	@Resource(name = DynamicServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	private BlockingQueue<UUID> pushOrchJobQueue;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionListResponseDTO pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
		logger.debug("pushSubscribe started...");

		final List<OrchestrationSubscription> subscriptions = new ArrayList<>();
		if (dto != null && !Utilities.isEmpty(dto.subscriptions())) {
			for (final OrchestrationSubscriptionRequestDTO subsReq : dto.subscriptions()) {
				subscriptions.add(new OrchestrationSubscription(requesterSystem, subsReq));
			}
		}

		validator.validateAndNormalizePushSubscribeService(subscriptions, origin);
		checkForDuplicateSubscriptionRequest(subscriptions, origin);
		subscriptions.forEach(s -> formContextValidator.validate(s.getOrchestrationForm(), origin));

		try {
			final List<Subscription> result;
			synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
				result = subscriptionDbService.create(subscriptions);
			}

			return dtoConverter.convertSubscriptionListToDTO(result, result.size());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationPushJobListResponseDTO pushTrigger(final String requesterSystem, final OrchestrationPushTriggerDTO dto, final String origin) {
		logger.debug("pushTrigger started...");

		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger(requesterSystem, dto);
		validator.validateAndNormalizePushTriggerService(trigger, origin);

		try {
			List<Subscription> subscriptions;
			if (Utilities.isEmpty(trigger.getSubscriptionIds()) && Utilities.isEmpty(trigger.getTargetSystems())) {
				subscriptions = subscriptionDbService.query(
						List.of(trigger.getRequesterSystem()),
						List.of(),
						List.of(),
						PageRequest.of(0, Integer.MAX_VALUE))
						.getContent();
			} else if (!Utilities.isEmpty(trigger.getSubscriptionIds())) {
				subscriptions = subscriptionDbService.get(trigger.getSubscriptionIds().stream().map(id -> UUID.fromString(id)).toList());
			} else {
				subscriptions = subscriptionDbService.query(
						List.of(),
						trigger.getTargetSystems(),
						List.of(),
						PageRequest.of(0, Integer.MAX_VALUE))
						.getContent();
			}

			final List<OrchestrationJob> existingJobs = new ArrayList<>();
			final List<OrchestrationJob> newJobs = new ArrayList<>();
			for (final Subscription subscription : subscriptions) {
				final List<OrchestrationJob> possiblySameJob = orchJobDbService.query(new OrchestrationJobFilter(
						List.of(),
						List.of(OrchestrationJobStatus.PENDING, OrchestrationJobStatus.IN_PROGRESS),
						OrchestrationType.PUSH,
						List.of(),
						List.of(),
						null,
						List.of(subscription.getId().toString())),
						PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, OrchestrationJob.DEFAULT_SORT_FIELD))
						.toList();

				if (!Utilities.isEmpty(possiblySameJob)) {
					existingJobs.addAll(possiblySameJob);
				} else {
					newJobs.add(new OrchestrationJob(
							OrchestrationType.PUSH,
							trigger.getRequesterSystem(),
							subscription.getTargetSystem(),
							subscription.getServiceDefinition(),
							subscription.getId().toString()));
				}
			}

			final List<OrchestrationJob> saved = orchJobDbService.create(newJobs);
			pushOrchJobQueue.addAll(saved.stream().map(job -> job.getId()).toList());
			existingJobs.addAll(saved);

			return dtoConverter.convertOrchestrationJobListToDTO(existingJobs);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void pushUnsubscribe(final String requesterSystem, final List<String> ids, final String origin) {
		logger.debug("pushUnsubscribe started...");

		final String normalizedRequesterSystem = validator.validateAndNormalizeRequesterSystem(requesterSystem, origin);
		final List<String> normalized = validator.validateAndNormalizePublishUnsubscribeService(ids, origin);
		final List<UUID> subscriptionIds = normalized.stream().map(id -> UUID.fromString(id)).toList();

		try {
			synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
				final List<Subscription> subscriptions = subscriptionDbService.get(subscriptionIds);
				for (final Subscription subscription : subscriptions) {
					if (!subscription.getOwnerSystem().equals(normalizedRequesterSystem)) {
						throw new ForbiddenException(subscription.getId().toString() + " is not owned by the requester", origin);
					}
				}

				subscriptionDbService.deleteInBatch(subscriptionIds);
			}
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionListResponseDTO queryPushSubscriptions(final OrchestrationSubscriptionQueryRequestDTO dto, final String origin) {
		logger.debug("queryPushSubscriptions started...");

		final OrchestrationSubscriptionQueryRequestDTO normalized = validator.validateAndNormalizeQueryPushSubscriptionsService(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, Subscription.SORTABLE_FIELDS_BY, Subscription.DEFAULT_SORT_FIELD, origin);

		try {
			final Page<Subscription> results = subscriptionDbService.query(normalized.ownerSystems(), normalized.targetSystems(), normalized.serviceDefinitions(), pageRequest);

			return dtoConverter.convertSubscriptionListToDTO(results.getContent(), results.getTotalElements());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void checkForDuplicateSubscriptionRequest(final List<OrchestrationSubscription> subscriptions, final String origin) {
		logger.debug("checkForDuplicateSubscriptionRequest started...");

		final Set<String> keys = new HashSet<>(subscriptions.size());

		for (final OrchestrationSubscription subsReq : subscriptions) {
			final String key = subsReq.getOrchestrationForm().getRequesterSystemName() + DELIMITER
					+ subsReq.getOrchestrationForm().getTargetSystemName() + DELIMITER
					+ subsReq.getOrchestrationForm().getServiceDefinition();
			if (keys.contains(key)) {
				throw new InvalidParameterException("Duplicate subscription request for: " + key, origin);
			}
			keys.add(key);
		}
	}
}