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

import eu.arrowhead.common.exception.InvalidParameterException;

public enum MqttQoS {
	AT_MOST_ONCE(0), AT_LEAST_ONCE(1), EXACTLY_ONCE(2);

	//=================================================================================================
	// members

	private final int value;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public int value() {
		return value;
	}

	//-------------------------------------------------------------------------------------------------
	public static MqttQoS valueOf(final int value) {
		switch (value) {
		case 0:
			return AT_MOST_ONCE;
		case 1:
			return AT_LEAST_ONCE;

		case 2:
			return EXACTLY_ONCE;

		default:
			throw new InvalidParameterException("Unknown MQTT QoS value:" + value);
		}
	}

	//=================================================================================================
	//  assistant method

	//-------------------------------------------------------------------------------------------------
	private MqttQoS(final int value) {
		this.value = value;
	}
}