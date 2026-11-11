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
package eu.arrowhead.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import eu.arrowhead.common.collector.HttpCollectorDriver;
import eu.arrowhead.common.collector.ICollectorDriver;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.http.filter.NoOpArrowheadFilter;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.filter.authentication.CertificateFilter;
import eu.arrowhead.common.http.filter.authentication.IAuthenticationPolicyFilter;
import eu.arrowhead.common.http.filter.authentication.OutsourcedFilter;
import eu.arrowhead.common.http.filter.authentication.SelfDeclaredFilter;
import eu.arrowhead.common.http.filter.authorization.ManagementServiceFilter;
import eu.arrowhead.common.mqtt.MqttResourceManager;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.filter.authentication.CertificateMqttFilter;
import eu.arrowhead.common.mqtt.filter.authentication.OutsourcedMqttFilter;
import eu.arrowhead.common.mqtt.filter.authentication.SelfDeclaredMqttFilter;
import eu.arrowhead.common.mqtt.filter.authorization.ManagementServiceMqttFilter;
import eu.arrowhead.common.mqtt.handler.MqttMessageContainerHandler;
import eu.arrowhead.common.mqtt.handler.MqttMessageContainerHandlerContext;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;

@Configuration
public class CommonBeanConfig {

	//=================================================================================================
	// methods

	@Autowired
	private ApplicationContext appContext;

	// -------------------------------------------------------------------------------------------------
	@Bean(Constants.ARROWHEAD_CONTEXT)
	Map<String, Object> getArrowheadContext() {
		return new ConcurrentHashMap<>();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	@ConditionalOnExpression("'" + Constants.$AUTHENTICATION_POLICY_WD + "' != '" + AuthenticationPolicy.INTERNAL_VALUE + "'")
	IAuthenticationPolicyFilter authenticationPolicyFilter(@Value(Constants.$AUTHENTICATION_POLICY_WD) final AuthenticationPolicy policy) {
		switch (policy) {
		case CERTIFICATE:
			return new CertificateFilter();
		case OUTSOURCED:
			return new OutsourcedFilter();
		case DECLARED:
			return new SelfDeclaredFilter();
		case INTERNAL:
			throw new IllegalArgumentException("Invalid policy: " + policy.name());
		default:
			throw new IllegalArgumentException("Unknown policy: " + policy.name());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	@ConditionalOnProperty(name = Constants.ENABLE_MANAGEMENT_FILTER, matchIfMissing = false)
	ArrowheadFilter managementServiceFilter() {
		final SystemInfo systemInfo = (SystemInfo) appContext.getBean(Constants.BEAN_NAME_SYSTEM_INFO);
		if (systemInfo.getSystemName().equals(Constants.SYS_NAME_CONSUMER_AUTHORIZATION)) {
			// this system use a special implementation of the management service filter => return a dummy filter that does not do anything (null is not possible here)
			return new NoOpArrowheadFilter();
		}

		return new ManagementServiceFilter();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	@ConditionalOnExpression("'" + Constants.$AUTHENTICATION_POLICY_WD + "' != '" + AuthenticationPolicy.INTERNAL_VALUE + "'")
	ArrowheadMqttFilter authenticationPolicyMqttFilter(@Value(Constants.$MQTT_API_ENABLED_WD) final boolean isMqttEnabled, @Value(Constants.$AUTHENTICATION_POLICY_WD) final AuthenticationPolicy policy) {
		if (!isMqttEnabled) {
			return null;
		}

		switch (policy) {
		case CERTIFICATE:
			return new CertificateMqttFilter();
		case OUTSOURCED:
			return new OutsourcedMqttFilter();
		case DECLARED:
			return new SelfDeclaredMqttFilter();
		case INTERNAL:
			throw new IllegalArgumentException("Invalid policy: " + policy.name());
		default:
			throw new IllegalArgumentException("Unknown policy: " + policy.name());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	@ConditionalOnProperty(name = { Constants.MQTT_API_ENABLED, Constants.ENABLE_MANAGEMENT_FILTER }, havingValue = "true", matchIfMissing = false)
	ArrowheadMqttFilter managementServiceMqttFilter() {
		final SystemInfo systemInfo = (SystemInfo) appContext.getBean(Constants.BEAN_NAME_SYSTEM_INFO);
		if (systemInfo.getSystemName().equals(Constants.SYS_NAME_CONSUMER_AUTHORIZATION)) {
			// this system use a special implementation of the management service filter
			return null;
		}

		return new ManagementServiceMqttFilter();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	ICollectorDriver getDefaultCollectorDriver() {
		return new HttpCollectorDriver();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	MqttMessageContainerHandler createMqttMessageContainerHandler(final MqttTopicHandler topicHandler, final MqttMessageContainer msgContainer, final MqttResourceManager resourceManager) {
		return new MqttMessageContainerHandler(topicHandler, msgContainer, resourceManager);
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	Function<MqttMessageContainerHandlerContext, MqttMessageContainerHandler> mqttMessageContainerHandlerFactory() {
		return context -> createMqttMessageContainerHandler(context.topicHandler(), context.msgContainer(), context.resourceManager());
	}
}