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

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class MqttController {

	//=================================================================================================
	// members

	@Autowired
	private MqttService mqttService;

	@Autowired
	private MqttDispatcher mqttDispatcher;

	@Autowired
	private SystemInfo sysInfo;

	private String templateName;

	private MqttClient client = null;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void listen(final ServiceModel model) {
		logger.debug("MqttController.listen started");
		Assert.notNull(model, "ServiceModel is null");

		final Optional<InterfaceModel> optional = model.interfaces().stream().filter(i -> i.templateName().equals(templateName)).findFirst();
		if (optional.isEmpty()) {
			logger.debug(model.serviceDefinition() + " has no MQTT interface.");
			return;
		}

		final MqttInterfaceModel interfaceModel = (MqttInterfaceModel) optional.get();

		try {
			if (client == null) {
				initMqttClient(interfaceModel.accessAddresses().getFirst(), interfaceModel.accessPort());
			}

			for (final String operation : interfaceModel.operations()) {
				final String topic = interfaceModel.baseTopic() + operation;
				mqttDispatcher.addTopic(topic);
				client.subscribe(topic);
				logger.info("Subscribed to service-providing MQTT topic: " + topic);
			}
		} catch (final MqttException ex) {
			logger.debug(ex);
			mqttDispatcher.revokeBaseTopic(interfaceModel.baseTopic());
			throw new ExternalServerError("MQTT service listener creation failed for '" + model.serviceDefinition() + "': " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void disconnect() {
		logger.debug("disconnect started");
		Assert.notNull(client, "client is null");

		try {
			final String[] topics = mqttDispatcher.getFullTopicSet().toArray(new String[0]);
			client.unsubscribe(topics);
			mqttService.disconnect(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID);
		} catch (final MqttException ex) {
			logger.debug(ex);
			throw new ExternalServerError("Disconnecting MQTT Broker failed: " + ex.getMessage());
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	private MqttClient initMqttClient(final String address, final int port) throws MqttException {
		mqttService.connect(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID, address, port, "AH-" + sysInfo.getSystemName(), sysInfo.getSystemName(), sysInfo.getMqttClientPassword());
		client = mqttService.client(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID);
		client.setCallback(createMqttCallback(client.getServerURI()));

		return client;
	}

	//-------------------------------------------------------------------------------------------------
	private MqttCallback createMqttCallback(final String brokerUri) {
		return new MqttCallbackExtended() {

			//-------------------------------------------------------------------------------------------------
			@Override
			public void messageArrived(final String topic, final MqttMessage message) throws Exception {
				logger.debug("MQTT message arrived to service topic: " + topic);
				mqttDispatcher.queueMessage(topic, message);
			}

			//-------------------------------------------------------------------------------------------------
			@Override
			public void deliveryComplete(final IMqttDeliveryToken token) {
				logger.debug("MQTT message delivered to broker " + brokerUri + ". Topic(s): " + String.join(", ", token.getTopics()));
			}

			//-------------------------------------------------------------------------------------------------
			@Override
			public void connectionLost(final Throwable cause) {
				logger.error("MQTT Broker connection lost: " + brokerUri + ". Reason: " + cause.getMessage());
			}

			//-------------------------------------------------------------------------------------------------
			@Override
			public void connectComplete(final boolean reconnect, final String serverURI) {
				if (reconnect) {
					for (final String topic : mqttDispatcher.getFullTopicSet()) {
						try {
							client.subscribe(topic);
							logger.info("Re-subscribed to service-providing MQTT topic: " + topic);
						} catch (final MqttException ex) {
							logger.error("Could not re-subscribe to service-providing MQTT topic: " + topic + ". Reason: " + ex.getMessage());
							logger.debug(ex);
						}
					}
				}
			}
		};
	}
}