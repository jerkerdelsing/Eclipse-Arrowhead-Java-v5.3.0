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
package eu.arrowhead.common.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.MqttPublishTemplate;
import eu.arrowhead.dto.MqttResponseTemplate;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class ArrowheadMqttService {

	//=================================================================================================
	// members

	@Autowired
	private MqttService mqttService;

	@Autowired
	private ObjectMapper mapper;

	private Map<String, MqttSubscriptionHandler> subscriptionMap = new ConcurrentHashMap<>();

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	/**
	 * Subscribe for consuming a push service
	 */
	public LinkedBlockingQueue<MqttMessage> subscribe(final String address, final int port, final boolean isSSl, final String topic) {
		logger.debug("subscribe started");
		Assert.isTrue(!Utilities.isEmpty(address), "address is empty");
		Assert.isTrue(!Utilities.isEmpty(topic), "topic is empty");

		final String connectionId = calculateConnectionId(address, port, isSSl);

		try {
			// Find or create subscription handler
			if (!subscriptionMap.containsKey(connectionId)) {
				MqttClient client = mqttService.client(connectionId);
				if (client == null) {
					mqttService.connect(connectionId, address, port, isSSl);
					client = mqttService.client(connectionId);
				}
				subscriptionMap.put(connectionId, new MqttSubscriptionHandler(connectionId, client));
			}

			final MqttSubscriptionHandler subscriptionHandler = subscriptionMap.get(connectionId);

			// Subscribe
			return subscriptionHandler.addSubscription(topic);
		} catch (final MqttException ex) {
			logger.debug(ex);
			throw new ExternalServerError("MQTT subscribe failed: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	/**
	 * Unsubscribe from consuming a push service
	 */
	public void unsubscribe(final String address, final int port, final boolean isSSl, final String topic) {
		logger.debug("unsubscribe started");
		Assert.isTrue(!Utilities.isEmpty(address), "address is empty");
		Assert.isTrue(!Utilities.isEmpty(topic), "topic is empty");

		final String connectionId = calculateConnectionId(address, port, isSSl);

		try {
			if (subscriptionMap.containsKey(connectionId)) {
				final MqttSubscriptionHandler subscriptionHandler = subscriptionMap.get(connectionId);
				subscriptionHandler.removeSubscription(topic);
				if (Utilities.isEmpty(subscriptionHandler.getSubscribedTopics())) {
					mqttService.client(connectionId).disconnect();
					subscriptionMap.remove(connectionId);
				}
			}
		} catch (final MqttException ex) {
			logger.debug(ex);
			throw new ExternalServerError("MQTT unsubscribe failed: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	/**
	 * Publish a non-response service message
	 */
	public void publish(final String baseTopic, final String operation, final String sender, final MqttQoS qos, final Object payload) {
		logger.debug("publish started");
		Assert.isTrue(!Utilities.isEmpty(baseTopic), "baseTopic is empty");
		Assert.isTrue(!Utilities.isEmpty(operation), "operation is empty");

		final MqttClient client = mqttService.client(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID);
		Assert.notNull(client, "Main broker is not initialized");

		try {
			final MqttPublishTemplate template = new MqttPublishTemplate(sender, payload);
			final MqttMessage msg = new MqttMessage(mapper.writeValueAsBytes(template));
			msg.setQos(qos == null ? Constants.MQTT_DEFAULT_QOS : qos.value());
			client.publish(baseTopic + operation, msg);
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new InternalServerError("MQTT service publish message creation failed: " + ex.getMessage());
		} catch (final MqttException ex) {
			logger.debug(ex);
			throw new ExternalServerError("MQTT service publish failed: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	/**
	 * Publish a response for a request-response service when it is provided via
	 * MQTT
	 */
	public void response(
			final String receiver,
			final String topic,
			final String traceId,
			final MqttQoS qos,
			final MqttStatus status,
			final Object payload) {
		logger.debug("response started");
		Assert.isTrue(!Utilities.isEmpty(topic), "topic is empty");

		final MqttClient client = mqttService.client(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID);
		Assert.notNull(client, "Main broker is not initialized");

		try {
			final MqttResponseTemplate template = new MqttResponseTemplate(status.value(), traceId, receiver, payload == null ? "" : payload);
			final MqttMessage msg = new MqttMessage(mapper.writeValueAsBytes(template));
			msg.setQos(qos == null ? Constants.MQTT_DEFAULT_QOS : qos.value());
			client.publish(topic, msg);
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new InternalServerError("MQTT service response message creation failed: " + ex.getMessage());
		} catch (final MqttException ex) {
			logger.debug(ex);
			throw new ExternalServerError("MQTT service response failed: " + ex.getMessage());
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private String calculateConnectionId(final String address, final int port, final boolean isSSl) {
		logger.debug("calculateConnectionId started...");

		return new String(Base64.getEncoder().encode((address + port + String.valueOf(isSSl)).getBytes()), StandardCharsets.UTF_8);
	}
}