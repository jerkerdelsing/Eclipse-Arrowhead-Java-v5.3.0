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
package eu.arrowhead.serviceregistry.api.mqtt.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryPolicy;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class ServiceLookupMqttFilter implements ArrowheadMqttFilter {

	//=================================================================================================
	// members

	@Autowired
	private ServiceRegistrySystemInfo sysInfo;

	//=================================================================================================
	// members

	//-------------------------------------------------------------------------------------------------
	@Override
	public int order() {
		return ServiceRegistryConstants.REQUEST_FILTER_ORDER_SERVICE_LOOKUP;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void doFilter(final String authKey, final MqttRequestModel request) {
		if (ServiceRegistryConstants.MQTT_API_SERVICE_DISCOVERY_BASE_TOPIC.equals(request.getBaseTopic())
				&& Constants.SERVICE_OP_LOOKUP.equals(request.getOperation())) {
			final ServiceDiscoveryPolicy policy = sysInfo.getServiceDiscoveryPolicy();
			if (policy == ServiceDiscoveryPolicy.RESTRICTED) {
				final Boolean isRestricted = !(request.isSysOp() || sysInfo.hasClientDirectAccess(request.getRequester()));
				request.setAttribute(ServiceRegistryConstants.REQUEST_ATTR_RESTRICTED_SERVICE_LOOKUP, isRestricted.toString());
			} else if (policy == ServiceDiscoveryPolicy.OPEN) {
				request.setAttribute(ServiceRegistryConstants.REQUEST_ATTR_RESTRICTED_SERVICE_LOOKUP, "false");
			}
		}
	}
}