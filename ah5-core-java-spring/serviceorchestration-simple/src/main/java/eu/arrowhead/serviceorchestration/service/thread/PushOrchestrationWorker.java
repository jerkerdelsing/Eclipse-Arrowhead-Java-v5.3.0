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

package eu.arrowhead.serviceorchestration.service.thread;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.mqtt.MqttQoS;
import eu.arrowhead.common.mqtt.MqttService;
import eu.arrowhead.dto.MqttNotifyTemplate;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.NotifyProtocol;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.utils.ServiceOrchestration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PushOrchestrationWorker implements Runnable {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServiceOrchestration serviceOrchestration;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private HttpService httpService;

	@Autowired(required = false)
	private MqttService mqttService;

	@Autowired
	private SimpleStoreServiceOrchestrationSystemInfo sysInfo;

	private UUID jobId;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public PushOrchestrationWorker(final UUID jobId) {
		this.jobId = jobId;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run() {
		doPushOrchestration();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void doPushOrchestration() {
		logger.debug("doPushOrchestration started...");

		try {
			final Optional<OrchestrationJob> jobOpt = orchJobDbService.getById(jobId);
			if (jobOpt.isEmpty()) {
				logger.error("Orchestration push job doesn't exist: " + jobId);
				return;
			}

			final OrchestrationJob job = orchJobDbService.setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, null);

			if (Utilities.isEmpty(job.getSubscriptionId())) {
				final String errorMsg = "Orchestration job " + jobId + " has no subscription id";
				logger.error(errorMsg);
				orchJobDbService.setStatus(jobId, OrchestrationJobStatus.ERROR, errorMsg);
				return;
			}

			final Optional<Subscription> subscriptionOpt = subscriptionDbService.get(UUID.fromString(job.getSubscriptionId()));
			if (subscriptionOpt.isEmpty()) {
				final String errorMsg = "Orchestration job " + jobId + " has no subscription with " + job.getSubscriptionId() + " subscription id";
				logger.error(errorMsg);
				orchJobDbService.setStatus(jobId, OrchestrationJobStatus.ERROR, errorMsg);
				return;
			}

			final Subscription subscription = subscriptionOpt.get();

			final SimpleOrchestrationRequest orchestrationRequest = mapper.readValue(subscription.getOrchestrationRequest(), SimpleOrchestrationRequest.class);

			final List<OrchestrationStore> orchResult = serviceOrchestration.orchestrate(jobId, job.getTargetSystem(), orchestrationRequest);
			final OrchestrationResponseDTO result = dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(orchResult, orchestrationRequest.getWarnings());

			if (subscription.getNotifyProtocol().equals(NotifyProtocol.HTTP.name())
					|| subscription.getNotifyProtocol().equals(NotifyProtocol.HTTPS.name())) {
				notifyViaHttp(subscription.getId(), subscription.getNotifyProtocol(), subscription.getNotifyProperties(), result);
				return;
			}

			if (subscription.getNotifyProtocol().equals(NotifyProtocol.MQTT.name())
					|| subscription.getNotifyProtocol().equals(NotifyProtocol.MQTTS.name())) {
				notifyViaMqtt(subscription.getId(), subscription.getTargetSystem(), subscription.getNotifyProperties(), result);
				return;
			}

			throw new ArrowheadException("Unsupported protocol: " + subscription.getNotifyProtocol());
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			orchJobDbService.setStatus(jobId, OrchestrationJobStatus.ERROR, ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void notifyViaHttp(final UUID subscriptionId, final String protocol, final String properties, final OrchestrationResponseDTO result) {
		logger.debug("notifyViaHttp started...");

		final Map<String, String> propsMap = readNotifyProperties(properties);
		final String address = propsMap.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS);
		final int port = Integer.parseInt(propsMap.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PORT));
		final String method = propsMap.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD);
		final String path = propsMap.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PATH);

		try {
			httpService.sendRequest(HttpUtilities.createURI(protocol, address, port, path), HttpMethod.valueOf(method.toUpperCase()), Void.class, result);
		} catch (final Exception ex) {
			logger.debug(ex);
			throw new ArrowheadException("Error occurred while sending push orchestration via HTTP to subscription: " + subscriptionId.toString() + ". Reason: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, String> readNotifyProperties(final String properties) {
		logger.debug("readNotifyProperties started...");

		final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {
		};

		try {
			return mapper.readValue(properties, typeReference);
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new IllegalArgumentException("Unreadable notify properties: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void notifyViaMqtt(final UUID subscriptionId, final String targetSystem, final String properties, final OrchestrationResponseDTO result) {
		logger.debug("notifyViaMqtt started...");

		if (!sysInfo.isMqttApiEnabled()) {
			throw new ArrowheadException("Orchestration push notification via MQTT is required for subscripiton: " + subscriptionId.toString() + ", but MQTT is not enabled");
		}

		// Sending MQTT notification is supported only via the main broker. Orchestrator does not connect to unknown brokers to send the orchestration results.
		final MqttClient mqttClient = mqttService.client(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID);
		final Map<String, String> propsMap = readNotifyProperties(properties);

		try {
			final MqttNotifyTemplate template = new MqttNotifyTemplate(targetSystem, sysInfo.getSystemName(), result);
			final MqttMessage msg = new MqttMessage(mapper.writeValueAsBytes(template));
			msg.setQos(MqttQoS.EXACTLY_ONCE.value());
			mqttClient.publish(propsMap.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_TOPIC), msg);
		} catch (final Exception ex) {
			logger.debug(ex);
			throw new ArrowheadException("Error occurred while sending push orchestration via MQTT to subscription: " + subscriptionId.toString() + ". Reason: " + ex.getMessage());
		}
	}
}
