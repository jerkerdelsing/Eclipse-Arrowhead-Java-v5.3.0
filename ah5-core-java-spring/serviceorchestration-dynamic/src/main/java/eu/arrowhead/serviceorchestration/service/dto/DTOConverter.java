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

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationHistoryResponseDTO;
import eu.arrowhead.dto.OrchestrationJobDTO;
import eu.arrowhead.dto.OrchestrationLockListResponseDTO;
import eu.arrowhead.dto.OrchestrationLockResponseDTO;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;

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
	public OrchestrationSubscriptionListResponseDTO convertSubscriptionListToDTO(final List<Subscription> subscriptions, final long count) {
		logger.debug("convertSubscriptionListToDTO started...");
		Assert.notNull(subscriptions, "subscriptions list is null");

		final List<OrchestrationSubscriptionResponseDTO> entries = subscriptions
				.stream()
				.map(subscription -> convertSubscriptionToDTO(subscription))
				.toList();

		return new OrchestrationSubscriptionListResponseDTO(entries, count);
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionResponseDTO convertSubscriptionToDTO(final Subscription subscription) {
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
	public OrchestrationPushJobListResponseDTO convertOrchestrationJobListToDTO(final List<OrchestrationJob> jobs) {
		logger.debug("convertOrchestrationJobListToDTO started...");
		Assert.notNull(jobs, "job list is null");

		return new OrchestrationPushJobListResponseDTO(jobs
				.stream()
				.map(j -> convertOrchestrationJobToDTO(j))
				.toList());
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

	//-------------------------------------------------------------------------------------------------
	public OrchestrationJobDTO convertOrchestrationJobToDTO(final OrchestrationJob job) {
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
	public OrchestrationLockListResponseDTO convertOrchestrationLockListToDTO(final List<OrchestrationLock> locks, final long count) {
		logger.debug("convertOrchestrationLockListToDTO started...");
		Assert.notNull(locks, "lock list is null");

		return new OrchestrationLockListResponseDTO(
				locks.stream().map(l -> convertOrchestrationLockToDTO(l)).toList(),
				count);

	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockResponseDTO convertOrchestrationLockToDTO(final OrchestrationLock lock) {
		logger.debug("convertOrchestrationLockToDTO started...");
		Assert.notNull(lock, "lock is null");

		return new OrchestrationLockResponseDTO(
				lock.getId(),
				lock.getOrchestrationJobId(),
				lock.getServiceInstanceId(),
				lock.getOwner(),
				Utilities.convertZonedDateTimeToUTCString(lock.getExpiresAt()),
				lock.isTemporary());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationRequestDTO createOrchestrationRequestDTO(final String orchestrationRequestStr) {
		logger.debug("createOrchestrationRequestDTO started...");

		try {
			return mapper.readValue(orchestrationRequestStr, OrchestrationRequestDTO.class);
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new IllegalArgumentException("DTOconverter.createOrchestrationRequestDTO failed. Error: " + ex.getMessage());
		}
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
}