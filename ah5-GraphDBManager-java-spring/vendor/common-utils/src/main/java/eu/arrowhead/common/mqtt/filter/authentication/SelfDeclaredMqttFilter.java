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
package eu.arrowhead.common.mqtt.filter.authentication;

import org.springframework.beans.factory.annotation.Autowired;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

public class SelfDeclaredMqttFilter implements ArrowheadMqttFilter {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public int order() {
		return Constants.REQUEST_FILTER_ORDER_AUTHENTICATION;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void doFilter(final String authInfo, final MqttRequestModel request) {
		if (Utilities.isEmpty(authInfo)) {
			throw new AuthException("No authentication info has been provided");
		}

		final String[] split = authInfo.split(Constants.MQTT_AUTH_INFO_DELIMITER);
		if (split.length != 2 || !split[0].equals(Constants.MQTT_AUTH_INFO_PREFIX_SYSTEM)) {
			throw new AuthException("Invalid authentication info");
		}

		final String systemName = systemNameNormalizer.normalize((split[1]));

		request.setRequester(systemName);
		request.setSysOp(Constants.SYSOP.equals(systemName));
	}
}