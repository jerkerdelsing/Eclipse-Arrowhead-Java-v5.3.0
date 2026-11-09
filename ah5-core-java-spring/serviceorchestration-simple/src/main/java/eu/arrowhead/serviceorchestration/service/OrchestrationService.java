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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.utils.ServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationServiceValidation;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;

@Service
public class OrchestrationService {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationServiceValidation validator;

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private ServiceOrchestration serviceOrchestration;

	@Resource(name = SimpleStoreServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	private BlockingQueue<UUID> pushOrchJobQueue;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO pull(final String requesterSystem, final OrchestrationRequestDTO dto, final String origin) {
		logger.debug("pull started...");

		// validate and normalize
		final String normalizedRequester = validator.validateAndNormalizeRequester(requesterSystem, origin);
		final SimpleOrchestrationRequest normalized = validator.validateAndNormalizePull(dto, origin);

		try {

			// create job
			final OrchestrationJob job = orchJobDbService.create(List.of(
					new OrchestrationJob(
							OrchestrationType.PULL,
							normalizedRequester,
							normalizedRequester, // target consumer system is the requester
							normalized.getServiceDefinition(),
							null)))
					.getFirst();

			// orchestrate
			final List<OrchestrationStore> orchResult = serviceOrchestration.orchestrate(job.getId(), requesterSystem, normalized);
			return dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(orchResult, normalized.getWarnings());

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<Boolean, String> pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionRequestDTO dto, final Boolean trigger, final String origin) {
		logger.debug("pushSubscribe started...");

		// validate and normalize
		final SimpleOrchestrationSubscriptionRequest normalized = validator.validateAndNormalizePushSubscribe(dto, requesterSystem, origin);

		// save subscription
		Pair<Boolean, String> response = null;
		synchronized (SimpleStoreServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
			final Optional<Subscription> recordOpt = subscriptionDbService.get(
					requesterSystem,
					normalized.getTargetSystemName(),
					normalized.getOrchestrationRequest().getServiceDefinition());

			final boolean isOverride = recordOpt.isPresent();

			final List<Subscription> result = subscriptionDbService.create(List.of(normalized), requesterSystem);
			response = Pair.of(isOverride, result.getFirst().getId().toString());
		}

		// orchestrate if needed
		if (trigger) {
			final OrchestrationJob orchestrationJob = new OrchestrationJob(
					OrchestrationType.PUSH,
					requesterSystem,
					requesterSystem,
					dto.orchestrationRequest() == null || dto.orchestrationRequest().serviceRequirement() == null ? null : dto.orchestrationRequest().serviceRequirement().serviceDefinition(),
					response.getValue());
			orchJobDbService.create(List.of(orchestrationJob));
			pushOrchJobQueue.add(orchestrationJob.getId());
		}

		return response;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean pushUnsubscribe(final String requesterSystem, final String subscriptionId, final String origin) {
		logger.debug("pushUnsubscribe started...");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requesterSystem, origin);
		final UUID normalizedSubscriptionId = validator.validateAndNormalizePushUnsubscribe(subscriptionId, origin);

		synchronized (SimpleStoreServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
			final Optional<Subscription> recordOpt = subscriptionDbService.get(normalizedSubscriptionId);
			if (recordOpt.isPresent()) {
				if (!recordOpt.get().getOwnerSystem().equals(normalizedRequester)) {
					throw new ForbiddenException(normalizedRequester + " is not the subscription owner", origin);
				}
				subscriptionDbService.deleteById(normalizedSubscriptionId);

				return true;
			}

			return false;
		}
	}
}
