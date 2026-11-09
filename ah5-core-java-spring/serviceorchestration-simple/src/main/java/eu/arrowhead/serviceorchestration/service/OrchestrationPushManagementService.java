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

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationJobQueryRequest;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationPushManagementServiceValidation;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

@Service
public class OrchestrationPushManagementService {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationPushManagementServiceValidation validator;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private PageService pageService;

	@Autowired
	private DTOConverter dtoConverter;

	@Resource(name = SimpleStoreServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	private BlockingQueue<UUID> pushOrchJobQueue;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionListResponseDTO pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
		logger.debug("pushSubscribe started...");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requesterSystem, origin);
		final List<SimpleOrchestrationSubscriptionRequest> normalized = validator.validateAndNormalizePushSubscribeBulk(dto, origin);

		try {
			final List<Subscription> result;
			synchronized (SimpleStoreServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
				result = subscriptionDbService.create(normalized, normalizedRequester);
			}

			return dtoConverter.convertSubscriptionListToDTO(result, result.size());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationPushJobListResponseDTO pushTrigger(final String requesterSystem, final OrchestrationPushTriggerDTO dto, final String origin) {
		logger.debug("pushTrigger started...");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requesterSystem, origin);
		final NormalizedOrchestrationPushTrigger normalizedTrigger = validator.validateAndNormalizePushTrigger(dto, origin);

		try {
			List<Subscription> subscriptions;
			if (Utilities.isEmpty(normalizedTrigger.getSubscriptionIds()) && Utilities.isEmpty(normalizedTrigger.getTargetSystems())) {
				subscriptions = subscriptionDbService.query(
								List.of(normalizedRequester),
								List.of(),
								List.of(),
								PageRequest.of(0, Integer.MAX_VALUE))
						.getContent();
			} else if (!Utilities.isEmpty(normalizedTrigger.getSubscriptionIds())) {
				subscriptions = subscriptionDbService.get(normalizedTrigger.getSubscriptionIds());
			} else {
				subscriptions = subscriptionDbService.query(
								List.of(),
								normalizedTrigger.getTargetSystems(),
								List.of(),
								PageRequest.of(0, Integer.MAX_VALUE))
						.getContent();
			}

			final List<OrchestrationJob> existingJobs = new ArrayList<>();
			final List<OrchestrationJob> newJobs = new ArrayList<>();
			for (final Subscription subscription : subscriptions) {
				final List<OrchestrationJob> possiblySameJob = orchJobDbService.query(
						new NormalizedOrchestrationJobQueryRequest(
								PageRequest.of(0, Integer.MAX_VALUE, Sort.Direction.DESC, OrchestrationJob.DEFAULT_SORT_FIELD),
								List.of(),
								List.of(OrchestrationJobStatus.PENDING, OrchestrationJobStatus.IN_PROGRESS),
								OrchestrationType.PUSH,
								List.of(),
								List.of(),
								null,
								List.of(subscription.getId()))).toList();

				if (!Utilities.isEmpty(possiblySameJob)) {
					existingJobs.addAll(possiblySameJob);
				} else {
					newJobs.add(new OrchestrationJob(
							OrchestrationType.PUSH,
							normalizedRequester,
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

		final String normalizedRequesterSystem = validator.validateAndNormalizeRequester(requesterSystem, origin);
		final List<UUID> normalizedIds = validator.validateAndNormalizePushUnsubscribe(ids, origin);

		try {
			synchronized (SimpleStoreServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
				final List<Subscription> subscriptions = subscriptionDbService.get(normalizedIds);
				for (final Subscription subscription : subscriptions) {
					if (!subscription.getOwnerSystem().equals(normalizedRequesterSystem)) {
						throw new ForbiddenException(subscription.getId().toString() + " is not owned by the requester", origin);
					}
				}

				subscriptionDbService.deleteInBatch(normalizedIds);
			}
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionListResponseDTO queryPushSubscriptions(final OrchestrationSubscriptionQueryRequestDTO dto, final String origin) {
		logger.debug("queryPushSubscriptions started...");

		final OrchestrationSubscriptionQueryRequestDTO normalized = validator.validateAndNormalizeQueryPushSubscriptionsService(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Sort.Direction.DESC, Subscription.SORTABLE_FIELDS_BY, Subscription.DEFAULT_SORT_FIELD, origin);

		try {
			final Page<Subscription> results = subscriptionDbService.query(normalized.ownerSystems(), normalized.targetSystems(), normalized.serviceDefinitions(), pageRequest);

			return dtoConverter.convertSubscriptionListToDTO(results.getContent(), results.getTotalElements());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}
