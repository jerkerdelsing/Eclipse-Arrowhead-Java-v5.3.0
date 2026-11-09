/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import eu.arrowhead.choreographer.ChoreographerConstants;
import eu.arrowhead.choreographer.api.dto.AbortStepRequestDTO;
import eu.arrowhead.choreographer.api.dto.ExecuteStepRequestDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorServiceInfoRequestDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorServiceInfoResponseDTO;
import eu.arrowhead.choreographer.api.dto.SessionNotificationDTO;
import eu.arrowhead.choreographer.engine.OrchestrationResultBroker;
import eu.arrowhead.choreographer.jpa.entity.Executor;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.NotifyProtocol;

// Outbound calls of the Choreographer: ServiceRegistry lookups, on-behalf orchestration through
// DynamicServiceOrchestration push management, the executor protocol and session notifications.
@Component
public class ChoreographerDriver {

	private static final String FLAG_MATCHMAKING = "MATCHMAKING";
	private static final String FLAG_ALLOW_INTERCLOUD = "ALLOW_INTERCLOUD";
	// a subscription outlives the wait for its push by this margin; DynamicServiceOrchestration then removes it
	private static final long SUBSCRIPTION_MARGIN_SECONDS = 30;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ArrowheadHttpService arrowheadHttpService;

	@Autowired
	private HttpService httpService;

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private OrchestrationResultBroker broker;

	@Value(ChoreographerConstants.$ORCHESTRATION_TIMEOUT_WD)
	private long orchestrationTimeoutSeconds;

	// ---------------------------------------------------------------------------------------- ServiceRegistry

	public Optional<SystemResponseDTO> lookupSystem(final String systemName) {
		final SystemLookupRequestDTO request = new SystemLookupRequestDTO(List.of(systemName), null, null, null, null, null);
		final SystemListResponseDTO response = arrowheadHttpService.consumeService(Constants.SERVICE_DEF_SYSTEM_DISCOVERY, Constants.SERVICE_OP_LOOKUP, SystemListResponseDTO.class, request);
		return response == null || response.entries() == null ? Optional.empty() : response.entries().stream().filter(s -> systemName.equals(s.name())).findFirst();
	}

	// true when ServiceRegistry knows at least one provider matching the requirement's definition, versions and templates
	public boolean isServiceAvailable(final OrchestrationServiceRequirementDTO requirement) {
		final ServiceInstanceLookupRequestDTO request = new ServiceInstanceLookupRequestDTO(null, null, List.of(requirement.serviceDefinition()),
				requirement.versions(), null, null, null, requirement.interfaceTemplateNames(), null, requirement.securityPolicies());
		final ServiceInstanceListResponseDTO response = arrowheadHttpService.consumeService(Constants.SERVICE_DEF_SERVICE_DISCOVERY, Constants.SERVICE_OP_LOOKUP, ServiceInstanceListResponseDTO.class, request);
		return response != null && response.entries() != null && !response.entries().isEmpty();
	}

	// ---------------------------------------------------------------------------------------- orchestration on behalf of an executor

	// DynamicServiceOrchestration keeps one push subscription per (owner, target system, service definition): a new
	// subscription replaces the previous one, so orchestrations with the same key are serialized.
	private final Map<String, ReentrantLock> orchestrationLocks = new ConcurrentHashMap<>();

