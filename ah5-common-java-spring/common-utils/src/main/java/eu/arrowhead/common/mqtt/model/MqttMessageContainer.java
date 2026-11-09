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
package eu.arrowhead.common.mqtt.model;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;

public class MqttMessageContainer {

	//=================================================================================================
	// members

	private final String topic;
	private final MqttMessage message;

	private final String baseTopic;
	private final String operation;

	public static final String DELIMITER = "/";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public MqttMessageContainer(final String topic, final MqttMessage message) {
		Assert.isTrue(!Utilities.isEmpty(topic), "topic is null");
		Assert.notNull(message, "message is null");

		this.topic = topic;
		this.message = message;

		final int basePathEndIdx = topic.lastIndexOf(DELIMITER) + 1;
		this.baseTopic = topic.substring(0, basePathEndIdx);
		this.operation = topic.substring(basePathEndIdx);
	}

	//-------------------------------------------------------------------------------------------------
	public String getTopic() {
		return topic;
	}

	//-------------------------------------------------------------------------------------------------
	public MqttMessage getMessage() {
		return message;
	}

	//-------------------------------------------------------------------------------------------------
	public String getBaseTopic() {
		return baseTopic;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOperation() {
		return operation;
	}
}