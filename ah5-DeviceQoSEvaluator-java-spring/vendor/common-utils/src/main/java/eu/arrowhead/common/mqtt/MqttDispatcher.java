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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;

@Component
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class MqttDispatcher {

	//=================================================================================================
	//members

	@Autowired
	private List<MqttTopicHandler> handlers;

	private final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = new ConcurrentHashMap<>();
	private final Set<String> fullTopicSet = new HashSet<>();

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	protected void addTopic(final String topic) {
		Assert.isTrue(!Utilities.isEmpty(topic), "topic is empty");

		final String baseTopic = getBaseTopic(topic);
		fullTopicSet.add(topic);

		if (baseTopicQueueMap.containsKey(baseTopic)) {
			return;
		}

		final Optional<MqttTopicHandler> handlerOpt = handlers.stream().filter(h -> h.baseTopic().equals(baseTopic)).findFirst();
		if (handlerOpt.isEmpty()) {
			fullTopicSet.remove(topic);
			throw new IllegalArgumentException("No service handler exists for topic: " + topic);
		}

		baseTopicQueueMap.put(baseTopic, new LinkedBlockingQueue<>());
		handlerOpt.get().init(baseTopicQueueMap.get(baseTopic));
		if (!handlerOpt.get().isAlive()) {
			handlerOpt.get().start();
		}
	}

	//-------------------------------------------------------------------------------------------------
	protected void revokeBaseTopic(final String baseTopic) {
		Assert.isTrue(!Utilities.isEmpty(baseTopic), "baseTopic is empty");

		if (!baseTopicQueueMap.containsKey(baseTopic)) {
			return;
		}

		fullTopicSet.removeIf(fullTopic -> baseTopic.equals(getBaseTopic(fullTopic)));
		baseTopicQueueMap.remove(baseTopic);

		final Optional<MqttTopicHandler> handlerOpt = handlers.stream().filter(h -> h.baseTopic().equals(baseTopic)).findFirst();
		if (handlerOpt.isPresent() && handlerOpt.get().isAlive()) {
			handlerOpt.get().interrupt();
		}
	}

	//-------------------------------------------------------------------------------------------------
	protected void queueMessage(final String topic, final MqttMessage msg) {
		Assert.isTrue(!Utilities.isEmpty(topic), "topic is empty");

		final String baseTopic = getBaseTopic(topic);
		Assert.isTrue(baseTopicQueueMap.containsKey(baseTopic), "unknown base topic");

		baseTopicQueueMap.get(baseTopic).add(new MqttMessageContainer(topic, msg));
	}

	//-------------------------------------------------------------------------------------------------
	protected Set<String> getFullTopicSet() {
		return fullTopicSet;
	}

	//-------------------------------------------------------------------------------------------------
	private String getBaseTopic(final String topic) {
		final int basepathEndIdx = topic.lastIndexOf(MqttMessageContainer.DELIMITER) + 1;
		Assert.isTrue(basepathEndIdx > 0, "Invalid base topic. It can't be empty and must end with " + MqttMessageContainer.DELIMITER);
		return topic.substring(0, basepathEndIdx);
	}
}