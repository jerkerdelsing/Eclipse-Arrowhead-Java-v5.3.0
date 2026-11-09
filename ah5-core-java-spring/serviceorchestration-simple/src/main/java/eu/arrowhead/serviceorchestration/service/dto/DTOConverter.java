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
package eu.arrowhead.serviceorchestration.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.dto.OrchestrationHistoryResponseDTO;
import eu.arrowhead.dto.OrchestrationJobDTO;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	@Autowired
	private ObjectMapper mapper;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO convertStoreEntityListToResponseListDTO(final List<OrchestrationStore> entities) {
		logger.debug("convertOrchestrationStoreListToResponseListDTO started...");
		Assert.notNull(entities, "entities is null");

		return new OrchestrationSimpleStoreListResponseDTO(
				entities.stream().map(this::convertOrchestrationStoreEntityToResponseDTO).collect(Collectors.toList()),
				entities.size());
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO convertStoreEntityPageToResponseListTO(final Page<OrchestrationStore> entities) {
		logger.debug("convertStoreEntityPageToResponseListTO started...");
		Assert.notNull(entities, "entities is null");

		return new OrchestrationSimpleStoreListResponseDTO(
				entities.stream().map(this::convertOrchestrationStoreEntityToResponseDTO).collect(Collectors.toList()),
				entities.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	public OrchestrationResponseDTO convertStoreEntitiesToOrchestrationResponseDTO(final List<OrchestrationStore> entities, final Set<String> warnings) {
		logger.debug("convertStoreEntitiesToOrchestrationResponseDTO started...");
		Assert.notNull(entities, "entities is null");

		final List<OrchestrationResultDTO> responseDTOS = new ArrayList<>(entities.size());
		entities.forEach(entity -> {

			responseDTOS.add(new OrchestrationResultDTO(
					entity.getServiceInstanceId(),
					Defaults.DEFAULT_CLOUD,
					ServiceInstanceIdUtils.retrieveSystemNameFromInstanceId(entity.getServiceInstanceId()),
					entity.getServiceDefinition(),
					ServiceInstanceIdUtils.retrieveVersionFromInstanceId(entity.getServiceInstanceId()),
					null,
					null,
					null,
					null,
					null
			));
		});

		return new OrchestrationResponseDTO(responseDTOS, Utilities.isEmpty(warnings) ? null : warnings.stream().toList());
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionListResponseDTO convertSubscriptionListToDTO(final List<Subscription> subscriptions, final long count) {
		logger.debug("convertSubscriptionListToDTO started...");
		Assert.notNull(subscriptions, "subscriptions list is null");

		final List<OrchestrationSubscriptionResponseDTO> entries = subscriptions
				.stream()
				.map(this::convertSubscriptionToDTO)
				.toList();

		return new OrchestrationSubscriptionListResponseDTO(entries, count);
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationPushJobListResponseDTO convertOrchestrationJobListToDTO(final List<OrchestrationJob> jobs) {
		logger.debug("convertOrchestrationJobListToDTO started...");
		Assert.notNull(jobs, "job list is null");

		return new OrchestrationPushJobListResponseDTO(jobs
				.stream()
				.map(this::convertOrchestrationJobToDTO)
				.toList());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSimpleStoreResponseDTO convertOrchestrationStoreEntityToResponseDTO(final OrchestrationStore entity) {
		logger.debug("convertOrchestrationStoreEntityToResponseDTO started...");
		Assert.notNull(entity, "entity is null");

		return new OrchestrationSimpleStoreResponseDTO(
				entity.getId().toString(),
				entity.getConsumer(),
				entity.getServiceDefinition(),
				entity.getServiceInstanceId(),
				entity.getPriority(),
				entity.getCreatedBy(),
				entity.getUpdatedBy(),
				Utilities.convertZonedDateTimeToUTCString(entity.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(entity.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSubscriptionResponseDTO convertSubscriptionToDTO(final Subscription subscription) {
		logger.debug("convertSubscriptionToDTO started...");
		Assert.notNull(subscription, "subscription is null");

		return new OrchestrationSubscriptionResponseDTO(
				subscription.getId().toString(),
				subscription.getOwnerSystem(),
				subscription.getTargetSystem(),
				createOrchestrationRequestDTO(subscription.getOrchestrationRequest()),
				createOrchestrationNotifyInterfaceDTO(subscription.getNotifyProtocol(), subscription.getNotifyProperties()),
				Utilities.convertZonedDateTimeToUTCString(subscription.getExpiresAt()),
				Utilities.convertZonedDateTimeToUTCString(subscription.getCreatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationRequestDTO createOrchestrationRequestDTO(final String orchestrationRequestStr) {
		logger.debug("createOrchestrationRequestDTO started...");

		SimpleOrchestrationRequest simpleOrchestrationRequest = null;
		try {
			simpleOrchestrationRequest = mapper.readValue(orchestrationRequestStr, SimpleOrchestrationRequest.class);
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new IllegalArgumentException("DTOconverter.createOrchestrationRequestDTO failed. Error: " + ex.getMessage());
		}
		OrchestrationServiceRequirementDTO serviceRequirementDTO = null;
		if (simpleOrchestrationRequest.getServiceDefinition() != null || simpleOrchestrationRequest.getPreferredProviders() != null) {
			serviceRequirementDTO = new OrchestrationServiceRequirementDTO(
					simpleOrchestrationRequest.getServiceDefinition(), null, null, null, null, null, null, null, null, simpleOrchestrationRequest.getPreferredProviders()
			);
		}
		return new OrchestrationRequestDTO(
				serviceRequirementDTO,
				simpleOrchestrationRequest.getOrchestrationFlags(),
				null,
				null
		);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationNotifyInterfaceDTO createOrchestrationNotifyInterfaceDTO(final String protocol, final String propertiesStr) {
		logger.debug("createOrchestrationNotifyInterfaceDTO started...");

		final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {
		};

		try {
			return new OrchestrationNotifyInterfaceDTO(protocol, mapper.readValue(propertiesStr, typeReference));
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new IllegalArgumentException("DTOconverter.createOrchestrationNotifyInterfaceDTO failed. Error: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationJobDTO convertOrchestrationJobToDTO(final OrchestrationJob job) {
		logger.debug("convertOrchestrationJobToDTO started...");
		Assert.notNull(job, "job is null");

		return new OrchestrationJobDTO(
				job.getId().toString(),
				job.getStatus().name(),
				job.getType().name(),
				job.getRequesterSystem(),
				job.getTargetSystem(),
				job.getServiceDefinition(),
				job.getSubscriptionId(),
				job.getMessage(),
				Utilities.convertZonedDateTimeToUTCString(job.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(job.getStartedAt()),
				Utilities.convertZonedDateTimeToUTCString(job.getFinishedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationHistoryResponseDTO convertOrchestrationJobPageToHistoryDTO(final Page<OrchestrationJob> page) {
		logger.debug("convertOrchestrationJobPageToHistoryDTO started...");
		Assert.notNull(page, "page is null");

		final List<OrchestrationJobDTO> entries = page
				.stream()
				.map(job -> convertOrchestrationJobToDTO(job))
				.toList();

		return new OrchestrationHistoryResponseDTO(entries, page.getTotalElements());
	}

}
