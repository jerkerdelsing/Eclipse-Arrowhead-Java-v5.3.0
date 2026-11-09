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
package eu.arrowhead.authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import eu.arrowhead.authentication.http.filter.InternalAuthenticationFilter;
import eu.arrowhead.authentication.mqtt.filter.InternalAuthenticationMqttFilter;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.filter.authentication.IAuthenticationPolicyFilter;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;

@Configuration
public class BeanConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	IAuthenticationPolicyFilter internalAuthenticationPolicyFilter() {
		return new InternalAuthenticationFilter();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	ArrowheadMqttFilter authenticationPolicyMqttFilter(@Value(Constants.$MQTT_API_ENABLED_WD) final boolean isMqttEnabled) {
		if (!isMqttEnabled) {
			return null;
		}

		return new InternalAuthenticationMqttFilter();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(AuthenticationConstants.ENCODER_STRENGTH);
	}
}