	// v4.6 "orchestration by proxy": DynamicServiceOrchestration orchestrates for the executor (authorization and tokens are
	// evaluated for the executor) and posts the result to the Choreographer's callback.
	// The subscription is not removed afterwards: DynamicServiceOrchestration authorizes management operations by their
	// exact path, so unsubscribe/{id} is available to sysops only. It expires shortly after the wait instead.
	public OrchestrationResponseDTO orchestrateFor(final String targetSystem, final OrchestrationServiceRequirementDTO requirement, final boolean allowInterCloud) {
		final ReentrantLock lock = orchestrationLocks.computeIfAbsent(targetSystem + "|" + requirement.serviceDefinition(), k -> new ReentrantLock());
		lock.lock();
		final String correlationId = broker.open();
		try {
			final Map<String, Boolean> flags = new HashMap<>();
			flags.put(FLAG_MATCHMAKING, true);
			flags.put(FLAG_ALLOW_INTERCLOUD, allowInterCloud);
			final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO(requirement, flags, null, null);
			final Map<String, String> notifyProperties = Map.of(
					"address", sysInfo.getDomainAddress(),
					"port", String.valueOf(sysInfo.getServerPort()),
					"method", HttpMethod.POST.name(),
					"path", ChoreographerConstants.HTTP_API_ORCHESTRATION_RESULT_PATH + "/" + correlationId);
			final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO(
					sysInfo.isSslEnabled() ? NotifyProtocol.HTTPS.name() : NotifyProtocol.HTTP.name(), notifyProperties);
			// SimpleStoreServiceOrchestration offers the same push management service; executors are orchestrated dynamically
			final OrchestrationSubscriptionListResponseDTO subscribed = arrowheadHttpService.consumeService(
					Constants.SERVICE_DEF_SERVICE_ORCHESTRATION_PUSH_MANAGEMENT,
					Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE,
					Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION,
					OrchestrationSubscriptionListResponseDTO.class,
					new OrchestrationSubscriptionListRequestDTO(List.of(new OrchestrationSubscriptionRequestDTO(targetSystem, orchestrationRequest, notifyInterface,
							orchestrationTimeoutSeconds + SUBSCRIPTION_MARGIN_SECONDS))));
			if (subscribed == null || subscribed.entries() == null || subscribed.entries().isEmpty()) {
				throw new ExternalServerError("Push orchestration subscription was not created");
			}
			final String subscriptionId = subscribed.entries().get(0).id();
			arrowheadHttpService.consumeService(
					Constants.SERVICE_DEF_SERVICE_ORCHESTRATION_PUSH_MANAGEMENT,
					Constants.SERVICE_OP_ORCHESTRATION_TRIGGER,
					Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION,
					OrchestrationPushJobListResponseDTO.class,
					new OrchestrationPushTriggerDTO(null, List.of(subscriptionId)));

			return broker.future(correlationId).get(orchestrationTimeoutSeconds, TimeUnit.SECONDS);
		} catch (final TimeoutException ex) {
			throw new ExternalServerError("No orchestration result for " + targetSystem + " within " + orchestrationTimeoutSeconds + " s");
		} catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ExternalServerError("Interrupted while waiting for an orchestration result");
		} catch (final java.util.concurrent.ExecutionException ex) {
			throw new ExternalServerError("Orchestration failed: " + ex.getCause().getMessage());
		} finally {
			broker.close(correlationId);
			lock.unlock();
		}
	}

	// ---------------------------------------------------------------------------------------- executor protocol

	public ExecutorServiceInfoResponseDTO queryExecutorServiceInfo(final Executor executor, final String serviceDefinition, final int minVersion, final int maxVersion) {
		return send(executor, ChoreographerConstants.EXECUTOR_OP_SERVICE_INFO_PATH, ExecutorServiceInfoResponseDTO.class,
				new ExecutorServiceInfoRequestDTO(serviceDefinition, minVersion, maxVersion));
	}

	public void startExecutor(final Executor executor, final ExecuteStepRequestDTO payload) {
		send(executor, ChoreographerConstants.EXECUTOR_OP_START_PATH, Void.class, payload);
	}

	public void abortExecutor(final Executor executor, final AbortStepRequestDTO payload) {
		send(executor, ChoreographerConstants.EXECUTOR_OP_ABORT_PATH, Void.class, payload);
	}

	public void sendSessionNotification(final String notifyUri, final SessionNotificationDTO payload) {
		final UriComponents uri = UriComponentsBuilder.fromUriString(notifyUri).build();
		httpService.sendRequest(uri, HttpMethod.POST, Void.class, payload, null, authorizationHeaders());
	}

	//-------------------------------------------------------------------------------------------------
	private <T, P> T send(final Executor executor, final String operationPath, final Class<T> responseType, final P payload) {
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final UriComponents uri = HttpUtilities.createURI(scheme, executor.getAddress(), executor.getPort(), executor.getBasePath() + operationPath);
		return httpService.sendRequest(uri, HttpMethod.POST, responseType, payload, null, authorizationHeaders());
	}

	private Map<String, String> authorizationHeaders() {
		final String header = HttpUtilities.calculateAuthorizationHeader(sysInfo);
		return header == null ? Map.of() : Map.of(HttpHeaders.AUTHORIZATION, header);
	}
}
