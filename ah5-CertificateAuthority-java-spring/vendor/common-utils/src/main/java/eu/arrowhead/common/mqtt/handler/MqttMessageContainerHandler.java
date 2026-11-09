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
package eu.arrowhead.common.mqtt.handler;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import eu.arrowhead.common.mqtt.MqttResourceManager;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;

public class MqttMessageContainerHandler implements Runnable {

	//=================================================================================================
	// members

	@Autowired
	private MqttHandlerUtils utils;

	@Autowired
	private List<ArrowheadMqttFilter> filters;

	private final Logger logger = LogManager.getLogger(getClass());

	private final MqttMessageContainer msgContainer;
	private final MqttTopicHandler topicHandler;
	private final MqttResourceManager resourceManager;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public MqttMessageContainerHandler(final MqttTopicHandler topicHandler, final MqttMessageContainer msgContainer, final MqttResourceManager resourceManager) {
		this.topicHandler = topicHandler;
		this.msgContainer = msgContainer;
		this.resourceManager = resourceManager;
	}

	//-------------------------------------------------------------------------------------------------
	public void run() {
		logger.debug("MqttMessageContainerHandler.run started...");
		Assert.notNull(msgContainer, "msgContainer is null");
		Assert.notNull(topicHandler, "topicHandler is null");
		Assert.notNull(resourceManager, "resourceManager is null");

		final long startTime = System.currentTimeMillis();
		long endTime = 0;
		MqttRequestModel request = null;
		try {
			final Entry<String, MqttRequestModel> parsed = utils.parseMqttMessage(msgContainer);
			request = parsed.getValue();

			// Filter chain, in filter order (authentication first): the injected list is not sorted
			for (final ArrowheadMqttFilter filter : filters.stream().sorted(Comparator.comparingInt(ArrowheadMqttFilter::order)).toList()) {
				filter.doFilter(parsed.getKey(), request);
			}

			// API call
			topicHandler.handle(request);
		} catch (final Exception ex) {
			utils.errorResponse(ex, request);
		} finally {
			endTime = System.currentTimeMillis();
			resourceManager.registerLatency(endTime - startTime);
		}
	}
}