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

import eu.arrowhead.dto.enums.OrchestrationType;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.utils.InterCloudServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.utils.LocalServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationFromContextValidation;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationValidation;
import jakarta.annotation.Resource;

@Service
public class OrchestrationService {

	//=================================================================================================
	// members

	@Autowired
	private LocalServiceOrchestration localOrch;

	@Autowired
	private InterCloudServiceOrchestration interCloudOrch;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Resource(name = DynamicServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	private BlockingQueue<UUID> pushOrchJobQueue;

	@Autowired
	private OrchestrationValidation validator;

	@Autowired
	private OrchestrationFromContextValidation formContextValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO pull(final String requesterSystem, final OrchestrationRequestDTO dto, final String origin) {
		logger.debug("pull started...");

		final OrchestrationForm form = new OrchestrationForm(requesterSystem, dto);
		validator.validateAndNormalizePullService(form, origin);
		formContextValidator.validate(form, origin);

		final OrchestrationJob job = orchJobDbService.create(List.of(
				new OrchestrationJob(
						OrchestrationType.PULL,
						form.getRequesterSystemName(),
						form.getTargetSystemName(),
						form.getServiceDefinition(),
						null)))
				.getFirst();

		return orchestrate(job.getId(), form);
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<Boolean, String> pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionRequestDTO dto, final boolean trigger, final String origin) {
		logger.debug("pushSubscribe started...");

		final OrchestrationSubscription subscription = new OrchestrationSubscription(requesterSystem, dto);

		if (Utilities.isEmpty(subscription.getOrchestrationForm().getTargetSystemName())) {
			subscription.getOrchestrationForm().setTargetSystemName(requesterSystem);
		}

		validator.validateAndNormalizePushSubscribeService(subscription, origin);
		formContextValidator.validate(subscription.getOrchestrationForm(), origin);

		Pair<Boolean, String> response = null;
		synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
			final Optional<Subscription> recordOpt = subscriptionDbService.get(
					subscription.getOrchestrationForm().getRequesterSystemName(),
					subscription.getOrchestrationForm().getTargetSystemName(),
					subscription.getOrchestrationForm().getServiceDefinition());

			final boolean isOverride = recordOpt.isPresent();

			final List<Subscription> result = subscriptionDbService.create(List.of(subscription));
			response = Pair.of(isOverride, result.getFirst().getId().toString());
		}

		if (trigger) {
			final OrchestrationJob orchestrationJob = new OrchestrationJob(
					OrchestrationType.PUSH,
					requesterSystem,
					requesterSystem,
					dto.orchestrationRequest().serviceRequirement().serviceDefinition(),
					response.getValue());
			orchJobDbService.create(List.of(orchestrationJob));
			pushOrchJobQueue.add(orchestrationJob.getId());
		}

		return response;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean pushUnsubscribe(final String requesterSystem, final String subscriptionId, final String origin) {
		logger.debug("pushUnsubscribe started...");

		final Pair<String, UUID> normalized = validator.validateAndNormalizePushUnsubscribeService(requesterSystem, subscriptionId, origin);

		synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
			final Optional<Subscription> recordOpt = subscriptionDbService.get(normalized.getRight());
			if (recordOpt.isPresent()) {
				if (!recordOpt.get().getOwnerSystem().equals(normalized.getLeft())) {
					throw new ForbiddenException(normalized.getLeft() + " is not the subscription owner", origin);
				}
				subscriptionDbService.deleteById(normalized.getRight());

				return true;
			}

			return false;
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationResponseDTO orchestrate(final UUID jobId, final OrchestrationForm form) {
		logger.debug("orchestrate started...");

		try {
			if (form.getFlag(OrchestrationFlag.ONLY_INTERCLOUD)) {
				return interCloudOrch.doInterCloudServiceOrchestration(jobId, form);
			}

			final OrchestrationResponseDTO local = localOrch.doLocalServiceOrchestration(jobId, form);
			// ALLOW_INTERCLOUD: fall back to the neighbour clouds when no local provider is found
			if (form.getFlag(OrchestrationFlag.ALLOW_INTERCLOUD) && Utilities.isEmpty(local.results())) {
				return interCloudOrch.doInterCloudServiceOrchestration(jobId, form);
			}

			return local;

		} catch (final ArrowheadException ex) {
			throw ex;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError(ex.getMessage());
		}
	}
}