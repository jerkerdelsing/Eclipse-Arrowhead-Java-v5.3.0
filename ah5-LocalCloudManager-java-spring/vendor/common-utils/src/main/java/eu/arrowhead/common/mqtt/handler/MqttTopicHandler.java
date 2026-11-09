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

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttResourceManager;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;

public abstract class MqttTopicHandler extends Thread {

	//=================================================================================================
	// members

	@Autowired
	private Function<MqttMessageContainerHandlerContext, MqttMessageContainerHandler> messageHandlerFactory;

	@Autowired
	private MqttHandlerUtils utils;

	@Autowired
	private List<ArrowheadMqttFilter> filters;

	private BlockingQueue<MqttMessageContainer> queue;

	private boolean doWork = false;

	private final MqttResourceManager resourceManager = new MqttResourceManager();

	private ThreadPoolExecutor threadpool = null;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void init(final BlockingQueue<MqttMessageContainer> queue) {
		logger.debug("init started...");

		this.queue = queue;
		this.threadpool = resourceManager.getThreadpool();
		filters.sort((a, b) -> a.order() - b.order());
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run() {
		logger.debug("run started...");
		Assert.isTrue(queue != null, getClass().getName() + " is not initialized");

		doWork = true;
		while (doWork) {
			try {
				final MqttMessageContainer msgContainer = queue.take();

				try {

					threadpool.execute(messageHandlerFactory.apply(new MqttMessageContainerHandlerContext(msgContainer, this, resourceManager)));
				} catch (final RejectedExecutionException ex) {
					Entry<String, MqttRequestModel> parsed = null;
					try {
						parsed = utils.parseMqttMessage(msgContainer);
					} catch (final InvalidParameterException invalidEx) {
						logger.debug(invalidEx);
						utils.errorResponse(ex, null);
						continue;
					}

					utils.errorResponse(ex, parsed.getValue());
				}
			} catch (final InterruptedException ex) {
				logger.debug(ex.getMessage());
				logger.debug(ex);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void interrupt() {
		logger.debug("interrupt started...");

		doWork = false;
		super.interrupt();
	}

	//-------------------------------------------------------------------------------------------------
	public abstract String baseTopic();

	//-------------------------------------------------------------------------------------------------
	public abstract void handle(final MqttRequestModel request) throws ArrowheadException;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	protected void successResponse(final MqttRequestModel request, final MqttStatus status, final Object response) {
		utils.successResponse(request, status, response);
	}

	//-------------------------------------------------------------------------------------------------
	protected <T> T readPayload(final Object payload, final Class<T> dtoClass) {
		return utils.readPayload(payload, dtoClass);
	}

	//-------------------------------------------------------------------------------------------------
	protected <T> T readPayload(final Object payload, final TypeReference<T> dtoTypeRef) {
		return utils.readPayload(payload, dtoTypeRef);
	}
}